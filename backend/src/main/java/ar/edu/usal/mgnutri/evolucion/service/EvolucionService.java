package ar.edu.usal.mgnutri.evolucion.service;

import ar.edu.usal.mgnutri.bitacora.domain.Modulo;
import ar.edu.usal.mgnutri.bitacora.domain.TipoEvento;
import ar.edu.usal.mgnutri.bitacora.service.Auditable;
import ar.edu.usal.mgnutri.catalogo.dao.EjercicioDao;
import ar.edu.usal.mgnutri.common.Area;
import ar.edu.usal.mgnutri.common.Textos;
import ar.edu.usal.mgnutri.common.error.BusinessException;
import ar.edu.usal.mgnutri.common.error.ErrorCode;
import ar.edu.usal.mgnutri.common.security.AccesoPacienteGuard;
import ar.edu.usal.mgnutri.common.security.CurrentUser;
import ar.edu.usal.mgnutri.entrenamiento.dao.RegistroEntrenamientoDao;
import ar.edu.usal.mgnutri.entrenamiento.domain.RegistroEntrenamiento;
import ar.edu.usal.mgnutri.evolucion.dao.EvaluacionFisicaDao;
import ar.edu.usal.mgnutri.evolucion.dao.MedicionAntropometricaDao;
import ar.edu.usal.mgnutri.evolucion.domain.EvaluacionFisica;
import ar.edu.usal.mgnutri.evolucion.domain.MedicionAntropometrica;
import ar.edu.usal.mgnutri.evolucion.dto.EvolucionDtos.EvaluacionRequest;
import ar.edu.usal.mgnutri.evolucion.dto.EvolucionDtos.EvaluacionResponse;
import ar.edu.usal.mgnutri.evolucion.dto.EvolucionDtos.EvaluacionUpdateRequest;
import ar.edu.usal.mgnutri.evolucion.dto.EvolucionDtos.EvolucionResponse;
import ar.edu.usal.mgnutri.evolucion.dto.EvolucionDtos.FilaTabla;
import ar.edu.usal.mgnutri.evolucion.dto.EvolucionDtos.MedicionRequest;
import ar.edu.usal.mgnutri.evolucion.dto.EvolucionDtos.MedicionResponse;
import ar.edu.usal.mgnutri.evolucion.dto.EvolucionDtos.Punto;
import ar.edu.usal.mgnutri.evolucion.dto.EvolucionDtos.PruebaDto;
import ar.edu.usal.mgnutri.evolucion.dto.EvolucionDtos.SerieNombrada;
import ar.edu.usal.mgnutri.profesionales.dao.ProfesionalDao;
import ar.edu.usal.mgnutri.profesionales.domain.Profesional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Evolución del paciente: antropometría, desempeño físico y series para gráficos (spec 06). */
@Service
public class EvolucionService {

    static final Duration VENTANA_EDICION = Duration.ofHours(48);

    private final MedicionAntropometricaDao medicionDao;
    private final EvaluacionFisicaDao evaluacionDao;
    private final RegistroEntrenamientoDao registroDao;
    private final EjercicioDao ejercicioDao;
    private final ProfesionalDao profesionalDao;
    private final AccesoPacienteGuard guard;
    private final Clock clock;

    public EvolucionService(MedicionAntropometricaDao medicionDao, EvaluacionFisicaDao evaluacionDao,
                            RegistroEntrenamientoDao registroDao, EjercicioDao ejercicioDao, ProfesionalDao profesionalDao,
                            AccesoPacienteGuard guard, Clock clock) {
        this.medicionDao = medicionDao;
        this.evaluacionDao = evaluacionDao;
        this.registroDao = registroDao;
        this.ejercicioDao = ejercicioDao;
        this.profesionalDao = profesionalDao;
        this.guard = guard;
        this.clock = clock;
    }

