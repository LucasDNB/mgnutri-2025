package ar.edu.usal.mgnutri.common.security;

import ar.edu.usal.mgnutri.common.error.BusinessException;
import ar.edu.usal.mgnutri.common.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

/** Acceso al usuario autenticado y a datos de la solicitud desde los servicios. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Optional<UsuarioPrincipal> opcional() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioPrincipal p) {
            return Optional.of(p);
        }
        return Optional.empty();
    }

    public static UsuarioPrincipal get() {
        return opcional().orElseThrow(() -> new BusinessException(ErrorCode.NO_AUTENTICADO, "Tenés que iniciar sesión."));
    }

    public static Long pacienteId() {
        UsuarioPrincipal p = get();
        if (p.pacienteId() == null) {
            throw BusinessException.sinPermiso();
        }
        return p.pacienteId();
    }

    public static Long profesionalId() {
        UsuarioPrincipal p = get();
        if (p.profesionalId() == null) {
            throw BusinessException.sinPermiso();
        }
        return p.profesionalId();
    }

    public static Optional<HttpServletRequest> request() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return Optional.of(attrs.getRequest());
        }
        return Optional.empty();
    }

    /** IP del cliente (primer valor de X-Forwarded-For detrás de Azure). */
    public static String ip() {
        return request().map(CurrentUser::ip).orElse(null);
    }

    public static String ip(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            String primera = xff.split(",")[0].trim();
            // Azure agrega el puerto (ip:puerto) en algunos casos
            int dosPuntos = primera.lastIndexOf(':');
            if (dosPuntos > 0 && primera.indexOf(':') == dosPuntos) {
                primera = primera.substring(0, dosPuntos);
            }
            return primera;
        }
        return req.getRemoteAddr();
    }

    public static String userAgent() {
        return request().map(r -> r.getHeader("User-Agent")).orElse(null);
    }
}
