package ar.edu.usal.mgnutri.notificaciones.domain;

/** Tipos de notificación y su plantilla de WhatsApp (spec 11 §Plantillas). */
public enum TipoNotificacion {
    CREDENCIALES("mg_credenciales"),
    TURNO_CONFIRMADO("mg_turno_confirmado"),
    TURNO_REPROGRAMADO("mg_turno_reprogramado"),
    TURNO_CANCELADO("mg_turno_cancelado"),
    RECORDATORIO_TURNO("mg_recordatorio_turno"),
    RECORDATORIO_DEUDA("mg_recordatorio_deuda"),
    CAMBIO_ARANCEL("mg_cambio_arancel"),
    RECUPERO_PASSWORD("mg_recupero_password");

    private final String plantilla;

    TipoNotificacion(String plantilla) {
        this.plantilla = plantilla;
    }

    public String plantilla() {
        return plantilla;
    }
}
