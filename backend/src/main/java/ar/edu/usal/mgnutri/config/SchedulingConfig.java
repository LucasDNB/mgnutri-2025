package ar.edu.usal.mgnutri.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Tareas programadas (D-32) con zona America/Argentina/Buenos_Aires. Se desactivan con JOBS_ENABLED=false. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "mgnutri.jobs", name = "habilitados", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
