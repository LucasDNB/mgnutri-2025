package ar.edu.usal.mgnutri.entrenamiento.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class RutinaDtos {

    private RutinaDtos() {
    }

    /**
     * Ejercicio de un día. bloque/esquema agrupan ejercicios seguidos («Entrada en calor» › «3 rondas»); fuera de un
     * bloque, series y repeticiones son obligatorias.
     */
    public record EjercicioItemDto(Long id, @NotNull Long ejercicioId,
                                   @Min(value = 1, message = "Series entre 1 y 20") @Max(value = 20, message = "Series entre 1 y 20") Integer series,
                                   @Size(max = 20) String repeticiones,
                                   @Min(0) @Max(900) Integer descansoSeg,
                                   @Size(max = 30) String cargaSugerida,
                                   @Size(max = 300) String notas,
                                   @Size(max = 60, message = "El nombre del bloque admite hasta 60 caracteres") String bloque,
                                   @Size(max = 60, message = "El esquema admite hasta 60 caracteres") String esquema) {}

    public record DiaDto(@NotNull @Min(1) @Max(7) Integer diaSemana, @Size(max = 80) String nombre,
                         @NotNull @Valid List<EjercicioItemDto> ejercicios) {}

    public record RutinaRequest(@NotBlank(message = "El nombre es obligatorio") @Size(max = 120) String nombre,
                                @Size(max = 300) String objetivo,
                                @NotNull LocalDate fechaInicio,
                                @NotNull @Min(1) @Max(52) Integer duracionSemanas,
                                @Size(max = 1000) String observaciones,
                                @NotNull @Valid List<DiaDto> dias,
                                Boolean finalizarActual,
                                Integer version) {}

    public record ReemplazoInfo(Long ejercicioId, String nombre, String videoUrl, String original) {}

    public record EjercicioItemResponse(Long id, Long ejercicioId, String nombre, String grupoMuscular, String dificultad,
                                        String equipamiento, String videoUrl, int orden, Integer series, String repeticiones,
                                        Integer descansoSeg, String cargaSugerida, String notas, ReemplazoInfo reemplazo,
                                        BigDecimal ultimaCarga, String bloque, String esquema) {}

    public record DiaResponse(Long id, int diaSemana, String nombre, List<EjercicioItemResponse> ejercicios) {}

    public record RutinaResponse(Long id, Long pacienteId, String paciente, Long entrenadorId, String entrenador, String nombre,
                                 String objetivo, LocalDate fechaInicio, int duracionSemanas, LocalDate fechaFin, String estado,
                                 String observaciones, List<DiaResponse> dias, Instant createdAt, Integer version,
                                 List<String> advertencias) {}

    public record RutinaListItem(Long id, String nombre, LocalDate fechaInicio, LocalDate fechaFin, String estado,
                                 String entrenador, Instant createdAt) {}

    public record RegistroRequest(@NotNull Long ejercicioRutinaId, @NotNull LocalDate fecha, boolean completado,
                                  @DecimalMin("0") @DecimalMax("500") BigDecimal cargaKg,
                                  @Min(0) @Max(20) Integer seriesRealizadas,
                                  @Min(0) @Max(200) Integer repeticionesRealizadas) {}

    public record RegistroResponse(Long ejercicioRutinaId, Long ejercicioId, String ejercicio, LocalDate fecha,
                                   boolean completado, BigDecimal cargaKg, Integer seriesRealizadas,
                                   Integer repeticionesRealizadas, boolean sesionCompleta) {}

    public record DiaCalendario(LocalDate fecha, boolean programado, String nombre, int ejercicios, int completados,
                                boolean cumplido) {}

    public record AlternativaEjercicio(Long ejercicioId, String nombre, String grupoMuscular, String dificultad,
                                       String equipamiento, String videoUrl) {}

    public record AlternativasEjercicioResponse(String original, List<AlternativaEjercicio> alternativas, String mensaje) {}

    public record ReemplazoEjercicioRequest(@NotNull Long ejercicioId) {}

    public record RegistrosEntrenamiento(List<RegistroResponse> registros, List<ReemplazoAplicado> reemplazos,
                                         double cumplimientoPct) {}

    public record ReemplazoAplicado(Long ejercicioRutinaId, String original, String variante, LocalDate fecha) {}
}
