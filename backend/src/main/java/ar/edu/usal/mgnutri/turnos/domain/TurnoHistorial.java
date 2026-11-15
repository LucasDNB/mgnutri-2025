package ar.edu.usal.mgnutri.turnos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Horario anterior de un turno reprogramado (CA-03.2.3). */
@Entity
@Table(name = "turno_historial")
public class TurnoHistorial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "turno_id", nullable = false)
    private Long turnoId;

    @Column(name = "inicio_anterior", nullable = false)
    private Instant inicioAnterior;

    @Column(name = "fin_anterior", nullable = false)
    private Instant finAnterior;

    @Column(length = 200)
    private String motivo;

    @Column(name = "cambiado_por", length = 40)
    private String cambiadoPor;

    @Column(name = "cambiado_at", nullable = false)
    private Instant cambiadoAt;

    protected TurnoHistorial() {
    }

    public TurnoHistorial(Long turnoId, Instant inicioAnterior, Instant finAnterior, String motivo, String cambiadoPor,
                          Instant cambiadoAt) {
        this.turnoId = turnoId;
        this.inicioAnterior = inicioAnterior;
        this.finAnterior = finAnterior;
        this.motivo = motivo;
        this.cambiadoPor = cambiadoPor;
        this.cambiadoAt = cambiadoAt;
    }

    public Long getId() { return id; }
    public Long getTurnoId() { return turnoId; }
    public Instant getInicioAnterior() { return inicioAnterior; }
    public Instant getFinAnterior() { return finAnterior; }
    public String getMotivo() { return motivo; }
    public String getCambiadoPor() { return cambiadoPor; }
    public Instant getCambiadoAt() { return cambiadoAt; }
}
