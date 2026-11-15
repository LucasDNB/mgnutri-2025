package ar.edu.usal.mgnutri.reportes.service;

import ar.edu.usal.mgnutri.common.Fechas;
import ar.edu.usal.mgnutri.reportes.dto.ReporteDto;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Map;

/** Exportación a Excel con Apache POI (CA-12.0.7): hoja de datos con encabezado congelado y hoja Resumen. */
@Component
public class ExcelReporteExporter implements ReporteExporter {

    @Override
    public String formato() {
        return "xlsx";
    }

    @Override
    public String contentType() {
        return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    }

    @Override
    public String extension() {
        return "xlsx";
    }

    @Override
    public byte[] exportar(ReporteDto r) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Estilos e = new Estilos(wb);
            hojaTabla(wb, e, nombreHoja(r.tabla().titulo(), "Datos"), r.tabla());
            for (ReporteDto.Tabla s : r.secciones()) {
                hojaTabla(wb, e, nombreHoja(s.titulo(), "Seccion"), s);
            }
            if (!r.resumen().isEmpty() || !r.filtros().isEmpty()) {
                Sheet res = wb.createSheet("Resumen");
                int fila = 0;
                Row t = res.createRow(fila++);
                celda(t, 0, r.titulo(), e.titulo);
                for (Map.Entry<String, String> f : r.filtros().entrySet()) {
                    Row row = res.createRow(fila++);
                    celda(row, 0, f.getKey(), e.encabezado);
                    celda(row, 1, f.getValue(), null);
                }
                fila++;
                for (Map.Entry<String, Object> f : r.resumen().entrySet()) {
                    Row row = res.createRow(fila++);
                    celda(row, 0, f.getKey(), e.encabezado);
                    valor(row.createCell(1), f.getValue(), e);
                }
                res.setColumnWidth(0, 40 * 256);
                res.setColumnWidth(1, 30 * 256);
            }
            wb.write(out);
            wb.dispose();
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo generar el Excel", ex);
        }
    }

    private void hojaTabla(SXSSFWorkbook wb, Estilos e, String nombre, ReporteDto.Tabla tabla) {
        SXSSFSheet hoja = wb.createSheet(nombre);
        hoja.trackAllColumnsForAutoSizing();
        Row enc = hoja.createRow(0);
        for (int i = 0; i < tabla.columnas().size(); i++) {
            celda(enc, i, tabla.columnas().get(i), e.encabezado);
        }
        int n = 1;
        for (List<Object> fila : tabla.filas()) {
            Row row = hoja.createRow(n++);
            for (int i = 0; i < fila.size(); i++) {
                valor(row.createCell(i), fila.get(i), e);
            }
        }
        hoja.createFreezePane(0, 1);
        if (!tabla.columnas().isEmpty()) {
            hoja.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, Math.max(0, n - 1), 0, tabla.columnas().size() - 1));
        }
        for (int i = 0; i < tabla.columnas().size(); i++) {
            hoja.autoSizeColumn(i);
            hoja.setColumnWidth(i, Math.min(hoja.getColumnWidth(i) + 512, 60 * 256));
        }
    }

    private static String nombreHoja(String titulo, String porDefecto) {
        String n = titulo == null || titulo.isBlank() ? porDefecto : titulo.replaceAll("[\\\\/?*\\[\\]:]", " ");
        return n.length() > 31 ? n.substring(0, 31) : n;
    }

    private static void celda(Row row, int col, String texto, CellStyle estilo) {
        Cell c = row.createCell(col);
        c.setCellValue(texto == null ? "" : texto);
        if (estilo != null) {
            c.setCellStyle(estilo);
        }
    }

    private static void valor(Cell c, Object v, Estilos e) {
        if (v == null) {
            c.setBlank();
        } else if (v instanceof BigDecimal bd) {
            c.setCellValue(bd.doubleValue());
            c.setCellStyle(e.moneda);
        } else if (v instanceof Number n) {
            c.setCellValue(n.doubleValue());
        } else if (v instanceof LocalDate d) {
            c.setCellValue(Date.from(d.atStartOfDay().toInstant(ZoneOffset.UTC)));
            c.setCellStyle(e.fecha);
        } else if (v instanceof Instant i) {
            c.setCellValue(Fechas.fechaHora(i));
        } else if (v instanceof Boolean b) {
            c.setCellValue(b ? "Sí" : "No");
        } else {
            c.setCellValue(String.valueOf(v));
        }
    }

    private static final class Estilos {
        final CellStyle encabezado;
        final CellStyle titulo;
        final CellStyle moneda;
        final CellStyle fecha;

        Estilos(SXSSFWorkbook wb) {
            CreationHelper h = wb.getCreationHelper();
            Font negrita = wb.createFont();
            negrita.setBold(true);
            encabezado = wb.createCellStyle();
            encabezado.setFont(negrita);
            titulo = wb.createCellStyle();
            Font grande = wb.createFont();
            grande.setBold(true);
            grande.setFontHeightInPoints((short) 14);
            titulo.setFont(grande);
            moneda = wb.createCellStyle();
            moneda.setDataFormat(h.createDataFormat().getFormat("\"$\" #,##0.00"));
            fecha = wb.createCellStyle();
            fecha.setDataFormat(h.createDataFormat().getFormat("dd/mm/yyyy"));
        }
    }
}
