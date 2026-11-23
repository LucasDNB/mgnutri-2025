package ar.edu.usal.mgnutri.nutricion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

/** Registro de cumplimiento de una comida (CU-17, RF-15). */
@Entity
@Table(name = "registro_comida")
public class RegistroComida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "paciente_id", nullable = false)
    private Long pacienteId;

    @Column(name = "comida_plan_id", nullable = false)
    private Long comidaPlanId;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private boolean realizada;

    @Column(name = "con_cambios", nullable = false)
    private boolean conCambios;

    @Column(name = "no_le_gusto", nullable = false)
    private boolean noLeGusto;

    @Column(name = "le_cayo_mal", nullable = false)
    private boolean leCayoMal;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RegistroComida() {
    }

    public RegistroComida(Long pacienteId, Long comidaPlanId, LocalDate fecha, Instant createdAt) {
        this.pacienteId = pacienteId;
        this.comidaPlanId = comidaPlanId;
        this.fecha = fecha;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Long getPacienteId() { return pacienteId; }
    public Long getComidaPlanId() { return comidaPlanId; }
    public LocalDate getFecha() { return fecha; }
    public boolean isRealizada() { return realizada; }
    public void setRealizada(boolean realizada) { this.realizada = realizada; }
    public boolean isConCambios() { return conCambios; }
    public void setConCambios(boolean conCambios) { this.conCambios = conCambios; }
    public boolean isNoLeGusto() { return noLeGusto; }
    public void setNoLeGusto(boolean noLeGusto) { this.noLeGusto = noLeGusto; }
    public boolean isLeCayoMal() { return leCayoMal; }
    public void setLeCayoMal(boolean leCayoMal) { this.leCayoMal = leCayoMal; }
    public Instant getCreatedAt() { return createdAt; }
}
