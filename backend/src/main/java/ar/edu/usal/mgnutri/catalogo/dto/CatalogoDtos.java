package ar.edu.usal.mgnutri.catalogo.dto;

import ar.edu.usal.mgnutri.catalogo.domain.Ejercicio;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public final class CatalogoDtos {

    private CatalogoDtos() {
    }

    public record GrupoAlimentoResponse(Long id, String nombre, String macroReferencia, String descripcion) {}

    public record GrupoMuscularResponse(Long id, String nombre) {}

    public record EquivalenciaDto(@NotNull Long alimentoEquivalenteId, String alimentoEquivalente,
                                  @NotNull @DecimalMin("1") @DecimalMax("2000") BigDecimal gramosOrigen,
                                  @NotNull @DecimalMin("1") @DecimalMax("2000") BigDecimal gramosEquivalente,
                                  @Size(max = 120) String nota) {}

    public record AlimentoRequest(
            @NotBlank(message = "El nombre es obligatorio") @Size(max = 120) String nombre,
            @NotNull(message = "El grupo es obligatorio") Long grupoId,
            @NotNull(message = "Las kcal son obligatorias") @DecimalMin(value = "0", message = "Mínimo 0") @DecimalMax(value = "900", message = "Máximo 900") BigDecimal kcal,
            @NotNull(message = "Los HC son obligatorios") @DecimalMin("0") @DecimalMax("100") BigDecimal hc,
            @NotNull(message = "Las proteínas son obligatorias") @DecimalMin("0") @DecimalMax("100") BigDecimal proteinas,
            @NotNull(message = "Las grasas son obligatorias") @DecimalMin("0") @DecimalMax("100") BigDecimal grasas,
            @DecimalMin("0") @DecimalMax("100") BigDecimal fibra,
            @DecimalMin("0") @DecimalMax("100000") BigDecimal sodioMg,
            @Size(max = 60) String unidadCasera,
            @DecimalMin("0.1") @DecimalMax("2000") BigDecimal gramosUnidadCasera,
            boolean aptoDesayuno, boolean aptoColacion, boolean aptoAlmuerzo,
            @Valid List<EquivalenciaDto> equivalencias,
            Boolean confirmarAdvertencias,
            Integer version) {}

    public record AlimentoResponse(Long id, String nombre, Long grupoId, String grupo, String macroReferencia,
                                   BigDecimal kcal, BigDecimal hc, BigDecimal proteinas, BigDecimal grasas, BigDecimal fibra,
                                   BigDecimal sodioMg, String unidadCasera, BigDecimal gramosUnidadCasera,
                                   boolean aptoDesayuno, boolean aptoColacion, boolean aptoAlmuerzo, boolean activo,
                                   List<EquivalenciaDto> equivalencias, Integer version) {}

    public record EjercicioRequest(
            @NotBlank(message = "El nombre es obligatorio") @Size(max = 120) String nombre,
            @Size(max = 500) String descripcion,
            @NotNull(message = "El grupo muscular principal es obligatorio") Long grupoMuscularId,
            List<Long> gruposSecundariosIds,
            @NotNull(message = "La dificultad es obligatoria") Ejercicio.Dificultad dificultad,
            @Size(max = 100) String equipamiento,
            @Size(max = 300) String videoUrl,
            Integer version) {}

    public record EjercicioResponse(Long id, String nombre, String descripcion, Long grupoMuscularId, String grupoMuscular,
                                    List<GrupoMuscularResponse> gruposSecundarios, String dificultad, String equipamiento,
                                    String videoUrl, boolean activo, Integer version) {}
}
