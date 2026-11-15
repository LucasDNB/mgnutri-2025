package ar.edu.usal.mgnutri.config;

import ar.edu.usal.mgnutri.common.error.ErrorCode;
import ar.edu.usal.mgnutri.seguridad.dao.UsuarioDao;
import ar.edu.usal.mgnutri.seguridad.service.JwtService;
import ar.edu.usal.mgnutri.seguridad.service.PerfilUsuarioService;
import ar.edu.usal.mgnutri.seguridad.service.TokenRevocacionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/** Seguridad stateless con JWT (spec 13, D-30). */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] PUBLICAS = {
            "/api/v1/auth/login", "/api/v1/auth/recupero", "/api/v1/auth/recupero/**", "/api/v1/publico/**",
            "/api/v1/salud", "/api/v1/terminos/vigentes", "/actuator/health"
    };

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtService jwt, TokenRevocacionService revocacion,
                                           UsuarioDao usuarioDao, PerfilUsuarioService perfiles, ProblemWriter writer,
                                           MgnutriProperties props) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(c -> c.configurationSource(cors(props)))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .headers(h -> h
                        .frameOptions(f -> f.deny())
                        .contentTypeOptions(c -> {
                        })
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31_536_000))
                        .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; script-src 'self' https://cdn.jsdelivr.net; style-src 'self' 'unsafe-inline'; "
                                        + "img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'")))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLICAS).permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> writer.escribir(req, res, ErrorCode.NO_AUTENTICADO,
                                "Tenés que iniciar sesión."))
                        .accessDeniedHandler((req, res, ex) -> writer.escribir(req, res, ErrorCode.SIN_PERMISO,
                                "No tenés permiso para realizar esta acción.")))
                .addFilterBefore(new JwtAuthenticationFilter(jwt, revocacion, usuarioDao, perfiles, writer),
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new RateLimitFilter(props.seguridad().rateLimitPorMinuto(), writer),
                        JwtAuthenticationFilter.class);
        return http.build();
    }

    private CorsConfigurationSource cors(MgnutriProperties props) {
        CorsConfiguration c = new CorsConfiguration();
        List<String> origenes = props.cors() == null || props.cors().allowedOrigins() == null ? List.of() : props.cors().allowedOrigins();
        c.setAllowedOrigins(origenes);
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        c.setExposedHeaders(List.of("Location", "Content-Disposition"));
        c.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", c);
        return source;
    }
}
