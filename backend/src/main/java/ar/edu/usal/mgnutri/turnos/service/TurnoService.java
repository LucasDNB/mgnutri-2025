package ar.edu.usal.mgnutri.turnos.service;

import ar.edu.usal.mgnutri.bitacora.domain.Modulo;
import ar.edu.usal.mgnutri.bitacora.domain.TipoEvento;
import ar.edu.usal.mgnutri.bitacora.service.Auditable;
import ar.edu.usal.mgnutri.bitacora.service.Auditoria;
import ar.edu.usal.mgnutri.bitacora.service.BitacoraService;
import ar.edu.usal.mgnutri.common.Area;
import ar.edu.usal.mgnutri.common.Fechas;
import ar.edu.usal.mgnutri.common.Rol;
import ar.edu.usal.mgnutri.common.Textos;
import ar.edu.usal.mgnutri.common.error.BusinessException;
import ar.edu.usal.mgnutri.common.error.ErrorCode;
import ar.edu.usal.mgnutri.common.eventos.Eventos;
import ar.edu.usal.mgnutri.common.security.CurrentUser;
import ar.edu.usal.mgnutri.common.security.UsuarioPrincipal;
import ar.edu.usal.mgnutri.notificaciones.service.Notificador;
import ar.edu.usal.mgnutri.pacientes.dao.PacienteDao;
import ar.edu.usal.mgnutri.pacientes.dao.PacienteProfesionalDao;
import ar.edu.usal.mgnutri.pacientes.domain.Paciente;
import ar.edu.usal.mgnutri.profesionales.dao.BloqueoAgendaDao;
import ar.edu.usal.mgnutri.profesionales.dao.DisponibilidadDao;
import ar.edu.usal.mgnutri.profesionales.dao.ProfesionalDao;
import ar.edu.usal.mgnutri.profesionales.domain.Disponibilidad;
import ar.edu.usal.mgnutri.profesionales.domain.Profesional;
import ar.edu.usal.mgnutri.turnos.dao.TurnoDao;
import ar.edu.usal.mgnutri.turnos.dao.TurnoHistorialDao;
import ar.edu.usal.mgnutri.turnos.domain.Turno;
import ar.edu.usal.mgnutri.turnos.domain.TurnoHistorial;
import ar.edu.usal.mgnutri.turnos.dto.TurnoDtos.CrearTurnoRequest;
import ar.edu.usal.mgnutri.turnos.dto.TurnoDtos.HistorialResponse;
import ar.edu.usal.mgnutri.turnos.dto.TurnoDtos.HuecoResponse;
import ar.edu.usal.mgnutri.turnos.dto.TurnoDtos.Persona;
import ar.edu.usal.mgnutri.turnos.dto.TurnoDtos.TurnoResponse;
import jakarta.persistence.criteria.Predicate;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Gestión de turnos (spec 03): solicitud, reprogramación, cancelación, calendario. */
@Service
public class TurnoService {

    private final TurnoDao dao;
    private final TurnoHistorialDao historialDao;
    private final PacienteDao pacienteDao;
    private final PacienteProfesionalDao asignaciones;
    private final ProfesionalDao profesionalDao;
    private final DisponibilidadDao disponibilidadDao;
    private final BloqueoAgendaDao bloqueoDao;
    private final ReglasTurno reglas;
    private final Notificador notificador;
    private final BitacoraService bitacora;

    public TurnoService(TurnoDao dao, TurnoHistorialDao historialDao, PacienteDao pacienteDao,
                        PacienteProfesionalDao asignaciones, ProfesionalDao profesionalDao,
                        DisponibilidadDao disponibilidadDao, BloqueoAgendaDao bloqueoDao, ReglasTurno reglas,
                        Notificador notificador, BitacoraService bitacora) {
        this.dao = dao;
        this.historialDao = historialDao;
        this.pacienteDao = pacienteDao;
        this.asignaciones = asignaciones;
        this.profesionalDao = profesionalDao;
        this.disponibilidadDao = disponibilidadDao;
        this.bloqueoDao = bloqueoDao;
        this.reglas = reglas;
        this.notificador = notificador;
        this.bitacora = bitacora;
    }

