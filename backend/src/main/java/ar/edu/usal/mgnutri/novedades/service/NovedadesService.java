package ar.edu.usal.mgnutri.novedades.service;

import ar.edu.usal.mgnutri.catalogo.dao.EjercicioDao;
import ar.edu.usal.mgnutri.common.Area;
import ar.edu.usal.mgnutri.common.Rol;
import ar.edu.usal.mgnutri.common.Textos;
import ar.edu.usal.mgnutri.common.error.BusinessException;
import ar.edu.usal.mgnutri.common.error.ErrorCode;
import ar.edu.usal.mgnutri.common.security.AccesoPacienteGuard;
import ar.edu.usal.mgnutri.common.security.CurrentUser;
import ar.edu.usal.mgnutri.common.security.UsuarioPrincipal;
import ar.edu.usal.mgnutri.configuracion.dto.ConfiguracionDtos.ContactoProfesional;
import ar.edu.usal.mgnutri.configuracion.dto.ConfiguracionDtos.MisContactos;
import ar.edu.usal.mgnutri.configuracion.service.ConfiguracionService;
import ar.edu.usal.mgnutri.entrenamiento.dao.EjercicioRutinaDao;
import ar.edu.usal.mgnutri.entrenamiento.domain.EjercicioRutina;
import ar.edu.usal.mgnutri.entrenamiento.domain.Rutina;
import ar.edu.usal.mgnutri.entrenamiento.service.ActividadFisicaService;
import ar.edu.usal.mgnutri.notificaciones.domain.Alerta;
import ar.edu.usal.mgnutri.notificaciones.service.AlertaService;
import ar.edu.usal.mgnutri.novedades.dao.ComentarioDao;
import ar.edu.usal.mgnutri.novedades.dao.MolestiaDao;
import ar.edu.usal.mgnutri.novedades.domain.Comentario;
import ar.edu.usal.mgnutri.novedades.domain.Molestia;
import ar.edu.usal.mgnutri.novedades.dto.NovedadesDtos.ComentarioRequest;
import ar.edu.usal.mgnutri.novedades.dto.NovedadesDtos.ComentarioResponse;
import ar.edu.usal.mgnutri.novedades.dto.NovedadesDtos.HomePaciente;
import ar.edu.usal.mgnutri.novedades.dto.NovedadesDtos.MolestiaRequest;
import ar.edu.usal.mgnutri.novedades.dto.NovedadesDtos.MolestiaResponse;
import ar.edu.usal.mgnutri.novedades.dto.NovedadesDtos.Novedad;
import ar.edu.usal.mgnutri.nutricion.dao.ComidaPlanDao;
import ar.edu.usal.mgnutri.nutricion.domain.ComidaPlan;
import ar.edu.usal.mgnutri.nutricion.domain.PlanAlimentario;
import ar.edu.usal.mgnutri.nutricion.service.ActividadNutricionalService;
import ar.edu.usal.mgnutri.pacientes.dao.PacienteDao;
import ar.edu.usal.mgnutri.pacientes.dao.PacienteProfesionalDao;
import ar.edu.usal.mgnutri.pacientes.domain.Paciente;
import ar.edu.usal.mgnutri.pacientes.domain.PacienteProfesional;
import ar.edu.usal.mgnutri.profesionales.dao.DisponibilidadDao;
import ar.edu.usal.mgnutri.profesionales.dao.ProfesionalDao;
import ar.edu.usal.mgnutri.profesionales.domain.Profesional;
import ar.edu.usal.mgnutri.turnos.service.TurnoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Comentarios (CU-40/41), molestias (CU-42), bandeja de novedades del profesional (D-23)
 * y home/contacto del paciente (RF-12).
 */
@Service
public class NovedadesService {

    private final ComentarioDao comentarioDao;
    private final MolestiaDao molestiaDao;
    private final ComidaPlanDao comidaDao;
    private final EjercicioRutinaDao erDao;
    private final EjercicioDao ejercicioDao;
    private final PacienteDao pacienteDao;
    private final PacienteProfesionalDao asignaciones;
    private final ProfesionalDao profesionalDao;
    private final DisponibilidadDao disponibilidadDao;
    private final ActividadNutricionalService nutricion;
    private final ActividadFisicaService entrenamiento;
    private final TurnoService turnos;
    private final ConfiguracionService config;
    private final AlertaService alertas;
    private final AccesoPacienteGuard guard;
    private final Clock clock;

