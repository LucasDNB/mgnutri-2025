package ar.edu.usal.mgnutri.common.security;

import ar.edu.usal.mgnutri.common.Rol;

import java.security.Principal;
import java.util.Set;
import java.util.stream.Collectors;

/** Usuario autenticado tal como lo ven los servicios. */
public record UsuarioPrincipal(Long id, String username, Set<Rol> roles, Long pacienteId, Long profesionalId,
                               boolean debeCambiarPassword, boolean debeAceptarTerminos, String jti)
        implements Principal {

    @Override
    public String getName() {
        return username;
    }

    public boolean tiene(Rol rol) {
        return roles.contains(rol);
    }

    public boolean esAdmin() {
        return tiene(Rol.ADMIN);
    }

    public boolean esPaciente() {
        return tiene(Rol.PACIENTE);
    }

    public boolean esProfesional() {
        return tiene(Rol.NUTRICIONISTA) || tiene(Rol.ENTRENADOR);
    }

    public String rolesTexto() {
        return roles.stream().map(Enum::name).sorted().collect(Collectors.joining(","));
    }
}
