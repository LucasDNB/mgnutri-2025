package ar.edu.usal.mgnutri.nutricion.service;

import ar.edu.usal.mgnutri.bitacora.domain.Modulo;
import ar.edu.usal.mgnutri.bitacora.domain.TipoEvento;
import ar.edu.usal.mgnutri.bitacora.service.Auditable;
import ar.edu.usal.mgnutri.bitacora.service.BitacoraService;
import ar.edu.usal.mgnutri.catalogo.dao.AlimentoDao;
import ar.edu.usal.mgnutri.catalogo.domain.Alimento;
import ar.edu.usal.mgnutri.catalogo.service.AlimentoService;
import ar.edu.usal.mgnutri.common.Textos;
import ar.edu.usal.mgnutri.common.error.BusinessException;
import ar.edu.usal.mgnutri.common.error.ErrorCode;
import ar.edu.usal.mgnutri.common.security.AccesoPacienteGuard;
import ar.edu.usal.mgnutri.common.security.CurrentUser;
import ar.edu.usal.mgnutri.nutricion.dao.ComidaPlanDao;
import ar.edu.usal.mgnutri.nutricion.dao.ItemComidaDao;
import ar.edu.usal.mgnutri.nutricion.dao.ObservacionDiariaDao;
import ar.edu.usal.mgnutri.nutricion.dao.PlanAlimentarioDao;
import ar.edu.usal.mgnutri.nutricion.dao.ReemplazoAlimentoDao;
import ar.edu.usal.mgnutri.nutricion.dao.RegistroComidaDao;
import ar.edu.usal.mgnutri.nutricion.domain.ComidaPlan;
import ar.edu.usal.mgnutri.nutricion.domain.ItemComida;
import ar.edu.usal.mgnutri.nutricion.domain.ObservacionDiaria;
import ar.edu.usal.mgnutri.nutricion.domain.PlanAlimentario;
import ar.edu.usal.mgnutri.nutricion.domain.ReemplazoAlimento;
import ar.edu.usal.mgnutri.nutricion.domain.RegistroComida;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.Alternativa;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.AlternativasResponse;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.ObservacionRequest;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.ObservacionResponse;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.PlanResponse;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.ReemplazoAplicado;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.ReemplazoInfo;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.RegistroComidaRequest;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.RegistroComidaResponse;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.RegistrosNutricion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/** Consulta del plan y actividad nutricional del paciente (spec 08). */
@Service
public class ActividadNutricionalService {

    private final PlanAlimentarioDao planDao;
    private final ComidaPlanDao comidaDao;
    private final ItemComidaDao itemDao;
    private final RegistroComidaDao registroDao;
    private final ObservacionDiariaDao observacionDao;
    private final ReemplazoAlimentoDao reemplazoDao;
    private final AlimentoDao alimentoDao;
    private final AlimentoService alimentos;
    private final EquivalenciaService equivalencias;
    private final PlanAlimentarioService planes;
    private final PlanAlimentarioPdf pdf;
    private final AccesoPacienteGuard guard;
    private final BitacoraService bitacora;
    private final Clock clock;

