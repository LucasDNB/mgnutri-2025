package ar.edu.usal.mgnutri.catalogo.domain;

import ar.edu.usal.mgnutri.common.EntidadAuditada;
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

/** Alimento del catálogo; valores por 100 g (spec 10). */
@Entity
@Table(name = "alimento")
public class Alimento extends EntidadAuditada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String nombre;

    @Column(name = "nombre_normalizado", nullable = false, unique = true, length = 120)
    private String nombreNormalizado;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "grupo_alimento_id")
    private GrupoAlimento grupo;

    @Column(name = "kcal_100g", nullable = false, precision = 7, scale = 2)
    private BigDecimal kcal;

    @Column(name = "hc_100g", nullable = false, precision = 7, scale = 2)
    private BigDecimal hc;

    @Column(name = "proteinas_100g", nullable = false, precision = 7, scale = 2)
    private BigDecimal proteinas;

    @Column(name = "grasas_100g", nullable = false, precision = 7, scale = 2)
    private BigDecimal grasas;

    @Column(name = "fibra_100g", nullable = false, precision = 7, scale = 2)
    private BigDecimal fibra = BigDecimal.ZERO;

    @Column(name = "sodio_mg_100g", precision = 8, scale = 2)
    private BigDecimal sodioMg;

    @Column(name = "unidad_casera", length = 60)
    private String unidadCasera;

    @Column(name = "gramos_unidad_casera", precision = 7, scale = 1)
    private BigDecimal gramosUnidadCasera;

    @Column(name = "apto_desayuno", nullable = false)
    private boolean aptoDesayuno;

    @Column(name = "apto_colacion", nullable = false)
    private boolean aptoColacion;

    @Column(name = "apto_almuerzo", nullable = false)
    private boolean aptoAlmuerzo;

    @Column(nullable = false)
    private boolean activo = true;

    /** Gramos de macronutriente de referencia por gramo de alimento. */
    public double referenciaPorGramo() {
        return switch (grupo.getMacroReferencia()) {
            case HC -> hc.doubleValue() / 100.0;
            case PROTEINAS -> proteinas.doubleValue() / 100.0;
            case GRASAS -> grasas.doubleValue() / 100.0;
            case KCAL -> kcal.doubleValue() / 100.0;
        };
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getNombreNormalizado() { return nombreNormalizado; }
    public void setNombreNormalizado(String v) { this.nombreNormalizado = v; }
    public GrupoAlimento getGrupo() { return grupo; }
    public void setGrupo(GrupoAlimento grupo) { this.grupo = grupo; }
    public BigDecimal getKcal() { return kcal; }
    public void setKcal(BigDecimal kcal) { this.kcal = kcal; }
    public BigDecimal getHc() { return hc; }
    public void setHc(BigDecimal hc) { this.hc = hc; }
    public BigDecimal getProteinas() { return proteinas; }
    public void setProteinas(BigDecimal proteinas) { this.proteinas = proteinas; }
    public BigDecimal getGrasas() { return grasas; }
    public void setGrasas(BigDecimal grasas) { this.grasas = grasas; }
    public BigDecimal getFibra() { return fibra; }
    public void setFibra(BigDecimal fibra) { this.fibra = fibra; }
    public BigDecimal getSodioMg() { return sodioMg; }
    public void setSodioMg(BigDecimal sodioMg) { this.sodioMg = sodioMg; }
    public String getUnidadCasera() { return unidadCasera; }
    public void setUnidadCasera(String unidadCasera) { this.unidadCasera = unidadCasera; }
    public BigDecimal getGramosUnidadCasera() { return gramosUnidadCasera; }
    public void setGramosUnidadCasera(BigDecimal v) { this.gramosUnidadCasera = v; }
    public boolean isAptoDesayuno() { return aptoDesayuno; }
    public void setAptoDesayuno(boolean v) { this.aptoDesayuno = v; }
    public boolean isAptoColacion() { return aptoColacion; }
    public void setAptoColacion(boolean v) { this.aptoColacion = v; }
    public boolean isAptoAlmuerzo() { return aptoAlmuerzo; }
    public void setAptoAlmuerzo(boolean v) { this.aptoAlmuerzo = v; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
