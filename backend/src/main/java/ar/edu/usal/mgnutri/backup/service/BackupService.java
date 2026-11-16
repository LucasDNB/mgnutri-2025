package ar.edu.usal.mgnutri.backup.service;

import ar.edu.usal.mgnutri.backup.dao.BackupRegistroDao;
import ar.edu.usal.mgnutri.backup.domain.BackupRegistro;
import ar.edu.usal.mgnutri.backup.dto.BackupDtos.BackupResponse;
import ar.edu.usal.mgnutri.backup.dto.BackupDtos.ProgramacionDto;
import ar.edu.usal.mgnutri.bitacora.domain.BitacoraEvento;
import ar.edu.usal.mgnutri.bitacora.domain.Modulo;
import ar.edu.usal.mgnutri.bitacora.domain.TipoEvento;
import ar.edu.usal.mgnutri.bitacora.service.BitacoraService;
import ar.edu.usal.mgnutri.common.Fechas;
import ar.edu.usal.mgnutri.common.PageResponse;
import ar.edu.usal.mgnutri.common.error.BusinessException;
import ar.edu.usal.mgnutri.common.error.ErrorCode;
import ar.edu.usal.mgnutri.common.security.CurrentUser;
import ar.edu.usal.mgnutri.configuracion.service.ConfiguracionService;
import ar.edu.usal.mgnutri.notificaciones.domain.Alerta;
import ar.edu.usal.mgnutri.notificaciones.service.AlertaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.Executor;

