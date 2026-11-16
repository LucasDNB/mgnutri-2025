package ar.edu.usal.mgnutri.entrenamiento.service;

import ar.edu.usal.mgnutri.bitacora.domain.Modulo;
import ar.edu.usal.mgnutri.bitacora.domain.TipoEvento;
import ar.edu.usal.mgnutri.bitacora.service.Auditable;
import ar.edu.usal.mgnutri.catalogo.dao.EjercicioDao;
import ar.edu.usal.mgnutri.catalogo.domain.Ejercicio;
import ar.edu.usal.mgnutri.common.Area;
import ar.edu.usal.mgnutri.common.Fechas;
import ar.edu.usal.mgnutri.common.Textos;
import ar.edu.usal.mgnutri.common.error.BusinessException;
import ar.edu.usal.mgnutri.common.error.ErrorCode;
import ar.edu.usal.mgnutri.common.eventos.Eventos;
import ar.edu.usal.mgnutri.common.security.AccesoPacienteGuard;
import ar.edu.usal.mgnutri.common.security.CurrentUser;
import ar.edu.usal.mgnutri.entrenamiento.dao.ReemplazoEjercicioDao;
import ar.edu.usal.mgnutri.entrenamiento.dao.RegistroEntrenamientoDao;
import ar.edu.usal.mgnutri.entrenamiento.dao.RutinaDao;
import ar.edu.usal.mgnutri.entrenamiento.domain.DiaRutina;
import ar.edu.usal.mgnutri.entrenamiento.domain.EjercicioRutina;
import ar.edu.usal.mgnutri.entrenamiento.domain.ReemplazoEjercicio;
import ar.edu.usal.mgnutri.entrenamiento.domain.Rutina;
import ar.edu.usal.mgnutri.entrenamiento.dto.RutinaDtos.DiaDto;
import ar.edu.usal.mgnutri.entrenamiento.dto.RutinaDtos.DiaResponse;
import ar.edu.usal.mgnutri.entrenamiento.dto.RutinaDtos.EjercicioItemDto;
import ar.edu.usal.mgnutri.entrenamiento.dto.RutinaDtos.EjercicioItemResponse;
import ar.edu.usal.mgnutri.entrenamiento.dto.RutinaDtos.ReemplazoInfo;
import ar.edu.usal.mgnutri.entrenamiento.dto.RutinaDtos.RutinaListItem;
import ar.edu.usal.mgnutri.entrenamiento.dto.RutinaDtos.RutinaRequest;
import ar.edu.usal.mgnutri.entrenamiento.dto.RutinaDtos.RutinaResponse;
import ar.edu.usal.mgnutri.pacientes.dao.PacienteDao;
import ar.edu.usal.mgnutri.profesionales.dao.ProfesionalDao;
import ar.edu.usal.mgnutri.profesionales.domain.Profesional;
import ar.edu.usal.mgnutri.profesionales.service.PlanesActivosConsulta;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Gestión de rutinas (CU-21/22/23, spec 05). */
@Service
public class RutinaService implements PlanesActivosConsulta {

    private final RutinaDao dao;
    private final EjercicioDao ejercicioDao;
    private final ReemplazoEjercicioDao reemplazoDao;
    private final RegistroEntrenamientoDao registroDao;
    private final PacienteDao pacienteDao;
    private final ProfesionalDao profesionalDao;
    private final AccesoPacienteGuard guard;
    private final Clock clock;

