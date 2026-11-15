package ar.edu.usal.mgnutri.reportes.service;

import ar.edu.usal.mgnutri.common.security.CurrentUser;
import ar.edu.usal.mgnutri.configuracion.service.ConfiguracionService;
import ar.edu.usal.mgnutri.reportes.dto.ReporteDto;
import com.lowagie.text.pdf.PdfPTable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Exportación de reportes a PDF con OpenPDF y gráficos JFreeChart (D-22). */
@Component
public class PdfReporteExporter implements ReporteExporter {

    private final ConfiguracionService config;

    public PdfReporteExporter(ConfiguracionService config) {
        this.config = config;
    }

    @Override
    public String formato() {
        return "pdf";
    }

    @Override
    public String contentType() {
        return "application/pdf";
    }

    @Override
    public String extension() {
        return "pdf";
    }

    @Override
    public byte[] exportar(ReporteDto r) {
        boolean apaisado = r.tabla().columnas().size() > 6;
        String emisor = CurrentUser.opcional().map(u -> u.username()).orElse(null);
        return PdfLayout.documento(apaisado, config.valor("centro.nombre", "MG Nutrición y Entrenamiento"), r.titulo(), emisor, doc -> {
            PdfLayout.agregar(doc, PdfLayout.parrafo(r.titulo(), PdfLayout.TITULO));
            PdfLayout.filtros(doc, r.filtros());
            if (r.vacio()) {
                PdfLayout.agregar(doc, PdfLayout.parrafo("No hay resultados para los filtros elegidos.", PdfLayout.NORMAL));
                return;
            }
            if (!r.resumen().isEmpty()) {
                PdfLayout.agregar(doc, PdfLayout.parrafo("Resumen", PdfLayout.SUBTITULO));
                List<List<Object>> filas = new ArrayList<>();
                r.resumen().forEach((k, v) -> filas.add(new ArrayList<>(java.util.Arrays.asList(k, v))));
                PdfPTable t = PdfLayout.tabla(List.of("Indicador", "Valor"), filas);
                t.setWidthPercentage(60);
                t.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_LEFT);
                PdfLayout.agregar(doc, t);
            }
            for (ReporteDto.Grafico g : r.graficos().values()) {
                if (!g.labels().isEmpty()) {
                    PdfLayout.agregar(doc, PdfLayout.grafico(g, apaisado ? 620 : 500, 220));
                }
            }
            if (!r.tabla().filas().isEmpty()) {
                PdfLayout.agregar(doc, PdfLayout.parrafo(r.tabla().titulo(), PdfLayout.SUBTITULO));
                PdfLayout.agregar(doc, PdfLayout.tabla(r.tabla().columnas(), r.tabla().filas()));
            }
            for (ReporteDto.Tabla s : r.secciones()) {
                PdfLayout.agregar(doc, PdfLayout.parrafo(s.titulo(), PdfLayout.SUBTITULO));
                if (s.filas().isEmpty()) {
                    PdfLayout.agregar(doc, PdfLayout.parrafo("Sin datos.", PdfLayout.NORMAL));
                } else {
                    PdfLayout.agregar(doc, PdfLayout.tabla(s.columnas(), s.filas()));
                }
            }
        });
    }
}
