package ar.edu.usal.mgnutri.pacientes.domain;

import ar.edu.usal.mgnutri.common.Area;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/** Asignación paciente ↔ profesional por área (CU-29, D-07). */
@Entity
@Table(name = "paciente_profesional")
public class PacienteProfesional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "paciente_id", nullable = false)
    private Long pacienteId;

    @Column(name = "profesional_id", nullable = false)
    private Long profesionalId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Area area;

    @Column(name = "fecha_asignacion", nullable = false)
    private LocalDate fechaAsignacion;

    @Column(nullable = false)
    private boolean activo = true;

    protected PacienteProfesional() {
    }

    public PacienteProfesional(Long pacienteId, Long profesionalId, Area area, LocalDate fechaAsignacion) {
        this.pacienteId = pacienteId;
        this.profesionalId = profesionalId;
        this.area = area;
        this.fechaAsignacion = fechaAsignacion;
    }

    public Long getId() { return id; }
    public Long getPacienteId() { return pacienteId; }
    public Long getProfesionalId() { return profesionalId; }
    public Area getArea() { return area; }
    public LocalDate getFechaAsignacion() { return fechaAsignacion; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
