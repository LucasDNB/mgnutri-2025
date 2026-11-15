package ar.edu.usal.mgnutri.bitacora.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.time.Instant;

/** Evento de bitácora (RF-17). Append-only: la BD rechaza UPDATE/DELETE (D-28). */
@Entity
@Immutable
@Table(name = "bitacora_evento")
public class BitacoraEvento {

    public enum Resultado { OK, ERROR }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_hora", nullable = false)
    private Instant fechaHora;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(length = 40)
    private String username;

    @Column(length = 100)
    private String roles;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Modulo modulo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento", nullable = false, length = 20)
    private TipoEvento tipoEvento;

    @Column(length = 40)
    private String entidad;

    @Column(name = "entidad_id", length = 40)
    private String entidadId;

    @Column(length = 500)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Resultado resultado;

    @Column(length = 60)
    private String ip;

    @Column(columnDefinition = "nvarchar(max)")
    private String detalle;

    protected BitacoraEvento() {
    }

    public BitacoraEvento(Instant fechaHora, Long usuarioId, String username, String roles, Modulo modulo,
                          TipoEvento tipoEvento, String entidad, String entidadId, String descripcion,
                          Resultado resultado, String ip, String detalle) {
        this.fechaHora = fechaHora;
        this.usuarioId = usuarioId;
        this.username = username;
        this.roles = roles;
        this.modulo = modulo;
        this.tipoEvento = tipoEvento;
        this.entidad = entidad;
        this.entidadId = entidadId;
        this.descripcion = descripcion == null ? null : descripcion.substring(0, Math.min(500, descripcion.length()));
        this.resultado = resultado;
        this.ip = ip;
        this.detalle = detalle;
    }

    public Long getId() { return id; }
    public Instant getFechaHora() { return fechaHora; }
    public Long getUsuarioId() { return usuarioId; }
    public String getUsername() { return username; }
    public String getRoles() { return roles; }
    public Modulo getModulo() { return modulo; }
    public TipoEvento getTipoEvento() { return tipoEvento; }
    public String getEntidad() { return entidad; }
    public String getEntidadId() { return entidadId; }
    public String getDescripcion() { return descripcion; }
    public Resultado getResultado() { return resultado; }
    public String getIp() { return ip; }
    public String getDetalle() { return detalle; }
}
