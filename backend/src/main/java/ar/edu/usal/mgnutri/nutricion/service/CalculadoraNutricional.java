package ar.edu.usal.mgnutri.nutricion.service;

import ar.edu.usal.mgnutri.catalogo.domain.Alimento;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.Desvio;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.Macros;

/** Cálculos nutricionales puros (sin BD). */
public final class CalculadoraNutricional {

    private CalculadoraNutricional() {
    }

    public static Macros macros(Alimento a, double gramos) {
        double f = gramos / 100.0;
        return new Macros(a.getKcal().doubleValue() * f, a.getHc().doubleValue() * f,
                a.getProteinas().doubleValue() * f, a.getGrasas().doubleValue() * f);
    }

    /** Redondeo a múltiplos de 5 g (mínimo 5 g). */
    public static double redondear5(double gramos) {
        return Math.max(5, Math.round(gramos / 5.0) * 5);
    }

    /** "≈ n unidades" cuando el alimento tiene medida casera. */
    public static String medidaCasera(Alimento a, double gramos) {
        if (a.getUnidadCasera() == null || a.getGramosUnidadCasera() == null || a.getGramosUnidadCasera().doubleValue() <= 0) {
            return null;
        }
        double n = gramos / a.getGramosUnidadCasera().doubleValue();
        double redondeado = Math.round(n * 2) / 2.0;
        if (redondeado <= 0) {
            redondeado = 0.5;
        }
        String cantidad = redondeado == Math.floor(redondeado) ? String.valueOf((long) redondeado) : String.valueOf(redondeado).replace('.', ',');
        return "≈ " + cantidad + " × " + a.getUnidadCasera();
    }

    /** Desvío porcentual de kcal y macros respecto del objetivo (CA-04.2.2). */
    public static Desvio desvio(Macros real, Macros objetivo) {
        if (objetivo == null || objetivo.kcal() <= 0) {
            return null;
        }
        double dk = Math.abs(real.kcal() - objetivo.kcal()) / objetivo.kcal() * 100;
        double dm = Math.max(rel(real.hc(), objetivo.hc()), Math.max(rel(real.proteinas(), objetivo.proteinas()), rel(real.grasas(), objetivo.grasas())));
        String nivel = dk <= 10 && dm <= 15 ? "VERDE" : dk <= 20 && dm <= 30 ? "AMARILLO" : "ROJO";
        return new Desvio(Math.round(dk * 10) / 10.0, Math.round(dm * 10) / 10.0, nivel);
    }

    private static double rel(double real, double obj) {
        return obj <= 0 ? 0 : Math.abs(real - obj) / obj * 100;
    }

    /** Objetivo de macros para un total de kcal y porcentajes. */
    public static Macros objetivo(double kcal, double pctHc, double pctProt, double pctGrasas) {
        return new Macros(kcal, kcal * pctHc / 400.0, kcal * pctProt / 400.0, kcal * pctGrasas / 900.0);
    }
}
