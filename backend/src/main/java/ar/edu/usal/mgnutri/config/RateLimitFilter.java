package ar.edu.usal.mgnutri.config;

import ar.edu.usal.mgnutri.common.error.ErrorCode;
import ar.edu.usal.mgnutri.common.security.CurrentUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Límite simple de solicitudes por IP en /auth/** (ventana fija de 1 minuto). */
public class RateLimitFilter extends OncePerRequestFilter {

    private final int limitePorMinuto;
    private final ProblemWriter writer;
    private final Map<String, Ventana> ventanas = new ConcurrentHashMap<>();

    public RateLimitFilter(int limitePorMinuto, ProblemWriter writer) {
        this.limitePorMinuto = limitePorMinuto <= 0 ? 10 : limitePorMinuto;
        this.writer = writer;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !(uri.startsWith("/api/v1/auth/login") || uri.startsWith("/api/v1/auth/recupero"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        long minuto = System.currentTimeMillis() / 60_000;
        String ip = CurrentUser.ip(req);
        Ventana v = ventanas.compute(ip, (k, actual) -> actual == null || actual.minuto != minuto ? new Ventana(minuto) : actual);
        if (v.contar() > limitePorMinuto) {
            writer.escribir(req, res, ErrorCode.DEMASIADAS_SOLICITUDES,
                    "Demasiados intentos en poco tiempo. Esperá un minuto y volvé a intentar.");
            return;
        }
        if (ventanas.size() > 10_000) {
            ventanas.entrySet().removeIf(e -> e.getValue().minuto != minuto);
        }
        chain.doFilter(req, res);
    }

    private static final class Ventana {
        final long minuto;
        int cantidad;

        Ventana(long minuto) {
            this.minuto = minuto;
        }

        synchronized int contar() {
            return ++cantidad;
        }
    }
}
