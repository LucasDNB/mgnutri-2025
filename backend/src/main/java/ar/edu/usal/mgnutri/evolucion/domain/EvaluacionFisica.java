package ar.edu.usal.mgnutri.evolucion.domain;

import ar.edu.usal.mgnutri.common.EntidadAuditada;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Evaluación de desempeño físico (D-16). */
@Entity
@Table(name = "evaluacion_fisica")
public class EvaluacionFisica extends EntidadAuditada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "paciente_id", nullable = false)
    private Long pacienteId;
    @Column(name = "profesional_id", nullable = false)
    private Long profesionalId;
    @Column(nullable = false)
    private LocalDate fecha;
    @Column(nullable = false, length = 80)
    private String prueba;
    @Column(name = "ejercicio_id")
    private Long ejercicioId;
    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal valor;
    @Column(nullable = false, length = 15)
    private String unidad;
    @Column(length = 300)
    private String observaciones;

    public Long getId() { return id; }
    public Long getPacienteId() { return pacienteId; }
    public void setPacienteId(Long v) { this.pacienteId = v; }
    public Long getProfesionalId() { return profesionalId; }
    public void setProfesionalId(Long v) { this.profesionalId = v; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate v) { this.fecha = v; }
    public String getPrueba() { return prueba; }
    public void setPrueba(String v) { this.prueba = v; }
    public Long getEjercicioId() { return ejercicioId; }
    public void setEjercicioId(Long v) { this.ejercicioId = v; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal v) { this.valor = v; }
    public String getUnidad() { return unidad; }
    public void setUnidad(String v) { this.unidad = v; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String v) { this.observaciones = v; }
}
