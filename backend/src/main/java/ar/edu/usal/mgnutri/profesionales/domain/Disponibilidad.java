package ar.edu.usal.mgnutri.profesionales.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalTime;

/** Franja semanal de atención (D-10). dia_semana: 1 = lunes … 7 = domingo. */
@Entity
@Table(name = "disponibilidad")
public class Disponibilidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "profesional_id", nullable = false)
    private Long profesionalId;

    @Column(name = "dia_semana", nullable = false)
    private int diaSemana;

    @Column(name = "hora_desde", nullable = false)
    private LocalTime horaDesde;

    @Column(name = "hora_hasta", nullable = false)
    private LocalTime horaHasta;

    @Column(length = 120)
    private String ubicacion;

    @Column(nullable = false)
    private boolean activo = true;

    protected Disponibilidad() {
    }

    public Disponibilidad(Long profesionalId, int diaSemana, LocalTime horaDesde, LocalTime horaHasta, String ubicacion) {
        this.profesionalId = profesionalId;
        this.diaSemana = diaSemana;
        this.horaDesde = horaDesde;
        this.horaHasta = horaHasta;
        this.ubicacion = ubicacion;
    }

    public Long getId() { return id; }
    public Long getProfesionalId() { return profesionalId; }
    public int getDiaSemana() { return diaSemana; }
    public LocalTime getHoraDesde() { return horaDesde; }
    public LocalTime getHoraHasta() { return horaHasta; }
    public String getUbicacion() { return ubicacion; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