    // ------------------------------------------------------------------ huecos

    /** Huecos libres de un profesional (máx. 31 días). */
    @Transactional(readOnly = true)
    public List<HuecoResponse> huecos(Long profesionalId, LocalDate desde, LocalDate hasta) {
        Profesional prof = profesionalDao.findById(profesionalId).filter(Profesional::activo)
                .orElseThrow(() -> BusinessException.noEncontrado("Profesional"));
        UsuarioPrincipal u = CurrentUser.get();
        if (u.esProfesional() && !u.esAdmin() && !profesionalId.equals(u.profesionalId())) {
            throw BusinessException.sinPermiso();
        }
        LocalDate hoy = Fechas.fecha(reglas.ahora());
        LocalDate d = desde == null || desde.isBefore(hoy) ? hoy : desde;
        LocalDate h = hasta == null ? d.plusDays(30) : hasta;
        if (h.isAfter(d.plusDays(31))) {
            h = d.plusDays(31);
        }
        Instant i = Fechas.inicioDelDia(d);
        Instant f = Fechas.inicioDelDia(h.plusDays(1));
        return reglas.huecos(disponibilidadDao.findByProfesionalIdAndActivoTrueOrderByDiaSemanaAscHoraDesdeAsc(profesionalId),
                        dao.reservadosDeProfesional(profesionalId, i, f), bloqueoDao.solapados(profesionalId, i, f), d, h,
                        prof.getDuracionTurnoMin())
                .stream().map(x -> new HuecoResponse(x.inicio(), x.fin(), x.ubicacion())).toList();
    }

    // ------------------------------------------------------------------ paciente

    /** CU-07: el paciente solicita un turno; queda confirmado automáticamente. */
    @Transactional
    @Auditable(modulo = Modulo.TURNOS, tipo = TipoEvento.ALTA, entidad = "Turno", entidadId = "#result.id()",
            descripcion = "Turno solicitado por el paciente para #{#result.inicio()}")
    public TurnoResponse solicitar(Long profesionalId, Instant inicio, Area areaPedida) {
        Paciente pac = pacienteActual();
        Profesional prof = profesionalActivo(profesionalId);
        Area area = resolverArea(prof, areaPedida, pac.getId());
        reglas.validarFuturo(inicio);
        if (inicio.isBefore(reglas.ahora().plus(ReglasTurno.MINIMO_HUECO))) {
            throw new BusinessException(ErrorCode.HORARIO_NO_DISPONIBLE, "Los turnos se piden con al menos 2 horas de anticipación.");
        }
        Instant ahora = reglas.ahora();
        reglas.validarVentana7Dias(dao.proximoConProfesional(pac.getId(), prof.getId(), ahora, ahora.plus(ReglasTurno.VENTANA_7_DIAS)));
        Turno t = nuevoTurno(pac, prof, area, inicio, false);
        return guardarYNotificar(t, pac, prof);
    }

    @Transactional(readOnly = true)
    public List<TurnoResponse> misTurnos(String estado, LocalDate desde, LocalDate hasta) {
        Long pacienteId = CurrentUser.pacienteId();
        Instant d = desde != null ? Fechas.inicioDelDia(desde) : reglas.ahora().minus(Duration.ofDays(183));
        Instant h = hasta != null ? Fechas.inicioDelDia(hasta.plusDays(1)) : reglas.ahora().plus(Duration.ofDays(400));
        Specification<Turno> spec = (root, q, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("pacienteId"), pacienteId));
            p.add(cb.greaterThanOrEqualTo(root.get("inicio"), d));
            p.add(cb.lessThan(root.get("inicio"), h));
            if (estado != null && !estado.isBlank()) {
                p.add(cb.equal(root.get("estado"), Turno.Estado.valueOf(estado)));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
        return respuestas(dao.findAll(spec, Sort.by("inicio")));
    }

