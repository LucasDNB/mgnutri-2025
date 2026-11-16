package ar.edu.usal.mgnutri.entrenamiento.domain;

import ar.edu.usal.mgnutri.common.EntidadAuditada;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Rutina de entrenamiento (spec 05). */
@Entity
@Table(name = "rutina")
public class Rutina extends EntidadAuditada {

    public enum Estado { ACTIVA, FINALIZADA, ELIMINADA }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "paciente_id", nullable = false)
    private Long pacienteId;
    @Column(name = "entrenador_id", nullable = false)
    private Long entrenadorId;
    @Column(nullable = false, length = 120)
    private String nombre;
    @Column(length = 300)
    private String objetivo;
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;
    @Column(name = "duracion_semanas", nullable = false)
    private int duracionSemanas;
    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Estado estado = Estado.ACTIVA;
    @Column(length = 1000)
    private String observaciones;
    @OneToMany(mappedBy = "rutina", cascade = CascadeType.ALL)
    @OrderBy("orden ASC")
    private List<DiaRutina> dias = new ArrayList<>();

    public void recalcularFin() {
        this.fechaFin = fechaInicio.plusDays(duracionSemanas * 7L - 1);
    }

    public List<DiaRutina> diasActivos() {
        return dias.stream().filter(DiaRutina::isActivo).toList();
    }

    public Long getId() { return id; }
    public Long getPacienteId() { return pacienteId; }
    public void setPacienteId(Long v) { this.pacienteId = v; }
    public Long getEntrenadorId() { return entrenadorId; }
    public void setEntrenadorId(Long v) { this.entrenadorId = v; }
    public String getNombre() { return nombre; }
    public void setNombre(String v) { this.nombre = v; }
    public String getObjetivo() { return objetivo; }
    public void setObjetivo(String v) { this.objetivo = v; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate v) { this.fechaInicio = v; }
    public int getDuracionSemanas() { return duracionSemanas; }
    public void setDuracionSemanas(int v) { this.duracionSemanas = v; }
    public LocalDate getFechaFin() { return fechaFin; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado v) { this.estado = v; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String v) { this.observaciones = v; }
    public List<DiaRutina> getDias() { return dias; }
}
