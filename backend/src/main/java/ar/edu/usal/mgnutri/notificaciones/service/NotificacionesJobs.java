package ar.edu.usal.mgnutri.notificaciones.service;

import ar.edu.usal.mgnutri.common.Fechas;
import ar.edu.usal.mgnutri.configuracion.service.ConfiguracionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalTime;
import java.util.Date;

/**
 * Jobs de notificaciones (docs/02-arquitectura.md §7). La hora del recordatorio de turnos y el cron de deuda
 * se leen de configuracion_sistema en cada disparo, así un cambio aplica sin reiniciar.
 */
@Component
@ConditionalOnProperty(prefix = "mgnutri.jobs", name = "habilitados", havingValue = "true", matchIfMissing = true)
public class NotificacionesJobs implements SchedulingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(NotificacionesJobs.class);

    private final NotificacionService notificaciones;
    private final RecordatoriosService recordatorios;
    private final ConfiguracionService config;

    public NotificacionesJobs(NotificacionService notificaciones, RecordatoriosService recordatorios, ConfiguracionService config) {
        this.notificaciones = notificaciones;
        this.recordatorios = recordatorios;
        this.config = config;
    }

    /** Despacho del outbox cada minuto. */
    @Scheduled(cron = "${mgnutri.jobs.despacho-cron}", zone = "America/Argentina/Buenos_Aires")
    public void despachar() {
        try {
            int n = notificaciones.despacharPendientes();
            if (n > 0) {
                log.info("Despachadas {} notificaciones", n);
            }
        } catch (RuntimeException e) {
            log.error("Error al despachar notificaciones", e);
        }
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(this::recordarTurnos, ctx -> proximo(cronTurnos(), ctx.lastCompletion()));
        registrar.addTriggerTask(this::recordarDeudas, ctx -> proximo(config.valor("pagos.recordatorio_cron", "0 0 10 * * SAT"),
                ctx.lastCompletion()));
    }

    String cronTurnos() {
        String hora = config.valor("turnos.recordatorio_hora", "09:00");
        try {
            LocalTime t = LocalTime.parse(hora);
            return "0 " + t.getMinute() + " " + t.getHour() + " * * *";
        } catch (RuntimeException e) {
            return "0 0 9 * * *";
        }
    }

    private static Instant proximo(String cron, Instant ultima) {
        String expr = CronExpression.isValidExpression(cron) ? cron : "0 0 9 * * *";
        CronTrigger trigger = new CronTrigger(expr, Fechas.ZONA);
        Instant base = ultima == null ? Instant.now() : ultima;
        return trigger.nextExecution(new org.springframework.scheduling.support.SimpleTriggerContext(base, base, base));
    }

    private void recordarTurnos() {
        try {
            recordatorios.recordarTurnosDeManiana();
        } catch (RuntimeException e) {
            log.error("Error en recordatorio de turnos", e);
        }
    }

    private void recordarDeudas() {
        try {
            recordatorios.recordarDeudas();
        } catch (RuntimeException e) {
            log.error("Error en recordatorio de deudas", e);
        }
    }

    @SuppressWarnings("unused")
    private static Date fecha(Instant i) {
        return Date.from(i);
    }
}
