package ar.edu.usal.mgnutri.catalogo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** Equivalencia cargada por el nutricionista: tiene prioridad sobre el cálculo automático (D-15). */
@Entity
@Table(name = "equivalencia_manual")
public class EquivalenciaManual {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alimento_id", nullable = false)
    private Long alimentoId;

    @Column(name = "alimento_equivalente_id", nullable = false)
    private Long alimentoEquivalenteId;

    @Column(name = "gramos_origen", nullable = false, precision = 7, scale = 1)
    private BigDecimal gramosOrigen;

    @Column(name = "gramos_equivalente", nullable = false, precision = 7, scale = 1)
    private BigDecimal gramosEquivalente;

    @Column(length = 120)
    private String nota;

    protected EquivalenciaManual() {
    }

    public EquivalenciaManual(Long alimentoId, Long alimentoEquivalenteId, BigDecimal gramosOrigen,
                              BigDecimal gramosEquivalente, String nota) {
        this.alimentoId = alimentoId;
        this.alimentoEquivalenteId = alimentoEquivalenteId;
        this.gramosOrigen = gramosOrigen;
        this.gramosEquivalente = gramosEquivalente;
        this.nota = nota;
    }

    public Long getId() { return id; }
    public Long getAlimentoId() { return alimentoId; }
    public Long getAlimentoEquivalenteId() { return alimentoEquivalenteId; }
    public BigDecimal getGramosOrigen() { return gramosOrigen; }
    public BigDecimal getGramosEquivalente() { return gramosEquivalente; }
    public String getNota() { return nota; }
}
