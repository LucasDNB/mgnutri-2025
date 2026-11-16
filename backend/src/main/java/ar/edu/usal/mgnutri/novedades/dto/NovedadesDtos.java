package ar.edu.usal.mgnutri.novedades.dto;

import ar.edu.usal.mgnutri.configuracion.dto.ConfiguracionDtos.MisContactos;
import ar.edu.usal.mgnutri.turnos.dto.TurnoDtos.TurnoResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;

public final class NovedadesDtos {

    private NovedadesDtos() {
    }

    public record ComentarioRequest(@NotNull String tipo, Long comidaPlanId, Long ejercicioRutinaId, LocalDate fecha,
                                    @NotBlank(message = "Escribí tu comentario") @Size(max = 300, message = "Máximo 300 caracteres") String texto) {}

    public record ComentarioResponse(Long id, Long pacienteId, String paciente, String tipo, String referencia, LocalDate fecha,
                                     String texto, Instant createdAt, Instant leidoAt) {}

    public record MolestiaRequest(@NotBlank(message = "Describí la molestia") @Size(max = 300) String descripcion,
                                  @NotNull(message = "Indicá la intensidad") @Min(1) @Max(10) Integer intensidad,
                                  @Size(max = 60) String ubicacionCorporal, @NotNull String contexto, Long ejercicioRutinaId,
                                  LocalDate fecha) {}

    public record MolestiaResponse(Long id, Long pacienteId, String paciente, LocalDate fecha, String descripcion, int intensidad,
                                   String ubicacionCorporal, String contexto, String ejercicio, boolean prioritaria,
                                   Instant createdAt, Instant revisadaAt) {}

    public record Novedad(String tipo, Long id, Long pacienteId, String paciente, String texto, String detalle,
                          boolean prioritaria, Instant fecha, boolean leida) {}

    public record HomePaciente(String nombre, TurnoResponse proximoTurno, boolean tienePlan, boolean tieneRutina,
                               MisContactos contactos, boolean entrenadorConAgenda, boolean nutricionistaConAgenda) {}
}
