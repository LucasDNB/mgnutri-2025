package ar.edu.usal.mgnutri.backup.service;

import ar.edu.usal.mgnutri.backup.domain.BackupRegistro;
import ar.edu.usal.mgnutri.common.Fechas;
import ar.edu.usal.mgnutri.configuracion.service.ConfiguracionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.SimpleTriggerContext;
import org.springframework.stereotype.Component;

import java.time.Instant;

/** Backup programado (CU-52): el cron se relee de configuracion_sistema en cada disparo (sin reinicio). */
@Component
@ConditionalOnProperty(prefix = "mgnutri.jobs", name = "habilitados", havingValue = "true", matchIfMissing = true)
public class BackupScheduler implements SchedulingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(BackupScheduler.class);
    private final BackupService backups;
    private final ConfiguracionService config;

    public BackupScheduler(BackupService backups, ConfiguracionService config) {
        this.backups = backups;
        this.config = config;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(this::ejecutar, ctx -> {
            String cron = config.valor("backup.programacion_cron", "0 0 3 * * SUN");
            if (!CronExpression.isValidExpression(cron)) {
                cron = "0 0 3 * * SUN";
            }
            Instant base = ctx.lastCompletion() == null ? Instant.now() : ctx.lastCompletion();
            // Se re-evalúa cada hora como máximo para tomar cambios de programación
            Instant proximo = new CronTrigger(cron, Fechas.ZONA).nextExecution(new SimpleTriggerContext(base, base, base));
            Instant enUnaHora = Instant.now().plusSeconds(3600);
            return proximo == null || proximo.isAfter(enUnaHora) ? enUnaHora : proximo;
        });
    }

    private void ejecutar() {
        if (!Boolean.parseBoolean(config.valor("backup.habilitado", "false"))) {
            return;
        }
        String cron = config.valor("backup.programacion_cron", "");
        if (!CronExpression.isValidExpression(cron)) {
            return;
        }
        // Sólo dispara si "ahora" coincide (±1 min) con una ejecución del cron
        Instant ahora = Instant.now();
        Instant previo = new CronTrigger(cron, Fechas.ZONA).nextExecution(new SimpleTriggerContext(ahora.minusSeconds(61), ahora.minusSeconds(61), ahora.minusSeconds(61)));
        if (previo == null || previo.isAfter(ahora.plusSeconds(1))) {
            return;
        }
        try {
            backups.iniciar(BackupRegistro.Tipo.AUTOMATICO);
        } catch (RuntimeException e) {
            log.warn("No se pudo iniciar el backup automático: {}", e.getMessage());
        }
    }
}
