package ar.edu.usal.mgnutri.notificaciones.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Aviso interno del sistema (destinatario null = todos los administradores). */
@Entity
@Table(name = "alerta")
public class Alerta {

    public enum Tipo { NOTIFICACION_FALLIDA, MOLESTIA, COMENTARIO, BACKUP_FALLIDO, ERROR_SISTEMA, PROFESIONAL_BAJA, CREDENCIALES_MANUALES }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "destinatario_usuario_id")
    private Long destinatarioUsuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Tipo tipo;

    @Column(nullable = false, length = 500)
    private String mensaje;

    @Column(nullable = false)
    private boolean prioritaria;

    @Column(name = "referencia_tipo", length = 40)
    private String referenciaTipo;

    @Column(name = "referencia_id")
    private Long referenciaId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "leida_at")
    private Instant leidaAt;

    protected Alerta() {
    }

    public Alerta(Long destinatarioUsuarioId, Tipo tipo, String mensaje, boolean prioritaria, String referenciaTipo,
                  Long referenciaId, Instant createdAt) {
        this.destinatarioUsuarioId = destinatarioUsuarioId;
        this.tipo = tipo;
        this.mensaje = mensaje.substring(0, Math.min(500, mensaje.length()));
        this.prioritaria = prioritaria;
        this.referenciaTipo = referenciaTipo;
        this.referenciaId = referenciaId;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Long getDestinatarioUsuarioId() { return destinatarioUsuarioId; }
    public Tipo getTipo() { return tipo; }
    public String getMensaje() { return mensaje; }
    public boolean isPrioritaria() { return prioritaria; }
    public String getReferenciaTipo() { return referenciaTipo; }
    public Long getReferenciaId() { return referenciaId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLeidaAt() { return leidaAt; }
    public void setLeidaAt(Instant leidaAt) { this.leidaAt = leidaAt; }
}