    public static BigDecimal imc(BigDecimal peso, BigDecimal tallaCm) {
        if (peso == null || tallaCm == null || tallaCm.signum() <= 0) {
            return null;
        }
        BigDecimal m = tallaCm.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_EVEN);
        return peso.divide(m.multiply(m), 1, RoundingMode.HALF_EVEN);
    }

    // ---------------------------------------------------------------- antropometría

    @Transactional(readOnly = true)
    public List<MedicionResponse> mediciones(Long pacienteId, LocalDate desde, LocalDate hasta) {
        guard.assertPuedeVer(pacienteId);
        return medicionDao.findByPacienteIdAndFechaBetweenOrderByFechaAsc(pacienteId, desde == null ? LocalDate.of(2000, 1, 1) : desde,
                hasta == null ? LocalDate.now(clock) : hasta).stream().map(m -> aDto(m, List.of())).toList();
    }

    @Transactional
    @Auditable(modulo = Modulo.EVOLUCION, tipo = TipoEvento.ALTA, entidad = "MedicionAntropometrica", entidadId = "#result.id()",
            descripcion = "Medición antropométrica del paciente #{#pacienteId}")
    public MedicionResponse crearMedicion(Long pacienteId, MedicionRequest req) {
        guard.assertPuedeEditar(pacienteId, Area.NUTRICION);
        validarMedicion(req);
        Long prof = CurrentUser.profesionalId();
        if (medicionDao.findByPacienteIdAndProfesionalIdAndFecha(pacienteId, prof, req.fecha()).isPresent()) {
            throw new BusinessException(ErrorCode.DUPLICADO, "Ya cargaste una medición en esa fecha. Podés editarla.");
        }
        MedicionAntropometrica m = new MedicionAntropometrica();
        m.setPacienteId(pacienteId);
        m.setProfesionalId(prof);
        aplicar(m, req, pacienteId);
        medicionDao.saveAndFlush(m);
        return aDto(m, advertencias(m));
    }

    @Transactional
    @Auditable(modulo = Modulo.EVOLUCION, tipo = TipoEvento.MODIFICACION, entidad = "MedicionAntropometrica", entidadId = "#id",
            descripcion = "Edición de medición antropométrica")
    public MedicionResponse modificarMedicion(Long id, MedicionRequest req) {
        MedicionAntropometrica m = medicionDao.findById(id).orElseThrow(() -> BusinessException.noEncontrado("Medición"));
        verificarAutor(m.getProfesionalId(), m.getCreatedAt());
        validarMedicion(req);
        aplicar(m, req, m.getPacienteId());
        medicionDao.saveAndFlush(m);
        return aDto(m, advertencias(m));
    }

    @Transactional
    @Auditable(modulo = Modulo.EVOLUCION, tipo = TipoEvento.BAJA, entidad = "MedicionAntropometrica", entidadId = "#id",
            descripcion = "Eliminación de medición antropométrica")
    public void eliminarMedicion(Long id) {
        MedicionAntropometrica m = medicionDao.findById(id).orElseThrow(() -> BusinessException.noEncontrado("Medición"));
        verificarAutor(m.getProfesionalId(), m.getCreatedAt());
        medicionDao.delete(m);
    }

    private void validarMedicion(MedicionRequest r) {
        LocalDate hoy = LocalDate.now(clock);
        if (r.fecha().isAfter(hoy) || r.fecha().isBefore(hoy.minusYears(5))) {
            throw new BusinessException(ErrorCode.VALOR_FUERA_DE_RANGO, "La fecha no puede ser futura ni anterior a 5 años.");
        }
        if (r.pctGrasa() != null && r.pctMusculo() != null && r.pctGrasa().add(r.pctMusculo()).compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BusinessException(ErrorCode.VALOR_FUERA_DE_RANGO, "% grasa + % músculo no puede superar 100.");
        }
    }

    private void aplicar(MedicionAntropometrica m, MedicionRequest r, Long pacienteId) {
        BigDecimal talla = r.tallaCm();
        if (talla == null) {
            talla = medicionDao.findFirstByPacienteIdOrderByFechaDescIdDesc(pacienteId).map(MedicionAntropometrica::getTallaCm).orElse(null);
        }
        m.setFecha(r.fecha());
        m.setPesoKg(r.pesoKg());
        m.setTallaCm(talla);
        m.setImc(imc(r.pesoKg(), talla));
        m.setPctGrasa(r.pctGrasa());
        m.setPctMusculo(r.pctMusculo());
        m.setCircCintura(r.circCintura());
        m.setCircCadera(r.circCadera());
        m.setCircBrazo(r.circBrazo());
        m.setCircMuslo(r.circMuslo());
        m.setCircPecho(r.circPecho());
        m.setCircPantorrilla(r.circPantorrilla());
        m.setObservaciones(Textos.vacioANull(r.observaciones()));
    }

    /** CA-06.1.4: variación de peso > 10 % respecto de la anterior. */
    private List<String> advertencias(MedicionAntropometrica m) {
        return medicionDao.findFirstByPacienteIdAndFechaBeforeOrderByFechaDesc(m.getPacienteId(), m.getFecha())
                .filter(a -> a.getPesoKg().subtract(m.getPesoKg()).abs().doubleValue() / a.getPesoKg().doubleValue() > 0.10)
                .map(a -> List.of("Variación inusual de peso respecto de la medición anterior, verificá el dato.")).orElse(List.of());
    }

    // ---------------------------------------------------------------- evaluaciones físicas

    @Transactional(readOnly = true)
    public List<EvaluacionResponse> evaluaciones(Long pacienteId, LocalDate desde, LocalDate hasta, String prueba) {
        guard.assertPuedeVer(pacienteId);
        return evaluacionDao.findByPacienteIdAndFechaBetweenOrderByFechaAsc(pacienteId, desde == null ? LocalDate.of(2000, 1, 1) : desde,
                        hasta == null ? LocalDate.now(clock) : hasta).stream()
                .filter(e -> prueba == null || prueba.isBlank() || e.getPrueba().equalsIgnoreCase(prueba)).map(this::aDto).toList();
    }

    @Transactional
    @Auditable(modulo = Modulo.EVOLUCION, tipo = TipoEvento.ALTA, entidad = "EvaluacionFisica", entidadId = "#pacienteId",
            descripcion = "Evaluación física del paciente #{#pacienteId}")
    public List<EvaluacionResponse> crearEvaluaciones(Long pacienteId, EvaluacionRequest req) {
        guard.assertPuedeEditar(pacienteId, Area.ENTRENAMIENTO);
        validarFecha(req.fecha());
        List<EvaluacionResponse> r = new ArrayList<>();
        for (PruebaDto p : req.pruebas()) {
            EvaluacionFisica e = new EvaluacionFisica();
            e.setPacienteId(pacienteId);
            e.setProfesionalId(CurrentUser.profesionalId());
            aplicar(e, req.fecha(), p);
            evaluacionDao.save(e);
            r.add(aDto(e));
        }
        evaluacionDao.flush();
        return r;
    }

    @Transactional
    @Auditable(modulo = Modulo.EVOLUCION, tipo = TipoEvento.MODIFICACION, entidad = "EvaluacionFisica", entidadId = "#id",
            descripcion = "Edición de evaluación física")
    public EvaluacionResponse modificarEvaluacion(Long id, EvaluacionUpdateRequest req) {
        EvaluacionFisica e = evaluacionDao.findById(id).orElseThrow(() -> BusinessException.noEncontrado("Evaluación"));
        verificarAutor(e.getProfesionalId(), e.getCreatedAt());
        validarFecha(req.fecha());
        aplicar(e, req.fecha(), req.prueba());
        evaluacionDao.saveAndFlush(e);
        return aDto(e);
    }

    @Transactional
    @Auditable(modulo = Modulo.EVOLUCION, tipo = TipoEvento.BAJA, entidad = "EvaluacionFisica", entidadId = "#id",
            descripcion = "Eliminación de evaluación física")
    public void eliminarEvaluacion(Long id) {
        EvaluacionFisica e = evaluacionDao.findById(id).orElseThrow(() -> BusinessException.noEncontrado("Evaluación"));
        verificarAutor(e.getProfesionalId(), e.getCreatedAt());
        evaluacionDao.delete(e);
    }

    private void aplicar(EvaluacionFisica e, LocalDate fecha, PruebaDto p) {
        if (p.ejercicioId() != null && !ejercicioDao.existsById(p.ejercicioId())) {
            throw BusinessException.validacion("ejercicioId", "Ejercicio inexistente.");
        }
        e.setFecha(fecha);
        e.setPrueba(p.prueba().trim());
        e.setEjercicioId(p.ejercicioId());
        e.setValor(p.valor());
        e.setUnidad(p.unidad().trim());
        e.setObservaciones(Textos.vacioANull(p.observaciones()));
    }

    private void validarFecha(LocalDate f) {
        LocalDate hoy = LocalDate.now(clock);
        if (f.isAfter(hoy) || f.isBefore(hoy.minusYears(5))) {
            throw new BusinessException(ErrorCode.VALOR_FUERA_DE_RANGO, "La fecha no puede ser futura ni anterior a 5 años.");
        }
    }

    /** CA-06.1.6: el mismo profesional, dentro de las 48 h. */
    private void verificarAutor(Long profesionalId, Instant creado) {
        Long yo = CurrentUser.get().profesionalId();
        if (yo == null || !yo.equals(profesionalId)) {
            throw BusinessException.sinPermiso();
        }
        if (creado != null && Duration.between(creado, clock.instant()).compareTo(VENTANA_EDICION) > 0) {
            throw new BusinessException(ErrorCode.REGLA_NEGOCIO, "Pasaron más de 48 h desde la carga: la medición ya no se puede modificar.");
        }
    }

    // ---------------------------------------------------------------- series para gráficos (CU-20)

    @Transactional(readOnly = true)
    public EvolucionResponse evolucion(Long pacienteId, LocalDate desde, LocalDate hasta) {
        guard.assertPuedeVer(pacienteId);
        LocalDate d = desde == null ? LocalDate.of(2000, 1, 1) : desde;
        LocalDate h = hasta == null ? LocalDate.now(clock) : hasta;
        List<MedicionAntropometrica> ms = medicionDao.findByPacienteIdAndFechaBetweenOrderByFechaAsc(pacienteId, d, h);
        List<EvaluacionFisica> evs = evaluacionDao.findByPacienteIdAndFechaBetweenOrderByFechaAsc(pacienteId, d, h);
        List<RegistroEntrenamiento> regs = registroDao.findByPacienteIdAndFechaBetweenOrderByFechaAsc(pacienteId, d, h);

        Map<String, List<Punto>> composicion = new LinkedHashMap<>();
        composicion.put("grasa", serie(ms, MedicionAntropometrica::getPctGrasa));
        composicion.put("musculo", serie(ms, MedicionAntropometrica::getPctMusculo));
        Map<String, List<Punto>> medidas = new LinkedHashMap<>();
        medidas.put("cintura", serie(ms, MedicionAntropometrica::getCircCintura));
        medidas.put("cadera", serie(ms, MedicionAntropometrica::getCircCadera));
        medidas.put("brazo", serie(ms, MedicionAntropometrica::getCircBrazo));
        medidas.put("muslo", serie(ms, MedicionAntropometrica::getCircMuslo));
        medidas.put("pecho", serie(ms, MedicionAntropometrica::getCircPecho));
        medidas.put("pantorrilla", serie(ms, MedicionAntropometrica::getCircPantorrilla));

        List<SerieNombrada> desempeno = evs.stream().collect(Collectors.groupingBy(e -> e.getPrueba() + "|" + e.getUnidad(), TreeMap::new, Collectors.toList()))
                .entrySet().stream().map(en -> new SerieNombrada(en.getKey().split("\\|")[0], en.getKey().split("\\|")[1],
                        en.getValue().stream().map(e -> new Punto(e.getFecha(), e.getValor().doubleValue())).toList())).toList();

        Map<Long, String> nombres = new java.util.HashMap<>();
        List<SerieNombrada> cargas = regs.stream().filter(r -> r.getCargaKg() != null)
                .collect(Collectors.groupingBy(RegistroEntrenamiento::getEjercicioId)).entrySet().stream()
                .map(en -> {
                    Map<LocalDate, Double> maxPorFecha = new TreeMap<>();
                    en.getValue().forEach(r -> maxPorFecha.merge(r.getFecha(), r.getCargaKg().doubleValue(), Math::max));
                    String nombre = nombres.computeIfAbsent(en.getKey(), k -> ejercicioDao.findById(k).map(x -> x.getNombre()).orElse("Ejercicio"));
                    return new SerieNombrada(nombre, "kg", maxPorFecha.entrySet().stream().map(x -> new Punto(x.getKey(), x.getValue())).toList());
                }).sorted(Comparator.comparing(SerieNombrada::nombre)).toList();

        List<FilaTabla> tabla = new ArrayList<>();
        Double primero = ms.isEmpty() ? null : ms.get(0).getPesoKg().doubleValue();
        Double anterior = null;
        for (MedicionAntropometrica m : ms) {
            double p = m.getPesoKg().doubleValue();
            tabla.add(new FilaTabla(m.getFecha(), p, num(m.getImc()), num(m.getPctGrasa()), num(m.getPctMusculo()), num(m.getCircCintura()),
                    anterior == null ? null : r1(p - anterior), r1(p - primero), r1((p - primero) / primero * 100)));
            anterior = p;
        }
        return new EvolucionResponse(serie(ms, MedicionAntropometrica::getPesoKg), serie(ms, MedicionAntropometrica::getImc),
                composicion, medidas, desempeno, cargas, tabla, ms.isEmpty() && evs.isEmpty() && cargas.isEmpty());
    }

    @Transactional(readOnly = true)
    public EvolucionResponse miEvolucion(LocalDate desde, LocalDate hasta) {
        return evolucion(CurrentUser.pacienteId(), desde, hasta);
    }

    private static List<Punto> serie(List<MedicionAntropometrica> ms, Function<MedicionAntropometrica, BigDecimal> f) {
        return ms.stream().filter(m -> f.apply(m) != null).map(m -> new Punto(m.getFecha(), f.apply(m).doubleValue())).toList();
    }

    private static Double num(BigDecimal b) {
        return b == null ? null : b.doubleValue();
    }

    private static double r1(double v) {
        return Math.round(v * 10) / 10.0;
    }

    private boolean editable(Long profesionalId, Instant creado) {
        Long yo = CurrentUser.opcional().map(u -> u.profesionalId()).orElse(null);
        return yo != null && yo.equals(profesionalId) && creado != null
                && Duration.between(creado, clock.instant()).compareTo(VENTANA_EDICION) <= 0;
    }

    private String nombreProf(Long id) {
        return profesionalDao.findById(id).map(Profesional::nombreVisible).orElse("");
    }

    MedicionResponse aDto(MedicionAntropometrica m, List<String> adv) {
        return new MedicionResponse(m.getId(), m.getPacienteId(), m.getProfesionalId(), nombreProf(m.getProfesionalId()), m.getFecha(),
                m.getPesoKg(), m.getTallaCm(), m.getImc(), m.getPctGrasa(), m.getPctMusculo(), m.getCircCintura(), m.getCircCadera(),
                m.getCircBrazo(), m.getCircMuslo(), m.getCircPecho(), m.getCircPantorrilla(), m.getObservaciones(), m.getCreatedAt(),
                editable(m.getProfesionalId(), m.getCreatedAt()), adv);
    }

    EvaluacionResponse aDto(EvaluacionFisica e) {
        return new EvaluacionResponse(e.getId(), e.getPacienteId(), e.getProfesionalId(), nombreProf(e.getProfesionalId()), e.getFecha(),
                e.getPrueba(), e.getEjercicioId(), e.getValor(), e.getUnidad(), e.getObservaciones(), e.getCreatedAt(),
                editable(e.getProfesionalId(), e.getCreatedAt()));
    }
}
