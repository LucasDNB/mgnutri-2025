package ar.edu.usal.mgnutri.seguridad.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "token_recupero")
public class TokenRecupero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "expira_at", nullable = false)
    private Instant expiraAt;

    @Column(name = "usado_at")
    private Instant usadoAt;

    @Column(nullable = false, length = 20)
    private String canal;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TokenRecupero() {
    }

    public TokenRecupero(Long usuarioId, String tokenHash, Instant expiraAt, String canal, Instant createdAt) {
        this.usuarioId = usuarioId;
        this.tokenHash = tokenHash;
        this.expiraAt = expiraAt;
        this.canal = canal;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public String getTokenHash() { return tokenHash; }
    public Instant getExpiraAt() { return expiraAt; }
    public Instant getUsadoAt() { return usadoAt; }
    public void setUsadoAt(Instant usadoAt) { this.usadoAt = usadoAt; }
    public String getCanal() { return canal; }
    public Instant getCreatedAt() { return createdAt; }
}
