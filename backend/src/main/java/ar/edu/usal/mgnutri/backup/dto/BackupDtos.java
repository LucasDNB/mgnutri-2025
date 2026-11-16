package ar.edu.usal.mgnutri.backup.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;

public final class BackupDtos {

    private BackupDtos() {
    }

    public record BackupResponse(Long id, String tipo, Instant iniciadoAt, Instant finalizadoAt, String estado, String archivo,
                                 Long tamanoBytes, String solicitadoPor, String error, boolean expirado) {}

    public record ProgramacionDto(@NotNull Boolean habilitado,
                                  @NotBlank @Pattern(regexp = "DIARIA|SEMANAL|MENSUAL", message = "Frecuencia inválida") String frecuencia,
                                  @Min(1) @Max(7) Integer diaSemana,
                                  @Min(value = 1, message = "Día del mes entre 1 y 28") @Max(value = 28, message = "Día del mes entre 1 y 28") Integer diaMes,
                                  @NotBlank @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Hora inválida (HH:mm)") String hora,
                                  @NotNull @Min(value = 7, message = "Retención entre 7 y 365 días") @Max(value = 365, message = "Retención entre 7 y 365 días") Integer retencionDias,
                                  String descripcion, String cron) {}
}
