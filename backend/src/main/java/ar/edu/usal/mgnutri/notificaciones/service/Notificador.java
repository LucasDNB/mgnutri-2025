package ar.edu.usal.mgnutri.notificaciones.service;

import ar.edu.usal.mgnutri.common.Fechas;
import ar.edu.usal.mgnutri.config.MgnutriProperties;
import ar.edu.usal.mgnutri.configuracion.service.ConfiguracionService;
import ar.edu.usal.mgnutri.notificaciones.domain.Notificacion;
import ar.edu.usal.mgnutri.notificaciones.domain.TipoNotificacion;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Fachada con un método por evento de negocio que arma los parámetros de cada plantilla. */
@Component
public class Notificador {

    private final NotificacionService notificaciones;
    private final ConfiguracionService config;
    private final MgnutriProperties props;

    public Notificador(NotificacionService notificaciones, ConfiguracionService config, MgnutriProperties props) {
        this.notificaciones = notificaciones;
        this.config = config;
        this.props = props;
    }

    public Notificacion credenciales(Long usuarioId, String nombre, String telefono, String username, String password,
                                     String enlaceActivacion) {
        String url = config.valor("centro.url_sistema", "");
        boolean modoEnlace = "ENLACE".equalsIgnoreCase(props.whatsapp().credencialesModo()) && enlaceActivacion != null;
        String tercero = modoEnlace ? "(usá el enlace de activación)" : password;
        String cuarto = modoEnlace ? enlaceActivacion : url;
        return notificaciones.encolar(TipoNotificacion.CREDENCIALES, usuarioId, nombre, telefono,
                List.of(nombre, username, tercero, cuarto), "Usuario", usuarioId);
    }

    public Notificacion turnoConfirmado(Long usuarioId, String nombre, String telefono, String profesional,
                                        Instant inicio, String ubicacion, Long turnoId) {
        return notificaciones.encolar(TipoNotificacion.TURNO_CONFIRMADO, usuarioId, nombre, telefono,
                List.of(nombre, profesional, Fechas.diaFecha(inicio), Fechas.hora(inicio), nvl(ubicacion)), "Turno", turnoId);
    }

    public Notificacion turnoReprogramado(Long usuarioId, String nombre, String telefono, String profesional,
                                          Instant anterior, Instant nuevo, String ubicacion, Long turnoId) {
        return notificaciones.encolar(TipoNotificacion.TURNO_REPROGRAMADO, usuarioId, nombre, telefono,
                List.of(nombre, profesional, Fechas.diaFecha(anterior) + " " + Fechas.hora(anterior),
                        Fechas.diaFecha(nuevo), Fechas.hora(nuevo), nvl(ubicacion)), "Turno", turnoId);
    }

    public Notificacion turnoCancelado(Long usuarioId, String nombre, String telefono, String profesional,
                                       Instant inicio, String motivo, Long turnoId) {
        return notificaciones.encolar(TipoNotificacion.TURNO_CANCELADO, usuarioId, nombre, telefono,
                List.of(nombre, profesional, Fechas.diaFecha(inicio), Fechas.hora(inicio),
                        motivo == null || motivo.isBlank() ? "sin especificar" : motivo), "Turno", turnoId);
    }

    public Notificacion recordatorioTurno(Long usuarioId, String nombre, String telefono, String profesional,
                                          Instant inicio, String ubicacion, Long turnoId) {
        return notificaciones.encolar(TipoNotificacion.RECORDATORIO_TURNO, usuarioId, nombre, telefono,
                List.of(nombre, Fechas.diaFecha(inicio), Fechas.hora(inicio), profesional, nvl(ubicacion)), "Turno", turnoId);
    }

    public Notificacion recordatorioDeuda(Long usuarioId, String nombre, String telefono, BigDecimal monto, Long pacienteId) {
        String link = config.valor("pagos.link", "");
        String texto = link.isBlank() ? "Acercate a la central para regularizarlo." : "Podés abonarlo en " + link;
        return notificaciones.encolar(TipoNotificacion.RECORDATORIO_DEUDA, usuarioId, nombre, telefono,
                List.of(nombre, moneda(monto), texto), "Paciente", pacienteId);
    }

    public Notificacion cambioArancel(Long usuarioId, String nombre, String telefono, String concepto,
                                      BigDecimal monto, LocalDate vigencia, Long arancelId) {
        return notificaciones.encolar(TipoNotificacion.CAMBIO_ARANCEL, usuarioId, nombre, telefono,
                List.of(nombre, Fechas.fecha(vigencia), concepto, moneda(monto)), "Arancel", arancelId);
    }

    public Notificacion recuperoPassword(Long usuarioId, String nombre, String telefono, String enlace) {
        return notificaciones.encolar(TipoNotificacion.RECUPERO_PASSWORD, usuarioId, nombre, telefono,
                List.of(nombre, enlace), "Usuario", usuarioId);
    }

    public static String moneda(BigDecimal monto) {
        return NumberFormat.getCurrencyInstance(Fechas.ES_AR).format(monto);
    }

    private static String nvl(String s) {
        return s == null || s.isBlank() ? "el centro" : s;
    }
}
