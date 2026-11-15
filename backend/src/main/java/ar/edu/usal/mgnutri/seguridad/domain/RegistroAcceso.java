package ar.edu.usal.mgnutri.seguridad.domain;

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

/** Registro de accesos (RF-16). Append-only: la BD rechaza UPDATE/DELETE. */
@Entity
@Immutable
@Table(name = "registro_acceso")
public class RegistroAcceso {

    public enum Accion { LOGIN_OK, LOGIN_FALLIDO, LOGOUT, BLOQUEO, RECUPERO_SOLICITADO, PASSWORD_RESTABLECIDA, PASSWORD_CAMBIADA }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_hora", nullable = false)
    private Instant fechaHora;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "username_intentado", length = 40)
    private String usernameIntentado;

    @Column(length = 100)
    private String roles;

    @Column(length = 60)
    private String ip;

    @Column(name = "user_agent", length = 300)
    private String userAgent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Accion accion;

    @Column(length = 300)
    private String detalle;

    protected RegistroAcceso() {
    }

    public RegistroAcceso(Instant fechaHora, Long usuarioId, String usernameIntentado, String roles, String ip,
                          String userAgent, Accion accion, String detalle) {
        this.fechaHora = fechaHora;
        this.usuarioId = usuarioId;
        this.usernameIntentado = usernameIntentado;
        this.roles = roles;
        this.ip = ip;
        this.userAgent = userAgent == null ? null : userAgent.substring(0, Math.min(300, userAgent.length()));
        this.accion = accion;
        this.detalle = detalle;
    }

    public Long getId() { return id; }
    public Instant getFechaHora() { return fechaHora; }
    public Long getUsuarioId() { return usuarioId; }
    public String getUsernameIntentado() { return usernameIntentado; }
    public String getRoles() { return roles; }
    public String getIp() { return ip; }
    public String getUserAgent() { return userAgent; }
    public Accion getAccion() { return accion; }
    public String getDetalle() { return detalle; }
}
