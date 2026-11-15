package ar.edu.usal.mgnutri.nutricion.service;

import ar.edu.usal.mgnutri.common.Fechas;
import ar.edu.usal.mgnutri.configuracion.service.ConfiguracionService;
import ar.edu.usal.mgnutri.nutricion.domain.TipoComida;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.ComidaResponse;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.ItemResponse;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.PlanResponse;
import ar.edu.usal.mgnutri.reportes.service.PdfLayout;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** PDF del plan alimentario: A4 apaisado, grilla semanal de 8 columnas (CA-04.5.2). */
@Component
public class PlanAlimentarioPdf {

    private final ConfiguracionService config;

    public PlanAlimentarioPdf(ConfiguracionService config) {
        this.config = config;
    }

    public byte[] generar(PlanResponse plan) {
        String centro = config.valor("centro.nombre", "MG Nutrición y Entrenamiento");
        return PdfLayout.documento(true, centro, "Plan alimentario · " + plan.paciente().nombre(), null, doc -> {
            PdfLayout.agregar(doc, PdfLayout.parrafo(plan.nombre(), PdfLayout.TITULO));
            String prof = plan.nutricionista() == null ? "" : plan.nutricionista().nombre()
                    + (plan.nutricionista().matricula() == null ? "" : " · Mat. " + plan.nutricionista().matricula());
            PdfLayout.agregar(doc, PdfLayout.parrafo("Paciente: " + plan.paciente().nombre() + "    Profesional: " + prof
                    + "    Vigencia: " + Fechas.fecha(plan.fechaInicio()) + " al " + Fechas.fecha(plan.fechaFin()), PdfLayout.NORMAL));
            List<TipoComida> tipos = plan.comidas().stream().map(ComidaResponse::tipo).distinct()
                    .sorted(Comparator.comparingInt(TipoComida::orden)).toList();
            Map<String, ComidaResponse> celdas = plan.comidas().stream()
                    .collect(Collectors.toMap(c -> c.diaSemana() + "-" + c.tipo(), c -> c, (a, b) -> a));
            PdfPTable t = new PdfPTable(8);
            t.setWidthPercentage(100);
            t.setHeaderRows(1);
            t.setSpacingBefore(6);
            try {
                t.setWidths(new float[]{1.1f, 1.6f, 1.6f, 1.6f, 1.6f, 1.6f, 1.6f, 1.6f});
            } catch (com.lowagie.text.DocumentException e) {
                throw new IllegalStateException(e);
            }
            encabezado(t, "Comida");
            for (int d = 1; d <= 7; d++) {
                encabezado(t, ValidadorPlan.DIAS[d]);
            }
            boolean alt = false;
            for (TipoComida tipo : tipos) {
                PdfPCell c0 = new PdfPCell(new Phrase(tipo.etiqueta(), PdfLayout.NEGRITA));
                c0.setPadding(4);
                c0.setBackgroundColor(PdfLayout.VERDE_CLARO);
                t.addCell(c0);
                for (int d = 1; d <= 7; d++) {
                    ComidaResponse c = celdas.get(d + "-" + tipo);
                    PdfPCell cell = new PdfPCell(new Phrase(texto(c), PdfLayout.CHICA));
                    cell.setPadding(3);
                    if (alt) {
                        cell.setBackgroundColor(new java.awt.Color(0xF6, 0xF8, 0xF3));
                    }
                    t.addCell(cell);
                }
                alt = !alt;
            }
            PdfLayout.agregar(doc, t);
            if (plan.observaciones() != null) {
                PdfLayout.agregar(doc, PdfLayout.parrafo("Observaciones: " + plan.observaciones(), PdfLayout.NORMAL));
            }
            PdfLayout.agregar(doc, PdfLayout.parrafo("Este plan fue elaborado por un profesional del centro. No reemplaza la consulta médica.",
                    PdfLayout.CHICA));
        });
    }

    private static void encabezado(PdfPTable t, String texto) {
        PdfPCell c = new PdfPCell(new Phrase(texto, PdfLayout.ENCABEZADO));
        c.setBackgroundColor(PdfLayout.VERDE);
        c.setPadding(4);
        t.addCell(c);
    }

    static String texto(ComidaResponse c) {
        if (c == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        boolean hayReemplazo = c.items().stream().anyMatch(i -> i.reemplazo() != null);
        if (c.descripcion() != null && !hayReemplazo) {
            sb.append(c.descripcion());
        }
        if (hayReemplazo || (c.descripcion() == null && !c.items().isEmpty())) {
            for (ItemResponse i : c.items()) {
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                if (i.reemplazo() != null) {
                    sb.append("• ").append(i.reemplazo().alimento()).append(" ").append(i.reemplazo().gramos().stripTrailingZeros().toPlainString())
                            .append(" g (reemplazo)");
                } else {
                    sb.append("• ").append(i.alimento()).append(" ").append(i.gramos().stripTrailingZeros().toPlainString()).append(" g");
                }
            }
        }
        if (c.totales() != null && c.totales().kcal() > 0) {
            sb.append("\n≈ ").append((long) c.totales().kcal()).append(" kcal");
        }
        return sb.toString();
    }
}
