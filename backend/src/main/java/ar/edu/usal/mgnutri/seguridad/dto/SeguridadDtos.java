package ar.edu.usal.mgnutri.seguridad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Set;

public final class SeguridadDtos {

    private SeguridadDtos() {
    }

    public record LoginRequest(@NotBlank(message = "Ingresá tu usuario") @Size(max = 40) String username,
                               @NotBlank(message = "Ingresá tu contraseña") @Size(max = 100) String password) {}

    public record UsuarioActual(Long id, String username, String nombre, Set<String> roles, String home,
                                boolean debeCambiarPassword, boolean debeAceptarTerminos, Long pacienteId,
                                Long profesionalId, String tipoProfesional, boolean tieneAgenda) {}

    public record LoginResponse(String token, Instant expiraEn, UsuarioActual usuario) {}

    public record CambiarPasswordRequest(@NotBlank String actual, @NotBlank String nueva) {}

    public record RecuperoRequest(@NotBlank(message = "Ingresá tu documento o teléfono") String documentoOTelefono) {}

    public record RecuperoResponse(String mensaje) {}

    public record RecuperoConfirmarRequest(@NotBlank String token, @NotBlank String nuevaPassword) {}

    public record TokenValidoResponse(boolean valido, String username) {}

    public record AccesoResponse(Long id, Instant fechaHora, Long usuarioId, String usuario, String roles, String ip,
                                 String userAgent, String accion, String detalle) {}

    /** Resultado de crear o regenerar credenciales: la contraseña temporal sólo vive en memoria. */
    public record Credenciales(Long usuarioId, String username, String passwordTemporal) {}
}
