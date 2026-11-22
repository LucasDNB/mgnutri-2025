package ar.edu.usal.mgnutri.notificaciones.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Número de destino que se manda a la API de WhatsApp. */
class DestinoWhatsAppTest {

    @Test
    void celularArgentinoSeEnviaSinElNueve() {
        assertEquals("542304217927", WhatsAppCloudAdapter.destino("+5492304217927"));
        assertEquals("541134567890", WhatsAppCloudAdapter.destino("+5491134567890"));
    }

    @Test
    void otrosNumerosNoCambian() {
        assertEquals("15551839035", WhatsAppCloudAdapter.destino("+15551839035"));
        assertEquals("541143210000", WhatsAppCloudAdapter.destino("+541143210000"));
    }

    @Test
    void elCuerpoUsaElDestinoNormalizado() {
        assertEquals("542304217927",
                WhatsAppCloudAdapter.cuerpo("mg_turno_confirmado", "+5492304217927", List.of("a"), "es_AR").get("to"));
    }
}
