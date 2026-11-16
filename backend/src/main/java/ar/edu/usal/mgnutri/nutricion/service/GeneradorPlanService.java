package ar.edu.usal.mgnutri.nutricion.service;

import ar.edu.usal.mgnutri.catalogo.domain.Alimento;
import ar.edu.usal.mgnutri.catalogo.service.AlimentoService;
import ar.edu.usal.mgnutri.common.Area;
import ar.edu.usal.mgnutri.common.error.BusinessException;
import ar.edu.usal.mgnutri.common.security.AccesoPacienteGuard;
import ar.edu.usal.mgnutri.nutricion.domain.TipoComida;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.BorradorGenerado;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.ComidaParam;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.ComidaResponse;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.GenerarRequest;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.ItemResponse;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.Macros;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.TotalDia;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Generación automática de un BORRADOR de plan (CU-14, D-14). No persiste: el nutricionista
 * lo revisa, edita y confirma (constitución art. II).
 */
@Service
public class GeneradorPlanService {

    static final String LACTEOS = "Lácteos";
    static final String CARNES = "Carnes y huevos";
    static final String CEREALES = "Cereales, legumbres y derivados";
    static final String FRUTAS = "Frutas";
    static final String VERDURAS = "Verduras";
    static final String ACEITES = "Aceites y grasas";
    static final String FRUTOS_SECOS = "Frutos secos y semillas";

    private final AlimentoService alimentos;
    private final AccesoPacienteGuard guard;

    public GeneradorPlanService(AlimentoService alimentos, AccesoPacienteGuard guard) {
        this.alimentos = alimentos;
        this.guard = guard;
    }

    public BorradorGenerado generar(GenerarRequest req) {
        guard.assertPuedeEditar(req.pacienteId(), Area.NUTRICION);
        return generar(req, alimentos.activos());
    }

    /** Núcleo puro del algoritmo, testeable con un catálogo fijo. */
    public BorradorGenerado generar(GenerarRequest req, List<Alimento> catalogo) {
        double sumaMacros = req.pctHc() + req.pctProteinas() + req.pctGrasas();
        if (Math.abs(sumaMacros - 100) > 0.01) {
            throw BusinessException.validacion("pctHc", "Los porcentajes de HC, proteínas y grasas deben sumar 100.");
        }
        double sumaComidas = req.comidas().stream().mapToDouble(ComidaParam::pct).sum();
        if (Math.abs(sumaComidas - 100) > 0.01) {
            throw BusinessException.validacion("comidas", "La distribución calórica de las comidas debe sumar 100.");
        }
        Set<TipoComida> tipos = new HashSet<>();
        for (ComidaParam c : req.comidas()) {
            if (!tipos.add(c.tipo())) {
                throw BusinessException.validacion("comidas", "Hay tipos de comida repetidos.");
            }
        }
        long semilla = req.semilla() != null ? req.semilla() : System.nanoTime();
        Random random = new Random(semilla);
        Set<Long> excluidos = req.excluirAlimentoIds() == null ? Set.of() : new HashSet<>(req.excluirAlimentoIds());
        List<Alimento> disponibles = catalogo.stream().filter(Alimento::isActivo).filter(a -> !excluidos.contains(a.getId())).toList();
        boolean semanal = "SEMANAL".equalsIgnoreCase(req.variedad());
        List<ComidaParam> ordenadas = req.comidas().stream().sorted(Comparator.comparingInt(c -> c.tipo().orden())).toList();

        List<ComidaResponse> comidas = new ArrayList<>();
        List<TotalDia> totales = new ArrayList<>();
        Map<String, List<Long>> usados = new HashMap<>(); // tipo -> alimentos usados por día (para variedad)
        List<String> advertencias = new ArrayList<>();
        Macros objetivoDia = CalculadoraNutricional.objetivo(req.kcal(), req.pctHc(), req.pctProteinas(), req.pctGrasas());

        List<ComidaResponse> primerDia = null;
        for (int dia = 1; dia <= 7; dia++) {
            List<ComidaResponse> delDia = new ArrayList<>();
            if (semanal && primerDia != null) {
                for (ComidaResponse c : primerDia) {
                    delDia.add(new ComidaResponse(null, dia, c.tipo(), c.tipoEtiqueta(), c.descripcion(), c.items(), c.totales(), c.desvio()));
                }
            } else {
                for (ComidaParam cp : ordenadas) {
                    delDia.add(generarComida(dia, cp, req, disponibles, random, usados, advertencias));
                }
                if (semanal) {
                    primerDia = delDia;
                }
            }
            Macros total = delDia.stream().map(ComidaResponse::totales).reduce(Macros.CERO, Macros::mas);
            totales.add(new TotalDia(dia, Math.round(total.kcal()), r1(total.hc()), r1(total.proteinas()), r1(total.grasas()),
                    CalculadoraNutricional.desvio(total, objetivoDia)));
            comidas.addAll(delDia);
        }
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("kcal", req.kcal());
        params.put("pctHc", req.pctHc());
        params.put("pctProteinas", req.pctProteinas());
        params.put("pctGrasas", req.pctGrasas());
        params.put("comidas", req.comidas().stream().map(c -> c.tipo() + ":" + c.pct()).collect(Collectors.joining(",")));
        params.put("excluidos", excluidos.size());
        params.put("variedad", semanal ? "SEMANAL" : "DIARIA");
        params.put("semilla", semilla);
        return new BorradorGenerado(comidas, totales, semilla, params, advertencias.stream().distinct().toList());
    }

