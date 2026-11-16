package ar.edu.usal.mgnutri.reportes.service;

import ar.edu.usal.mgnutri.common.Area;
import ar.edu.usal.mgnutri.common.Fechas;
import ar.edu.usal.mgnutri.common.error.BusinessException;
import ar.edu.usal.mgnutri.common.error.ErrorCode;
import ar.edu.usal.mgnutri.common.security.AccesoPacienteGuard;
import ar.edu.usal.mgnutri.common.security.CurrentUser;
import ar.edu.usal.mgnutri.common.security.UsuarioPrincipal;
import ar.edu.usal.mgnutri.entrenamiento.dao.ReemplazoEjercicioDao;
import ar.edu.usal.mgnutri.entrenamiento.dao.RegistroEntrenamientoDao;
import ar.edu.usal.mgnutri.entrenamiento.dao.RutinaDao;
import ar.edu.usal.mgnutri.entrenamiento.domain.DiaRutina;
import ar.edu.usal.mgnutri.entrenamiento.domain.RegistroEntrenamiento;
import ar.edu.usal.mgnutri.entrenamiento.domain.Rutina;
import ar.edu.usal.mgnutri.evolucion.dto.EvolucionDtos.EvolucionResponse;
import ar.edu.usal.mgnutri.evolucion.dto.EvolucionDtos.FilaTabla;
import ar.edu.usal.mgnutri.evolucion.dto.EvolucionDtos.Punto;
import ar.edu.usal.mgnutri.evolucion.dto.EvolucionDtos.SerieNombrada;
import ar.edu.usal.mgnutri.evolucion.service.EvolucionService;
import ar.edu.usal.mgnutri.novedades.dao.ComentarioDao;
import ar.edu.usal.mgnutri.novedades.dao.MolestiaDao;
import ar.edu.usal.mgnutri.novedades.domain.Comentario;
import ar.edu.usal.mgnutri.nutricion.dao.ObservacionDiariaDao;
import ar.edu.usal.mgnutri.nutricion.dao.PlanAlimentarioDao;
import ar.edu.usal.mgnutri.nutricion.dao.ReemplazoAlimentoDao;
import ar.edu.usal.mgnutri.nutricion.dao.RegistroComidaDao;
import ar.edu.usal.mgnutri.nutricion.domain.ObservacionDiaria;
import ar.edu.usal.mgnutri.nutricion.domain.PlanAlimentario;
import ar.edu.usal.mgnutri.pacientes.dao.PacienteDao;
import ar.edu.usal.mgnutri.pacientes.domain.Paciente;
import ar.edu.usal.mgnutri.pagos.dto.PagoDtos.DeudorResponse;
import ar.edu.usal.mgnutri.pagos.dao.PagoDao;
import ar.edu.usal.mgnutri.pagos.domain.Pago;
import ar.edu.usal.mgnutri.pagos.service.CuentaCorrienteService;
import ar.edu.usal.mgnutri.profesionales.dao.ProfesionalDao;
import ar.edu.usal.mgnutri.profesionales.domain.Profesional;
import ar.edu.usal.mgnutri.reportes.dto.ReporteDto;
import ar.edu.usal.mgnutri.turnos.dao.TurnoDao;
import ar.edu.usal.mgnutri.turnos.domain.Turno;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Reportes del sistema R-1 … R-5 (spec 12). Datos consultados en el momento (sin caché). */
@Service
public class ReporteService {

    private final TurnoDao turnoDao;
    private final PagoDao pagoDao;
    private final CuentaCorrienteService cuentas;
    private final PacienteDao pacienteDao;
    private final ProfesionalDao profesionalDao;
    private final RutinaDao rutinaDao;
    private final RegistroEntrenamientoDao regEntDao;
    private final ReemplazoEjercicioDao repEjDao;
    private final PlanAlimentarioDao planDao;
    private final RegistroComidaDao regComDao;
    private final ReemplazoAlimentoDao repAlDao;
    private final ObservacionDiariaDao obsDao;
    private final ComentarioDao comentarioDao;
    private final MolestiaDao molestiaDao;
    private final EvolucionService evolucion;
    private final AccesoPacienteGuard guard;
    private final Clock clock;

