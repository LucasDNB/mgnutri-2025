package ar.edu.usal.mgnutri.seguridad.service;

import ar.edu.usal.mgnutri.config.MgnutriProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Emisión y validación de JWT HS256 con jti revocable (D-30). */
@Service
public class JwtService {

    private final SecretKey clave;
    private final Duration vigencia;
    private final Clock clock;

    public JwtService(MgnutriProperties props, Clock clock) {
        String secreto = props.jwt() == null ? null : props.jwt().secret();
        if (secreto == null || secreto.isBlank()) {
            throw new IllegalStateException("Falta JWT_SECRET");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(secreto.trim());
        } catch (IllegalArgumentException e) {
            bytes = secreto.getBytes(StandardCharsets.UTF_8);
        }
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 256 bits");
        }
        this.clave = Keys.hmacShaKeyFor(bytes);
        this.vigencia = Duration.ofHours(props.jwt().expiracionHoras() <= 0 ? 8 : props.jwt().expiracionHoras());
        this.clock = clock;
    }

    public TokenEmitido emitir(Long usuarioId, Collection<String> roles, Long pacienteId, Long profesionalId) {
        Instant ahora = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        Instant expira = ahora.plus(vigencia);
        String jti = UUID.randomUUID().toString();
        var builder = Jwts.builder()
                .subject(String.valueOf(usuarioId))
                .id(jti)
                .claim("roles", List.copyOf(roles))
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(expira));
        if (pacienteId != null) {
            builder.claim("pacienteId", pacienteId);
        }
        if (profesionalId != null) {
            builder.claim("profesionalId", profesionalId);
        }
        return new TokenEmitido(builder.signWith(clave).compact(), jti, ahora, expira);
    }

    /** Valida firma y vencimiento; vacío si el token no es válido. */
    public Optional<Claims> validar(String token) {
        try {
            return Optional.of(Jwts.parser().verifyWith(clave).clock(() -> Date.from(clock.instant())).build()
                    .parseSignedClaims(token).getPayload());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public record TokenEmitido(String token, String jti, Instant emitido, Instant expira) {}
}
