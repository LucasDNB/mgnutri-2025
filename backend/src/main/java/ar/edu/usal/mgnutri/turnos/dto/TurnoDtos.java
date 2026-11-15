package ar.edu.usal.mgnutri.turnos.dto;

import ar.edu.usal.mgnutri.common.Area;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class TurnoDtos {

    private TurnoDtos() {
    }

    public record SolicitarTurnoRequest(@NotNull Long profesionalId, @NotNull Instant inicio, Area area) {}

    public record CrearTurnoRequest(@NotNull Long pacienteId, @NotNull Long profesionalId, @NotNull Instant inicio,
                                    Area area, Boolean forzar) {}

    public record ReprogramarRequest(@NotNull Instant inicio, @Size(max = 200) String motivo, Integer version) {}

    public record CancelarRequest(@Size(max = 200) String motivo, Integer version) {}

    public record CambiarEstadoRequest(@NotBlank String estado, Integer version) {}

    public record Persona(Long id, String nombre, String telefono) {}

    public record TurnoResponse(Long id, Instant inicio, Instant fin, String estado, Area area, String ubicacion,
                                Persona paciente, Persona profesional, boolean puedeModificar, String motivoCancelacion,
                                String canceladoPor, boolean fueraDeAgenda, boolean pasado, Integer version) {}

    public record HuecoResponse(Instant inicio, Instant fin, String ubicacion) {}

    public record HistorialResponse(Instant inicioAnterior, Instant finAnterior, String motivo, String cambiadoPor,
                                    Instant cambiadoAt) {}
}