    public ReporteService(TurnoDao turnoDao, PagoDao pagoDao, CuentaCorrienteService cuentas, PacienteDao pacienteDao,
                          ProfesionalDao profesionalDao, RutinaDao rutinaDao, RegistroEntrenamientoDao regEntDao,
                          ReemplazoEjercicioDao repEjDao, PlanAlimentarioDao planDao, RegistroComidaDao regComDao,
                          ReemplazoAlimentoDao repAlDao, ObservacionDiariaDao obsDao, ComentarioDao comentarioDao,
                          MolestiaDao molestiaDao, EvolucionService evolucion, AccesoPacienteGuard guard, Clock clock) {
        this.turnoDao = turnoDao;
        this.pagoDao = pagoDao;
        this.cuentas = cuentas;
        this.pacienteDao = pacienteDao;
        this.profesionalDao = profesionalDao;
        this.rutinaDao = rutinaDao;
        this.regEntDao = regEntDao;
        this.repEjDao = repEjDao;
        this.planDao = planDao;
        this.regComDao = regComDao;
        this.repAlDao = repAlDao;
        this.obsDao = obsDao;
        this.comentarioDao = comentarioDao;
        this.molestiaDao = molestiaDao;
        this.evolucion = evolucion;
        this.guard = guard;
        this.clock = clock;
    }

    public record Rango(LocalDate desde, LocalDate hasta) {}

    /** Default: último mes; máximo 2 años (CA-12.0.9). */
    public Rango rango(LocalDate desde, LocalDate hasta) {
        LocalDate h = hasta == null ? LocalDate.now(clock) : hasta;
        LocalDate d = desde == null ? h.minusMonths(1) : desde;
        if (d.isAfter(h)) {
            throw BusinessException.validacion("desde", "La fecha desde debe ser anterior a la fecha hasta.");
        }
        if (ChronoUnit.DAYS.between(d, h) > 731) {
            throw BusinessException.validacion("hasta", "El rango máximo es de 2 años.");
        }
        return new Rango(d, h);
    }

    /** RN-12.1: un profesional sólo ve a sus pacientes; un paciente no asignado → 403. */
    private Set<Long> pacientesPermitidos(Long pacienteId) {
        Set<Long> visibles = guard.pacientesVisibles();
        if (pacienteId != null) {
            if (visibles != null && !visibles.contains(pacienteId)) {
                throw new BusinessException(ErrorCode.SIN_PERMISO, "No tenés asignado a este paciente.");
            }
            return Set.of(pacienteId);
        }
        return visibles;
    }

    private Map<Long, String> nombresPacientes() {
        return pacienteDao.findAll().stream().collect(Collectors.toMap(Paciente::getId,
                p -> p.nombreCompleto() + (p.activo() ? "" : " (baja)")));
    }

    private Map<Long, String> nombresProfesionales() {
        return profesionalDao.findAll().stream().collect(Collectors.toMap(Profesional::getId, Profesional::nombreVisible));
    }

    // ------------------------------------------------------------------ R-1 Turnos (CU-30)