    private ComidaResponse generarComida(int dia, ComidaParam cp, GenerarRequest req, List<Alimento> disponibles, Random random,
                                         Map<String, List<Long>> usados, List<String> advertencias) {
        TipoComida tipo = cp.tipo();
        double kcalComida = req.kcal() * cp.pct() / 100.0;
        Macros obj = CalculadoraNutricional.objetivo(kcalComida, req.pctHc(), req.pctProteinas(), req.pctGrasas());
        Predicate<Alimento> apto = a -> tipo.esDesayunoOMerienda() ? a.isAptoDesayuno() : tipo.esColacion() ? a.isAptoColacion() : a.isAptoAlmuerzo();
        List<Long> recientes = usados.computeIfAbsent(tipo.name(), k -> new ArrayList<>());
        List<Elegido> items = new ArrayList<>();
        boolean faltante = false;

        if (tipo.esPrincipal()) {
            faltante |= !fijo(items, elegir(VERDURAS, disponibles, apto, recientes, random), 150);
            faltante |= !variable(items, elegir(CARNES, disponibles, apto, recientes, random), obj, Macro.P, 50, 250);
            faltante |= !variable(items, elegir(CEREALES, disponibles, apto, recientes, random), obj, Macro.HC, 20, 300);
            variable(items, elegir(ACEITES, disponibles, apto, recientes, random), obj, Macro.G, 0, 30);
        } else if (tipo.esDesayunoOMerienda()) {
            faltante |= !fijo(items, elegir(LACTEOS, disponibles, apto, recientes, random), 200);
            Alimento fruta = elegir(FRUTAS, disponibles, apto, recientes, random);
            faltante |= !fijo(items, fruta, fruta != null && fruta.getGramosUnidadCasera() != null ? fruta.getGramosUnidadCasera().doubleValue() : 150);
            faltante |= !variable(items, elegir(CEREALES, disponibles, apto, recientes, random), obj, Macro.HC, 20, 300);
            variable(items, elegir(FRUTOS_SECOS, disponibles, apto, recientes, random), obj, Macro.G, 0, 30);
        } else {
            // colación: fruta, lácteo o frutos secos, rotando por día
            String[] opciones = {FRUTAS, LACTEOS, FRUTOS_SECOS};
            String grupo = opciones[(dia + random.nextInt(3)) % 3];
            Alimento a = elegir(grupo, disponibles, apto, recientes, random);
            if (a == null) {
                a = elegir(FRUTAS, disponibles, apto, recientes, random);
            }
            if (a != null) {
                double g = a.getKcal().doubleValue() > 0 ? kcalComida / (a.getKcal().doubleValue() / 100.0) : 100;
                double max = FRUTOS_SECOS.equals(a.getGrupo().getNombre()) ? 40 : 300;
                items.add(new Elegido(a, CalculadoraNutricional.redondear5(Math.min(Math.max(g, 10), max)), false));
            } else {
                faltante = true;
            }
        }
        ajustarKcal(items, kcalComida);
        items.forEach(e -> recientes.add(e.alimento.getId()));
        while (recientes.size() > 8) {
            recientes.remove(0);
        }
        if (faltante) {
            advertencias.add("El catálogo no tiene alimentos suficientes para algunas comidas: completalas manualmente.");
        }
        List<ItemResponse> respuesta = new ArrayList<>();
        Macros total = Macros.CERO;
        for (Elegido e : items) {
            Macros m = CalculadoraNutricional.macros(e.alimento, e.gramos);
            total = total.mas(m);
            respuesta.add(new ItemResponse(null, e.alimento.getId(), e.alimento.getNombre(), e.alimento.getGrupo().getNombre(),
                    BigDecimal.valueOf(e.gramos), CalculadoraNutricional.medidaCasera(e.alimento, e.gramos), m.redondeado(), null));
        }
        String descripcion = items.isEmpty() ? "Completar manualmente" : items.stream()
                .map(e -> {
                    String casera = CalculadoraNutricional.medidaCasera(e.alimento, e.gramos);
                    return e.alimento.getNombre() + " " + (long) e.gramos + " g" + (casera == null ? "" : " (" + casera + ")");
                })
                .collect(Collectors.joining(" + "));
        return new ComidaResponse(null, dia, tipo, tipo.etiqueta(), descripcion, respuesta, total.redondeado(),
                CalculadoraNutricional.desvio(total, obj));
    }

