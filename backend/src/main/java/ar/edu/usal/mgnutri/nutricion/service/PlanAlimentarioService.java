package ar.edu.usal.mgnutri.nutricion.service;

import ar.edu.usal.mgnutri.bitacora.domain.Modulo;
import ar.edu.usal.mgnutri.bitacora.domain.TipoEvento;
import ar.edu.usal.mgnutri.bitacora.service.Auditable;
import ar.edu.usal.mgnutri.bitacora.service.Auditoria;
import ar.edu.usal.mgnutri.catalogo.dao.AlimentoDao;
import ar.edu.usal.mgnutri.catalogo.domain.Alimento;
import ar.edu.usal.mgnutri.common.Area;
import ar.edu.usal.mgnutri.common.Textos;
import ar.edu.usal.mgnutri.common.error.BusinessException;
import ar.edu.usal.mgnutri.common.error.ErrorCode;
import ar.edu.usal.mgnutri.common.eventos.Eventos;
import ar.edu.usal.mgnutri.common.security.AccesoPacienteGuard;
import ar.edu.usal.mgnutri.common.security.CurrentUser;
import ar.edu.usal.mgnutri.common.security.UsuarioPrincipal;
import ar.edu.usal.mgnutri.evolucion.dao.MedicionAntropometricaDao;
import ar.edu.usal.mgnutri.nutricion.dao.PlanAlimentarioDao;
import ar.edu.usal.mgnutri.nutricion.dao.ReemplazoAlimentoDao;
import ar.edu.usal.mgnutri.nutricion.domain.ComidaPlan;
import ar.edu.usal.mgnutri.nutricion.domain.ItemComida;
import ar.edu.usal.mgnutri.nutricion.domain.PlanAlimentario;
import ar.edu.usal.mgnutri.nutricion.domain.ReemplazoAlimento;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.ComidaDto;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.ComidaResponse;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.ItemDto;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.ItemResponse;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.Macros;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.PacienteResumen;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.PlanListItem;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.PlanRequest;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.PlanResponse;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.ProfesionalResumen;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.ReemplazoInfo;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.TotalDia;
import ar.edu.usal.mgnutri.pacientes.dao.PacienteDao;
import ar.edu.usal.mgnutri.pacientes.domain.Paciente;
import ar.edu.usal.mgnutri.profesionales.dao.ProfesionalDao;
import ar.edu.usal.mgnutri.profesionales.domain.Profesional;
import ar.edu.usal.mgnutri.profesionales.service.PlanesActivosConsulta;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Gestión de planes alimentarios (CU-11/12/13, spec 04). */
@Service
public class PlanAlimentarioService implements PlanesActivosConsulta {

    private final PlanAlimentarioDao dao;
    private final ReemplazoAlimentoDao reemplazoDao;
    private final AlimentoDao alimentoDao;
    private final PacienteDao pacienteDao;
    private final ProfesionalDao profesionalDao;
    private final MedicionAntropometricaDao medicionDao;
    private final AccesoPacienteGuard guard;
    private final Clock clock;