    public RutinaService(RutinaDao dao, EjercicioDao ejercicioDao, ReemplazoEjercicioDao reemplazoDao,
                         RegistroEntrenamientoDao registroDao, PacienteDao pacienteDao, ProfesionalDao profesionalDao,
                         AccesoPacienteGuard guard, Clock clock) {
        this.dao = dao;
        this.ejercicioDao = ejercicioDao;
        this.reemplazoDao = reemplazoDao;
        this.registroDao = registroDao;
        this.pacienteDao = pacienteDao;
        this.profesionalDao = profesionalDao;
        this.guard = guard;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<RutinaListItem> listar(Long pacienteId, String estado) {
        guard.assertPuedeVer(pacienteId);
        return dao.findByPacienteIdOrderByFechaInicioDescIdDesc(pacienteId).stream()
                .filter(r -> estado == null || estado.isBlank() || r.getEstado().name().equals(estado))
                .map(r -> new RutinaListItem(r.getId(), r.getNombre(), r.getFechaInicio(), r.getFechaFin(), r.getEstado().name(),
                        profesionalDao.findById(r.getEntrenadorId()).map(Profesional::nombreVisible).orElse(""), r.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public RutinaResponse obtener(Long id) {
        Rutina r = dao.findById(id).orElseThrow(() -> BusinessException.noEncontrado("Rutina"));
        guard.assertPuedeVer(r.getPacienteId());
        return respuesta(r, List.of());
    }

    @Transactional
    @Auditable(modulo = Modulo.ENTRENAMIENTO, tipo = TipoEvento.ALTA, entidad = "Rutina", entidadId = "#result.id()",
            descripcion = "Rutina '#{#req.nombre()}' para el paciente #{#pacienteId}")
    public RutinaResponse crear(Long pacienteId, RutinaRequest req) {
        guard.assertPuedeEditar(pacienteId, Area.ENTRENAMIENTO);
        validar(req);
        Optional<Rutina> actual = dao.findFirstByPacienteIdAndEstado(pacienteId, Rutina.Estado.ACTIVA);
        if (actual.isPresent()) {
            if (!Boolean.TRUE.equals(req.finalizarActual())) {
                throw new BusinessException(ErrorCode.RUTINA_ACTIVA_EXISTENTE, "La rutina actual (desde "
                        + Fechas.fecha(actual.get().getFechaInicio()) + ") se finalizará. ¿Confirmás?")
                        .con("rutinaActualId", actual.get().getId());
            }
            actual.get().setEstado(Rutina.Estado.FINALIZADA);
            dao.saveAndFlush(actual.get());
        }
        Rutina r = new Rutina();
        r.setPacienteId(pacienteId);
        r.setEntrenadorId(CurrentUser.profesionalId());
        encabezado(r, req);
        List<String> adv = new ArrayList<>();
        sincronizar(r, req.dias(), adv);
        dao.saveAndFlush(r);
        return respuesta(r, adv);
    }

    @Transactional
    @Auditable(modulo = Modulo.ENTRENAMIENTO, tipo = TipoEvento.MODIFICACION, entidad = "Rutina", entidadId = "#id",
            descripcion = "Modificación de rutina '#{#req.nombre()}'")
    public RutinaResponse actualizar(Long id, RutinaRequest req) {
        Rutina r = dao.findById(id).orElseThrow(() -> BusinessException.noEncontrado("Rutina"));
        guard.assertPuedeEditar(r.getPacienteId(), Area.ENTRENAMIENTO);
        if (r.getEstado() != Rutina.Estado.ACTIVA) {
            throw new BusinessException(ErrorCode.REGLA_NEGOCIO, "Sólo se puede editar la rutina activa. Podés duplicar una anterior.");
        }
        Textos.verificarVersion(r.getVersion(), req.version());
        validar(req);
        encabezado(r, req);
        List<String> adv = new ArrayList<>();
        sincronizar(r, req.dias(), adv);
        dao.saveAndFlush(r);
        return respuesta(r, adv);
    }

    @Transactional
    @Auditable(modulo = Modulo.ENTRENAMIENTO, tipo = TipoEvento.BAJA, entidad = "Rutina", entidadId = "#id",
            descripcion = "Eliminación de rutina")
    public void eliminar(Long id) {
        Rutina r = dao.findById(id).orElseThrow(() -> BusinessException.noEncontrado("Rutina"));
        guard.assertPuedeEditar(r.getPacienteId(), Area.ENTRENAMIENTO);
        if (r.getEstado() == Rutina.Estado.ELIMINADA) {
            throw new BusinessException(ErrorCode.REGLA_NEGOCIO, "La rutina ya está eliminada.");
        }
        r.setEstado(Rutina.Estado.ELIMINADA);
    }

    /** Duplica una rutina como base: la nueva queda ACTIVA (finaliza la actual si existe). */
    @Transactional
    @Auditable(modulo = Modulo.ENTRENAMIENTO, tipo = TipoEvento.ALTA, entidad = "Rutina", entidadId = "#result.id()",
            descripcion = "Duplicación de rutina #{#id}")
    public RutinaResponse duplicar(Long id) {
        Rutina o = dao.findById(id).orElseThrow(() -> BusinessException.noEncontrado("Rutina"));
        guard.assertPuedeEditar(o.getPacienteId(), Area.ENTRENAMIENTO);
        dao.findFirstByPacienteIdAndEstado(o.getPacienteId(), Rutina.Estado.ACTIVA).ifPresent(a -> {
            a.setEstado(Rutina.Estado.FINALIZADA);
            dao.saveAndFlush(a);
        });
        Rutina r = new Rutina();
        r.setPacienteId(o.getPacienteId());
        r.setEntrenadorId(CurrentUser.profesionalId());
        r.setNombre(o.getNombre() + " (copia)");
        r.setObjetivo(o.getObjetivo());
        r.setObservaciones(o.getObservaciones());
        r.setFechaInicio(LocalDate.now(clock));
        r.setDuracionSemanas(o.getDuracionSemanas());
        r.recalcularFin();
        sincronizar(r, aDtos(o), new ArrayList<>());
        dao.saveAndFlush(r);
        return respuesta(r, List.of());
    }

    @Scheduled(cron = "${mgnutri.jobs.finalizar-vencidos-cron}", zone = "America/Argentina/Buenos_Aires")
    @Transactional
    public int finalizarVencidas() {
        List<Rutina> v = dao.findByEstadoAndFechaFinBefore(Rutina.Estado.ACTIVA, LocalDate.now(clock));
        v.forEach(r -> r.setEstado(Rutina.Estado.FINALIZADA));
        return v.size();
    }

    @EventListener
    @Transactional
    public void alDarDeBajaPaciente(Eventos.PacienteDadoDeBaja e) {
        dao.findFirstByPacienteIdAndEstado(e.pacienteId(), Rutina.Estado.ACTIVA).ifPresent(r -> r.setEstado(Rutina.Estado.FINALIZADA));
    }

    @Override
    @Transactional(readOnly = true)
    public long activosDe(Long profesionalId, Area area) {
        return area == Area.ENTRENAMIENTO ? dao.countByEntrenadorIdAndEstado(profesionalId, Rutina.Estado.ACTIVA) : 0;
    }

    // ------------------------------------------------------------------

    private void validar(RutinaRequest req) {
        if (req.dias().isEmpty()) {
            throw BusinessException.validacion("dias", "La rutina debe tener al menos un día de entrenamiento.");
        }
        Set<Integer> dias = new HashSet<>();
        for (DiaDto d : req.dias()) {
            if (!dias.add(d.diaSemana())) {
                throw BusinessException.validacion("dias", "El día " + nombreDia(d.diaSemana()) + " está repetido.");
            }
            if (d.ejercicios().isEmpty()) {
                throw BusinessException.validacion("dias", "El día " + nombreDia(d.diaSemana()) + " no tiene ejercicios.");
            }
            for (EjercicioItemDto e : d.ejercicios()) {
                Ejercicio ej = ejercicioDao.findById(e.ejercicioId())
                        .orElseThrow(() -> BusinessException.validacion("dias", "Ejercicio inexistente."));
                if (!ej.isActivo() && e.id() == null) {
                    throw BusinessException.validacion("dias", "El ejercicio " + ej.getNombre() + " está dado de baja.");
                }
            }
        }
        if (req.fechaInicio().isBefore(LocalDate.now(clock).minusDays(30))) {
            throw BusinessException.validacion("fechaInicio", "La fecha de inicio no puede ser anterior a 30 días atrás.");
        }
    }

    private void encabezado(Rutina r, RutinaRequest req) {
        r.setNombre(req.nombre().trim());
        r.setObjetivo(Textos.vacioANull(req.objetivo()));
        r.setFechaInicio(req.fechaInicio());
        r.setDuracionSemanas(req.duracionSemanas());
        r.recalcularFin();
        r.setObservaciones(Textos.vacioANull(req.observaciones()));
    }

    /** Merge: días por día de semana, ejercicios por id; los quitados quedan inactivos (CA-05.2.3/4). */
    private void sincronizar(Rutina r, List<DiaDto> dtos, List<String> adv) {
        Map<Integer, DiaRutina> existentes = r.diasActivos().stream().collect(Collectors.toMap(DiaRutina::getDiaSemana, Function.identity()));
        int ordenDia = 1;
        for (DiaDto dto : dtos.stream().sorted(Comparator.comparing(DiaDto::diaSemana)).toList()) {
            DiaRutina dia = existentes.remove(dto.diaSemana());
            if (dia == null) {
                dia = new DiaRutina(r, dto.diaSemana(), Textos.vacioANull(dto.nombre()), ordenDia);
                r.getDias().add(dia);
            }
            dia.setNombre(Textos.vacioANull(dto.nombre()));
            dia.setOrden(ordenDia++);
            Map<Long, EjercicioRutina> previos = dia.ejerciciosActivos().stream().filter(e -> e.getId() != null)
                    .collect(Collectors.toMap(EjercicioRutina::getId, Function.identity()));
            int orden = 1;
            for (EjercicioItemDto e : dto.ejercicios()) {
                EjercicioRutina er = e.id() == null ? null : previos.remove(e.id());
                if (er != null && !er.getEjercicioId().equals(e.ejercicioId())) {
                    desactivar(er, adv);
                    er = null;
                }
                if (er == null) {
                    er = new EjercicioRutina(dia, e.ejercicioId(), orden);
                    dia.getEjercicios().add(er);
                }
                er.setOrden(orden++);
                er.setSeries(e.series());
                er.setRepeticiones(e.repeticiones().trim());
                er.setDescansoSeg(e.descansoSeg());
                er.setCargaSugerida(Textos.vacioANull(e.cargaSugerida()));
                er.setNotas(Textos.vacioANull(e.notas()));
            }
            previos.values().forEach(x -> desactivar(x, adv));
            List<EjercicioRutina> act = dia.ejerciciosActivos();
            for (int i = 1; i < act.size(); i++) {
                if (act.get(i).getEjercicioId().equals(act.get(i - 1).getEjercicioId())) {
                    adv.add("El mismo ejercicio aparece dos veces seguidas el " + nombreDia(dia.getDiaSemana()) + " (RN-05.4).");
                }
            }
        }
        for (DiaRutina d : existentes.values()) {
            d.setActivo(false);
            d.ejerciciosActivos().forEach(x -> desactivar(x, adv));
        }
    }

    private void desactivar(EjercicioRutina er, List<String> adv) {
        er.setActivo(false);
        if (er.getId() != null) {
            reemplazoDao.findByEjercicioRutinaIdAndActivoTrue(er.getId()).ifPresent(rep -> {
                rep.setActivo(false);
                adv.add("Se desactivó una variante elegida por el paciente el " + nombreDia(er.getDia().getDiaSemana()) + ".");
            });
        }
    }

    List<DiaDto> aDtos(Rutina r) {
        return r.diasActivos().stream().map(d -> new DiaDto(d.getDiaSemana(), d.getNombre(), d.ejerciciosActivos().stream()
                .map(e -> new EjercicioItemDto(null, e.getEjercicioId(), e.getSeries(), e.getRepeticiones(), e.getDescansoSeg(),
                        e.getCargaSugerida(), e.getNotas())).toList())).toList();
    }

    public static String nombreDia(int d) {
        return new String[]{"", "lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo"}[d];
    }

    public RutinaResponse respuesta(Rutina r, List<String> adv) {
        Map<Long, Ejercicio> cache = new HashMap<>();
        Function<Long, Ejercicio> ej = id -> cache.computeIfAbsent(id, k -> ejercicioDao.findById(k).orElse(null));
        Map<Long, ReemplazoEjercicio> reemplazos = reemplazoDao.findByPacienteIdAndActivoTrue(r.getPacienteId()).stream()
                .collect(Collectors.toMap(ReemplazoEjercicio::getEjercicioRutinaId, Function.identity(), (a, b) -> b));
        List<DiaResponse> dias = new ArrayList<>();
        for (DiaRutina d : r.diasActivos().stream().sorted(Comparator.comparingInt(DiaRutina::getDiaSemana)).toList()) {
            List<EjercicioItemResponse> items = new ArrayList<>();
            for (EjercicioRutina e : d.ejerciciosActivos()) {
                Ejercicio orig = ej.apply(e.getEjercicioId());
                ReemplazoEjercicio rep = e.getId() == null ? null : reemplazos.get(e.getId());
                Ejercicio mostrado = rep == null ? orig : ej.apply(rep.getEjercicioReemplazoId());
                ReemplazoInfo info = rep == null || mostrado == null ? null
                        : new ReemplazoInfo(mostrado.getId(), mostrado.getNombre(), mostrado.getVideoUrl(), orig == null ? "" : orig.getNombre());
                Long ejId = mostrado == null ? e.getEjercicioId() : mostrado.getId();
                var ultima = registroDao.findFirstByPacienteIdAndEjercicioIdAndCargaKgIsNotNullOrderByFechaDesc(r.getPacienteId(), ejId)
                        .map(x -> x.getCargaKg()).orElse(null);
                items.add(new EjercicioItemResponse(e.getId(), e.getEjercicioId(), orig == null ? "" : orig.getNombre(),
                        orig == null ? "" : orig.getGrupoPrincipal().getNombre(), orig == null ? "" : orig.getDificultad().name(),
                        orig == null ? null : orig.getEquipamiento(), orig == null ? null : orig.getVideoUrl(), e.getOrden(),
                        e.getSeries(), e.getRepeticiones(), e.getDescansoSeg(), e.getCargaSugerida(), e.getNotas(), info, ultima));
            }
            dias.add(new DiaResponse(d.getId(), d.getDiaSemana(), d.getNombre(), items));
        }
        String paciente = pacienteDao.findById(r.getPacienteId()).map(p -> p.nombreCompleto()).orElse("");
        String entrenador = profesionalDao.findById(r.getEntrenadorId()).map(p -> p.nombreVisible() + " (" + p.nombreCompleto() + ")").orElse("");
        return new RutinaResponse(r.getId(), r.getPacienteId(), paciente, r.getEntrenadorId(), entrenador, r.getNombre(), r.getObjetivo(),
                r.getFechaInicio(), r.getDuracionSemanas(), r.getFechaFin(), r.getEstado().name(), r.getObservaciones(), dias,
                r.getCreatedAt(), r.getVersion(), adv.stream().filter(Objects::nonNull).distinct().toList());
    }
}