/** Backup manual (CU-51) y programado (CU-52). */
@Service
public class BackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupService.class);
    private static final DateTimeFormatter NOMBRE = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final String[] DIAS_CRON = {"", "MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};
    private static final String[] DIAS = {"", "lunes", "martes", "miércoles", "jueves", "viernes", "sábados", "domingos"};

    private final BackupRegistroDao dao;
    private final BackupProvider provider;
    private final ConfiguracionService config;
    private final AlertaService alertas;
    private final BitacoraService bitacora;
    private final TransactionTemplate tx;
    private final Executor executor;
    private final Clock clock;

    public BackupService(BackupRegistroDao dao, BackupProvider provider, ConfiguracionService config, AlertaService alertas,
                         BitacoraService bitacora, TransactionTemplate tx,
                         @org.springframework.beans.factory.annotation.Qualifier("tareasExecutor") Executor executor, Clock clock) {
        this.dao = dao;
        this.provider = provider;
        this.config = config;
        this.alertas = alertas;
        this.bitacora = bitacora;
        this.tx = tx;
        this.executor = executor;
        this.clock = clock;
    }

    public PageResponse<BackupResponse> listar(Pageable p) {
        return PageResponse.de(dao.findAll(p), BackupService::aDto);
    }

    public BackupResponse obtener(Long id) {
        return dao.findById(id).map(BackupService::aDto).orElseThrow(() -> BusinessException.noEncontrado("Backup"));
    }

    /** Inicia el backup en segundo plano; 409 si hay otro en curso (CA-15.1.5). */
    public synchronized BackupResponse iniciar(BackupRegistro.Tipo tipo) {
        if (dao.existsByEstado(BackupRegistro.Estado.EN_CURSO)) {
            throw new BusinessException(ErrorCode.BACKUP_EN_CURSO, "Ya hay un backup en curso. Esperá a que termine.");
        }
        String quien = CurrentUser.opcional().map(u -> u.username()).orElse("SISTEMA");
        Instant ahora = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        String archivo = "mgnutri-" + NOMBRE.format(ahora.atZone(Fechas.ZONA)) + "." + provider.extension();
        BackupRegistro r = tx.execute(s -> dao.save(new BackupRegistro(tipo, ahora, archivo, quien)));
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        executor.execute(() -> {
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
            try {
                ejecutar(r.getId());
            } finally {
                org.springframework.security.core.context.SecurityContextHolder.clearContext();
            }
        });
        return aDto(r);
    }

    void ejecutar(Long id) {
        BackupRegistro r = dao.findById(id).orElseThrow();
        try {
            BackupProvider.Resultado res = provider.ejecutar(r.getArchivo());
            tx.executeWithoutResult(s -> {
                BackupRegistro x = dao.findById(id).orElseThrow();
                x.setEstado(BackupRegistro.Estado.OK);
                x.setTamanoBytes(res.tamanoBytes());
                x.setUbicacion(res.ubicacion());
                x.setFinalizadoAt(clock.instant().truncatedTo(ChronoUnit.SECONDS));
            });
            bitacora.ok(Modulo.BACKUP, TipoEvento.BACKUP, "BackupRegistro", id, "Backup " + r.getTipo() + " generado: " + r.getArchivo());
            if (r.getTipo() == BackupRegistro.Tipo.AUTOMATICO) {
                aplicarRetencion();
            }
        } catch (RuntimeException e) {
            log.error("Error en backup {}", id, e);
            tx.executeWithoutResult(s -> {
                BackupRegistro x = dao.findById(id).orElseThrow();
                x.setEstado(BackupRegistro.Estado.ERROR);
                x.setError(e.getMessage());
                x.setFinalizadoAt(clock.instant().truncatedTo(ChronoUnit.SECONDS));
            });
            bitacora.registrar(Modulo.BACKUP, TipoEvento.BACKUP, "BackupRegistro", id, "Backup fallido",
                    BitacoraEvento.Resultado.ERROR, Map.of("error", String.valueOf(e.getMessage())));
            alertas.paraAdmins(Alerta.Tipo.BACKUP_FALLIDO, "Falló el backup " + r.getArchivo() + ": " + e.getMessage(), "BackupRegistro", id);
        }
    }

    /** CA-15.1.3: descarga con enlace temporal (15 min) o contenido directo; auditada. */
    public BackupProvider.Descarga descargar(Long id) {
        BackupRegistro r = dao.findById(id).orElseThrow(() -> BusinessException.noEncontrado("Backup"));
        if (r.getEstado() != BackupRegistro.Estado.OK || r.isExpirado()) {
            throw new BusinessException(ErrorCode.REGLA_NEGOCIO, "El backup no está disponible para descargar.");
        }
        BackupProvider.Descarga d = provider.descargar(r.getArchivo(), Duration.ofMinutes(15));
        bitacora.ok(Modulo.BACKUP, TipoEvento.DESCARGA, "BackupRegistro", id, "Descarga de backup " + r.getArchivo());
        return d;
    }

    public String archivo(Long id) {
        return dao.findById(id).map(BackupRegistro::getArchivo).orElse("backup");
    }

    /** CA-15.2.5: borra del almacenamiento los archivos más viejos que la retención; el registro queda marcado. */
    public void aplicarRetencion() {
        int dias = Integer.parseInt(config.valor("backup.retencion_dias", "30"));
        Instant limite = clock.instant().minus(Duration.ofDays(dias));
        for (BackupRegistro r : dao.findByEstadoAndExpiradoFalseAndIniciadoAtBefore(BackupRegistro.Estado.OK, limite)) {
            provider.eliminar(r.getArchivo());
            tx.executeWithoutResult(s -> dao.findById(r.getId()).ifPresent(x -> x.setExpirado(true)));
        }
    }

    // ---------------------------------------------------------------- programación (CU-52)

    public ProgramacionDto programacion() {
        ProgramacionDto p = new ProgramacionDto(Boolean.parseBoolean(config.valor("backup.habilitado", "false")),
                config.valor("backup.frecuencia", "SEMANAL"), Integer.valueOf(config.valor("backup.dia_semana", "7")),
                Integer.valueOf(config.valor("backup.dia_mes", "1")), config.valor("backup.hora", "03:00"),
                Integer.valueOf(config.valor("backup.retencion_dias", "30")), null, config.valor("backup.programacion_cron", ""));
        return new ProgramacionDto(p.habilitado(), p.frecuencia(), p.diaSemana(), p.diaMes(), p.hora(), p.retencionDias(), describir(p), cron(p));
    }

    public ProgramacionDto guardarProgramacion(ProgramacionDto p) {
        if ("SEMANAL".equals(p.frecuencia()) && p.diaSemana() == null) {
            throw BusinessException.validacion("diaSemana", "Elegí el día de la semana.");
        }
        if ("MENSUAL".equals(p.frecuencia()) && p.diaMes() == null) {
            throw BusinessException.validacion("diaMes", "Elegí el día del mes.");
        }
        tx.executeWithoutResult(s -> {
            config.establecer("backup.habilitado", String.valueOf(p.habilitado()));
            config.establecer("backup.frecuencia", p.frecuencia());
            config.establecer("backup.dia_semana", String.valueOf(p.diaSemana() == null ? 7 : p.diaSemana()));
            config.establecer("backup.dia_mes", String.valueOf(p.diaMes() == null ? 1 : p.diaMes()));
            config.establecer("backup.hora", p.hora());
            config.establecer("backup.retencion_dias", String.valueOf(p.retencionDias()));
            config.establecer("backup.programacion_cron", cron(p));
        });
        ProgramacionDto r = programacion();
        bitacora.ok(Modulo.BACKUP, TipoEvento.CONFIGURACION, "ConfiguracionSistema", "backup", "Programación de backup: " + r.descripcion());
        return r;
    }

    /** Conversión UI → cron: diaria 0 m h * * *; semanal 0 m h * * DOW; mensual 0 m h D * *. */
    public static String cron(ProgramacionDto p) {
        String[] hm = p.hora().split(":");
        int h = Integer.parseInt(hm[0]);
        int m = Integer.parseInt(hm[1]);
        return switch (p.frecuencia()) {
            case "DIARIA" -> "0 " + m + " " + h + " * * *";
            case "MENSUAL" -> "0 " + m + " " + h + " " + (p.diaMes() == null ? 1 : p.diaMes()) + " * *";
            default -> "0 " + m + " " + h + " * * " + DIAS_CRON[p.diaSemana() == null ? 7 : p.diaSemana()];
        };
    }

    public static String describir(ProgramacionDto p) {
        if (!Boolean.TRUE.equals(p.habilitado())) {
            return "Backup automático deshabilitado";
        }
        String cuando = switch (p.frecuencia()) {
            case "DIARIA" -> "Todos los días";
            case "MENSUAL" -> "El día " + p.diaMes() + " de cada mes";
            default -> "Todos los " + DIAS[p.diaSemana() == null ? 7 : p.diaSemana()];
        };
        return cuando + " a las " + p.hora() + ", se conservan " + p.retencionDias() + " días";
    }

    static BackupResponse aDto(BackupRegistro r) {
        return new BackupResponse(r.getId(), r.getTipo().name(), r.getIniciadoAt(), r.getFinalizadoAt(), r.getEstado().name(),
                r.getArchivo(), r.getTamanoBytes(), r.getSolicitadoPor(), r.getError(), r.isExpirado());
    }

    @SuppressWarnings("unused")
    private TaskExecutor ninguno() {
        return null;
    }
}
