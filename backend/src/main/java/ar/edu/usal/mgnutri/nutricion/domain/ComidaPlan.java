package ar.edu.usal.mgnutri.nutricion.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Celda de la grilla semanal: día × tipo de comida. */
@Entity
@Table(name = "comida_plan")
public class ComidaPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id")
    private PlanAlimentario plan;

    @Column(name = "dia_semana", nullable = false)
    private int diaSemana;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_comida", nullable = false, length = 20)
    private TipoComida tipo;

    @Column(nullable = false)
    private int orden;

    @Column(length = 500)
    private String descripcion;

    @Column(name = "kcal_estimadas", precision = 7, scale = 1)
    private BigDecimal kcalEstimadas;

    @Column(nullable = false)
    private boolean activo = true;

    @OneToMany(mappedBy = "comida", cascade = CascadeType.ALL)
    @OrderBy("orden ASC")
    private List<ItemComida> items = new ArrayList<>();

    protected ComidaPlan() {
    }

    public ComidaPlan(PlanAlimentario plan, int diaSemana, TipoComida tipo) {
        this.plan = plan;
        this.diaSemana = diaSemana;
        this.tipo = tipo;
        this.orden = tipo.orden();
    }

    public List<ItemComida> itemsActivos() {
        return items.stream().filter(ItemComida::isActivo).toList();
    }

    public boolean tieneContenido() {
        return (descripcion != null && !descripcion.isBlank()) || !itemsActivos().isEmpty();
    }

    public Long getId() { return id; }
    public PlanAlimentario getPlan() { return plan; }
    public int getDiaSemana() { return diaSemana; }
    public TipoComida getTipo() { return tipo; }
    public int getOrden() { return orden; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public BigDecimal getKcalEstimadas() { return kcalEstimadas; }
    public void setKcalEstimadas(BigDecimal kcalEstimadas) { this.kcalEstimadas = kcalEstimadas; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public List<ItemComida> getItems() { return items; }
}
