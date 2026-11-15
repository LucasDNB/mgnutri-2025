package ar.edu.usal.mgnutri.reportes.service;

import ar.edu.usal.mgnutri.reportes.dto.ReporteDto;

/** Exportador de reportes (patrón Factory, D-02). */
public interface ReporteExporter {

    String formato();

    String contentType();

    String extension();

    byte[] exportar(ReporteDto reporte);
}
