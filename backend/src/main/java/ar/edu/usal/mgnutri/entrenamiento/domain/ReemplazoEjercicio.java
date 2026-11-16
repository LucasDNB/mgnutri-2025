package ar.edu.usal.mgnutri.entrenamiento.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/** Variante de ejercicio elegida por el paciente (CU-26, D-18). */
@Entity
@Table(name = "reemplazo_ejercicio")
public class ReemplazoEjercicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "ejercicio_rutina_id", nullable = false)
    private Long ejercicioRutinaId;
    @Column(name = "ejercicio_reemplazo_id", nullable = false)
    private Long ejercicioReemplazoId;
    @Column(name = "paciente_id", nullable = false)
    private Long pacienteId;
    @Column(nullable = false)
    private LocalDate fecha;
    @Column(nullable = false)
    private boolean activo = true;

    protected ReemplazoEjercicio() {
    }

    public ReemplazoEjercicio(Long ejercicioRutinaId, Long ejercicioReemplazoId, Long pacienteId, LocalDate fecha) {
        this.ejercicioRutinaId = ejercicioRutinaId;
        this.ejercicioReemplazoId = ejercicioReemplazoId;
        this.pacienteId = pacienteId;
        this.fecha = fecha;
    }

    public Long getId() { return id; }
    public Long getEjercicioRutinaId() { return ejercicioRutinaId; }
    public Long getEjercicioReemplazoId() { return ejercicioReemplazoId; }
    public Long getPacienteId() { return pacienteId; }
    public LocalDate getFecha() { return fecha; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean v) { this.activo = v; }
}
