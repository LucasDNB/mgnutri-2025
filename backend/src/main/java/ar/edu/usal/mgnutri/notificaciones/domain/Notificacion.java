package ar.edu.usal.mgnutri.notificaciones.domain;

import ar.edu.usal.mgnutri.common.crypto.CryptoConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Fila del outbox de notificaciones (D-35). */
@Entity
@Table(name = "notificacion")
public class Notificacion {

    public enum Estado { PENDIENTE, ENVIADA, FALLIDA, OMITIDA }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoNotificacion tipo;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(length = 120)
    private String destinatario;

    @Convert(converter = CryptoConverter.class)
    @Column(length = 1024)
    private String telefono;

    @Column(nullable = false, length = 60)
    private String plantilla;

    /** JSON con los parámetros de la plantilla; cifrado y borrado tras el envío (CA-11.1.3). */
    @Convert(converter = CryptoConverter.class)
    @Column(columnDefinition = "nvarchar(max)")
    private String parametros;

    @Column(name = "referencia_tipo", length = 40)
    private String referenciaTipo;

    @Column(name = "referencia_id")
    private Long referenciaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Estado estado;

    @Column(nullable = false)
    private int intentos;

    @Column(name = "proximo_intento_at")
    private Instant proximoIntentoAt;

    @Column(length = 500)
    private String error;

    @Column(name = "wa_message_id", length = 120)
    private String waMessageId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "enviada_at")
    private Instant enviadaAt;

    protected Notificacion() {
    }

    public Notificacion(TipoNotificacion tipo, Long usuarioId, String destinatario, String telefono, String parametros,
                        String referenciaTipo, Long referenciaId, Instant ahora) {
        this.tipo = tipo;
        this.usuarioId = usuarioId;
        this.destinatario = destinatario;
        this.telefono = telefono;
        this.plantilla = tipo.plantilla();
        this.parametros = parametros;
        this.referenciaTipo = referenciaTipo;
        this.referenciaId = referenciaId;
        this.estado = Estado.PENDIENTE;
        this.createdAt = ahora;
        this.proximoIntentoAt = ahora;
    }

    public Long getId() { return id; }
    public TipoNotificacion getTipo() { return tipo; }
    public Long getUsuarioId() { return usuarioId; }
    public String getDestinatario() { return destinatario; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getPlantilla() { return plantilla; }
    public String getParametros() { return parametros; }
    public void setParametros(String parametros) { this.parametros = parametros; }
    public String getReferenciaTipo() { return referenciaTipo; }
    public Long getReferenciaId() { return referenciaId; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
    public int getIntentos() { return intentos; }
    public void setIntentos(int intentos) { this.intentos = intentos; }
    public Instant getProximoIntentoAt() { return proximoIntentoAt; }
    public void setProximoIntentoAt(Instant v) { this.proximoIntentoAt = v; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error == null ? null : error.substring(0, Math.min(500, error.length())); }
    public String getWaMessageId() { return waMessageId; }
    public void setWaMessageId(String waMessageId) { this.waMessageId = waMessageId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getEnviadaAt() { return enviadaAt; }
    public void setEnviadaAt(Instant enviadaAt) { this.enviadaAt = enviadaAt; }
}