    @Transactional(readOnly = true)
    public ReporteDto turnos(LocalDate desde, LocalDate hasta, Long profesionalId, Long pacienteId, Area area, String estado) {
        Rango r = rango(desde, hasta);
        UsuarioPrincipal u = CurrentUser.get();
        Long prof = u.esAdmin() ? profesionalId : u.profesionalId();
        Set<Long> pacientes = pacientesPermitidos(pacienteId);
        Specification<Turno> spec = (root, q, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.greaterThanOrEqualTo(root.get("inicio"), Fechas.inicioDelDia(r.desde())));
            p.add(cb.lessThan(root.get("inicio"), Fechas.inicioDelDia(r.hasta().plusDays(1))));
            if (prof != null) {
                p.add(cb.equal(root.get("profesionalId"), prof));
            }
            if (pacientes != null) {
                p.add(pacientes.isEmpty() ? cb.disjunction() : root.get("pacienteId").in(pacientes));
            }
            if (area != null) {
                p.add(cb.equal(root.get("area"), area));
            }
            if (estado != null && !estado.isBlank()) {
                p.add(cb.equal(root.get("estado"), Turno.Estado.valueOf(estado)));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
        List<Turno> turnos = turnoDao.findAll(spec, Sort.by("inicio"));
        Map<Long, String> pacs = nombresPacientes();
        Map<Long, String> profs = nombresProfesionales();
        ReporteDto rep = ReporteDto.nuevo("turnos", "Reporte de turnos");
        filtros(rep, r);
        if (prof != null) {
            rep.filtros().put("Profesional", profs.getOrDefault(prof, ""));
        }
        if (pacienteId != null) {
            rep.filtros().put("Paciente", pacs.getOrDefault(pacienteId, ""));
        }
        if (area != null) {
            rep.filtros().put("Área", area.etiqueta());
        }
        if (estado != null && !estado.isBlank()) {
            rep.filtros().put("Estado", estado);
        }
        rep.tabla().con("Fecha", "Hora", "Paciente", "Profesional", "Área", "Estado", "Cancelado por", "Motivo");
        for (Turno t : turnos) {
            rep.tabla().fila(Fechas.fecha(t.getInicio()), Fechas.hora(t.getInicio()), pacs.get(t.getPacienteId()),
                    profs.get(t.getProfesionalId()), t.getArea().etiqueta(), t.getEstado().name(), t.getCanceladoPorRol(), t.getMotivoCancelacion());
        }
        Map<Turno.Estado, Long> porEstado = turnos.stream().collect(Collectors.groupingBy(Turno::getEstado, () -> new java.util.EnumMap<>(Turno.Estado.class), Collectors.counting()));
        rep.resumen().put("Total de turnos", turnos.size());
        for (Turno.Estado e : Turno.Estado.values()) {
            rep.resumen().put(etiquetaEstado(e), porEstado.getOrDefault(e, 0L));
        }
        Map<String, Long> canceladosPor = turnos.stream().filter(t -> t.getEstado() == Turno.Estado.CANCELADO)
                .collect(Collectors.groupingBy(t -> t.getCanceladoPorRol() == null ? "SISTEMA" : t.getCanceladoPorRol(), TreeMap::new, Collectors.counting()));
        canceladosPor.forEach((k, v) -> rep.resumen().put("Cancelados por " + k.toLowerCase(), v));
        long pasados = turnos.stream().filter(t -> t.getEstado() == Turno.Estado.ATENDIDO || t.getEstado() == Turno.Estado.AUSENTE).count();
        rep.resumen().put("Tasa de ausentismo", pasados == 0 ? "0 %" : pct(porEstado.getOrDefault(Turno.Estado.AUSENTE, 0L), pasados));
        rep.resumen().put("Tasa de cancelación", turnos.isEmpty() ? "0 %" : pct(porEstado.getOrDefault(Turno.Estado.CANCELADO, 0L), turnos.size()));
        // Tasas por profesional
        ReporteDto.Tabla porProf = new ReporteDto.Tabla("Por profesional", new ArrayList<>(), new ArrayList<>())
                .con("Profesional", "Turnos", "Atendidos", "Ausentes", "Cancelados", "% ausentismo", "% cancelación");
        turnos.stream().collect(Collectors.groupingBy(Turno::getProfesionalId)).forEach((pid, lista) -> {
            long at = lista.stream().filter(t -> t.getEstado() == Turno.Estado.ATENDIDO).count();
            long au = lista.stream().filter(t -> t.getEstado() == Turno.Estado.AUSENTE).count();
            long ca = lista.stream().filter(t -> t.getEstado() == Turno.Estado.CANCELADO).count();
            porProf.fila(profs.get(pid), (long) lista.size(), at, au, ca, at + au == 0 ? "0 %" : pct(au, at + au), pct(ca, lista.size()));
        });
        rep.secciones().add(porProf);
        // Gráfico por semana
        Map<String, Long> porSemana = new TreeMap<>();
        turnos.forEach(t -> {
            LocalDate f = Fechas.fecha(t.getInicio());
            porSemana.merge(f.get(IsoFields.WEEK_BASED_YEAR) + "-S" + String.format("%02d", f.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)), 1L, Long::sum);
        });
        rep.graficos().put("porSemana", new ReporteDto.Grafico("Turnos por semana", "bar", new ArrayList<>(porSemana.keySet()),
                List.of(new ReporteDto.Serie("Turnos", new ArrayList<>(porSemana.values())))));
        return rep;
    }

