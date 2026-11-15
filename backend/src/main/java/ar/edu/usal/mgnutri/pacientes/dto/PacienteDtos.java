package ar.edu.usal.mgnutri.pacientes.dto;

import ar.edu.usal.mgnutri.common.Area;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class PacienteDtos {

    private PacienteDtos() {
    }

    public static final String NOMBRE_REGEX = "^[\\p{L} '\\-]{2,80}$";
    public static final String NOMBRE_MSJ = "Debe tener entre 2 y 80 letras (se permiten espacios, apóstrofes y guiones)";

    public record PacienteRequest(
            @NotBlank(message = "El nombre es obligatorio") @Pattern(regexp = NOMBRE_REGEX, message = NOMBRE_MSJ) String nombre,
            @NotBlank(message = "El apellido es obligatorio") @Pattern(regexp = NOMBRE_REGEX, message = NOMBRE_MSJ) String apellido,
            @NotBlank(message = "El tipo de documento es obligatorio") @Size(max = 10) String tipoDocumento,
            @NotBlank(message = "El número de documento es obligatorio") @Pattern(regexp = "^[A-Za-z0-9]{6,20}$", message = "Documento inválido") String nroDocumento,
            @NotNull(message = "La fecha de nacimiento es obligatoria") @Past(message = "La fecha de nacimiento debe ser pasada") LocalDate fechaNacimiento,
            @NotBlank(message = "El sexo es obligatorio") @Pattern(regexp = "^[FMX]$", message = "Sexo inválido") String sexo,
            @NotBlank(message = "El teléfono de WhatsApp es obligatorio") String telefonoWhatsapp,
            @Email(message = "Email inválido") @Size(max = 120) String email,
            @Size(max = 200) String domicilio,
            @Size(max = 300, message = "Máximo 300 caracteres") String objetivo,
            @Size(max = 2000, message = "Máximo 2000 caracteres") String antecedentesSalud,
            @Size(max = 2000, message = "Máximo 2000 caracteres") String observaciones,
            @Valid List<AsignacionItem> asignaciones,
            Integer version) {}

    public record AsignacionItem(@NotNull Long profesionalId, @NotNull Area area) {}

    public record AsignacionRequest(@NotNull @Size(min = 1, message = "Elegí al menos un profesional") @Valid List<AsignacionItem> asignaciones) {}

    public record AsignacionResponse(Long id, Long profesionalId, String profesional, String tipoProfesional, Area area,
                                     LocalDate fechaAsignacion, String telefono) {}

    public record BajaRequest(@Size(max = 300) String motivo, Boolean notificarCancelaciones) {}

    public record NotificacionInfo(Long id, String estado) {}

    public record AltaResponse(Long id, String username, NotificacionInfo notificacion, String passwordTemporal) {}

    public record CredencialesResponse(String username, NotificacionInfo notificacion, String passwordTemporal) {}

    public record PacienteResponse(Long id, Long usuarioId, String username, String nombre, String apellido, String tipoDocumento,
                                   String nroDocumento, LocalDate fechaNacimiento, int edad, String sexo,
                                   String telefonoWhatsapp, String email, String domicilio, String objetivo,
                                   String antecedentesSalud, String observaciones, String estado, LocalDate fechaBaja,
                                   String motivoBaja, List<AsignacionResponse> asignaciones, BigDecimal saldo,
                                   Integer version) {}

    public record PacienteListItem(Long id, String apellido, String nombre, String nroDocumento, String telefono,
                                   List<String> profesionales, BigDecimal saldo, String estado, int edad, String objetivo) {}

    public record PerfilRequest(@Email(message = "Email inválido") @Size(max = 120) String email,
                                @Size(max = 200) String domicilio) {}
}
