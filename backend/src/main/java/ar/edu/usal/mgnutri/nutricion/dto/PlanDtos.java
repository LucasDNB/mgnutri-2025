package ar.edu.usal.mgnutri.nutricion.dto;

import ar.edu.usal.mgnutri.nutricion.domain.TipoComida;
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
import java.util.Map;

public final class PlanDtos {

    private PlanDtos() {
    }

    public record ItemDto(Long id, @NotNull Long alimentoId,
                          @NotNull @DecimalMin(value = "1", message = "Mínimo 1 g") @DecimalMax(value = "2000", message = "Máximo 2000 g") BigDecimal gramos,
                          @Size(max = 60) String cantidadCasera) {}

    public record ComidaDto(@NotNull @Min(1) @Max(7) Integer diaSemana, @NotNull TipoComida tipo,
                            @Size(max = 500, message = "Máximo 500 caracteres") String descripcion,
                            @Valid List<ItemDto> items) {}

    public record PlanRequest(
            @NotBlank(message = "El nombre es obligatorio") @Size(max = 120) String nombre,
            @NotNull(message = "La fecha de inicio es obligatoria") LocalDate fechaInicio,
            @NotNull(message = "La duración es obligatoria") @Min(value = 1, message = "Mínimo 1 semana") @Max(value = 52, message = "Máximo 52 semanas") Integer duracionSemanas,
            @Size(max = 1000) String observaciones,
            String modo,
            @Min(800) @Max(5000) Integer kcalObjetivo,
            BigDecimal pctHc, BigDecimal pctProteinas, BigDecimal pctGrasas,
            @Size(max = 2000) String parametrosGeneracion,
            @NotNull @Valid List<ComidaDto> comidas,
            String estado,
            Boolean finalizarActual,
            Integer version) {}

    public record ActivarRequest(Boolean finalizarActual) {}

    public record Macros(double kcal, double hc, double proteinas, double grasas) {
        public static final Macros CERO = new Macros(0, 0, 0, 0);

        public Macros mas(Macros o) {
            return new Macros(kcal + o.kcal, hc + o.hc, proteinas + o.proteinas, grasas + o.grasas);
        }

        public Macros redondeado() {
            return new Macros(Math.round(kcal), Math.round(hc * 10) / 10.0, Math.round(proteinas * 10) / 10.0, Math.round(grasas * 10) / 10.0);
        }
    }

    public record ReemplazoInfo(Long alimentoId, String alimento, BigDecimal gramos, String cantidadCasera) {}

    public record ItemResponse(Long id, Long alimentoId, String alimento, String grupo, BigDecimal gramos,
                               String cantidadCasera, Macros macros, ReemplazoInfo reemplazo) {}

    public record ComidaResponse(Long id, int diaSemana, TipoComida tipo, String tipoEtiqueta, String descripcion,
                                 List<ItemResponse> items, Macros totales, Desvio desvio) {}

    public record TotalDia(int diaSemana, double kcal, double hc, double proteinas, double grasas, Desvio desvio) {}

    /** Desvío respecto del objetivo: VERDE ≤ 10 % kcal y ≤ 15 % macros; AMARILLO hasta el doble; ROJO más. */
    public record Desvio(double kcalPct, double macrosPct, String nivel) {}

    public record PacienteResumen(Long id, String nombre, int edad, String objetivo, BigDecimal ultimoPeso) {}

    public record ProfesionalResumen(Long id, String nombre, String matricula) {}

    public record PlanResponse(Long id, PacienteResumen paciente, ProfesionalResumen nutricionista, String nombre,
                               LocalDate fechaInicio, int duracionSemanas, LocalDate fechaFin, String modo,
                               Integer kcalObjetivo, BigDecimal pctHc, BigDecimal pctProteinas, BigDecimal pctGrasas,
                               String parametrosGeneracion, String estado, String observaciones,
                               List<ComidaResponse> comidas, List<TotalDia> totalesPorDia, Instant createdAt,
                               Integer version, List<String> advertencias) {}

    public record PlanListItem(Long id, String nombre, LocalDate fechaInicio, LocalDate fechaFin, String estado, String modo,
                               String nutricionista, Instant createdAt) {}

    // ---- Generación automática (CU-14)

    public record ComidaParam(@NotNull TipoComida tipo, @NotNull @DecimalMin("1") @DecimalMax("100") Double pct) {}

    public record GenerarRequest(
            @NotNull Long pacienteId,
            @NotNull @Min(value = 800, message = "Mínimo 800 kcal") @Max(value = 5000, message = "Máximo 5000 kcal") Integer kcal,
            @NotNull @DecimalMin("5") @DecimalMax("80") Double pctHc,
            @NotNull @DecimalMin("5") @DecimalMax("80") Double pctProteinas,
            @NotNull @DecimalMin("5") @DecimalMax("80") Double pctGrasas,
            @NotNull @Size(min = 4, message = "Elegí al menos 4 comidas") @Valid List<ComidaParam> comidas,
            List<Long> excluirAlimentoIds,
            String variedad,
            Long semilla) {}

    public record BorradorGenerado(List<ComidaResponse> comidas, List<TotalDia> totalesPorDia, long semillaUsada,
                                   Map<String, Object> parametros, List<String> advertencias) {}

    // ---- Paciente (spec 08)

    /**
     * realizada: «La hice como estaba propuesta»; conCambios: «La hice con cambios»; omitida: «No completada»
     * (saltada o reemplazada). Las tres son excluyentes; sin ninguna, la comida queda pendiente.
     */
    public record RegistroComidaRequest(@NotNull Long comidaPlanId, @NotNull LocalDate fecha, boolean realizada,
                                        boolean conCambios, boolean noLeGusto, boolean leCayoMal, boolean omitida) {}

    public record RegistroComidaResponse(Long comidaPlanId, LocalDate fecha, boolean realizada, boolean conCambios,
                                         boolean noLeGusto, boolean leCayoMal, boolean omitida) {}

    public record ObservacionRequest(@NotNull LocalDate fecha,
                                     @NotNull(message = "Indicá tu nivel de hambre") @Min(1) @Max(5) Integer nivelHambre,
                                     @NotNull(message = "Indicá tu nivel de saciedad") @Min(1) @Max(5) Integer nivelSaciedad,
                                     @Min(1) @Max(5) Integer estadoAnimo,
                                     @Size(max = 300, message = "Máximo 300 caracteres") String comentario) {}

    public record ObservacionResponse(Long id, LocalDate fecha, int nivelHambre, int nivelSaciedad, Integer estadoAnimo,
                                      String comentario) {}

    public record Alternativa(Long alimentoId, String nombre, BigDecimal gramos, String cantidadCasera, String origen,
                              double kcal) {}

    public record AlternativasResponse(ReemplazoInfo original, List<Alternativa> alternativas, String mensaje) {}

    public record ReemplazoRequest(@NotNull Long alimentoId) {}

    public record RegistrosNutricion(List<RegistroComidaResponse> registros, List<ObservacionResponse> observaciones,
                                     List<ReemplazoAplicado> reemplazos, double cumplimientoPct) {}

    public record ReemplazoAplicado(Long itemId, String original, String reemplazo, BigDecimal gramos, LocalDate desde,
                                    int diaSemana, String tipoComida) {}
}
