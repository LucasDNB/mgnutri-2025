package ar.edu.usal.mgnutri.config;

import ar.edu.usal.mgnutri.common.Fechas;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/** Reloj inyectable (tests de reglas de 24 h / 7 días y jobs) y auditoría de filas. */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditor", dateTimeProviderRef = "proveedorFecha")
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.system(Fechas.ZONA);
    }

    @Bean
    public DateTimeProvider proveedorFecha(Clock clock) {
        return () -> Optional.of(clock.instant().truncatedTo(ChronoUnit.SECONDS));
    }

    @Bean
    public AuditorAware<String> auditor() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getName() == null || "anonymousUser".equals(auth.getName())) {
                return Optional.of("SISTEMA");
            }
            return Optional.of(auth.getName());
        };
    }
}
