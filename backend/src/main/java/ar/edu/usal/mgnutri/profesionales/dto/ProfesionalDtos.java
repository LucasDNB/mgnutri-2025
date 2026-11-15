package ar.edu.usal.mgnutri.profesionales.dto;

import ar.edu.usal.mgnutri.pacientes.dto.PacienteDtos;
import ar.edu.usal.mgnutri.profesionales.domain.Profesional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class ProfesionalDtos {

    private ProfesionalDtos() {
    }

    public record ProfesionalRequest(
            @NotBlank(message = "El nombre es obligatorio") @Pattern(regexp = PacienteDtos.NOMBRE_REGEX, message = PacienteDtos.NOMBRE_MSJ) String nombre,
            @NotBlank(message = "El apellido es obligatorio") @Pattern(regexp = PacienteDtos.NOMBRE_REGEX, message = PacienteDtos.NOMBRE_MSJ) String apellido,
            @NotBlank(message = "El tipo de documento es obligatorio") @Size(max = 10) String tipoDocumento,
            @NotBlank(message = "El número de documento es obligatorio") @Pattern(regexp = "^[A-Za-z0-9]{6,20}$", message = "Documento inválido") String nroDocumento,
            @NotNull(message = "El tipo de profesional es obligatorio") Profesional.Tipo tipo,
            @Pattern(regexp = "^[A-Za-z0-9 .\\-/]{0,30}$", message = "Matrícula alfanumérica de hasta 30 caracteres") String matricula,
            @Size(max = 100) String especialidad,
            String telefonoWhatsapp,
            @Email(message = "Email inválido") @Size(max = 120) String email,
            @Min(value = 15, message = "Mínimo 15 minutos") @Max(value = 120, message = "Máximo 120 minutos") Integer duracionTurnoMin,
            Integer version) {}

    public record ProfesionalResponse(Long id, Long usuarioId, String username, String nombre, String apellido,
                                      String nombreVisible, String tipoDocumento, String nroDocumento, String matricula,
                                      Profesional.Tipo tipo, String especialidad, String telefonoWhatsapp, String email,
                                      int duracionTurnoMin, String estado, LocalDate fechaBaja, boolean tieneAgenda,
                                      int pacientesAsignados, Integer version) {}

    public record ProfesionalListItem(Long id, String apellido, String nombre, String nroDocumento, Profesional.Tipo tipo,
                                      String especialidad, String matricula, String telefono, String estado,
                                      boolean tieneAgenda, int pacientesAsignados) {}

    public record ProfesionalDisponible(Long id, String nombre, String especialidad, Profesional.Tipo tipo,
                                        boolean asignado, int duracionTurnoMin) {}

    public record ModificacionResponse(ProfesionalResponse profesional, List<String> advertencias) {}

    public record FranjaDto(@NotNull @Min(1) @Max(7) Integer diaSemana, @NotNull LocalTime horaDesde,
                            @NotNull LocalTime horaHasta, @Size(max = 120) String ubicacion) {}

    public record FranjasRequest(@NotNull @Valid List<FranjaDto> franjas) {}

    public record FranjaResponse(Long id, int diaSemana, LocalTime horaDesde, LocalTime horaHasta, String ubicacion) {}

    public record BloqueoRequest(@NotNull Instant desde, @NotNull Instant hasta, @Size(max = 200) String motivo,
                                 Boolean confirmarCancelacion) {}

    public record BloqueoResponse(Long id, Instant desde, Instant hasta, String motivo, int turnosCancelados) {}
}
