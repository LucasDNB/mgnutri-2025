package ar.edu.usal.mgnutri.evolucion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class EvolucionDtos {

    private EvolucionDtos() {
    }

    public record MedicionRequest(
            @NotNull(message = "La fecha es obligatoria") LocalDate fecha,
            @NotNull(message = "El peso es obligatorio") @DecimalMin(value = "20", message = "Peso entre 20 y 300 kg") @DecimalMax(value = "300", message = "Peso entre 20 y 300 kg") BigDecimal pesoKg,
            @DecimalMin(value = "100", message = "Talla entre 100 y 230 cm") @DecimalMax(value = "230", message = "Talla entre 100 y 230 cm") BigDecimal tallaCm,
            @DecimalMin(value = "3", message = "% grasa entre 3 y 60") @DecimalMax(value = "60", message = "% grasa entre 3 y 60") BigDecimal pctGrasa,
            @DecimalMin(value = "10", message = "% músculo entre 10 y 70") @DecimalMax(value = "70", message = "% músculo entre 10 y 70") BigDecimal pctMusculo,
            @DecimalMin(value = "10", message = "Perímetros entre 10 y 200 cm") @DecimalMax(value = "200", message = "Perímetros entre 10 y 200 cm") BigDecimal circCintura,
            @DecimalMin("10") @DecimalMax("200") BigDecimal circCadera,
            @DecimalMin("10") @DecimalMax("200") BigDecimal circBrazo,
            @DecimalMin("10") @DecimalMax("200") BigDecimal circMuslo,
            @DecimalMin("10") @DecimalMax("200") BigDecimal circPecho,
            @DecimalMin("10") @DecimalMax("200") BigDecimal circPantorrilla,
            @Size(max = 2000) String observaciones) {}

    public record MedicionResponse(Long id, Long pacienteId, Long profesionalId, String profesional, LocalDate fecha,
                                   BigDecimal pesoKg, BigDecimal tallaCm, BigDecimal imc, BigDecimal pctGrasa,
                                   BigDecimal pctMusculo, BigDecimal circCintura, BigDecimal circCadera, BigDecimal circBrazo,
                                   BigDecimal circMuslo, BigDecimal circPecho, BigDecimal circPantorrilla,
                                   String observaciones, Instant createdAt, boolean editable, List<String> advertencias) {}

    public record PruebaDto(@NotBlank(message = "Indicá la prueba") @Size(max = 80) String prueba, Long ejercicioId,
                            @NotNull @DecimalMin(value = "0", message = "Valor entre 0 y 10000") @DecimalMax(value = "10000", message = "Valor entre 0 y 10000") BigDecimal valor,
                            @NotBlank @Size(max = 15) String unidad, @Size(max = 300) String observaciones) {}

    public record EvaluacionRequest(@NotNull LocalDate fecha, @NotNull @Size(min = 1) @Valid List<PruebaDto> pruebas) {}

    public record EvaluacionUpdateRequest(@NotNull LocalDate fecha, @NotNull @Valid PruebaDto prueba) {}

    public record EvaluacionResponse(Long id, Long pacienteId, Long profesionalId, String profesional, LocalDate fecha,
                                     String prueba, Long ejercicioId, BigDecimal valor, String unidad, String observaciones,
                                     Instant createdAt, boolean editable) {}

    public record Punto(LocalDate fecha, Double valor) {}

    public record SerieNombrada(String nombre, String unidad, List<Punto> serie) {}

    public record FilaTabla(LocalDate fecha, Double peso, Double imc, Double pctGrasa, Double pctMusculo, Double cintura,
                            Double deltaAnterior, Double deltaInicial, Double deltaInicialPct) {}

    public record EvolucionResponse(List<Punto> peso, List<Punto> imc, Map<String, List<Punto>> composicion,
                                    Map<String, List<Punto>> medidas, List<SerieNombrada> desempeno,
                                    List<SerieNombrada> cargas, List<FilaTabla> tabla, boolean sinDatos) {}
}
