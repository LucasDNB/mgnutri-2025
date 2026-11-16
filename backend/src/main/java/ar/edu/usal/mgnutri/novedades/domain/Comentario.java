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

/** Comentario del paciente sobre su plan o rutina (CU-40, CU-41). */
@Entity
@Table(name = "comentario")
public class Comentario {

    public enum Tipo { PLAN, RUTINA }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "paciente_id", nullable = false)
    private Long pacienteId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Tipo tipo;
    @Column(name = "plan_id")
    private Long planId;
    @Column(name = "comida_plan_id")
    private Long comidaPlanId;
    @Column(name = "rutina_id")
    private Long rutinaId;
    @Column(name = "ejercicio_rutina_id")
    private Long ejercicioRutinaId;
    @Column(nullable = false)
    private LocalDate fecha;
    @Column(nullable = false, length = 300)
    private String texto;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "leido_at")
    private Instant leidoAt;
    @Column(name = "leido_por", length = 40)
    private String leidoPor;

    public Long getId() { return id; }
    public Long getPacienteId() { return pacienteId; }
    public void setPacienteId(Long v) { this.pacienteId = v; }
    public Tipo getTipo() { return tipo; }
    public void setTipo(Tipo v) { this.tipo = v; }
    public Long getPlanId() { return planId; }
    public void setPlanId(Long v) { this.planId = v; }
    public Long getComidaPlanId() { return comidaPlanId; }
    public void setComidaPlanId(Long v) { this.comidaPlanId = v; }
    public Long getRutinaId() { return rutinaId; }
    public void setRutinaId(Long v) { this.rutinaId = v; }
    public Long getEjercicioRutinaId() { return ejercicioRutinaId; }
    public void setEjercicioRutinaId(Long v) { this.ejercicioRutinaId = v; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate v) { this.fecha = v; }
    public String getTexto() { return texto; }
    public void setTexto(String v) { this.texto = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getLeidoAt() { return leidoAt; }
    public void setLeidoAt(Instant v) { this.leidoAt = v; }
    public String getLeidoPor() { return leidoPor; }
    public void setLeidoPor(String v) { this.leidoPor = v; }
}