    public NovedadesService(ComentarioDao comentarioDao, MolestiaDao molestiaDao, ComidaPlanDao comidaDao, EjercicioRutinaDao erDao,
                            EjercicioDao ejercicioDao, PacienteDao pacienteDao, PacienteProfesionalDao asignaciones,
                            ProfesionalDao profesionalDao, DisponibilidadDao disponibilidadDao, ActividadNutricionalService nutricion,
                            ActividadFisicaService entrenamiento, TurnoService turnos, ConfiguracionService config,
                            AlertaService alertas, AccesoPacienteGuard guard, Clock clock) {
        this.comentarioDao = comentarioDao;
        this.molestiaDao = molestiaDao;
        this.comidaDao = comidaDao;
        this.erDao = erDao;
        this.ejercicioDao = ejercicioDao;
        this.pacienteDao = pacienteDao;
        this.asignaciones = asignaciones;
        this.profesionalDao = profesionalDao;
        this.disponibilidadDao = disponibilidadDao;
        this.nutricion = nutricion;
        this.entrenamiento = entrenamiento;
        this.turnos = turnos;
        this.config = config;
        this.alertas = alertas;
        this.guard = guard;
        this.clock = clock;
    }

    // ---------------------------------------------------------------- paciente

    @Transactional
    public ComentarioResponse comentar(ComentarioRequest req) {
        Long pacienteId = CurrentUser.pacienteId();
        Comentario c = new Comentario();
        c.setPacienteId(pacienteId);
        c.setFecha(req.fecha() == null ? LocalDate.now(clock) : req.fecha());
        c.setTexto(req.texto().trim());
        c.setCreatedAt(ahora());
        if ("PLAN".equalsIgnoreCase(req.tipo())) {
            PlanAlimentario plan = nutricion.planActivo(pacienteId).orElseThrow(() ->
                    new BusinessException(ErrorCode.SIN_PLAN_ASIGNADO, "Todavía no tenés un plan alimentario asignado"));
            c.setTipo(Comentario.Tipo.PLAN);
            c.setPlanId(plan.getId());
            if (req.comidaPlanId() != null) {
                ComidaPlan cp = comidaDao.findById(req.comidaPlanId()).filter(x -> x.getPlan().getId().equals(plan.getId()))
                        .orElseThrow(() -> BusinessException.validacion("comidaPlanId", "La comida no pertenece a tu plan."));
                c.setComidaPlanId(cp.getId());
            }
        } else if ("RUTINA".equalsIgnoreCase(req.tipo())) {
            Rutina r = entrenamiento.rutinaActiva(pacienteId).orElseThrow(() ->
                    new BusinessException(ErrorCode.SIN_RUTINA_ASIGNADA, "Todavía no tenés una rutina asignada"));
            c.setTipo(Comentario.Tipo.RUTINA);
            c.setRutinaId(r.getId());
            if (req.ejercicioRutinaId() != null) {
                c.setEjercicioRutinaId(entrenamiento.ejercicioRutinaPropio(req.ejercicioRutinaId()).getId());
            }
        } else {
            throw BusinessException.validacion("tipo", "Tipo de comentario inválido.");
        }
        comentarioDao.save(c);
        return aDto(c, null);
    }

    @Transactional(readOnly = true)
    public List<ComentarioResponse> misComentarios() {
        return comentarioDao.findByPacienteIdOrderByCreatedAtDesc(CurrentUser.pacienteId()).stream().map(c -> aDto(c, null)).toList();
    }

    /** CA-09.6.4: avisa al profesional del área (ambos para GENERAL), prioritaria si intensidad ≥ 7. */
    @Transactional
    public MolestiaResponse registrarMolestia(MolestiaRequest req) {
        Long pacienteId = CurrentUser.pacienteId();
        Molestia.Contexto ctx;
        try {
            ctx = Molestia.Contexto.valueOf(req.contexto());
        } catch (IllegalArgumentException e) {
            throw BusinessException.validacion("contexto", "Contexto inválido.");
        }
        Molestia m = new Molestia();
        m.setPacienteId(pacienteId);
        LocalDate hoy = LocalDate.now(clock);
        m.setFecha(req.fecha() == null || req.fecha().isAfter(hoy) ? hoy : req.fecha());
        m.setDescripcion(req.descripcion().trim());
        m.setIntensidad(req.intensidad());
        m.setUbicacionCorporal(Textos.vacioANull(req.ubicacionCorporal()));
        m.setContexto(ctx);
        if (req.ejercicioRutinaId() != null) {
            m.setEjercicioRutinaId(entrenamiento.ejercicioRutinaPropio(req.ejercicioRutinaId()).getId());
        }
        m.setCreatedAt(ahora());
        molestiaDao.save(m);
        Paciente p = pacienteDao.findById(pacienteId).orElseThrow();
        Set<Area> areas = switch (ctx) {
            case ENTRENAMIENTO -> EnumSet.of(Area.ENTRENAMIENTO);
            case ALIMENTACION -> EnumSet.of(Area.NUTRICION);
            case GENERAL -> EnumSet.allOf(Area.class);
        };
        boolean prioritaria = req.intensidad() >= 7;
        Set<Long> avisados = new java.util.HashSet<>();
        for (PacienteProfesional a : asignaciones.findByPacienteIdAndActivoTrue(pacienteId)) {
            if (areas.contains(a.getArea())) {
                profesionalDao.findById(a.getProfesionalId()).filter(pr -> avisados.add(pr.getUsuarioId())).ifPresent(pr ->
                        alertas.paraUsuario(pr.getUsuarioId(), Alerta.Tipo.MOLESTIA, p.nombreCompleto() + " reportó una molestia (intensidad "
                                + req.intensidad() + "/10): " + m.getDescripcion(), prioritaria, "Molestia", m.getId()));
            }
        }
        return aDto(m, p.nombreCompleto());
    }