    public PlanAlimentarioService(PlanAlimentarioDao dao, ReemplazoAlimentoDao reemplazoDao, AlimentoDao alimentoDao,
                                  PacienteDao pacienteDao, ProfesionalDao profesionalDao, MedicionAntropometricaDao medicionDao,
                                  AccesoPacienteGuard guard, Clock clock) {
        this.dao = dao;
        this.reemplazoDao = reemplazoDao;
        this.alimentoDao = alimentoDao;
        this.pacienteDao = pacienteDao;
        this.profesionalDao = profesionalDao;
        this.medicionDao = medicionDao;
        this.guard = guard;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<PlanListItem> listar(Long pacienteId, String estado) {
        guard.assertPuedeVer(pacienteId);
        Map<Long, String> nombres = new HashMap<>();
        return dao.findByPacienteIdOrderByFechaInicioDescIdDesc(pacienteId).stream()
                .filter(p -> estado == null || estado.isBlank() || p.getEstado().name().equals(estado))
                .map(p -> new PlanListItem(p.getId(), p.getNombre(), p.getFechaInicio(), p.getFechaFin(), p.getEstado().name(),
                        p.getModo().name(), nombres.computeIfAbsent(p.getNutricionistaId(),
                        id -> profesionalDao.findById(id).map(Profesional::nombreVisible).orElse("")), p.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public PlanResponse obtener(Long id) {
        PlanAlimentario p = dao.findById(id).orElseThrow(() -> BusinessException.noEncontrado("Plan"));
        guard.assertPuedeVer(p.getPacienteId());
        return respuesta(p, true, List.of());
    }

    /** CU-11: crear plan (borrador o confirmado). */
    @Transactional
    @Auditable(modulo = Modulo.NUTRICION, tipo = TipoEvento.ALTA, entidad = "PlanAlimentario", entidadId = "#result.id()",
            descripcion = "Plan alimentario '#{#req.nombre()}' (#{#result.estado()}) para el paciente #{#pacienteId}")
    public PlanResponse crear(Long pacienteId, PlanRequest req) {
        guard.assertPuedeEditar(pacienteId, Area.NUTRICION);
        PlanAlimentario plan = new PlanAlimentario();
        plan.setPacienteId(pacienteId);
        plan.setNutricionistaId(CurrentUser.profesionalId());
        boolean confirmar = "ACTIVO".equalsIgnoreCase(req.estado());
        validar(req, confirmar);
        aplicarEncabezado(plan, req);
        plan.setEstado(PlanAlimentario.Estado.BORRADOR);
        List<String> advertencias = new ArrayList<>();
        sincronizarComidas(plan, req.comidas(), advertencias);
        dao.saveAndFlush(plan);
        if (confirmar) {
            activarInterno(plan, Boolean.TRUE.equals(req.finalizarActual()));
        }
        return respuesta(plan, false, advertencias);
    }

    /** CU-12: editar plan activo o borrador; mismas validaciones al confirmar. */
    @Transactional
    @Auditable(modulo = Modulo.NUTRICION, tipo = TipoEvento.MODIFICACION, entidad = "PlanAlimentario", entidadId = "#id",
            descripcion = "Modificación de plan alimentario '#{#req.nombre()}'")
    public PlanResponse actualizar(Long id, PlanRequest req) {
        PlanAlimentario plan = dao.findById(id).orElseThrow(() -> BusinessException.noEncontrado("Plan"));
        guard.assertPuedeEditar(plan.getPacienteId(), Area.NUTRICION);
        if (plan.getEstado() == PlanAlimentario.Estado.FINALIZADO || plan.getEstado() == PlanAlimentario.Estado.ELIMINADO) {
            throw new BusinessException(ErrorCode.REGLA_NEGOCIO, "Los planes finalizados o eliminados son de sólo lectura. Podés duplicarlo como base.");
        }
        Textos.verificarVersion(plan.getVersion(), req.version());
        boolean confirmar = plan.getEstado() == PlanAlimentario.Estado.ACTIVO || "ACTIVO".equalsIgnoreCase(req.estado());
        validar(req, confirmar);
        aplicarEncabezado(plan, req);
        List<String> advertencias = new ArrayList<>();
        int cambios = sincronizarComidas(plan, req.comidas(), advertencias);
        Auditoria.detalle("celdasModificadas", cambios);
        dao.saveAndFlush(plan);
        if (confirmar && plan.getEstado() == PlanAlimentario.Estado.BORRADOR) {
            activarInterno(plan, Boolean.TRUE.equals(req.finalizarActual()));
        }
        return respuesta(plan, false, advertencias);
    }

    @Transactional
    @Auditable(modulo = Modulo.NUTRICION, tipo = TipoEvento.MODIFICACION, entidad = "PlanAlimentario", entidadId = "#id",
            descripcion = "Confirmación de plan alimentario (borrador → activo)")
    public PlanResponse activar(Long id, boolean finalizarActual) {
        PlanAlimentario plan = dao.findById(id).orElseThrow(() -> BusinessException.noEncontrado("Plan"));
        guard.assertPuedeEditar(plan.getPacienteId(), Area.NUTRICION);
        if (plan.getEstado() != PlanAlimentario.Estado.BORRADOR) {
            throw new BusinessException(ErrorCode.REGLA_NEGOCIO, "Sólo se pueden confirmar borradores.");
        }
        ValidadorPlan.validarConfirmacion(aDtos(plan));
        activarInterno(plan, finalizarActual);
        return respuesta(plan, false, List.of());
    }

    /** CU-13: eliminación lógica; deja de ser visible para el paciente. */
    @Transactional
    @Auditable(modulo = Modulo.NUTRICION, tipo = TipoEvento.BAJA, entidad = "PlanAlimentario", entidadId = "#id",
            descripcion = "Eliminación de plan alimentario")
    public void eliminar(Long id) {
        PlanAlimentario plan = dao.findById(id).orElseThrow(() -> BusinessException.noEncontrado("Plan"));
        guard.assertPuedeEditar(plan.getPacienteId(), Area.NUTRICION);
        if (plan.getEstado() == PlanAlimentario.Estado.ELIMINADO) {
            throw new BusinessException(ErrorCode.REGLA_NEGOCIO, "El plan ya está eliminado.");
        }
        plan.setEstado(PlanAlimentario.Estado.ELIMINADO);
    }

    /** US-04.6: duplicar como base de un nuevo borrador. */
    @Transactional
    @Auditable(modulo = Modulo.NUTRICION, tipo = TipoEvento.ALTA, entidad = "PlanAlimentario", entidadId = "#result.id()",
            descripcion = "Duplicación de plan #{#id} como borrador")
    public PlanResponse duplicar(Long id) {
        PlanAlimentario origen = dao.findById(id).orElseThrow(() -> BusinessException.noEncontrado("Plan"));
        guard.assertPuedeEditar(origen.getPacienteId(), Area.NUTRICION);
        PlanAlimentario copia = new PlanAlimentario();
        copia.setPacienteId(origen.getPacienteId());
        copia.setNutricionistaId(CurrentUser.profesionalId());
        copia.setNombre(origen.getNombre() + " (copia)");
        copia.setFechaInicio(LocalDate.now(clock));
        copia.setDuracionSemanas(origen.getDuracionSemanas());
        copia.recalcularFin();
        copia.setModo(origen.getModo());
        copia.setKcalObjetivo(origen.getKcalObjetivo());
        copia.setPctHc(origen.getPctHc());
        copia.setPctProteinas(origen.getPctProteinas());
        copia.setPctGrasas(origen.getPctGrasas());
        copia.setObservaciones(origen.getObservaciones());
        copia.setEstado(PlanAlimentario.Estado.BORRADOR);
        sincronizarComidas(copia, aDtos(origen), new ArrayList<>());
        dao.saveAndFlush(copia);
        return respuesta(copia, false, List.of());
    }

    /** Job diario: los planes vencidos pasan a FINALIZADO (RN-04.4). */
    @Scheduled(cron = "${mgnutri.jobs.finalizar-vencidos-cron}", zone = "America/Argentina/Buenos_Aires")
    @Transactional
    public int finalizarVencidos() {
        List<PlanAlimentario> vencidos = dao.findByEstadoAndFechaFinBefore(PlanAlimentario.Estado.ACTIVO, LocalDate.now(clock));
        vencidos.forEach(p -> p.setEstado(PlanAlimentario.Estado.FINALIZADO));
        return vencidos.size();
    }

    @EventListener
    @Transactional
    public void alDarDeBajaPaciente(Eventos.PacienteDadoDeBaja e) {
        dao.findFirstByPacienteIdAndEstado(e.pacienteId(), PlanAlimentario.Estado.ACTIVO)
                .ifPresent(p -> p.setEstado(PlanAlimentario.Estado.FINALIZADO));
    }

    @Override
    @Transactional(readOnly = true)
    public long activosDe(Long profesionalId, Area area) {
        return area == Area.NUTRICION ? dao.countByNutricionistaIdAndEstado(profesionalId, PlanAlimentario.Estado.ACTIVO) : 0;
    }

    // ------------------------------------------------------------------ internos

    private void activarInterno(PlanAlimentario plan, boolean finalizarActual) {
        Optional<PlanAlimentario> actual = dao.findFirstByPacienteIdAndEstado(plan.getPacienteId(), PlanAlimentario.Estado.ACTIVO)
                .filter(p -> !p.getId().equals(plan.getId()));
        if (actual.isPresent()) {
            if (!finalizarActual) {
                throw new BusinessException(ErrorCode.PLAN_ACTIVO_EXISTENTE, "El plan actual (desde "
                        + ar.edu.usal.mgnutri.common.Fechas.fecha(actual.get().getFechaInicio()) + ") se finalizará. ¿Confirmás?")
                        .con("planActualId", actual.get().getId()).con("fechaInicio", actual.get().getFechaInicio().toString());
            }
            actual.get().setEstado(PlanAlimentario.Estado.FINALIZADO);
            dao.saveAndFlush(actual.get());
        }
        plan.setEstado(PlanAlimentario.Estado.ACTIVO);
        dao.saveAndFlush(plan);
    }

    private void validar(PlanRequest req, boolean confirmar) {
        ValidadorPlan.validarEstructura(req.comidas());
        ValidadorPlan.validarFechas(req.fechaInicio(), LocalDate.now(clock));
        if (confirmar) {
            ValidadorPlan.validarConfirmacion(req.comidas());
        }
        for (ComidaDto c : req.comidas()) {
            if (c.items() != null) {
                for (ItemDto i : c.items()) {
                    Alimento a = alimentoDao.findById(i.alimentoId())
                            .orElseThrow(() -> BusinessException.validacion("comidas", "Alimento inexistente."));
                    if (!a.isActivo() && i.id() == null) {
                        throw BusinessException.validacion("comidas", "El alimento " + a.getNombre() + " está dado de baja.");
                    }
                }
            }
        }
    }

    private void aplicarEncabezado(PlanAlimentario plan, PlanRequest req) {
        plan.setNombre(req.nombre().trim());
        plan.setFechaInicio(req.fechaInicio());
        plan.setDuracionSemanas(req.duracionSemanas());
        plan.recalcularFin();
        plan.setObservaciones(Textos.vacioANull(req.observaciones()));
        plan.setModo("AUTOMATICO".equalsIgnoreCase(req.modo()) ? PlanAlimentario.Modo.AUTOMATICO : PlanAlimentario.Modo.MANUAL);
        plan.setKcalObjetivo(req.kcalObjetivo());
        plan.setPctHc(req.pctHc());
        plan.setPctProteinas(req.pctProteinas());
        plan.setPctGrasas(req.pctGrasas());
        plan.setParametrosGeneracion(Textos.vacioANull(req.parametrosGeneracion()));
    }

    /**
     * Merge de comidas por clave (día, tipo): las quitadas quedan inactivas (sus registros se conservan, CA-04.3.4);
     * si cambian los ítems, los anteriores se desactivan y sus reemplazos también (CA-04.3.5).
     */
    int sincronizarComidas(PlanAlimentario plan, List<ComidaDto> nuevas, List<String> advertencias) {
        Map<String, ComidaPlan> existentes = plan.comidasActivas().stream()
                .collect(Collectors.toMap(c -> c.getDiaSemana() + "-" + c.getTipo(), Function.identity()));
        int cambios = 0;
        for (ComidaDto dto : nuevas) {
            String clave = dto.diaSemana() + "-" + dto.tipo();
            boolean tieneContenido = (dto.descripcion() != null && !dto.descripcion().isBlank())
                    || (dto.items() != null && !dto.items().isEmpty());
            ComidaPlan c = existentes.remove(clave);
            if (!tieneContenido) {
                if (c != null) {
                    desactivar(c, advertencias);
                    cambios++;
                }
                continue;
            }
            if (c == null) {
                c = new ComidaPlan(plan, dto.diaSemana(), dto.tipo());
                plan.getComidas().add(c);
                cambios++;
            }
            if (!Objects.equals(c.getDescripcion(), Textos.vacioANull(dto.descripcion()))) {
                cambios++;
            }
            c.setDescripcion(Textos.vacioANull(dto.descripcion()));
            if (itemsCambiaron(c, dto.items())) {
                cambios++;
                for (ItemComida i : c.itemsActivos()) {
                    i.setActivo(false);
                    desactivarReemplazo(i, advertencias);
                }
                int orden = 1;
                if (dto.items() != null) {
                    for (ItemDto i : dto.items()) {
                        c.getItems().add(new ItemComida(c, i.alimentoId(), i.gramos(), Textos.vacioANull(i.cantidadCasera()), orden++));
                    }
                }
            }
            c.setKcalEstimadas(BigDecimal.valueOf(Math.round(totales(c.itemsActivos()).kcal())));
        }
        for (ComidaPlan restante : existentes.values()) {
            desactivar(restante, advertencias);
            cambios++;
        }
        return cambios;
    }

    private void desactivar(ComidaPlan c, List<String> advertencias) {
        c.setActivo(false);
        c.itemsActivos().forEach(i -> desactivarReemplazo(i, advertencias));
    }

    private void desactivarReemplazo(ItemComida i, List<String> advertencias) {
        if (i.getId() == null) {
            return;
        }
        reemplazoDao.findByItemComidaIdAndActivoTrue(i.getId()).ifPresent(r -> {
            r.setActivo(false);
            advertencias.add("Se desactivó un reemplazo que el paciente había elegido en " + i.getComida().getTipo().etiqueta()
                    + " del " + ValidadorPlan.DIAS[i.getComida().getDiaSemana()] + ".");
        });
    }

    private static boolean itemsCambiaron(ComidaPlan c, List<ItemDto> nuevos) {
        List<ItemComida> actuales = c.itemsActivos();
        List<ItemDto> lista = nuevos == null ? List.of() : nuevos;
        if (actuales.size() != lista.size()) {
            return true;
        }
        for (int i = 0; i < lista.size(); i++) {
            ItemComida a = actuales.get(i);
            ItemDto n = lista.get(i);
            if (!a.getAlimentoId().equals(n.alimentoId()) || a.getGramos().compareTo(n.gramos()) != 0
                    || !Objects.equals(a.getCantidadCasera(), Textos.vacioANull(n.cantidadCasera()))) {
                return true;
            }
        }
        return false;
    }

    List<ComidaDto> aDtos(PlanAlimentario plan) {
        return plan.comidasActivas().stream().map(c -> new ComidaDto(c.getDiaSemana(), c.getTipo(), c.getDescripcion(),
                c.itemsActivos().stream().map(i -> new ItemDto(null, i.getAlimentoId(), i.getGramos(), i.getCantidadCasera())).toList()))
                .toList();
    }

    private Macros totales(List<ItemComida> items) {
        Macros t = Macros.CERO;
        for (ItemComida i : items) {
            Alimento a = alimentoDao.findById(i.getAlimentoId()).orElse(null);
            if (a != null) {
                t = t.mas(CalculadoraNutricional.macros(a, i.getGramos().doubleValue()));
            }
        }
        return t;
    }

    /** Arma la respuesta; con reemplazos aplicados si el plan es activo (para paciente, PDF y profesional). */
    public PlanResponse respuesta(PlanAlimentario p, boolean conReemplazos, List<String> advertencias) {
        Paciente pac = pacienteDao.findById(p.getPacienteId()).orElseThrow();
        Profesional prof = profesionalDao.findById(p.getNutricionistaId()).orElse(null);
        List<ComidaPlan> comidas = p.comidasActivas().stream()
                .sorted(Comparator.comparingInt(ComidaPlan::getDiaSemana).thenComparingInt(c -> c.getTipo().orden())).toList();
        List<Long> itemIds = comidas.stream().flatMap(c -> c.itemsActivos().stream()).map(ItemComida::getId).filter(Objects::nonNull).toList();
        Map<Long, ReemplazoAlimento> reemplazos = conReemplazos && !itemIds.isEmpty()
                ? reemplazoDao.findByItemComidaIdIn(itemIds).stream().filter(ReemplazoAlimento::isActivo)
                .collect(Collectors.toMap(ReemplazoAlimento::getItemComidaId, Function.identity(), (a, b) -> b))
                : Map.of();
        Map<Long, Alimento> cache = new HashMap<>();
        Function<Long, Alimento> alimento = id -> cache.computeIfAbsent(id, k -> alimentoDao.findById(k).orElse(null));
        List<ComidaResponse> res = new ArrayList<>();
        Map<Integer, Macros> porDia = new HashMap<>();
        for (ComidaPlan c : comidas) {
            List<ItemResponse> items = new ArrayList<>();
            Macros total = Macros.CERO;
            for (ItemComida i : c.itemsActivos()) {
                Alimento a = alimento.apply(i.getAlimentoId());
                ReemplazoAlimento r = i.getId() == null ? null : reemplazos.get(i.getId());
                ReemplazoInfo info = null;
                Macros m;
                if (r != null) {
                    Alimento ar = alimento.apply(r.getAlimentoReemplazoId());
                    info = new ReemplazoInfo(r.getAlimentoReemplazoId(), ar == null ? "" : ar.getNombre(), r.getGramos(), r.getCantidadCasera());
                    m = ar == null ? Macros.CERO : CalculadoraNutricional.macros(ar, r.getGramos().doubleValue());
                } else {
                    m = a == null ? Macros.CERO : CalculadoraNutricional.macros(a, i.getGramos().doubleValue());
                }
                total = total.mas(m);
                items.add(new ItemResponse(i.getId(), i.getAlimentoId(), a == null ? "" : a.getNombre(),
                        a == null ? "" : a.getGrupo().getNombre(), i.getGramos(), i.getCantidadCasera(), m.redondeado(), info));
            }
            porDia.merge(c.getDiaSemana(), total, Macros::mas);
            res.add(new ComidaResponse(c.getId(), c.getDiaSemana(), c.getTipo(), c.getTipo().etiqueta(), c.getDescripcion(),
                    items, total.redondeado(), null));
        }
        Macros objetivo = p.getKcalObjetivo() != null && p.getPctHc() != null && p.getPctProteinas() != null && p.getPctGrasas() != null
                ? CalculadoraNutricional.objetivo(p.getKcalObjetivo(), p.getPctHc().doubleValue(), p.getPctProteinas().doubleValue(),
                p.getPctGrasas().doubleValue()) : null;
        List<TotalDia> totales = new ArrayList<>();
        for (int d = 1; d <= 7; d++) {
            Macros m = porDia.getOrDefault(d, Macros.CERO);
            totales.add(new TotalDia(d, Math.round(m.kcal()), Math.round(m.hc() * 10) / 10.0, Math.round(m.proteinas() * 10) / 10.0,
                    Math.round(m.grasas() * 10) / 10.0, objetivo == null || m.kcal() == 0 ? null : CalculadoraNutricional.desvio(m, objetivo)));
        }
        BigDecimal ultimoPeso = medicionDao.findFirstByPacienteIdOrderByFechaDescIdDesc(pac.getId())
                .map(x -> x.getPesoKg()).orElse(null);
        UsuarioPrincipal u = CurrentUser.opcional().orElse(null);
        return new PlanResponse(p.getId(),
                new PacienteResumen(pac.getId(), pac.nombreCompleto(), pac.edad(LocalDate.now(clock)), pac.getObjetivo(), ultimoPeso),
                prof == null ? null : new ProfesionalResumen(prof.getId(), prof.nombreVisible() + " (" + prof.nombreCompleto() + ")", prof.getMatricula()),
                p.getNombre(), p.getFechaInicio(), p.getDuracionSemanas(), p.getFechaFin(), p.getModo().name(), p.getKcalObjetivo(),
                p.getPctHc(), p.getPctProteinas(), p.getPctGrasas(), u != null && u.esPaciente() ? null : p.getParametrosGeneracion(),
                p.getEstado().name(), p.getObservaciones(), res, totales, p.getCreatedAt(), p.getVersion(),
                advertencias.stream().distinct().toList());
    }
}
