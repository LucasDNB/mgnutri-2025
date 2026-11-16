package ar.edu.usal.mgnutri.novedades.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

/** Molestia física reportada por el paciente (CU-42). */
@Entity
@Table(name = "molestia")
public class Molestia {

    public enum Contexto { ENTRENAMIENTO, ALIMENTACION, GENERAL }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "paciente_id", nullable = false)
    private Long pacienteId;
    @Column(nullable = false)
    private LocalDate fecha;
    @Column(nullable = false, length = 300)
    private String descripcion;
    @Column(nullable = false)
    private int intensidad;
    @Column(name = "ubicacion_corporal", length = 60)
    private String ubicacionCorporal;
    @Column(name = "ejercicio_rutina_id")
    private Long ejercicioRutinaId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Contexto contexto;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "revisada_at")
    private Instant revisadaAt;
    @Column(name = "revisada_por", length = 40)
    private String revisadaPor;

    public Long getId() { return id; }
    public Long getPacienteId() { return pacienteId; }
    public void setPacienteId(Long v) { this.pacienteId = v; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate v) { this.fecha = v; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String v) { this.descripcion = v; }
    public int getIntensidad() { return intensidad; }
    public void setIntensidad(int v) { this.intensidad = v; }
    public String getUbicacionCorporal() { return ubicacionCorporal; }
    public void setUbicacionCorporal(String v) { this.ubicacionCorporal = v; }
    public Long getEjercicioRutinaId() { return ejercicioRutinaId; }
    public void setEjercicioRutinaId(Long v) { this.ejercicioRutinaId = v; }
    public Contexto getContexto() { return contexto; }
    public void setContexto(Contexto v) { this.contexto = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getRevisadaAt() { return revisadaAt; }
    public void setRevisadaAt(Instant v) { this.revisadaAt = v; }
    public String getRevisadaPor() { return revisadaPor; }
    public void setRevisadaPor(String v) { this.revisadaPor = v; }
}
