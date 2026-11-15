package ar.edu.usal.mgnutri.config;

import ar.edu.usal.mgnutri.common.Rol;
import ar.edu.usal.mgnutri.common.error.ErrorCode;
import ar.edu.usal.mgnutri.common.security.UsuarioPrincipal;
import ar.edu.usal.mgnutri.seguridad.dao.UsuarioDao;
import ar.edu.usal.mgnutri.seguridad.domain.Usuario;
import ar.edu.usal.mgnutri.seguridad.service.JwtService;
import ar.edu.usal.mgnutri.seguridad.service.PerfilUsuarioService;
import ar.edu.usal.mgnutri.seguridad.service.TokenRevocacionService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Valida el Bearer token, verifica revocación y estado del usuario, y aplica la regla de primer ingreso:
 * con contraseña temporal o T&C sin aceptar sólo se permite /auth/**, /terminos/** y /me (428).
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwt;
    private final TokenRevocacionService revocacion;
    private final UsuarioDao usuarioDao;
    private final PerfilUsuarioService perfiles;
    private final ProblemWriter writer;

    public JwtAuthenticationFilter(JwtService jwt, TokenRevocacionService revocacion, UsuarioDao usuarioDao,
                                   PerfilUsuarioService perfiles, ProblemWriter writer) {
        this.jwt = jwt;
        this.revocacion = revocacion;
        this.usuarioDao = usuarioDao;
        this.perfiles = perfiles;
        this.writer = writer;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(req, res);
            return;
        }
        Optional<Claims> claims = jwt.validar(header.substring(7).trim());
        if (claims.isEmpty()) {
            writer.escribir(req, res, ErrorCode.NO_AUTENTICADO, "Tu sesión expiró. Volvé a iniciar sesión.");
            return;
        }
        Claims c = claims.get();
        if (revocacion.revocado(c.getId())) {
            writer.escribir(req, res, ErrorCode.NO_AUTENTICADO, "Tu sesión fue cerrada. Volvé a iniciar sesión.");
            return;
        }
        Usuario u = usuarioDao.findById(Long.valueOf(c.getSubject())).orElse(null);
        Instant emitido = c.getIssuedAt() == null ? Instant.EPOCH : c.getIssuedAt().toInstant();
        if (u == null || u.getEstado() == Usuario.Estado.BAJA
                || (u.getTokensValidosDesde() != null && emitido.isBefore(u.getTokensValidosDesde()))) {
            writer.escribir(req, res, ErrorCode.NO_AUTENTICADO, "Tu sesión ya no es válida. Volvé a iniciar sesión.");
            return;
        }
        Set<Rol> roles = u.getRoles().isEmpty() ? EnumSet.noneOf(Rol.class) : EnumSet.copyOf(u.getRoles());
        boolean terminos = perfiles.debeAceptarTerminos(u);
        UsuarioPrincipal principal = new UsuarioPrincipal(u.getId(), u.getUsername(), roles,
                c.get("pacienteId", Long.class), c.get("profesionalId", Long.class),
                u.isDebeCambiarPassword(), terminos, c.getId());
        List<SimpleGrantedAuthority> authorities = roles.stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r.name())).toList();
        var auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);

        if ((principal.debeCambiarPassword() || principal.debeAceptarTerminos()) && !rutaPermitidaPrimerIngreso(req)) {
            if (principal.debeCambiarPassword()) {
                writer.escribir(req, res, ErrorCode.DEBE_CAMBIAR_PASSWORD, "Antes de continuar tenés que cambiar tu contraseña.");
            } else {
                writer.escribir(req, res, ErrorCode.DEBE_ACEPTAR_TERMINOS, "Antes de continuar tenés que aceptar los términos y condiciones.");
            }
            SecurityContextHolder.clearContext();
            return;
        }
        chain.doFilter(req, res);
    }

    static boolean rutaPermitidaPrimerIngreso(HttpServletRequest req) {
        String uri = req.getRequestURI();
        return uri.startsWith("/api/v1/auth/") || uri.startsWith("/api/v1/terminos") || uri.equals("/api/v1/me")
                || uri.startsWith("/api/v1/publico/");
    }
}
