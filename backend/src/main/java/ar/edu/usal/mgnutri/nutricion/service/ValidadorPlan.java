package ar.edu.usal.mgnutri.nutricion.service;

import ar.edu.usal.mgnutri.common.error.BusinessException;
import ar.edu.usal.mgnutri.common.error.ErrorCode;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.ComidaDto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Validaciones de plan (RN-04.3, RN-04.5, CA-04.1.6/7). */
public final class ValidadorPlan {

    public static final String[] DIAS = {"", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};

    private ValidadorPlan() {
    }

    /** Sin duplicados (día, tipo) — vale para borradores y planes confirmados. */
    public static void validarEstructura(List<ComidaDto> comidas) {
        Set<String> claves = new HashSet<>();
        for (ComidaDto c : comidas) {
            if (!claves.add(c.diaSemana() + "-" + c.tipo())) {
                throw BusinessException.validacion("comidas", "Hay dos " + c.tipo().etiqueta().toLowerCase() + " el " + DIAS[c.diaSemana()] + ".");
            }
        }
    }

    public static void validarFechas(LocalDate inicio, LocalDate hoy) {
        if (inicio.isBefore(hoy.minusDays(30))) {
            throw BusinessException.validacion("fechaInicio", "La fecha de inicio no puede ser anterior a 30 días atrás.");
        }
    }

    /** CA-04.1.6: al confirmar, cada día debe tener al menos 4 comidas con contenido. */
    public static void validarConfirmacion(List<ComidaDto> comidas) {
        Map<Integer, Integer> porDia = new HashMap<>();
        for (ComidaDto c : comidas) {
            boolean contenido = (c.descripcion() != null && !c.descripcion().isBlank()) || (c.items() != null && !c.items().isEmpty());
            if (contenido) {
                porDia.merge(c.diaSemana(), 1, Integer::sum);
            }
        }
        List<String> faltantes = new ArrayList<>();
        for (int d = 1; d <= 7; d++) {
            if (porDia.getOrDefault(d, 0) < 4) {
                faltantes.add(DIAS[d] + " (" + porDia.getOrDefault(d, 0) + ")");
            }
        }
        if (!faltantes.isEmpty()) {
            throw new BusinessException(ErrorCode.PLAN_MIN_4_COMIDAS,
                    "Cada día debe tener al menos 4 comidas con contenido. Revisá: " + String.join(", ", faltantes) + ".");
        }
    }
}