    public ActividadNutricionalService(PlanAlimentarioDao planDao, ComidaPlanDao comidaDao, ItemComidaDao itemDao,
                                       RegistroComidaDao registroDao, ObservacionDiariaDao observacionDao,
                                       ReemplazoAlimentoDao reemplazoDao, AlimentoDao alimentoDao, AlimentoService alimentos,
                                       EquivalenciaService equivalencias, PlanAlimentarioService planes, PlanAlimentarioPdf pdf,
                                       AccesoPacienteGuard guard, BitacoraService bitacora, Clock clock) {
        this.planDao = planDao;
        this.comidaDao = comidaDao;
        this.itemDao = itemDao;
        this.registroDao = registroDao;
        this.observacionDao = observacionDao;
        this.reemplazoDao = reemplazoDao;
        this.alimentoDao = alimentoDao;
        this.alimentos = alimentos;
        this.equivalencias = equivalencias;
        this.planes = planes;
        this.pdf = pdf;
        this.guard = guard;
        this.bitacora = bitacora;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Optional<PlanAlimentario> planActivo(Long pacienteId) {
        return planDao.findFirstByPacienteIdAndEstado(pacienteId, PlanAlimentario.Estado.ACTIVO);
    }

    /** CU-15: plan activo del paciente con reemplazos aplicados. */
    @Transactional(readOnly = true)
    public PlanResponse miPlan() {
        PlanAlimentario p = planActivo(CurrentUser.pacienteId()).orElseThrow(() ->
                new BusinessException(ErrorCode.SIN_PLAN_ASIGNADO, "Todavía no tenés un plan alimentario asignado"));
        return planes.respuesta(p, true, List.of());
    }

    /** CU-16: PDF del plan activo (paciente o profesional). */
    @Transactional(readOnly = true)
    public byte[] pdfDe(Long planId) {
        PlanAlimentario p = planDao.findById(planId).orElseThrow(() -> BusinessException.noEncontrado("Plan"));
        guard.assertPuedeVer(p.getPacienteId());
        byte[] r = pdf.generar(planes.respuesta(p, true, List.of()));
        bitacora.ok(Modulo.NUTRICION, TipoEvento.DESCARGA, "PlanAlimentario", planId, "Descarga de PDF del plan alimentario");
        return r;
    }

    @Transactional(readOnly = true)
    public byte[] miPdf() {
        PlanAlimentario p = planActivo(CurrentUser.pacienteId()).orElseThrow(() ->
                new BusinessException(ErrorCode.SIN_PLAN_ASIGNADO, "No tenés un plan alimentario asignado"));
        byte[] r = pdf.generar(planes.respuesta(p, true, List.of()));
        bitacora.ok(Modulo.NUTRICION, TipoEvento.DESCARGA, "PlanAlimentario", p.getId(), "El paciente descargó su plan alimentario");
        return r;
    }

    // ---------------------------------------------------------------- registros (CU-17)

    @Transactional(readOnly = true)
    public List<RegistroComidaResponse> misRegistros(LocalDate desde, LocalDate hasta) {
        LocalDate hoy = LocalDate.now(clock);
        return registroDao.findByPacienteIdAndFechaBetweenOrderByFechaAsc(CurrentUser.pacienteId(),
                        desde == null ? hoy.minusDays(6) : desde, hasta == null ? hoy : hasta).stream()
                .map(ActividadNutricionalService::aDto).toList();
    }

    /** Upsert; fecha entre hoy−2 y hoy; la comida debe ser del plan activo (RN-08.1). */
    @Transactional
    public RegistroComidaResponse registrar(RegistroComidaRequest req) {
        Long pacienteId = CurrentUser.pacienteId();
        validarFechaRegistro(req.fecha());
        if ((req.realizada() ? 1 : 0) + (req.conCambios() ? 1 : 0) + (req.omitida() ? 1 : 0) > 1) {
            throw new BusinessException(ErrorCode.REGLA_NEGOCIO, "Elegí una sola opción: como estaba propuesta, con cambios o no completada.");
        }
        ComidaPlan c = comidaDao.findById(req.comidaPlanId()).orElseThrow(() -> BusinessException.noEncontrado("Comida"));
        PlanAlimentario plan = c.getPlan();
        if (!plan.getPacienteId().equals(pacienteId) || plan.getEstado() != PlanAlimentario.Estado.ACTIVO || !c.isActivo()) {
            throw new BusinessException(ErrorCode.SIN_PLAN_ASIGNADO, "Sólo podés registrar comidas de tu plan activo.");
        }
        RegistroComida r = registroDao.findByPacienteIdAndComidaPlanIdAndFecha(pacienteId, c.getId(), req.fecha())
                .orElseGet(() -> new RegistroComida(pacienteId, c.getId(), req.fecha(), clock.instant().truncatedTo(ChronoUnit.SECONDS)));
        r.setRealizada(req.realizada());
        r.setConCambios(req.conCambios());
        r.setOmitida(req.omitida());
        r.setNoLeGusto(req.noLeGusto());
        r.setLeCayoMal(req.leCayoMal());
        registroDao.save(r);
        return aDto(r);
    }

    void validarFechaRegistro(LocalDate fecha) {
        LocalDate hoy = LocalDate.now(clock);
        if (fecha.isAfter(hoy) || fecha.isBefore(hoy.minusDays(2))) {
            throw new BusinessException(ErrorCode.REGLA_NEGOCIO, "Podés registrar el día de hoy y hasta 2 días anteriores.");
        }
    }

    // ---------------------------------------------------------------- observaciones (CU-43)

    @Transactional(readOnly = true)
    public List<ObservacionResponse> misObservaciones(LocalDate desde, LocalDate hasta) {
        LocalDate hoy = LocalDate.now(clock);
        return observacionDao.findByPacienteIdAndFechaBetweenOrderByFechaAsc(CurrentUser.pacienteId(),
                desde == null ? hoy.minusDays(30) : desde, hasta == null ? hoy : hasta).stream().map(ActividadNutricionalService::aDto).toList();
    }

    @Transactional
    public ObservacionResponse guardarObservacion(ObservacionRequest req) {
        Long pacienteId = CurrentUser.pacienteId();
        LocalDate hoy = LocalDate.now(clock);
        if (req.fecha().isAfter(hoy) || req.fecha().isBefore(hoy.minusDays(2))) {
            throw new BusinessException(ErrorCode.REGLA_NEGOCIO, "Podés registrar el día de hoy y hasta 2 días anteriores.");
        }
        ObservacionDiaria o = observacionDao.findByPacienteIdAndFecha(pacienteId, req.fecha())
                .orElseGet(() -> new ObservacionDiaria(pacienteId, req.fecha()));
        o.setNivelHambre(req.nivelHambre());
        o.setNivelSaciedad(req.nivelSaciedad());
        o.setEstadoAnimo(req.estadoAnimo());
        o.setComentario(Textos.vacioANull(req.comentario()));
        observacionDao.save(o);
        return aDto(o);
    }

    // ---------------------------------------------------------------- reemplazos (CU-18)

    @Transactional(readOnly = true)
    public AlternativasResponse alternativas(Long itemId) {
        ItemComida item = itemDelPacienteActual(itemId);
        Alimento original = alimentoDao.findById(item.getAlimentoId()).orElseThrow();
        List<Alternativa> lista = equivalencias.alternativas(original, item.getGramos().doubleValue(), alimentos.activos());
        return new AlternativasResponse(new ReemplazoInfo(original.getId(), original.getNombre(), item.getGramos(), item.getCantidadCasera()),
                lista, lista.isEmpty() ? "No hay alternativas cargadas para este alimento. Consultá con tu nutricionista." : null);
    }

    @Transactional
    @Auditable(modulo = Modulo.NUTRICION, tipo = TipoEvento.MODIFICACION, entidad = "ReemplazoAlimento", entidadId = "#itemId",
            descripcion = "El paciente reemplazó un alimento de su plan")
    public PlanResponse reemplazar(Long itemId, Long alimentoId) {
        ItemComida item = itemDelPacienteActual(itemId);
        Alternativa elegida = alternativas(itemId).alternativas().stream().filter(a -> a.alimentoId().equals(alimentoId)).findFirst()
                .orElseThrow(() -> BusinessException.validacion("alimentoId", "Ese alimento no es una alternativa válida."));
        reemplazoDao.findByItemComidaIdAndActivoTrue(itemId).ifPresent(r -> r.setActivo(false));
        reemplazoDao.flush();
        reemplazoDao.save(new ReemplazoAlimento(itemId, alimentoId, elegida.gramos(), elegida.cantidadCasera(),
                CurrentUser.pacienteId(), LocalDate.now(clock)));
        return planes.respuesta(item.getComida().getPlan(), true, List.of());
    }

    @Transactional
    @Auditable(modulo = Modulo.NUTRICION, tipo = TipoEvento.MODIFICACION, entidad = "ReemplazoAlimento", entidadId = "#itemId",
            descripcion = "El paciente volvió al alimento original")
    public PlanResponse volverAlOriginal(Long itemId) {
        ItemComida item = itemDelPacienteActual(itemId);
        reemplazoDao.findByItemComidaIdAndActivoTrue(itemId).ifPresent(r -> r.setActivo(false));
        return planes.respuesta(item.getComida().getPlan(), true, List.of());
    }

    private ItemComida itemDelPacienteActual(Long itemId) {
        Long pacienteId = CurrentUser.pacienteId();
        ItemComida item = itemDao.findById(itemId).orElseThrow(() -> BusinessException.noEncontrado("Ítem"));
        PlanAlimentario plan = item.getComida().getPlan();
        if (!plan.getPacienteId().equals(pacienteId)) {
            throw BusinessException.noEncontrado("Ítem");
        }
        if (plan.getEstado() != PlanAlimentario.Estado.ACTIVO || !item.isActivo()) {
            throw new BusinessException(ErrorCode.SIN_PLAN_ASIGNADO, "El alimento no pertenece a tu plan activo.");
        }
        return item;
    }

    // ---------------------------------------------------------------- vista del nutricionista (US-08.6)

    @Transactional(readOnly = true)
    public RegistrosNutricion registrosDePaciente(Long pacienteId, LocalDate desde, LocalDate hasta) {
        guard.assertPuedeVer(pacienteId);
        LocalDate hoy = LocalDate.now(clock);
        LocalDate d = desde == null ? hoy.minusDays(27) : desde;
        LocalDate h = hasta == null ? hoy : hasta;
        List<RegistroComidaResponse> regs = registroDao.findByPacienteIdAndFechaBetweenOrderByFechaAsc(pacienteId, d, h).stream()
                .map(ActividadNutricionalService::aDto).toList();
        List<ObservacionResponse> obs = observacionDao.findByPacienteIdAndFechaBetweenOrderByFechaAsc(pacienteId, d, h).stream()
                .map(ActividadNutricionalService::aDto).toList();
        List<ReemplazoAplicado> reemplazos = reemplazoDao.findByPacienteIdAndActivoTrue(pacienteId).stream().map(r -> {
            ItemComida i = itemDao.findById(r.getItemComidaId()).orElse(null);
            String orig = i == null ? "" : alimentoDao.findById(i.getAlimentoId()).map(Alimento::getNombre).orElse("");
            String nuevo = alimentoDao.findById(r.getAlimentoReemplazoId()).map(Alimento::getNombre).orElse("");
            return new ReemplazoAplicado(r.getItemComidaId(), orig, nuevo, r.getGramos(), r.getFechaDesde(),
                    i == null ? 0 : i.getComida().getDiaSemana(), i == null ? "" : i.getComida().getTipo().etiqueta());
        }).toList();
        double cumplimiento = 0;
        Optional<PlanAlimentario> plan = planActivo(pacienteId);
        if (plan.isPresent()) {
            long programadas = 0;
            for (LocalDate f = d; !f.isAfter(h); f = f.plusDays(1)) {
                if (f.isBefore(plan.get().getFechaInicio())) {
                    continue;
                }
                int dia = f.getDayOfWeek().getValue();
                programadas += plan.get().comidasActivas().stream().filter(c -> c.getDiaSemana() == dia).count();
            }
            long hechas = regs.stream().filter(RegistroComidaResponse::realizada).count();
            cumplimiento = programadas == 0 ? 0 : Math.round(hechas * 1000.0 / programadas) / 10.0;
        }
        return new RegistrosNutricion(regs, obs, reemplazos, cumplimiento);
    }

    static RegistroComidaResponse aDto(RegistroComida r) {
        return new RegistroComidaResponse(r.getComidaPlanId(), r.getFecha(), r.isRealizada(), r.isConCambios(), r.isNoLeGusto(), r.isLeCayoMal(), r.isOmitida());
    }

    static ObservacionResponse aDto(ObservacionDiaria o) {
        return new ObservacionResponse(o.getId(), o.getFecha(), o.getNivelHambre(), o.getNivelSaciedad(), o.getEstadoAnimo(), o.getComentario());
    }
}