    @Transactional(readOnly = true)
    public List<MolestiaResponse> misMolestias() {
        return molestiaDao.findByPacienteIdOrderByCreatedAtDesc(CurrentUser.pacienteId()).stream().map(m -> aDto(m, null)).toList();
    }

    @Transactional(readOnly = true)
    public HomePaciente home() {
        Long pacienteId = CurrentUser.pacienteId();
        Paciente p = pacienteDao.findById(pacienteId).orElseThrow();
        boolean agendaE = false;
        boolean agendaN = false;
        for (PacienteProfesional a : asignaciones.findByPacienteIdAndActivoTrue(pacienteId)) {
            boolean ag = disponibilidadDao.existsByProfesionalIdAndActivoTrue(a.getProfesionalId());
            if (a.getArea() == Area.ENTRENAMIENTO) {
                agendaE |= ag;
            } else {
                agendaN |= ag;
            }
        }
        return new HomePaciente(p.getNombre(), turnos.proximoDelPaciente(pacienteId).orElse(null),
                nutricion.planActivo(pacienteId).isPresent(), entrenamiento.rutinaActiva(pacienteId).isPresent(),
                contactos(), agendaE, agendaN);
    }

    /** RF-12: central y profesionales asignados con enlace a WhatsApp (CA-08.0.2/3). */
    @Transactional(readOnly = true)
    public MisContactos contactos() {
        Long pacienteId = CurrentUser.pacienteId();
        Map<Long, List<Area>> areas = asignaciones.findByPacienteIdAndActivoTrue(pacienteId).stream()
                .collect(Collectors.groupingBy(PacienteProfesional::getProfesionalId, Collectors.mapping(PacienteProfesional::getArea, Collectors.toList())));
        List<ContactoProfesional> profs = new ArrayList<>();
        for (Profesional pr : profesionalDao.findAllById(areas.keySet())) {
            String dig = Textos.digitos(pr.getTelefonoWhatsapp());
            profs.add(new ContactoProfesional(pr.getId(), pr.nombreVisible() + " (" + pr.nombreCompleto() + ")", pr.getEspecialidad(),
                    areas.get(pr.getId()).stream().map(Area::etiqueta).collect(Collectors.joining(" y ")),
                    dig == null || dig.isEmpty() ? null : "https://wa.me/" + dig + "?text=Hola%2C%20te%20escribo%20desde%20MG%20Nutrici%C3%B3n"));
        }
        return new MisContactos(config.contactoCentral(), profs);
    }

    // ---------------------------------------------------------------- profesional

    @Transactional(readOnly = true)
    public List<Novedad> novedades(boolean incluirLeidas) {
        UsuarioPrincipal u = CurrentUser.get();
        Set<Long> pacientes = guard.pacientesVisibles();
        Set<Comentario.Tipo> tipos = EnumSet.noneOf(Comentario.Tipo.class);
        if (u.esAdmin() || u.tiene(Rol.NUTRICIONISTA)) {
            tipos.add(Comentario.Tipo.PLAN);
        }
        if (u.esAdmin() || u.tiene(Rol.ENTRENADOR)) {
            tipos.add(Comentario.Tipo.RUTINA);
        }
        List<Long> ids = pacientes == null ? pacienteDao.findAll().stream().map(Paciente::getId).toList() : List.copyOf(pacientes);
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<Long, String> nombres = new HashMap<>();
        pacienteDao.findAllById(ids).forEach(p -> nombres.put(p.getId(), p.nombreCompleto()));
        List<Novedad> r = new ArrayList<>();
        if (!tipos.isEmpty()) {
            comentarioDao.findByPacienteIdInAndTipoInOrderByCreatedAtDesc(ids, tipos).stream()
                    .filter(c -> incluirLeidas || c.getLeidoAt() == null).limit(200)
                    .forEach(c -> r.add(new Novedad("COMENTARIO", c.getId(), c.getPacienteId(), nombres.get(c.getPacienteId()), c.getTexto(),
                            referencia(c), false, c.getCreatedAt(), c.getLeidoAt() != null)));
        }
        molestiaDao.findByPacienteIdInOrderByCreatedAtDesc(ids).stream()
                .filter(m -> incluirLeidas || m.getRevisadaAt() == null).limit(200)
                .forEach(m -> r.add(new Novedad("MOLESTIA", m.getId(), m.getPacienteId(), nombres.get(m.getPacienteId()), m.getDescripcion(),
                        "Intensidad " + m.getIntensidad() + "/10" + (m.getUbicacionCorporal() == null ? "" : " · " + m.getUbicacionCorporal())
                                + " · " + m.getContexto().name().toLowerCase(), m.getIntensidad() >= 7, m.getCreatedAt(), m.getRevisadaAt() != null)));
        r.sort(Comparator.comparing(Novedad::leida).thenComparing(Novedad::prioritaria, Comparator.reverseOrder())
                .thenComparing(Novedad::fecha, Comparator.reverseOrder()));
        return r;
    }

