package ar.edu.usal.mgnutri.nutricion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** Ítem estructurado de una comida: alimento del catálogo + gramos + medida casera. */
@Entity
@Table(name = "item_comida")
public class ItemComida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comida_plan_id")
    private ComidaPlan comida;

    @Column(name = "alimento_id", nullable = false)
    private Long alimentoId;

    @Column(nullable = false, precision = 7, scale = 1)
    private BigDecimal gramos;

    @Column(name = "cantidad_casera", length = 60)
    private String cantidadCasera;

    @Column(nullable = false)
    private int orden;

    @Column(nullable = false)
    private boolean activo = true;

    protected ItemComida() {
    }

    public ItemComida(ComidaPlan comida, Long alimentoId, BigDecimal gramos, String cantidadCasera, int orden) {
        this.comida = comida;
        this.alimentoId = alimentoId;
        this.gramos = gramos;
        this.cantidadCasera = cantidadCasera;
        this.orden = orden;
    }

    public Long getId() { return id; }
    public ComidaPlan getComida() { return comida; }
    public Long getAlimentoId() { return alimentoId; }
    public BigDecimal getGramos() { return gramos; }
    public String getCantidadCasera() { return cantidadCasera; }
    public int getOrden() { return orden; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
