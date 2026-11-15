package ar.edu.usal.mgnutri.nutricion.domain;

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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Plan alimentario semanal (spec 04). */
@Entity
@Table(name = "plan_alimentario")
public class PlanAlimentario extends EntidadAuditada {

    public enum Estado { BORRADOR, ACTIVO, FINALIZADO, ELIMINADO }

    public enum Modo { MANUAL, AUTOMATICO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "paciente_id", nullable = false)
    private Long pacienteId;

    @Column(name = "nutricionista_id", nullable = false)
    private Long nutricionistaId;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "duracion_semanas", nullable = false)
    private int duracionSemanas;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Modo modo = Modo.MANUAL;

    @Column(name = "kcal_objetivo")
    private Integer kcalObjetivo;

    @Column(name = "pct_hc", precision = 5, scale = 2)
    private BigDecimal pctHc;

    @Column(name = "pct_proteinas", precision = 5, scale = 2)
    private BigDecimal pctProteinas;

    @Column(name = "pct_grasas", precision = 5, scale = 2)
    private BigDecimal pctGrasas;

    @Column(name = "parametros_generacion", length = 2000)
    private String parametrosGeneracion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Estado estado = Estado.BORRADOR;

    @Column(length = 1000)
    private String observaciones;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = false)
    @OrderBy("diaSemana ASC, orden ASC")
    private List<ComidaPlan> comidas = new ArrayList<>();

    /** RN-04.4: fecha_fin = inicio + semanas·7 − 1. */
    public void recalcularFin() {
        this.fechaFin = fechaInicio.plusDays(duracionSemanas * 7L - 1);
    }

    public List<ComidaPlan> comidasActivas() {
        return comidas.stream().filter(ComidaPlan::isActivo).toList();
    }

    public Long getId() { return id; }
    public Long getPacienteId() { return pacienteId; }
    public void setPacienteId(Long pacienteId) { this.pacienteId = pacienteId; }
    public Long getNutricionistaId() { return nutricionistaId; }
    public void setNutricionistaId(Long nutricionistaId) { this.nutricionistaId = nutricionistaId; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
    public int getDuracionSemanas() { return duracionSemanas; }
    public void setDuracionSemanas(int duracionSemanas) { this.duracionSemanas = duracionSemanas; }
    public LocalDate getFechaFin() { return fechaFin; }
    public Modo getModo() { return modo; }
    public void setModo(Modo modo) { this.modo = modo; }
    public Integer getKcalObjetivo() { return kcalObjetivo; }
    public void setKcalObjetivo(Integer kcalObjetivo) { this.kcalObjetivo = kcalObjetivo; }
    public BigDecimal getPctHc() { return pctHc; }
    public void setPctHc(BigDecimal pctHc) { this.pctHc = pctHc; }
    public BigDecimal getPctProteinas() { return pctProteinas; }
    public void setPctProteinas(BigDecimal pctProteinas) { this.pctProteinas = pctProteinas; }
    public BigDecimal getPctGrasas() { return pctGrasas; }
    public void setPctGrasas(BigDecimal pctGrasas) { this.pctGrasas = pctGrasas; }
    public String getParametrosGeneracion() { return parametrosGeneracion; }
    public void setParametrosGeneracion(String v) { this.parametrosGeneracion = v; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public List<ComidaPlan> getComidas() { return comidas; }
}