    @Transactional
    public void marcarLeida(String tipo, Long id) {
        String quien = CurrentUser.get().username();
        if ("COMENTARIO".equalsIgnoreCase(tipo)) {
            Comentario c = comentarioDao.findById(id).orElseThrow(() -> BusinessException.noEncontrado("Comentario"));
            guard.assertPuedeVer(c.getPacienteId());
            c.setLeidoAt(ahora());
            c.setLeidoPor(quien);
        } else {
            Molestia m = molestiaDao.findById(id).orElseThrow(() -> BusinessException.noEncontrado("Molestia"));
            guard.assertPuedeVer(m.getPacienteId());
            m.setRevisadaAt(ahora());
            m.setRevisadaPor(quien);
        }
    }

    @Transactional(readOnly = true)
    public List<ComentarioResponse> comentariosDe(Long pacienteId, LocalDate desde, LocalDate hasta) {
        guard.assertPuedeVer(pacienteId);
        LocalDate hoy = LocalDate.now(clock);
        return comentarioDao.findByPacienteIdAndFechaBetweenOrderByCreatedAtDesc(pacienteId, desde == null ? hoy.minusYears(1) : desde,
                hasta == null ? hoy : hasta).stream().map(c -> aDto(c, null)).toList();
    }

    @Transactional(readOnly = true)
    public List<MolestiaResponse> molestiasDe(Long pacienteId, LocalDate desde, LocalDate hasta) {
        guard.assertPuedeVer(pacienteId);
        LocalDate hoy = LocalDate.now(clock);
        return molestiaDao.findByPacienteIdAndFechaBetweenOrderByCreatedAtDesc(pacienteId, desde == null ? hoy.minusYears(1) : desde,
                hasta == null ? hoy : hasta).stream().map(m -> aDto(m, null)).toList();
    }

    @Transactional(readOnly = true)
    public long noLeidas() {
        return novedades(false).size();
    }

    private String referencia(Comentario c) {
        if (c.getComidaPlanId() != null) {
            return comidaDao.findById(c.getComidaPlanId()).map(cp -> cp.getTipo().etiqueta() + " · día " + cp.getDiaSemana()).orElse("Plan");
        }
        if (c.getEjercicioRutinaId() != null) {
            return erDao.findById(c.getEjercicioRutinaId()).flatMap(e -> ejercicioDao.findById(e.getEjercicioId()))
                    .map(e -> "Ejercicio " + e.getNombre()).orElse("Rutina");
        }
        return c.getTipo() == Comentario.Tipo.PLAN ? "Plan alimentario" : "Rutina";
    }

    private ComentarioResponse aDto(Comentario c, String paciente) {
        return new ComentarioResponse(c.getId(), c.getPacienteId(), paciente, c.getTipo().name(), referencia(c), c.getFecha(), c.getTexto(),
                c.getCreatedAt(), c.getLeidoAt());
    }

    private MolestiaResponse aDto(Molestia m, String paciente) {
        String ej = m.getEjercicioRutinaId() == null ? null : erDao.findById(m.getEjercicioRutinaId())
                .map(EjercicioRutina::getEjercicioId).flatMap(ejercicioDao::findById).map(e -> e.getNombre()).orElse(null);
        return new MolestiaResponse(m.getId(), m.getPacienteId(), paciente, m.getFecha(), m.getDescripcion(), m.getIntensidad(),
                m.getUbicacionCorporal(), m.getContexto().name(), ej, m.getIntensidad() >= 7, m.getCreatedAt(), m.getRevisadaAt());
    }

    private Instant ahora() {
        return clock.instant().truncatedTo(ChronoUnit.SECONDS);
    }
}
