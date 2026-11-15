package ar.edu.usal.mgnutri.profesionales.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Bloqueo de agenda: vacaciones, feriados, ausencias. */
@Entity
@Table(name = "bloqueo_agenda")
public class BloqueoAgenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "profesional_id", nullable = false)
    private Long profesionalId;

    @Column(nullable = false)
    private Instant desde;

    @Column(nullable = false)
    private Instant hasta;

    @Column(length = 200)
    private String motivo;

    protected BloqueoAgenda() {
    }

    public BloqueoAgenda(Long profesionalId, Instant desde, Instant hasta, String motivo) {
        this.profesionalId = profesionalId;
        this.desde = desde;
        this.hasta = hasta;
        this.motivo = motivo;
    }

    public boolean solapa(Instant inicio, Instant fin) {
        return desde.isBefore(fin) && hasta.isAfter(inicio);
    }

    public Long getId() { return id; }
    public Long getProfesionalId() { return profesionalId; }
    public Instant getDesde() { return desde; }
    public Instant getHasta() { return hasta; }
    public String getMotivo() { return motivo; }
}
