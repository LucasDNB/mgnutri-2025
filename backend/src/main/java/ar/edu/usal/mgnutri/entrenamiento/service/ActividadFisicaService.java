package ar.edu.usal.mgnutri.entrenamiento.service;

import ar.edu.usal.mgnutri.bitacora.domain.Modulo;
import ar.edu.usal.mgnutri.bitacora.domain.TipoEvento;
import ar.edu.usal.mgnutri.bitacora.service.Auditable;
import ar.edu.usal.mgnutri.bitacora.service.BitacoraService;
import ar.edu.usal.mgnutri.catalogo.dao.EjercicioDao;
import ar.edu.usal.mgnutri.catalogo.domain.Ejercicio;
import ar.edu.usal.mgnutri.catalogo.service.EjercicioService;
import ar.edu.usal.mgnutri.common.error.BusinessException;
import ar.edu.usal.mgnutri.common.error.ErrorCode;
import ar.edu.usal.mgnutri.common.security.AccesoPacienteGuard;
import ar.edu.usal.mgnutri.common.security.CurrentUser;
import ar.edu.usal.mgnutri.entrenamiento.dao.EjercicioRutinaDao;
import ar.edu.usal.mgnutri.entrenamiento.dao.ReemplazoEjercicioDao;
import ar.edu.usal.mgnutri.entrenamiento.dao.RegistroEntrenamientoDao;
import ar.edu.usal.mgnutri.entrenamiento.dao.RutinaDao;
import ar.edu.usal.mgnutri.entrenamiento.domain.DiaRutina;
import ar.edu.usal.mgnutri.entrenamiento.domain.EjercicioRutina;
import ar.edu.usal.mgnutri.entrenamiento.domain.ReemplazoEjercicio;
import ar.edu.usal.mgnutri.entrenamiento.domain.RegistroEntrenamiento;
import ar.edu.usal.mgnutri.entrenamiento.domain.Rutina;
import ar.edu.usal.mgnutri.entrenamiento.dto.RutinaDtos.AlternativaEjercicio;
import ar.edu.usal.mgnutri.entrenamiento.dto.RutinaDtos.AlternativasEjercicioResponse;
import ar.edu.usal.mgnutri.entrenamiento.dto.RutinaDtos.DiaCalendario;
import ar.edu.usal.mgnutri.entrenamiento.dto.RutinaDtos.RegistroRequest;
import ar.edu.usal.mgnutri.entrenamiento.dto.RutinaDtos.RegistroResponse;
import ar.edu.usal.mgnutri.entrenamiento.dto.RutinaDtos.RegistrosEntrenamiento;
import ar.edu.usal.mgnutri.entrenamiento.dto.RutinaDtos.ReemplazoAplicado;
import ar.edu.usal.mgnutri.entrenamiento.dto.RutinaDtos.RutinaResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** Consulta de la rutina y actividad física del paciente (spec 09). */
@Service
public class ActividadFisicaService {

    private final RutinaDao rutinaDao;
    private final EjercicioRutinaDao erDao;
    private final RegistroEntrenamientoDao registroDao;
    private final ReemplazoEjercicioDao reemplazoDao;
    private final EjercicioDao ejercicioDao;
    private final EjercicioService ejercicios;
    private final RutinaService rutinas;
    private final RutinaPdf pdf;
    private final AccesoPacienteGuard guard;
    private final BitacoraService bitacora;
    private final Clock clock;