    private enum Macro { HC, P, G }

    /** Escala las porciones variables para que la comida se acerque a las kcal objetivo. */
    private static void ajustarKcal(List<Elegido> items, double objetivo) {
        double fijas = items.stream().filter(e -> !e.variable).mapToDouble(e -> CalculadoraNutricional.macros(e.alimento, e.gramos).kcal()).sum();
        double variables = items.stream().filter(e -> e.variable).mapToDouble(e -> CalculadoraNutricional.macros(e.alimento, e.gramos).kcal()).sum();
        if (variables <= 0 || objetivo <= fijas) {
            return;
        }
        double factor = Math.min(2.0, Math.max(0.4, (objetivo - fijas) / variables));
        for (int i = 0; i < items.size(); i++) {
            Elegido e = items.get(i);
            if (e.variable) {
                items.set(i, new Elegido(e.alimento, CalculadoraNutricional.redondear5(e.gramos * factor), true));
            }
        }
    }

    private static boolean fijo(List<Elegido> items, Alimento a, double gramos) {
        if (a == null) {
            return false;
        }
        items.add(new Elegido(a, CalculadoraNutricional.redondear5(gramos), false));
        return true;
    }

    /** Porción variable para aproximar el objetivo del macro (proteína → hidratos → grasas). */
    private static boolean variable(List<Elegido> items, Alimento a, Macros obj, Macro macro, double min, double max) {
        if (a == null) {
            return false;
        }
        Macros actual = items.stream().map(e -> CalculadoraNutricional.macros(e.alimento, e.gramos)).reduce(Macros.CERO, Macros::mas);
        double faltante;
        double porGramo;
        switch (macro) {
            case P -> {
                faltante = obj.proteinas() - actual.proteinas();
                porGramo = a.getProteinas().doubleValue() / 100.0;
            }
            case HC -> {
                faltante = obj.hc() - actual.hc();
                porGramo = a.getHc().doubleValue() / 100.0;
            }
            default -> {
                faltante = obj.grasas() - actual.grasas();
                porGramo = a.getGrasas().doubleValue() / 100.0;
            }
        }
        double g = porGramo <= 0 ? min : faltante / porGramo;
        g = Math.min(Math.max(g, min), max);
        if (g < 5) {
            return true; // no hace falta agregarlo
        }
        items.add(new Elegido(a, CalculadoraNutricional.redondear5(g), true));
        return true;
    }

    private static Alimento elegir(String grupo, List<Alimento> disponibles, Predicate<Alimento> apto, List<Long> recientes,
                                   Random random) {
        List<Alimento> candidatos = disponibles.stream()
                .filter(a -> grupo.equals(a.getGrupo().getNombre()))
                .filter(apto)
                .sorted(Comparator.comparing(Alimento::getId))
                .toList();
        if (candidatos.isEmpty()) {
            return null;
        }
        List<Alimento> nuevos = candidatos.stream().filter(a -> !recientes.contains(a.getId())).toList();
        List<Alimento> pool = nuevos.isEmpty() ? candidatos : nuevos;
        return pool.get(random.nextInt(pool.size()));
    }

    private static double r1(double v) {
        return Math.round(v * 10) / 10.0;
    }

    private record Elegido(Alimento alimento, double gramos, boolean variable) {}
}
