package ar.edu.usal.mgnutri.entrenamiento.service;

import ar.edu.usal.mgnutri.entrenamiento.dto.RutinaDtos.DiaResponse;
import ar.edu.usal.mgnutri.entrenamiento.dto.RutinaDtos.EjercicioItemResponse;
import ar.edu.usal.mgnutri.entrenamiento.dto.RutinaDtos.ReemplazoInfo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** La impresión de la rutina omite las columnas sin datos. */
class RutinaPdfColumnasTest {

    private static EjercicioItemResponse item(String grupo, Integer descanso, String carga, String notas, String video, ReemplazoInfo r) {
        return new EjercicioItemResponse(1L, 1L, "Sentadilla", grupo, null, null, video, 1, 3, "12", descanso, carga, notas, r, null, null, null);
    }

    private static EjercicioItemResponse enBloque(String nombre, Integer series, String reps, String bloque, String esquema) {
        return new EjercicioItemResponse(1L, 1L, nombre, null, null, null, null, 1, series, reps, null, null, null, null, null, bloque, esquema);
    }

    @Test
    void agregaFilasDeBloqueYEsquema() {
        // Entrada en calor: 5 min cinta + 3 rondas (crunch, push up); WOD 21-15-9 (burpees, thruster)
        var dia = new DiaResponse(1L, 1, null, List.of(
                enBloque("Cinta", null, "5 min", "Entrada en calor", null),
                enBloque("Crunch", null, "30", "Entrada en calor", "3 rondas"),
                enBloque("Push up", null, "20", "Entrada en calor", "3 rondas"),
                enBloque("Burpees", null, null, "WOD", "21-15-9"),
                enBloque("Thruster", null, null, "WOD", "21-15-9")));
        // encabezado + 5 ejercicios + «Entrada en calor» + «3 rondas» + «WOD» + «21-15-9»
        assertEquals(10, RutinaPdf.tabla(dia).getRows().size());
        assertEquals(List.of("Ejercicio", "Series × reps"), RutinaPdf.titulosConDatos(dia.ejercicios()));
    }

    @Test
    void omiteColumnasVacias() {
        assertEquals(List.of("Ejercicio", "Series × reps"),
                RutinaPdf.titulosConDatos(List.of(item(null, null, null, " ", null, null), item("", null, "", null, null, null))));
    }

    @Test
    void mantieneColumnaSiAlgunEjercicioTieneDato() {
        assertEquals(List.of("Ejercicio", "Grupo muscular", "Series × reps", "Descanso", "Video"),
                RutinaPdf.titulosConDatos(List.of(item("Piernas", null, null, null, null, null),
                        item(null, 60, null, null, null, new ReemplazoInfo(2L, "Prensa", "https://v", "Sentadilla")))));
    }
}
