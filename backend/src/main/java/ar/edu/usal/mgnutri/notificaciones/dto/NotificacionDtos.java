package ar.edu.usal.mgnutri.notificaciones.dto;

import java.time.Instant;

public final class NotificacionDtos {

    private NotificacionDtos() {
    }

    public record NotificacionResponse(Long id, String tipo, Long usuarioId, String destinatario, String telefono,
                                       String plantilla, String referenciaTipo, Long referenciaId, String estado,
                                       int intentos, Instant proximoIntentoAt, String error, String waMessageId,
                                       Instant createdAt, Instant enviadaAt) {}

    public record NotificacionEncolada(Long id, String estado) {}

    public record AlertaResponse(Long id, String tipo, String mensaje, boolean prioritaria, String referenciaTipo,
                                 Long referenciaId, Instant createdAt, Instant leidaAt) {}

    public record ResumenNotificaciones(long pendientes, long fallidas, long omitidas) {}

    public record ResultadoJob(String job, int encoladas) {}
}