    /** CU-08: reprogramación por el paciente (mismo profesional, R-T2, R-T3). */
    @Transactional
    @Auditable(modulo = Modulo.TURNOS, tipo = TipoEvento.MODIFICACION, entidad = "Turno", entidadId = "#id",
            descripcion = "Reprogramación de turno por el paciente")
    public TurnoResponse reprogramarPorPaciente(Long id, Instant nuevoInicio, Integer version) {
        Turno t = turnoPropio(id);
        Textos.verificarVersion(t.getVersion(), version);
        reglas.validar24h(t.getInicio());
        if (nuevoInicio.isBefore(reglas.ahora().plus(ReglasTurno.MINIMO_HUECO))) {
            throw new BusinessException(ErrorCode.HORARIO_NO_DISPONIBLE, "Elegí un horario con al menos 2 horas de anticipación.");
        }
        return reprogramar(t, nuevoInicio, null, false);
    }

    /** CU-09: cancelación por el paciente (R-T2). */
    @Transactional
    @Auditable(modulo = Modulo.TURNOS, tipo = TipoEvento.BAJA, entidad = "Turno", entidadId = "#id",
            descripcion = "Cancelación de turno por el paciente")
    public TurnoResponse cancelarPorPaciente(Long id, Integer version) {
        Turno t = turnoPropio(id);
        Textos.verificarVersion(t.getVersion(), version);
        reglas.validar24h(t.getInicio());
        return cancelar(t, "Cancelado por el paciente", true);
    }

    // ------------------------------------------------------------------ staff

    /** RF-03: profesional (a sus pacientes) o admin (a cualquiera) crean turnos sin R-T1/R-T2. */
    @Transactional
    @Auditable(modulo = Modulo.TURNOS, tipo = TipoEvento.ALTA, entidad = "Turno", entidadId = "#result.id()",
            descripcion = "Turno otorgado para #{#result.inicio()}")
    public TurnoResponse crearPorStaff(CrearTurnoRequest req) {
        UsuarioPrincipal u = CurrentUser.get();
        Paciente pac = pacienteDao.findById(req.pacienteId()).filter(Paciente::activo)
                .orElseThrow(() -> new BusinessException(ErrorCode.REGLA_NEGOCIO, "El paciente no existe o está dado de baja."));
        Profesional prof = profesionalActivo(req.profesionalId());
        if (!u.esAdmin()) {
            if (!prof.getId().equals(u.profesionalId())) {
                throw BusinessException.sinPermiso();
            }
            if (!asignaciones.existsByPacienteIdAndProfesionalIdAndActivoTrue(pac.getId(), prof.getId())) {
                throw new BusinessException(ErrorCode.PACIENTE_NO_ASIGNADO, "No tenés asignado a este paciente.");
            }
        }
        boolean forzar = u.esAdmin() && Boolean.TRUE.equals(req.forzar());
        Area area = resolverArea(prof, req.area(), pac.getId());
        reglas.validarFuturo(req.inicio());
        Turno t = nuevoTurno(pac, prof, area, req.inicio(), forzar);
        return guardarYNotificar(t, pac, prof);
    }

    @Transactional
    @Auditable(modulo = Modulo.TURNOS, tipo = TipoEvento.MODIFICACION, entidad = "Turno", entidadId = "#id",
            descripcion = "Reprogramación de turno por el centro: #{#motivo}")
    public TurnoResponse reprogramarPorStaff(Long id, Instant nuevoInicio, String motivo, Integer version) {
        Turno t = turnoDeStaff(id);
        Textos.verificarVersion(t.getVersion(), version);
        exigirMotivo(motivo);
        reglas.validarFuturo(nuevoInicio);
        return reprogramar(t, nuevoInicio, motivo, CurrentUser.get().esAdmin());
    }

    @Transactional
    @Auditable(modulo = Modulo.TURNOS, tipo = TipoEvento.BAJA, entidad = "Turno", entidadId = "#id",
            descripcion = "Cancelación de turno por el centro: #{#motivo}")
    public TurnoResponse cancelarPorStaff(Long id, String motivo, Integer version) {
        Turno t = turnoDeStaff(id);
        Textos.verificarVersion(t.getVersion(), version);
        exigirMotivo(motivo);
        return cancelar(t, motivo, true);
    }

