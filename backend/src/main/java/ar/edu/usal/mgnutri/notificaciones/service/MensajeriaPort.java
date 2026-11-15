package ar.edu.usal.mgnutri.notificaciones.service;

import java.util.List;

/** Puerto de mensajería (constitución art. IV.4): WhatsApp Cloud API o adaptador "log". */
public interface MensajeriaPort {

    ResultadoEnvio enviar(String plantilla, String telefonoE164, List<String> parametros);

    record ResultadoEnvio(boolean ok, boolean reintentable, String messageId, String error) {
        public static ResultadoEnvio exito(String id) {
            return new ResultadoEnvio(true, false, id, null);
        }

        public static ResultadoEnvio falla(boolean reintentable, String error) {
            return new ResultadoEnvio(false, reintentable, null, error);
        }
    }
}
