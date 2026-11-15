package ar.edu.usal.mgnutri.seguridad.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "token_revocado")
public class TokenRevocado {

    @Id
    @Column(length = 36)
    private String jti;

    @Column(name = "expira_at", nullable = false)
    private Instant expiraAt;

    protected TokenRevocado() {
    }

    public TokenRevocado(String jti, Instant expiraAt) {
        this.jti = jti;
        this.expiraAt = expiraAt;
    }

    public String getJti() { return jti; }
    public Instant getExpiraAt() { return expiraAt; }
}