    /** CA-03.4.3: ATENDIDO / AUSENTE sólo después de la hora de inicio. */
    @Transactional
    @Auditable(modulo = Modulo.TURNOS, tipo = TipoEvento.MODIFICACION, entidad = "Turno", entidadId = "#id",
            descripcion = "Turno marcado como #{#estado}")
    public TurnoResponse cambiarEstado(Long id, String estado, Integer version) {
        Turno t = turnoDeStaff(id);
        Textos.verificarVersion(t.getVersion(), version);
        Turno.Estado nuevo;
        try {
            nuevo = Turno.Estado.valueOf(estado);
        } catch (IllegalArgumentException e) {
            throw BusinessException.validacion("estado", "Estado inválido.");
        }
        if (nuevo != Turno.Estado.ATENDIDO && nuevo != Turno.Estado.AUSENTE) {
            throw BusinessException.validacion("estado", "Sólo se puede marcar como atendido o ausente.");
        }
        if (t.getEstado() == Turno.Estado.CANCELADO) {
            throw new BusinessException(ErrorCode.REGLA_NEGOCIO, "El turno está cancelado.");
        }
        if (t.getInicio().isAfter(reglas.ahora())) {
            throw new BusinessException(ErrorCode.REGLA_NEGOCIO, "Un turno no puede marcarse antes de su hora de inicio.");
        }
        t.setEstado(nuevo);
        dao.saveAndFlush(t);
        return respuesta(t);
    }