    public ActividadFisicaService(RutinaDao rutinaDao, EjercicioRutinaDao erDao, RegistroEntrenamientoDao registroDao,
                                  ReemplazoEjercicioDao reemplazoDao, EjercicioDao ejercicioDao, EjercicioService ejercicios,
                                  RutinaService rutinas, RutinaPdf pdf, AccesoPacienteGuard guard, BitacoraService bitacora,
                                  Clock clock) {
        this.rutinaDao = rutinaDao;
        this.erDao = erDao;
        this.registroDao = registroDao;
        this.reemplazoDao = reemplazoDao;
        this.ejercicioDao = ejercicioDao;
        this.ejercicios = ejercicios;
        this.rutinas = rutinas;
        this.pdf = pdf;
        this.guard = guard;
        this.bitacora = bitacora;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Optional<Rutina> rutinaActiva(Long pacienteId) {
        return rutinaDao.findFirstByPacienteIdAndEstado(pacienteId, Rutina.Estado.ACTIVA);
    }

    private Rutina miRutinaEntidad() {
        return rutinaActiva(CurrentUser.pacienteId()).orElseThrow(() ->
                new BusinessException(ErrorCode.SIN_RUTINA_ASIGNADA, "Todavía no tenés una rutina asignada"));
    }

    @Transactional(readOnly = true)
    public RutinaResponse miRutina() {
        return rutinas.respuesta(miRutinaEntidad(), List.of());
    }

    @Transactional(readOnly = true)
    public byte[] miPdf() {
        Rutina r = miRutinaEntidad();
        byte[] b = pdf.generar(rutinas.respuesta(r, List.of()));
        bitacora.ok(Modulo.ENTRENAMIENTO, TipoEvento.DESCARGA, "Rutina", r.getId(), "El paciente descargó su rutina");
        return b;
    }

    @Transactional(readOnly = true)
    public byte[] pdfDe(Long rutinaId) {
        Rutina r = rutinaDao.findById(rutinaId).orElseThrow(() -> BusinessException.noEncontrado("Rutina"));
        guard.assertPuedeVer(r.getPacienteId());
        byte[] b = pdf.generar(rutinas.respuesta(r, List.of()));
        bitacora.ok(Modulo.ENTRENAMIENTO, TipoEvento.DESCARGA, "Rutina", r.getId(), "Descarga de PDF de rutina");
        return b;
    }

    /** Vista mes (CA-09.1.2): días programados y cumplimiento. */
    @Transactional(readOnly = true)
    public List<DiaCalendario> calendario(YearMonth mes) {
        Rutina r = miRutinaEntidad();
        Map<Integer, DiaRutina> porDia = r.diasActivos().stream().collect(Collectors.toMap(DiaRutina::getDiaSemana, d -> d, (a, b) -> a));
        List<RegistroEntrenamiento> regs = registroDao.findByPacienteIdAndFechaBetweenOrderByFechaAsc(r.getPacienteId(),
                mes.atDay(1), mes.atEndOfMonth());
        List<DiaCalendario> res = new ArrayList<>();
        for (LocalDate f = mes.atDay(1); !f.isAfter(mes.atEndOfMonth()); f = f.plusDays(1)) {
            DiaRutina d = porDia.get(f.getDayOfWeek().getValue());
            boolean programado = d != null && !f.isBefore(r.getFechaInicio()) && !f.isAfter(r.getFechaFin());
            int total = programado ? d.ejerciciosActivos().size() : 0;
            LocalDate ff = f;
            int hechos = programado ? (int) regs.stream().filter(x -> x.getFecha().equals(ff) && x.isCompletado()
                    && d.ejerciciosActivos().stream().anyMatch(e -> e.getId().equals(x.getEjercicioRutinaId()))).count() : 0;
            res.add(new DiaCalendario(f, programado, programado ? d.getNombre() : null, total, hechos, programado && total > 0 && hechos >= total));
        }
        return res;
    }

    @Transactional(readOnly = true)
    public List<RegistroResponse> misRegistros(LocalDate desde, LocalDate hasta) {
        LocalDate hoy = LocalDate.now(clock);
        return registroDao.findByPacienteIdAndFechaBetweenOrderByFechaAsc(CurrentUser.pacienteId(),
                desde == null ? hoy.minusDays(6) : desde, hasta == null ? hoy : hasta).stream().map(x -> aDto(x, false)).toList();
    }

    /** CU-25: upsert de registro; hoy y hasta 2 días antes; sólo ejercicios activos de la rutina activa (RN-09.1). */
    @Transactional
    public RegistroResponse registrar(RegistroRequest req) {
        Long pacienteId = CurrentUser.pacienteId();
        LocalDate hoy = LocalDate.now(clock);
        if (req.fecha().isAfter(hoy) || req.fecha().isBefore(hoy.minusDays(2))) {
            throw new BusinessException(ErrorCode.REGLA_NEGOCIO, "Podés registrar el día de hoy y hasta 2 días anteriores.");
        }
        if (req.cargaKg() != null && req.cargaKg().remainder(new java.math.BigDecimal("0.5")).signum() != 0) {
            throw BusinessException.validacion("cargaKg", "La carga se registra con precisión de 0,5 kg.");
        }
        EjercicioRutina er = ejercicioRutinaPropio(req.ejercicioRutinaId());
        Long ejercicioId = reemplazoDao.findByEjercicioRutinaIdAndActivoTrue(er.getId())
                .map(ReemplazoEjercicio::getEjercicioReemplazoId).orElse(er.getEjercicioId());
        RegistroEntrenamiento r = registroDao.findByPacienteIdAndEjercicioRutinaIdAndFecha(pacienteId, er.getId(), req.fecha())
                .orElseGet(() -> new RegistroEntrenamiento(pacienteId, er.getId(), ejercicioId, req.fecha(),
                        clock.instant().truncatedTo(ChronoUnit.SECONDS)));
        r.setEjercicioId(ejercicioId);
        r.setCompletado(req.completado());
        r.setCargaKg(req.cargaKg());
        r.setSeriesRealizadas(req.seriesRealizadas());
        r.setRepeticionesRealizadas(req.repeticionesRealizadas());
        registroDao.saveAndFlush(r);
        DiaRutina dia = er.getDia();
        List<RegistroEntrenamiento> delDia = registroDao.findByPacienteIdAndFechaBetweenOrderByFechaAsc(pacienteId, req.fecha(), req.fecha());
        boolean completa = dia.ejerciciosActivos().stream().allMatch(e -> delDia.stream()
                .anyMatch(x -> x.getEjercicioRutinaId().equals(e.getId()) && x.isCompletado()));
        return aDto(r, completa);
    }

    @Transactional(readOnly = true)
    public AlternativasEjercicioResponse alternativas(Long ejercicioRutinaId) {
        EjercicioRutina er = ejercicioRutinaPropio(ejercicioRutinaId);
        Ejercicio actual = ejercicioDao.findById(er.getEjercicioId()).orElseThrow();
        List<AlternativaEjercicio> lista = ejercicios.alternativas(actual, 10).stream()
                .map(e -> new AlternativaEjercicio(e.getId(), e.getNombre(), e.getGrupoPrincipal().getNombre(), e.getDificultad().name(),
                        e.getEquipamiento(), e.getVideoUrl())).toList();
        return new AlternativasEjercicioResponse(actual.getNombre(), lista,
                lista.isEmpty() ? "No hay variantes cargadas para este grupo muscular" : null);
    }

    @Transactional
    @Auditable(modulo = Modulo.ENTRENAMIENTO, tipo = TipoEvento.MODIFICACION, entidad = "ReemplazoEjercicio",
            entidadId = "#ejercicioRutinaId", descripcion = "El paciente eligió una variante de ejercicio")
    public RutinaResponse reemplazar(Long ejercicioRutinaId, Long ejercicioId) {
        EjercicioRutina er = ejercicioRutinaPropio(ejercicioRutinaId);
        boolean valida = alternativas(ejercicioRutinaId).alternativas().stream().anyMatch(a -> a.ejercicioId().equals(ejercicioId));
        if (!valida) {
            throw BusinessException.validacion("ejercicioId", "Ese ejercicio no es una variante válida.");
        }
        reemplazoDao.findByEjercicioRutinaIdAndActivoTrue(er.getId()).ifPresent(r -> r.setActivo(false));
        reemplazoDao.flush();
        reemplazoDao.save(new ReemplazoEjercicio(er.getId(), ejercicioId, CurrentUser.pacienteId(), LocalDate.now(clock)));
        reemplazoDao.flush();
        return rutinas.respuesta(er.getDia().getRutina(), List.of());
    }

    @Transactional
    @Auditable(modulo = Modulo.ENTRENAMIENTO, tipo = TipoEvento.MODIFICACION, entidad = "ReemplazoEjercicio",
            entidadId = "#ejercicioRutinaId", descripcion = "El paciente volvió al ejercicio original")
    public RutinaResponse volverAlOriginal(Long ejercicioRutinaId) {
        EjercicioRutina er = ejercicioRutinaPropio(ejercicioRutinaId);
        reemplazoDao.findByEjercicioRutinaIdAndActivoTrue(er.getId()).ifPresent(r -> r.setActivo(false));
        reemplazoDao.flush();
        return rutinas.respuesta(er.getDia().getRutina(), List.of());
    }

    public EjercicioRutina ejercicioRutinaPropio(Long id) {
        Long pacienteId = CurrentUser.pacienteId();
        EjercicioRutina er = erDao.findById(id).orElseThrow(() -> BusinessException.noEncontrado("Ejercicio"));
        Rutina r = er.getDia().getRutina();
        if (!r.getPacienteId().equals(pacienteId)) {
            throw BusinessException.noEncontrado("Ejercicio");
        }
        if (r.getEstado() != Rutina.Estado.ACTIVA || !er.isActivo() || !er.getDia().isActivo()) {
            throw new BusinessException(ErrorCode.SIN_RUTINA_ASIGNADA, "El ejercicio no pertenece a tu rutina activa.");
        }
        return er;
    }

    /** US-09.7: vista del entrenador. */
    @Transactional(readOnly = true)
    public RegistrosEntrenamiento registrosDePaciente(Long pacienteId, LocalDate desde, LocalDate hasta) {
        guard.assertPuedeVer(pacienteId);
        LocalDate hoy = LocalDate.now(clock);
        LocalDate d = desde == null ? hoy.minusDays(27) : desde;
        LocalDate h = hasta == null ? hoy : hasta;
        List<RegistroResponse> regs = registroDao.findByPacienteIdAndFechaBetweenOrderByFechaAsc(pacienteId, d, h).stream()
                .map(x -> aDto(x, false)).toList();
        List<ReemplazoAplicado> reps = reemplazoDao.findByPacienteIdAndActivoTrue(pacienteId).stream().map(r -> {
            String orig = erDao.findById(r.getEjercicioRutinaId()).flatMap(e -> ejercicioDao.findById(e.getEjercicioId()))
                    .map(Ejercicio::getNombre).orElse("");
            String var = ejercicioDao.findById(r.getEjercicioReemplazoId()).map(Ejercicio::getNombre).orElse("");
            return new ReemplazoAplicado(r.getEjercicioRutinaId(), orig, var, r.getFecha());
        }).toList();
        return new RegistrosEntrenamiento(regs, reps, cumplimiento(pacienteId, d, h));
    }

    /** % de ejercicios completados sobre programados en el período (también usado por el reporte R-4). */
    @Transactional(readOnly = true)
    public double cumplimiento(Long pacienteId, LocalDate d, LocalDate h) {
        Optional<Rutina> r = rutinaActiva(pacienteId);
        if (r.isEmpty()) {
            return 0;
        }
        long programados = 0;
        for (LocalDate f = d; !f.isAfter(h); f = f.plusDays(1)) {
            if (f.isBefore(r.get().getFechaInicio())) {
                continue;
            }
            int dia = f.getDayOfWeek().getValue();
            programados += r.get().diasActivos().stream().filter(x -> x.getDiaSemana() == dia).mapToLong(x -> x.ejerciciosActivos().size()).sum();
        }
        long hechos = registroDao.findByPacienteIdAndFechaBetweenOrderByFechaAsc(pacienteId, d, h).stream().filter(RegistroEntrenamiento::isCompletado).count();
        return programados == 0 ? 0 : Math.min(100, Math.round(hechos * 1000.0 / programados) / 10.0);
    }

    private RegistroResponse aDto(RegistroEntrenamiento r, boolean completa) {
        String nombre = ejercicioDao.findById(r.getEjercicioId()).map(Ejercicio::getNombre).orElse("");
        return new RegistroResponse(r.getEjercicioRutinaId(), r.getEjercicioId(), nombre, r.getFecha(), r.isCompletado(), r.getCargaKg(),
                r.getSeriesRealizadas(), r.getRepeticionesRealizadas(), completa);
    }
}