    private static String etiquetaEstado(Turno.Estado e) {
        return switch (e) {
            case RESERVADO -> "Reservados";
            case ATENDIDO -> "Atendidos";
            case AUSENTE -> "Ausentes";
            case CANCELADO -> "Cancelados";
        };
    }

    // ------------------------------------------------------------------ R-2 Pagos (CU-31)

    @Transactional(readOnly = true)
    public ReporteDto pagos(LocalDate desde, LocalDate hasta, Long pacienteId, String estado, String medio) {
        Rango r = rango(desde, hasta);
        Map<Long, String> pacs = nombresPacientes();
        List<Pago> pagos = pagoDao.enRango(r.desde(), r.hasta(), pacienteId).stream()
                .filter(p -> estado == null || estado.isBlank() || ("ANULADO".equals(estado) == p.isAnulado()))
                .filter(p -> medio == null || medio.isBlank() || p.getMedio().name().equals(medio)).toList();
        ReporteDto rep = ReporteDto.nuevo("pagos", "Reporte de pagos y deudas");
        filtros(rep, r);
        if (pacienteId != null) {
            rep.filtros().put("Paciente", pacs.getOrDefault(pacienteId, ""));
        }
        if (medio != null && !medio.isBlank()) {
            rep.filtros().put("Medio", medio);
        }
        rep.tabla().con("Fecha", "Comprobante", "Paciente", "Medio", "Monto", "Estado");
        for (Pago p : pagos) {
            rep.tabla().fila(p.getFecha(), p.getNroComprobante(), pacs.get(p.getPacienteId()), p.getMedio().etiqueta(), p.getMonto(),
                    p.isAnulado() ? "Anulado" : "Vigente");
        }
        List<Pago> vigentes = pagos.stream().filter(p -> !p.isAnulado()).toList();
        rep.resumen().put("Pagos registrados", pagos.size());
        rep.resumen().put("Total recaudado", vigentes.stream().map(Pago::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add));
        vigentes.stream().collect(Collectors.groupingBy(p -> p.getMedio().etiqueta(), TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO, Pago::getMonto, BigDecimal::add)))
                .forEach((k, v) -> rep.resumen().put("Total " + k, v));
        ReporteDto.Tabla deudas = new ReporteDto.Tabla("Deudas pendientes", new ArrayList<>(), new ArrayList<>())
                .con("Paciente", "Saldo", "Fecha de deuda", "Días de atraso");
        List<DeudorResponse> deudores = cuentas.deudores("monto");
        deudores.stream().filter(d -> pacienteId == null || d.pacienteId().equals(pacienteId))
                .forEach(d -> deudas.fila(d.paciente(), d.saldo(), d.fechaDeuda(), d.diasAtraso()));
        rep.secciones().add(deudas);
        rep.resumen().put("Total adeudado", deudores.stream().filter(d -> pacienteId == null || d.pacienteId().equals(pacienteId))
                .map(DeudorResponse::saldo).reduce(BigDecimal.ZERO, BigDecimal::add));
        Map<String, Double> porMes = new TreeMap<>();
        vigentes.forEach(p -> porMes.merge(YearMonth.from(p.getFecha()).toString(), p.getMonto().doubleValue(), Double::sum));
        rep.graficos().put("recaudacion", new ReporteDto.Grafico("Recaudación por mes", "bar", new ArrayList<>(porMes.keySet()),
                List.of(new ReporteDto.Serie("Recaudado", new ArrayList<>(porMes.values())))));
        return rep;
    }

    // ------------------------------------------------------------------ R-3 Evolución (CU-32)

    @Transactional(readOnly = true)
    public ReporteDto evolucion(Long pacienteId, LocalDate desde, LocalDate hasta) {
        Paciente p = guard.assertPuedeVer(pacienteId);
        LocalDate d = desde == null ? LocalDate.of(2000, 1, 1) : desde;
        LocalDate h = hasta == null ? LocalDate.now(clock) : hasta;
        EvolucionResponse ev = evolucion.evolucion(pacienteId, d, h);
        ReporteDto rep = ReporteDto.nuevo("evolucion", "Evolución de " + p.nombreCompleto());
        rep.filtros().put("Paciente", p.nombreCompleto() + " · " + p.edad(LocalDate.now(clock)) + " años");
        if (desde != null) {
            rep.filtros().put("Desde", Fechas.fecha(desde));
        }
        rep.filtros().put("Hasta", Fechas.fecha(h));
        rep.tabla().con("Fecha", "Peso (kg)", "IMC", "% grasa", "% músculo", "Cintura (cm)", "Δ anterior", "Δ inicial", "Δ inicial %");
        for (FilaTabla f : ev.tabla()) {
            rep.tabla().fila(f.fecha(), f.peso(), f.imc(), f.pctGrasa(), f.pctMusculo(), f.cintura(), f.deltaAnterior(), f.deltaInicial(),
                    f.deltaInicialPct());
        }
        if (!ev.tabla().isEmpty()) {
            FilaTabla primera = ev.tabla().get(0);
            FilaTabla ultima = ev.tabla().get(ev.tabla().size() - 1);
            rep.resumen().put("Mediciones", ev.tabla().size());
            rep.resumen().put("Peso inicial → final", primera.peso() + " → " + ultima.peso() + " kg");
            rep.resumen().put("Variación de peso", ultima.deltaInicial() + " kg (" + ultima.deltaInicialPct() + " %)");
            if (primera.pctGrasa() != null && ultima.pctGrasa() != null) {
                rep.resumen().put("% grasa inicial → final", primera.pctGrasa() + " → " + ultima.pctGrasa());
            }
        }
        rep.graficos().put("peso", grafico("Peso (kg)", Map.of("Peso", ev.peso())));
        Map<String, List<Punto>> comp = new LinkedHashMap<>();
        comp.put("% grasa", ev.composicion().get("grasa"));
        comp.put("% músculo", ev.composicion().get("musculo"));
        rep.graficos().put("composicion", grafico("Composición corporal (%)", comp));
        Map<String, List<Punto>> medidas = new LinkedHashMap<>();
        ev.medidas().forEach((k, v) -> {
            if (!v.isEmpty()) {
                medidas.put(k, v);
            }
        });
        if (!medidas.isEmpty()) {
            rep.graficos().put("medidas", grafico("Perímetros (cm)", medidas));
        }
        ReporteDto.Tabla desempeno = new ReporteDto.Tabla("Desempeño físico", new ArrayList<>(), new ArrayList<>())
                .con("Prueba", "Unidad", "Primera", "Última", "Variación");
        for (SerieNombrada s : ev.desempeno()) {
            double a = s.serie().get(0).valor();
            double b = s.serie().get(s.serie().size() - 1).valor();
            desempeno.fila(s.nombre(), s.unidad(), a, b, Math.round((b - a) * 10) / 10.0);
        }
        rep.secciones().add(desempeno);
        return rep;
    }

    @Transactional(readOnly = true)
    public ReporteDto miEvolucion(LocalDate desde, LocalDate hasta) {
        return evolucion(CurrentUser.pacienteId(), desde, hasta);
    }

    private static ReporteDto.Grafico grafico(String titulo, Map<String, List<Punto>> series) {
        TreeMap<LocalDate, Map<String, Double>> porFecha = new TreeMap<>();
        series.forEach((nombre, puntos) -> puntos.forEach(p -> porFecha.computeIfAbsent(p.fecha(), k -> new HashMap<>()).put(nombre, p.valor())));
        List<String> labels = porFecha.keySet().stream().map(Fechas::fecha).toList();
        List<ReporteDto.Serie> ss = new ArrayList<>();
        series.keySet().forEach(nombre -> ss.add(new ReporteDto.Serie(nombre,
                porFecha.values().stream().map(m -> (Number) m.get(nombre)).collect(Collectors.toList()))));
        return new ReporteDto.Grafico(titulo, "line", labels, ss);
    }

    // ------------------------------------------------------------------ R-4 Rutinas (CU-33)

    @Transactional(readOnly = true)
    public ReporteDto rutinas(LocalDate desde, LocalDate hasta, Long pacienteId, Long entrenadorId, String estado, String agrupar) {
        Rango r = rango(desde, hasta);
        UsuarioPrincipal u = CurrentUser.get();
        Long entr = u.esAdmin() ? entrenadorId : u.profesionalId();
        Set<Long> pacientes = pacientesPermitidos(pacienteId);
        Map<Long, String> pacs = nombresPacientes();
        Map<Long, String> profs = nombresProfesionales();
        List<Rutina> rutinas = rutinaDao.findAll().stream()
                .filter(x -> pacientes == null || pacientes.contains(x.getPacienteId()))
                .filter(x -> entr == null || x.getEntrenadorId().equals(entr))
                .filter(x -> estado == null || estado.isBlank() || x.getEstado().name().equals(estado))
                .filter(x -> !x.getFechaInicio().isAfter(r.hasta()) && !x.getFechaFin().isBefore(r.desde()))
                .toList();
        boolean porSemana = "SEMANA".equalsIgnoreCase(agrupar);
        ReporteDto rep = ReporteDto.nuevo("rutinas", "Reporte de rutinas");
        filtros(rep, r);
        rep.filtros().put("Agrupado por", porSemana ? "semana" : "mes");
        rep.tabla().con("Paciente", "Entrenador", "Rutina", "Vigencia", "Estado", "% cumplimiento", "Reemplazos", "Comentarios", "Molestias");
        TreeMap<String, long[]> periodos = new TreeMap<>();
        for (Rutina rt : rutinas) {
            LocalDate d = r.desde().isBefore(rt.getFechaInicio()) ? rt.getFechaInicio() : r.desde();
            LocalDate h = r.hasta().isAfter(rt.getFechaFin()) ? rt.getFechaFin() : r.hasta();
            long programados = 0;
            for (LocalDate f = d; !f.isAfter(h); f = f.plusDays(1)) {
                int dia = f.getDayOfWeek().getValue();
                long n = rt.diasActivos().stream().filter(x -> x.getDiaSemana() == dia).mapToLong(x -> x.ejerciciosActivos().size()).sum();
                programados += n;
                periodos.computeIfAbsent(clavePeriodo(f, porSemana), k -> new long[2])[0] += n;
            }
            Set<Long> ids = rt.getDias().stream().flatMap(x -> x.getEjercicios().stream()).map(e -> e.getId()).collect(Collectors.toSet());
            List<RegistroEntrenamiento> regs = regEntDao.findByPacienteIdAndFechaBetweenOrderByFechaAsc(rt.getPacienteId(), d, h).stream()
                    .filter(x -> ids.contains(x.getEjercicioRutinaId()) && x.isCompletado()).toList();
            regs.forEach(x -> periodos.computeIfAbsent(clavePeriodo(x.getFecha(), porSemana), k -> new long[2])[1]++);
            long reemplazos = repEjDao.findByPacienteIdAndFechaBetween(rt.getPacienteId(), d, h).stream()
                    .filter(x -> ids.contains(x.getEjercicioRutinaId())).count();
            long comentarios = comentarioDao.findByPacienteIdAndFechaBetweenOrderByCreatedAtDesc(rt.getPacienteId(), d, h).stream()
                    .filter(c -> c.getTipo() == Comentario.Tipo.RUTINA && rt.getId().equals(c.getRutinaId())).count();
            long molestias = molestiaDao.findByPacienteIdAndFechaBetweenOrderByCreatedAtDesc(rt.getPacienteId(), d, h).size();
            rep.tabla().fila(pacs.get(rt.getPacienteId()), profs.get(rt.getEntrenadorId()), rt.getNombre(),
                    Fechas.fecha(rt.getFechaInicio()) + " – " + Fechas.fecha(rt.getFechaFin()), rt.getEstado().name(),
                    programados == 0 ? "0 %" : pct(regs.size(), programados), reemplazos, comentarios, molestias);
        }
        rep.resumen().put("Rutinas", rutinas.size());
        ReporteDto.Tabla comp = new ReporteDto.Tabla("Comparativo por período", new ArrayList<>(), new ArrayList<>())
                .con("Período", "Programados", "Completados", "% cumplimiento");
        List<Number> serie = new ArrayList<>();
        periodos.forEach((k, v) -> {
            comp.fila(k, v[0], v[1], v[0] == 0 ? "0 %" : pct(v[1], v[0]));
            serie.add(v[0] == 0 ? 0 : Math.round(v[1] * 1000.0 / v[0]) / 10.0);
        });
        rep.secciones().add(comp);
        rep.graficos().put("cumplimiento", new ReporteDto.Grafico("% de cumplimiento", "bar", new ArrayList<>(periodos.keySet()),
                List.of(new ReporteDto.Serie("% cumplimiento", serie))));
        return rep;
    }

    private static String clavePeriodo(LocalDate f, boolean semana) {
        return semana ? f.get(IsoFields.WEEK_BASED_YEAR) + "-S" + String.format("%02d", f.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR))
                : YearMonth.from(f).toString();
    }

    // ------------------------------------------------------------------ R-5 Planes (CU-34)

    @Transactional(readOnly = true)
    public ReporteDto planes(LocalDate desde, LocalDate hasta, Long pacienteId, Long nutricionistaId, String estado, String modo) {
        Rango r = rango(desde, hasta);
        UsuarioPrincipal u = CurrentUser.get();
        Long nut = u.esAdmin() ? nutricionistaId : u.profesionalId();
        Set<Long> pacientes = pacientesPermitidos(pacienteId);
        Map<Long, String> pacs = nombresPacientes();
        Map<Long, String> profs = nombresProfesionales();
        List<PlanAlimentario> planes = planDao.creadosEntre(Fechas.inicioDelDia(r.desde()), Fechas.inicioDelDia(r.hasta().plusDays(1))).stream()
                .filter(x -> pacientes == null || pacientes.contains(x.getPacienteId()))
                .filter(x -> nut == null || x.getNutricionistaId().equals(nut))
                .filter(x -> estado == null || estado.isBlank() || x.getEstado().name().equals(estado))
                .filter(x -> modo == null || modo.isBlank() || x.getModo().name().equals(modo))
                .toList();
        ReporteDto rep = ReporteDto.nuevo("planes", "Reporte de planes alimentarios");
        filtros(rep, r);
        rep.tabla().con("Paciente", "Nutricionista", "Plan", "Asignado", "Vigencia", "Estado", "Modo", "% comidas realizadas",
                "Reemplazos", "Comentarios", "Hambre prom.", "Saciedad prom.");
        for (PlanAlimentario p : planes) {
            LocalDate d = p.getFechaInicio().isAfter(r.desde()) ? p.getFechaInicio() : r.desde();
            LocalDate h = p.getFechaFin().isBefore(r.hasta()) ? p.getFechaFin() : r.hasta();
            Set<Long> comidas = p.getComidas().stream().map(c -> c.getId()).collect(Collectors.toSet());
            long programadas = 0;
            for (LocalDate f = d; !f.isAfter(h); f = f.plusDays(1)) {
                int dia = f.getDayOfWeek().getValue();
                programadas += p.comidasActivas().stream().filter(c -> c.getDiaSemana() == dia).count();
            }
            long hechas = comidas.isEmpty() ? 0 : regComDao.findByComidaPlanIdIn(comidas).stream()
                    .filter(x -> x.isRealizada() && !x.getFecha().isBefore(d) && !x.getFecha().isAfter(h)).count();
            Set<Long> items = p.getComidas().stream().flatMap(c -> c.getItems().stream()).map(i -> i.getId()).collect(Collectors.toSet());
            long reemplazos = items.isEmpty() ? 0 : repAlDao.findByItemComidaIdIn(items).size();
            long comentarios = comentarioDao.findByPacienteIdOrderByCreatedAtDesc(p.getPacienteId()).stream()
                    .filter(c -> p.getId().equals(c.getPlanId())).count();
            List<ObservacionDiaria> obs = obsDao.findByPacienteIdAndFechaBetweenOrderByFechaAsc(p.getPacienteId(), d, h);
            Double hambre = obs.isEmpty() ? null : Math.round(obs.stream().mapToInt(ObservacionDiaria::getNivelHambre).average().orElse(0) * 10) / 10.0;
            Double saciedad = obs.isEmpty() ? null : Math.round(obs.stream().mapToInt(ObservacionDiaria::getNivelSaciedad).average().orElse(0) * 10) / 10.0;
            rep.tabla().fila(pacs.get(p.getPacienteId()), profs.get(p.getNutricionistaId()), p.getNombre(), Fechas.fecha(Fechas.fecha(p.getCreatedAt())),
                    Fechas.fecha(p.getFechaInicio()) + " – " + Fechas.fecha(p.getFechaFin()), p.getEstado().name(), p.getModo().name(),
                    programadas == 0 || d.isAfter(h) ? "-" : pct(hechas, programadas), reemplazos, comentarios, hambre, saciedad);
        }
        rep.resumen().put("Planes", planes.size());
        rep.resumen().put("Automáticos", planes.stream().filter(p -> p.getModo() == PlanAlimentario.Modo.AUTOMATICO).count());
        rep.resumen().put("Activos", planes.stream().filter(p -> p.getEstado() == PlanAlimentario.Estado.ACTIVO).count());
        return rep;
    }

    // ------------------------------------------------------------------

    private static void filtros(ReporteDto rep, Rango r) {
        rep.filtros().put("Desde", Fechas.fecha(r.desde()));
        rep.filtros().put("Hasta", Fechas.fecha(r.hasta()));
    }

    private static String pct(long a, long b) {
        return b == 0 ? "0 %" : (Math.round(a * 1000.0 / b) / 10.0) + " %";
    }

    @SuppressWarnings("unused")
    private static <T> Map<Long, T> porId(List<T> l, Function<T, Long> id) {
        return l.stream().collect(Collectors.toMap(id, Function.identity()));
    }

    @SuppressWarnings("unused")
    private static List<DiaRutina> dias(Rutina r) {
        return r.diasActivos();
    }
}
