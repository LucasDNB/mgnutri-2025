package ar.edu.usal.mgnutri.reportes.service;

import ar.edu.usal.mgnutri.common.Fechas;
import ar.edu.usal.mgnutri.reportes.dto.ReporteDto;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Plantilla común de PDF: encabezado del centro, pie con número de página (CA-12.0.6). */
public final class PdfLayout {

    public static final Color VERDE = new Color(0x2E, 0x5E, 0x3A);
    public static final Color VERDE_CLARO = new Color(0xE3, 0xEC, 0xDD);
    public static final Font TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, VERDE);
    public static final Font SUBTITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, VERDE);
    public static final Font NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 9);
    public static final Font NEGRITA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
    public static final Font CHICA = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, Color.DARK_GRAY);
    public static final Font ENCABEZADO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, Color.WHITE);
    public static final Font ENLACE = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, Font.UNDERLINE, new Color(0x1A, 0x5D, 0xAB));

    private PdfLayout() {
    }

    /** Crea un PDF A4 (apaisado si se pide) con encabezado y pie, y deja que el llamador agregue contenido. */
    public static byte[] documento(boolean apaisado, String centro, String titulo, String emisor,
                                   Consumer<Document> contenido) {
        Rectangle tam = apaisado ? PageSize.A4.rotate() : PageSize.A4;
        Document doc = new Document(tam, 32, 32, 70, 40);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new Marco(centro, titulo, emisor));
            doc.addTitle(titulo);
            doc.addCreator("MG Nutrición y Entrenamiento");
            doc.open();
            contenido.accept(doc);
            doc.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo generar el PDF", e);
        }
        return out.toByteArray();
    }

    public static void agregar(Document doc, Element e) {
        try {
            doc.add(e);
        } catch (DocumentException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static Paragraph parrafo(String texto, Font font) {
        Paragraph p = new Paragraph(texto == null ? "" : texto, font);
        p.setSpacingAfter(4);
        return p;
    }

    public static void filtros(Document doc, Map<String, String> filtros) {
        if (filtros == null || filtros.isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        filtros.forEach((k, v) -> sb.append(k).append(": ").append(v).append("   "));
        agregar(doc, parrafo(sb.toString().trim(), CHICA));
    }

    public static PdfPTable tabla(List<String> columnas, List<List<Object>> filas) {
        PdfPTable t = new PdfPTable(Math.max(1, columnas.size()));
        t.setWidthPercentage(100);
        t.setHeaderRows(1);
        t.setSpacingBefore(4);
        t.setSpacingAfter(8);
        for (String c : columnas) {
            PdfPCell cell = new PdfPCell(new Phrase(c, ENCABEZADO));
            cell.setBackgroundColor(VERDE);
            cell.setPadding(4);
            t.addCell(cell);
        }
        boolean alternar = false;
        for (List<Object> fila : filas) {
            for (int i = 0; i < columnas.size(); i++) {
                Object v = i < fila.size() ? fila.get(i) : null;
                PdfPCell cell = new PdfPCell(new Phrase(texto(v), NORMAL));
                cell.setPadding(3);
                if (v instanceof Number) {
                    cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                }
                if (alternar) {
                    cell.setBackgroundColor(VERDE_CLARO);
                }
                t.addCell(cell);
            }
            alternar = !alternar;
        }
        return t;
    }

    public static String texto(Object v) {
        if (v == null) {
            return "";
        }
        if (v instanceof BigDecimal bd) {
            return NumberFormat.getCurrencyInstance(Fechas.ES_AR).format(bd);
        }
        if (v instanceof Double || v instanceof Float) {
            NumberFormat nf = NumberFormat.getNumberInstance(Fechas.ES_AR);
            nf.setMaximumFractionDigits(1);
            return nf.format(((Number) v).doubleValue());
        }
        if (v instanceof LocalDate d) {
            return Fechas.fecha(d);
        }
        if (v instanceof Instant i) {
            return Fechas.fechaHora(i);
        }
        if (v instanceof Boolean b) {
            return b ? "Sí" : "No";
        }
        return String.valueOf(v);
    }

    /** Gráfico de líneas o barras con JFreeChart embebido como imagen. */
    public static Image grafico(ReporteDto.Grafico g, int ancho, int alto) {
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        for (ReporteDto.Serie s : g.series()) {
            for (int i = 0; i < g.labels().size() && i < s.datos().size(); i++) {
                Number n = s.datos().get(i);
                ds.addValue(n == null ? null : n.doubleValue(), s.nombre(), g.labels().get(i));
            }
        }
        JFreeChart chart = "bar".equals(g.tipo())
                ? ChartFactory.createBarChart(g.titulo(), null, null, ds, PlotOrientation.VERTICAL, true, false, false)
                : ChartFactory.createLineChart(g.titulo(), null, null, ds, PlotOrientation.VERTICAL, true, false, false);
        chart.setBackgroundPaint(Color.WHITE);
        chart.getCategoryPlot().setBackgroundPaint(Color.WHITE);
        chart.getCategoryPlot().setRangeGridlinePaint(Color.LIGHT_GRAY);
        chart.getCategoryPlot().getRenderer().setSeriesPaint(0, VERDE);
        chart.getCategoryPlot().getRenderer().setSeriesPaint(1, new Color(0x7F, 0xA3, 0x6B));
        chart.getCategoryPlot().getRenderer().setSeriesPaint(2, new Color(0xC9, 0x8B, 0x2B));
        try {
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            ChartUtils.writeChartAsPNG(png, chart, ancho * 2, alto * 2);
            Image img = Image.getInstance(png.toByteArray());
            img.scaleToFit(ancho, alto);
            img.setAlignment(Element.ALIGN_CENTER);
            return img;
        } catch (IOException | DocumentException e) {
            throw new IllegalStateException("No se pudo dibujar el gráfico", e);
        }
    }

    private static final class Marco extends PdfPageEventHelper {
        private final String centro;
        private final String titulo;
        private final String emisor;
        private final String emision;

        Marco(String centro, String titulo, String emisor) {
            this.centro = centro;
            this.titulo = titulo;
            this.emisor = emisor;
            this.emision = Fechas.fechaHora(Instant.now());
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            Rectangle p = document.getPageSize();
            var cb = writer.getDirectContent();
            cb.setColorFill(VERDE);
            cb.circle(document.left() + 14, p.getTop() - 34, 14);
            cb.fill();
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                    new Phrase("MG", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE)),
                    document.left() + 14, p.getTop() - 38, 0);
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, new Phrase(centro, SUBTITULO),
                    document.left() + 34, p.getTop() - 30, 0);
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, new Phrase(titulo, NORMAL),
                    document.left() + 34, p.getTop() - 43, 0);
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                    new Phrase("Emitido: " + emision + (emisor == null ? "" : " · " + emisor), CHICA),
                    document.right(), p.getTop() - 30, 0);
            cb.setColorStroke(VERDE);
            cb.moveTo(document.left(), p.getTop() - 52);
            cb.lineTo(document.right(), p.getTop() - 52);
            cb.stroke();
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                    new Phrase("Página " + writer.getPageNumber(), CHICA), (document.left() + document.right()) / 2,
                    document.bottom() - 20, 0);
        }
    }

    public static Chunk espacio() {
        return Chunk.NEWLINE;
    }
}