    @Transactional(readOnly = true)
    public List<TurnoResponse> listar(Long profesionalId, Long pacienteId, Area area, String estado, LocalDate desde,
                                      LocalDate hasta, boolean incluirCancelados) {
        UsuarioPrincipal u = CurrentUser.get();
        Long prof = u.esAdmin() ? profesionalId : u.profesionalId();
        if (!u.esAdmin() && prof == null) {
            throw BusinessException.sinPermiso();
        }
        LocalDate d = desde == null ? Fechas.fecha(reglas.ahora()) : desde;
        LocalDate h = hasta == null ? d.plusDays(31) : hasta;
        if (h.isAfter(d.plusDays(400))) {
            throw BusinessException.validacion("hasta", "El rango máximo es de un año.");
        }
        Specification<Turno> spec = (root, q, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.greaterThanOrEqualTo(root.get("inicio"), Fechas.inicioDelDia(d)));
            p.add(cb.lessThan(root.get("inicio"), Fechas.inicioDelDia(h.plusDays(1))));
            if (prof != null) {
                p.add(cb.equal(root.get("profesionalId"), prof));
            }
            if (pacienteId != null) {
                p.add(cb.equal(root.get("pacienteId"), pacienteId));
            }
            if (area != null) {
                p.add(cb.equal(root.get("area"), area));
            }
            if (estado != null && !estado.isBlank()) {
                p.add(cb.equal(root.get("estado"), Turno.Estado.valueOf(estado)));
            } else if (!incluirCancelados) {
                p.add(cb.notEqual(root.get("estado"), Turno.Estado.CANCELADO));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
        return respuestas(dao.findAll(spec, Sort.by("inicio")));
    }

    @Transactional(readOnly = true)
    public TurnoResponse obtener(Long id) {
        UsuarioPrincipal u = CurrentUser.get();
        Turno t = dao.findById(id).orElseThrow(() -> BusinessException.noEncontrado("Turno"));
        boolean visible = u.esAdmin() || t.getPacienteId().equals(u.pacienteId()) || t.getProfesionalId().equals(u.profesionalId());
        if (!visible) {
            throw BusinessException.noEncontrado("Turno");
        }
        return respuesta(t);
    }

    @Transactional(readOnly = true)
    public List<HistorialResponse> historial(Long id) {
        obtener(id);
        return historialDao.findByTurnoIdOrderByCambiadoAtDesc(id).stream()
                .map(h -> new HistorialResponse(h.getInicioAnterior(), h.getFinAnterior(), h.getMotivo(), h.getCambiadoPor(), h.getCambiadoAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<TurnoResponse> proximoDelPaciente(Long pacienteId) {
        return dao.findFirstByPacienteIdAndEstadoAndInicioAfterOrderByInicioAsc(pacienteId, Turno.Estado.RESERVADO, reglas.ahora())
                .map(this::respuesta);
    }

    // ------------------------------------------------------------------ cancelaciones masivas

    @EventListener
    @Transactional
    public void alDarDeBajaPaciente(Eventos.PacienteDadoDeBaja e) {
        for (Turno t : dao.futurosDePaciente(e.pacienteId(), reglas.ahora())) {
            cancelar(t, "Baja del paciente", e.notificarCancelaciones());
        }
    }

    @EventListener
    @Transactional
    public void alDarDeBajaProfesional(Eventos.ProfesionalDadoDeBaja e) {
        for (Turno t : dao.futurosDeProfesional(e.profesionalId(), reglas.ahora())) {
            cancelar(t, e.motivo() == null ? "El profesional ya no atiende en el centro" : e.motivo(), true);
        }
    }

    /** Turnos reservados afectados por un bloqueo de agenda. */
    @Transactional(readOnly = true)
    public List<Turno> afectadosPorBloqueo(Long profesionalId, Instant desde, Instant hasta) {
        return dao.reservadosDeProfesional(profesionalId, desde, hasta);
    }

    @Transactional
    public int cancelarPorBloqueo(Long profesionalId, Instant desde, Instant hasta, String motivo) {
        List<Turno> lista = dao.reservadosDeProfesional(profesionalId, desde, hasta);
        for (Turno t : lista) {
            cancelar(t, motivo == null || motivo.isBlank() ? "El profesional no atenderá en ese horario" : motivo, true);
            bitacora.ok(Modulo.TURNOS, TipoEvento.BAJA, "Turno", t.getId(), "Cancelación por bloqueo de agenda");
        }
        return lista.size();
    }

    // ------------------------------------------------------------------ internos

    private Turno nuevoTurno(Paciente pac, Profesional prof, Area area, Instant inicio, boolean forzar) {
        Instant fin = inicio.plus(Duration.ofMinutes(prof.getDuracionTurnoMin()));
        List<Disponibilidad> franjas = disponibilidadDao.findByProfesionalIdAndActivoTrueOrderByDiaSemanaAscHoraDesdeAsc(prof.getId());
        Optional<Disponibilidad> franja = reglas.franjaQueContiene(franjas, inicio, fin);
        if (franja.isEmpty() && !forzar) {
            throw new BusinessException(ErrorCode.HORARIO_NO_DISPONIBLE, "El horario elegido está fuera de la agenda del profesional.");
        }
        if (!forzar && reglas.bloqueado(bloqueoDao.solapados(prof.getId(), inicio, fin), inicio, fin)) {
            throw new BusinessException(ErrorCode.HORARIO_NO_DISPONIBLE, "El profesional no atiende en ese horario.");
        }
        reglas.validarSinSolapamiento(dao.reservadosDeProfesional(prof.getId(), inicio, fin), inicio, fin, null,
                "Ese horario ya fue tomado. Elegí otro.");
        reglas.validarSinSolapamiento(dao.reservadosDePaciente(pac.getId(), inicio, fin), inicio, fin, null,
                "El paciente ya tiene otro turno en ese horario.");
        Turno t = new Turno();
        t.setPacienteId(pac.getId());
        t.setProfesionalId(prof.getId());
        t.setArea(area);
        t.setInicio(inicio);
        t.setFin(fin);
        t.setUbicacion(franja.map(Disponibilidad::getUbicacion).orElse(null));
        t.setFueraDeAgenda(franja.isEmpty());
        t.setCreadoPorUsuarioId(CurrentUser.get().id());
        return t;
    }

    private TurnoResponse guardarYNotificar(Turno t, Paciente pac, Profesional prof) {
        try {
            dao.saveAndFlush(t);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.HORARIO_NO_DISPONIBLE, "Ese horario acaba de ser tomado por otra persona. Elegí otro.");
        }
        notificador.turnoConfirmado(pac.getUsuarioId(), pac.getNombre(), pac.getTelefonoWhatsapp(), prof.nombreVisible(),
                t.getInicio(), t.getUbicacion(), t.getId());
        return respuesta(t);
    }

    private TurnoResponse reprogramar(Turno t, Instant nuevoInicio, String motivo, boolean forzar) {
        if (t.getEstado() != Turno.Estado.RESERVADO) {
            throw new BusinessException(ErrorCode.REGLA_NEGOCIO, "Sólo se pueden reprogramar turnos reservados.");
        }
        Profesional prof = profesionalActivo(t.getProfesionalId());
        Paciente pac = pacienteDao.findById(t.getPacienteId()).orElseThrow();
        Instant fin = nuevoInicio.plus(Duration.ofMinutes(prof.getDuracionTurnoMin()));
        Optional<Disponibilidad> franja = reglas.franjaQueContiene(
                disponibilidadDao.findByProfesionalIdAndActivoTrueOrderByDiaSemanaAscHoraDesdeAsc(prof.getId()), nuevoInicio, fin);
        if (franja.isEmpty() && !forzar) {
            throw new BusinessException(ErrorCode.HORARIO_NO_DISPONIBLE, "El horario elegido está fuera de la agenda del profesional.");
        }
        if (!forzar && reglas.bloqueado(bloqueoDao.solapados(prof.getId(), nuevoInicio, fin), nuevoInicio, fin)) {
            throw new BusinessException(ErrorCode.HORARIO_NO_DISPONIBLE, "El profesional no atiende en ese horario.");
        }
        reglas.validarSinSolapamiento(dao.reservadosDeProfesional(prof.getId(), nuevoInicio, fin), nuevoInicio, fin, t.getId(),
                "Ese horario ya fue tomado. Elegí otro.");
        reglas.validarSinSolapamiento(dao.reservadosDePaciente(pac.getId(), nuevoInicio, fin), nuevoInicio, fin, t.getId(),
                "El paciente ya tiene otro turno en ese horario.");
        Instant anterior = t.getInicio();
        historialDao.save(new TurnoHistorial(t.getId(), t.getInicio(), t.getFin(), motivo, CurrentUser.get().username(),
                reglas.ahora().truncatedTo(ChronoUnit.SECONDS)));
        Auditoria.detalle("inicio", Map.of("anterior", Fechas.fechaHora(anterior), "nuevo", Fechas.fechaHora(nuevoInicio)));
        t.setInicio(nuevoInicio);
        t.setFin(fin);
        t.setUbicacion(franja.map(Disponibilidad::getUbicacion).orElse(t.getUbicacion()));
        t.setFueraDeAgenda(franja.isEmpty());
        t.setRecordatorioEnviadoAt(null); // CA-03.2.4
        try {
            dao.saveAndFlush(t);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.HORARIO_NO_DISPONIBLE, "Ese horario acaba de ser tomado por otra persona. Elegí otro.");
        }
        notificador.turnoReprogramado(pac.getUsuarioId(), pac.getNombre(), pac.getTelefonoWhatsapp(), prof.nombreVisible(),
                anterior, nuevoInicio, t.getUbicacion(), t.getId());
        return respuesta(t);
    }

    private TurnoResponse cancelar(Turno t, String motivo, boolean notificar) {
        if (t.getEstado() != Turno.Estado.RESERVADO) {
            throw new BusinessException(ErrorCode.REGLA_NEGOCIO, "Sólo se pueden cancelar turnos reservados.");
        }
        UsuarioPrincipal u = CurrentUser.opcional().orElse(null);
        t.setEstado(Turno.Estado.CANCELADO);
        t.setMotivoCancelacion(motivo);
        t.setCanceladoPorUsuarioId(u == null ? null : u.id());
        t.setCanceladoPorRol(u == null ? "SISTEMA" : u.esPaciente() ? Rol.PACIENTE.name() : u.esAdmin() ? Rol.ADMIN.name() : "PROFESIONAL");
        dao.saveAndFlush(t);
        if (notificar) {
            Paciente pac = pacienteDao.findById(t.getPacienteId()).orElseThrow();
            Profesional prof = profesionalDao.findById(t.getProfesionalId()).orElseThrow();
            notificador.turnoCancelado(pac.getUsuarioId(), pac.getNombre(), pac.getTelefonoWhatsapp(), prof.nombreVisible(),
                    t.getInicio(), motivo, t.getId());
        }
        return respuesta(t);
    }

    private Area resolverArea(Profesional prof, Area pedida, Long pacienteId) {
        if (pedida != null) {
            if (!prof.getTipo().atiende(pedida)) {
                throw BusinessException.validacion("area", prof.nombreVisible() + " no atiende esa área.");
            }
            return pedida;
        }
        return switch (prof.getTipo()) {
            case NUTRICIONISTA -> Area.NUTRICION;
            case ENTRENADOR -> Area.ENTRENAMIENTO;
            case INTEGRAL -> asignaciones.existsByPacienteIdAndProfesionalIdAndAreaAndActivoTrue(pacienteId, prof.getId(), Area.NUTRICION)
                    ? Area.NUTRICION : Area.ENTRENAMIENTO;
        };
    }

    private Paciente pacienteActual() {
        return pacienteDao.findById(CurrentUser.pacienteId()).filter(Paciente::activo)
                .orElseThrow(() -> BusinessException.sinPermiso());
    }

    private Profesional profesionalActivo(Long id) {
        return profesionalDao.findById(id).filter(Profesional::activo)
                .orElseThrow(() -> new BusinessException(ErrorCode.REGLA_NEGOCIO, "El profesional no existe o está dado de baja."));
    }

    private Turno turnoPropio(Long id) {
        Long pacienteId = CurrentUser.pacienteId();
        return dao.findById(id).filter(t -> t.getPacienteId().equals(pacienteId))
                .orElseThrow(() -> BusinessException.noEncontrado("Turno"));
    }

    private Turno turnoDeStaff(Long id) {
        UsuarioPrincipal u = CurrentUser.get();
        Turno t = dao.findById(id).orElseThrow(() -> BusinessException.noEncontrado("Turno"));
        if (!u.esAdmin() && !t.getProfesionalId().equals(u.profesionalId())) {
            throw BusinessException.noEncontrado("Turno");
        }
        return t;
    }

    private static void exigirMotivo(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw BusinessException.validacion("motivo", "Indicá el motivo.");
        }
    }

    // ------------------------------------------------------------------ mapeo

    TurnoResponse respuesta(Turno t) {
        return respuestas(List.of(t)).get(0);
    }

    List<TurnoResponse> respuestas(List<Turno> turnos) {
        Set<Long> pacIds = turnos.stream().map(Turno::getPacienteId).collect(Collectors.toSet());
        Set<Long> profIds = turnos.stream().map(Turno::getProfesionalId).collect(Collectors.toSet());
        Map<Long, Paciente> pacs = new HashMap<>(pacienteDao.findAllById(pacIds).stream().collect(Collectors.toMap(Paciente::getId, Function.identity())));
        Map<Long, Profesional> profs = profesionalDao.findAllById(profIds).stream().collect(Collectors.toMap(Profesional::getId, Function.identity()));
        UsuarioPrincipal u = CurrentUser.opcional().orElse(null);
        Instant ahora = reglas.ahora();
        return turnos.stream().sorted(Comparator.comparing(Turno::getInicio)).map(t -> {
            Paciente p = pacs.get(t.getPacienteId());
            Profesional pr = profs.get(t.getProfesionalId());
            boolean reservadoFuturo = t.getEstado() == Turno.Estado.RESERVADO && t.getInicio().isAfter(ahora);
            boolean puede = reservadoFuturo && (u == null || !u.esPaciente() || !reglas.dentroDe24h(t.getInicio()));
            boolean verTelefono = u != null && !u.esPaciente();
            return new TurnoResponse(t.getId(), t.getInicio(), t.getFin(), t.getEstado().name(), t.getArea(), t.getUbicacion(),
                    new Persona(t.getPacienteId(), p == null ? "" : p.nombreCompleto() + (p.activo() ? "" : " (baja)"),
                            verTelefono && p != null ? p.getTelefonoWhatsapp() : null),
                    new Persona(t.getProfesionalId(), pr == null ? "" : pr.nombreVisible(), null),
                    puede, t.getMotivoCancelacion(), t.getCanceladoPorRol(), t.isFueraDeAgenda(),
                    !t.getInicio().isAfter(ahora), t.getVersion());
        }).toList();
    }
}
