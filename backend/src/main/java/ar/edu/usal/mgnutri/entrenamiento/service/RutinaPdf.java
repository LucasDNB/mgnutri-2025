package ar.edu.usal.mgnutri.entrenamiento.service;

import ar.edu.usal.mgnutri.common.Fechas;
import ar.edu.usal.mgnutri.configuracion.service.ConfiguracionService;
import ar.edu.usal.mgnutri.entrenamiento.dto.RutinaDtos.DiaResponse;
import ar.edu.usal.mgnutri.entrenamiento.dto.RutinaDtos.EjercicioItemResponse;
import ar.edu.usal.mgnutri.entrenamiento.dto.RutinaDtos.RutinaResponse;
import ar.edu.usal.mgnutri.reportes.service.PdfLayout;
import com.lowagie.text.Anchor;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

/** PDF de la rutina con enlaces clickeables a los videos (CA-05.4.2). */
@Component
public class RutinaPdf {

    private final ConfiguracionService config;

    public RutinaPdf(ConfiguracionService config) {
        this.config = config;
    }

    public byte[] generar(RutinaResponse r) {
        return PdfLayout.documento(true, config.valor("centro.nombre", "MG Nutrición y Entrenamiento"),
                "Rutina de entrenamiento · " + r.paciente(), null, doc -> {
                    PdfLayout.agregar(doc, PdfLayout.parrafo(r.nombre(), PdfLayout.TITULO));
                    PdfLayout.agregar(doc, PdfLayout.parrafo("Paciente: " + r.paciente() + "    Entrenador: " + r.entrenador()
                            + "    Vigencia: " + Fechas.fecha(r.fechaInicio()) + " al " + Fechas.fecha(r.fechaFin())
                            + (r.objetivo() == null ? "" : "    Objetivo: " + r.objetivo()), PdfLayout.NORMAL));
                    for (DiaResponse d : r.dias()) {
                        String titulo = Character.toUpperCase(RutinaService.nombreDia(d.diaSemana()).charAt(0))
                                + RutinaService.nombreDia(d.diaSemana()).substring(1) + (d.nombre() == null ? "" : " – " + d.nombre());
                        PdfLayout.agregar(doc, PdfLayout.parrafo(titulo, PdfLayout.SUBTITULO));
                        PdfLayout.agregar(doc, tabla(d));
                    }
                    if (r.observaciones() != null) {
                        PdfLayout.agregar(doc, PdfLayout.parrafo("Observaciones: " + r.observaciones(), PdfLayout.NORMAL));
                    }
                    PdfLayout.agregar(doc, PdfLayout.parrafo("Rutina elaborada por un profesional del centro. Ante dolor o molestias, suspendé el ejercicio y consultá.",
                            PdfLayout.CHICA));
                });
    }

    private static final String VIDEO = "Video";

    private record Columna(String titulo, float ancho, Function<EjercicioItemResponse, String> valor) {}

    private static final List<Columna> COLUMNAS = List.of(
            new Columna("Ejercicio", 2.4f, e -> e.reemplazo() != null ? e.reemplazo().nombre() + " (variante de " + e.nombre() + ")" : e.nombre()),
            new Columna("Grupo muscular", 1.3f, EjercicioItemResponse::grupoMuscular),
            new Columna("Series × reps", 1f, RutinaPdf::seriesReps),
            new Columna("Descanso", 0.8f, e -> e.descansoSeg() == null ? null : e.descansoSeg() + " s"),
            new Columna("Carga sug.", 1f, EjercicioItemResponse::cargaSugerida),
            new Columna("Notas", 2f, EjercicioItemResponse::notas),
            new Columna(VIDEO, 1f, e -> e.reemplazo() != null ? e.reemplazo().videoUrl() : e.videoUrl()));

    /** Tabla del día sin las columnas que no tienen ningún dato (el ejercicio siempre se muestra). */
    static PdfPTable tabla(DiaResponse d) {
        List<Columna> cols = columnasConDatos(d.ejercicios());
        float[] anchos = new float[cols.size()];
        for (int i = 0; i < anchos.length; i++) {
            anchos[i] = cols.get(i).ancho();
        }
        PdfPTable t = new PdfPTable(anchos);
        t.setWidthPercentage(100);
        t.setHeaderRows(1);
        for (Columna col : cols) {
            PdfPCell c = new PdfPCell(new Phrase(col.titulo(), PdfLayout.ENCABEZADO));
            c.setBackgroundColor(PdfLayout.VERDE);
            c.setPadding(4);
            t.addCell(c);
        }
        String bloque = null;
        String esquema = null;
        for (EjercicioItemResponse e : d.ejercicios()) {
            // Encabezados de bloque («Entrada en calor») y de esquema («3 rondas») al cambiar de grupo
            if (!java.util.Objects.equals(e.bloque(), bloque) || (e.bloque() != null && !java.util.Objects.equals(e.esquema(), esquema))) {
                if (e.bloque() != null && !e.bloque().equals(bloque)) {
                    t.addCell(fila(e.bloque(), PdfLayout.NEGRITA, cols.size(), PdfLayout.VERDE_CLARO, 4, 4));
                }
                if (e.bloque() != null && e.esquema() != null) {
                    t.addCell(fila(e.esquema(), PdfLayout.NEGRITA, cols.size(), null, 3, 13));
                }
                bloque = e.bloque();
                esquema = e.esquema();
            }
            for (Columna col : cols) {
                String v = col.valor().apply(e);
                if (VIDEO.equals(col.titulo()) && !vacio(v)) {
                    Anchor a = new Anchor("Ver video", PdfLayout.ENLACE);
                    a.setReference(v);
                    PdfPCell c = new PdfPCell(new Phrase(a));
                    c.setPadding(3);
                    t.addCell(c);
                } else if (col == COLUMNAS.get(0)) {
                    // Sangría: los ejercicios de un bloque (y de su esquema) se distinguen de los sueltos
                    PdfPCell c = celda(v);
                    c.setPaddingLeft(3 + (e.bloque() == null ? 0 : 10) + (e.esquema() == null ? 0 : 10));
                    t.addCell(c);
                } else {
                    t.addCell(celda(v));
                }
            }
        }
        return t;
    }

    static List<Columna> columnasConDatos(List<EjercicioItemResponse> ejercicios) {
        return COLUMNAS.stream()
                .filter(c -> c == COLUMNAS.get(0) || ejercicios.stream().anyMatch(e -> !vacio(c.valor().apply(e))))
                .toList();
    }

    static List<String> titulosConDatos(List<EjercicioItemResponse> ejercicios) {
        return columnasConDatos(ejercicios).stream().map(Columna::titulo).toList();
    }

    private static String seriesReps(EjercicioItemResponse e) {
        if (e.series() == null) {
            return e.repeticiones();
        }
        return e.series() + " × " + (e.repeticiones() == null ? "" : e.repeticiones());
    }

    private static PdfPCell fila(String texto, com.lowagie.text.Font font, int columnas, java.awt.Color fondo, float padding,
                                 float sangria) {
        PdfPCell c = new PdfPCell(new Phrase(texto, font));
        c.setColspan(columnas);
        c.setPadding(padding);
        c.setPaddingLeft(sangria);
        if (fondo != null) {
            c.setBackgroundColor(fondo);
        }
        return c;
    }

    private static boolean vacio(String s) {
        return s == null || s.isBlank();
    }

    private static PdfPCell celda(String t) {
        PdfPCell c = new PdfPCell(new Phrase(t == null ? "" : t, PdfLayout.CHICA));
        c.setPadding(3);
        return c;
    }
}
