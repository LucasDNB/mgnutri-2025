package ar.edu.usal.mgnutri.reportes.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resultado genérico de un reporte: se muestra en pantalla (JSON) o se exporta a PDF/Excel.
 * {@code secciones} permite tablas adicionales (p. ej. deudas pendientes en el reporte de pagos).
 */
public record ReporteDto(String tipo, String titulo, Map<String, String> filtros, Map<String, Object> resumen,
                         Tabla tabla, List<Tabla> secciones, Map<String, Grafico> graficos) {

    public static ReporteDto nuevo(String tipo, String titulo) {
        return new ReporteDto(tipo, titulo, new LinkedHashMap<>(), new LinkedHashMap<>(), new Tabla("Detalle", new ArrayList<>(), new ArrayList<>()),
                new ArrayList<>(), new LinkedHashMap<>());
    }

    public boolean vacio() {
        return tabla.filas().isEmpty() && secciones.stream().allMatch(s -> s.filas().isEmpty());
    }

    public record Tabla(String titulo, List<String> columnas, List<List<Object>> filas) {
        public Tabla con(String... cols) {
            columnas.addAll(List.of(cols));
            return this;
        }

        public void fila(Object... valores) {
            filas.add(new ArrayList<>(java.util.Arrays.asList(valores)));
        }
    }

    /** Gráfico listo para Chart.js y JFreeChart: tipo "line" o "bar". */
    public record Grafico(String titulo, String tipo, List<String> labels, List<Serie> series) {}

    public record Serie(String nombre, List<Number> datos) {}
}
