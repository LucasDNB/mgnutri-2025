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
                        PdfPTable t = new PdfPTable(new float[]{2.4f, 1.3f, 1f, 0.8f, 1f, 2f, 1f});
                        t.setWidthPercentage(100);
                        t.setHeaderRows(1);
                        for (String h : new String[]{"Ejercicio", "Grupo muscular", "Series × reps", "Descanso", "Carga sug.", "Notas", "Video"}) {
                            PdfPCell c = new PdfPCell(new Phrase(h, PdfLayout.ENCABEZADO));
                            c.setBackgroundColor(PdfLayout.VERDE);
                            c.setPadding(4);
                            t.addCell(c);
                        }
                        for (EjercicioItemResponse e : d.ejercicios()) {
                            String nombre = e.reemplazo() != null ? e.reemplazo().nombre() + " (variante de " + e.nombre() + ")" : e.nombre();
                            String video = e.reemplazo() != null ? e.reemplazo().videoUrl() : e.videoUrl();
                            t.addCell(celda(nombre));
                            t.addCell(celda(e.grupoMuscular()));
                            t.addCell(celda(e.series() + " × " + e.repeticiones()));
                            t.addCell(celda(e.descansoSeg() == null ? "" : e.descansoSeg() + " s"));
                            t.addCell(celda(e.cargaSugerida()));
                            t.addCell(celda(e.notas()));
                            if (video != null) {
                                Anchor a = new Anchor("Ver video", PdfLayout.ENLACE);
                                a.setReference(video);
                                PdfPCell c = new PdfPCell(new Phrase(a));
                                c.setPadding(3);
                                t.addCell(c);
                            } else {
                                t.addCell(celda(""));
                            }
                        }
                        PdfLayout.agregar(doc, t);
                    }
                    if (r.observaciones() != null) {
                        PdfLayout.agregar(doc, PdfLayout.parrafo("Observaciones: " + r.observaciones(), PdfLayout.NORMAL));
                    }
                    PdfLayout.agregar(doc, PdfLayout.parrafo("Rutina elaborada por un profesional del centro. Ante dolor o molestias, suspendé el ejercicio y consultá.",
                            PdfLayout.CHICA));
                });
    }

    private static PdfPCell celda(String t) {
        PdfPCell c = new PdfPCell(new Phrase(t == null ? "" : t, PdfLayout.CHICA));
        c.setPadding(3);
        return c;
    }
}
