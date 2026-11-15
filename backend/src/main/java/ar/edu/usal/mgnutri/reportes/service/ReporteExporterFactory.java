package ar.edu.usal.mgnutri.reportes.service;

import ar.edu.usal.mgnutri.common.error.BusinessException;
import ar.edu.usal.mgnutri.reportes.dto.ReporteDto;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Fábrica de exportadores PDF / Excel (D-02, D-21). */
@Component
public class ReporteExporterFactory {

    private final Map<String, ReporteExporter> exportadores;

    public ReporteExporterFactory(List<ReporteExporter> lista) {
        this.exportadores = lista.stream().collect(Collectors.toMap(ReporteExporter::formato, Function.identity()));
    }

    public ReporteExporter para(String formato) {
        ReporteExporter e = exportadores.get(formato == null ? "" : formato.toLowerCase(Locale.ROOT));
        if (e == null) {
            throw BusinessException.validacion("formato", "Formato no soportado: " + formato);
        }
        return e;
    }

    /** Respuesta HTTP de descarga del reporte en el formato pedido. */
    public ResponseEntity<byte[]> descargar(ReporteDto reporte, String formato, String nombreBase) {
        ReporteExporter e = para(formato);
        return archivo(e.exportar(reporte), e.contentType(), nombreBase + "." + e.extension());
    }

    public static ResponseEntity<byte[]> archivo(byte[] contenido, String contentType, String nombre) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(nombre, StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType(contentType))
                .body(contenido);
    }
}
