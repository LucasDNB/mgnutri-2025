package ar.edu.usal.mgnutri.seguridad.service;

import ar.edu.usal.mgnutri.common.Textos;
import ar.edu.usal.mgnutri.config.MgnutriProperties;
import ar.edu.usal.mgnutri.notificaciones.domain.Alerta;
import ar.edu.usal.mgnutri.notificaciones.domain.Notificacion;
import ar.edu.usal.mgnutri.notificaciones.service.AlertaService;
import ar.edu.usal.mgnutri.notificaciones.service.Notificador;
import ar.edu.usal.mgnutri.seguridad.dto.SeguridadDtos.Credenciales;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Envía las credenciales por WhatsApp (CU-37). Si el teléfono no es válido o WhatsApp está deshabilitado,
 * la contraseña temporal se devuelve una única vez al administrador para entregarla por otro medio (CA-01.1.6).
 */
@Service
public class EnvioCredencialesService {

    private final Notificador notificador;
    private final AlertaService alertas;
    private final RecuperoService recupero;
    private final MgnutriProperties props;

    public EnvioCredencialesService(Notificador notificador, AlertaService alertas, RecuperoService recupero,
                                    MgnutriProperties props) {
        this.notificador = notificador;
        this.alertas = alertas;
        this.recupero = recupero;
        this.props = props;
    }

    @Transactional
    public Resultado enviar(Credenciales c, String nombre, String telefono) {
        String enlace = "ENLACE".equalsIgnoreCase(props.whatsapp().credencialesModo())
                ? recupero.tokenActivacion(c.usuarioId()) : null;
        Notificacion n = notificador.credenciales(c.usuarioId(), nombre, telefono, c.username(), c.passwordTemporal(), enlace);
        boolean entregaManual = !Textos.telefonoValido(telefono) || !props.whatsapp().habilitado();
        if (entregaManual && Textos.telefonoValido(telefono)) {
            alertas.paraAdmins(Alerta.Tipo.CREDENCIALES_MANUALES, "WhatsApp deshabilitado: entregá a " + nombre
                    + " la contraseña temporal por otro medio.", "Usuario", c.usuarioId());
        }
        return new Resultado(n.getId(), n.getEstado().name(), entregaManual ? c.passwordTemporal() : null);
    }

    public record Resultado(Long notificacionId, String estado, String passwordVisible) {}
}
