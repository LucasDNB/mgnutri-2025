package ar.edu.usal.mgnutri.nutricion.service;

import ar.edu.usal.mgnutri.catalogo.dao.EquivalenciaManualDao;
import ar.edu.usal.mgnutri.catalogo.domain.Alimento;
import ar.edu.usal.mgnutri.catalogo.domain.EquivalenciaManual;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.Alternativa;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Equivalencias alimentarias (D-15): primero las manuales, luego las calculadas por macronutriente de
 * referencia del grupo de intercambio: gramosB = gramosA × refA / refB, redondeado a 5 g.
 */
@Service
public class EquivalenciaService {

    static final int MAXIMO = 10;
    private final EquivalenciaManualDao manualDao;

    public EquivalenciaService(EquivalenciaManualDao manualDao) {
        this.manualDao = manualDao;
    }

    public List<Alternativa> alternativas(Alimento original, double gramos, List<Alimento> catalogoActivo) {
        return calcular(original, gramos, catalogoActivo, manualDao.findByAlimentoId(original.getId()));
    }

    /** Algoritmo puro (testeable sin BD). */
    public static List<Alternativa> calcular(Alimento original, double gramos, List<Alimento> catalogo,
                                             List<EquivalenciaManual> manuales) {
        Map<Long, Alimento> porId = catalogo.stream().collect(Collectors.toMap(Alimento::getId, Function.identity(), (a, b) -> a));
        List<Alternativa> r = new ArrayList<>();
        Set<Long> usados = new HashSet<>();
        usados.add(original.getId());
        for (EquivalenciaManual m : manuales) {
            Alimento b = porId.get(m.getAlimentoEquivalenteId());
            if (b == null || !b.isActivo() || !usados.add(b.getId())) {
                continue;
            }
            double g = CalculadoraNutricional.redondear5(gramos * m.getGramosEquivalente().doubleValue() / m.getGramosOrigen().doubleValue());
            r.add(alt(b, g, "MANUAL"));
        }
        double refA = original.referenciaPorGramo();
        double kcalA = CalculadoraNutricional.macros(original, gramos).kcal();
        List<Alternativa> calculadas = new ArrayList<>();
        if (refA > 0) {
            for (Alimento b : catalogo) {
                if (!b.isActivo() || usados.contains(b.getId()) || !b.getGrupo().getId().equals(original.getGrupo().getId())) {
                    continue;
                }
                double refB = b.referenciaPorGramo();
                if (refB <= 0) {
                    continue;
                }
                double g = gramos * refA / refB;
                if (g > gramos * 3 || g < gramos / 3) {
                    continue; // proporciones poco prácticas
                }
                calculadas.add(alt(b, CalculadoraNutricional.redondear5(g), "CALCULADA"));
            }
        }
        calculadas.sort(Comparator.comparingDouble(a -> Math.abs(a.kcal() - kcalA)));
        r.addAll(calculadas);
        return r.stream().limit(MAXIMO).toList();
    }

    private static Alternativa alt(Alimento b, double gramos, String origen) {
        return new Alternativa(b.getId(), b.getNombre(), BigDecimal.valueOf(gramos), CalculadoraNutricional.medidaCasera(b, gramos),
                origen, Math.round(CalculadoraNutricional.macros(b, gramos).kcal()));
    }
}
