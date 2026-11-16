package ar.edu.usal.mgnutri.entrenamiento.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Registro de ejercicio realizado (CU-25, RF-15). Guarda el ejercicio de catálogo para recordar cargas entre rutinas. */
@Entity
@Table(name = "registro_entrenamiento")
public class RegistroEntrenamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "paciente_id", nullable = false)
    private Long pacienteId;
    @Column(name = "ejercicio_rutina_id", nullable = false)
    private Long ejercicioRutinaId;
    @Column(name = "ejercicio_id", nullable = false)
    private Long ejercicioId;
    @Column(nullable = false)
    private LocalDate fecha;
    @Column(nullable = false)
    private boolean completado;
    @Column(name = "series_realizadas")
    private Integer seriesRealizadas;
    @Column(name = "repeticiones_realizadas")
    private Integer repeticionesRealizadas;
    @Column(name = "carga_kg", precision = 6, scale = 2)
    private BigDecimal cargaKg;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RegistroEntrenamiento() {
    }

    public RegistroEntrenamiento(Long pacienteId, Long ejercicioRutinaId, Long ejercicioId, LocalDate fecha, Instant createdAt) {
        this.pacienteId = pacienteId;
        this.ejercicioRutinaId = ejercicioRutinaId;
        this.ejercicioId = ejercicioId;
        this.fecha = fecha;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Long getPacienteId() { return pacienteId; }
    public Long getEjercicioRutinaId() { return ejercicioRutinaId; }
    public Long getEjercicioId() { return ejercicioId; }
    public void setEjercicioId(Long v) { this.ejercicioId = v; }
    public LocalDate getFecha() { return fecha; }
    public boolean isCompletado() { return completado; }
    public void setCompletado(boolean v) { this.completado = v; }
    public Integer getSeriesRealizadas() { return seriesRealizadas; }
    public void setSeriesRealizadas(Integer v) { this.seriesRealizadas = v; }
    public Integer getRepeticionesRealizadas() { return repeticionesRealizadas; }
    public void setRepeticionesRealizadas(Integer v) { this.repeticionesRealizadas = v; }
    public BigDecimal getCargaKg() { return cargaKg; }
    public void setCargaKg(BigDecimal v) { this.cargaKg = v; }
    public Instant getCreatedAt() { return createdAt; }
}
