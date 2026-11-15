package ar.edu.usal.mgnutri.notificaciones.service;

import ar.edu.usal.mgnutri.common.Textos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/** Adaptador local: registra el envío en el log sin datos sensibles (CA-11.5.4). */
@Component
@ConditionalOnProperty(prefix = "mgnutri.whatsapp", name = "habilitado", havingValue = "false", matchIfMissing = true)
public class LogMensajeriaAdapter implements MensajeriaPort {

    private static final Logger log = LoggerFactory.getLogger(LogMensajeriaAdapter.class);

    @Override
    public ResultadoEnvio enviar(String plantilla, String telefonoE164, List<String> parametros) {
        String id = "LOCAL-" + UUID.randomUUID();
        log.info("[WhatsApp simulado] plantilla={} destino=••{} parametros={} id={}", plantilla,
                Textos.ultimosDigitos(telefonoE164, 4), parametros.size(), id);
        return ResultadoEnvio.exito(id);
    }
}
