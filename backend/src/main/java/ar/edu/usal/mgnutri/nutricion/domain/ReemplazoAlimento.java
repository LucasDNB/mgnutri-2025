package ar.edu.usal.mgnutri.nutricion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Reemplazo de alimento elegido por el paciente (CU-18). Un reemplazo activo por ítem. */
@Entity
@Table(name = "reemplazo_alimento")
public class ReemplazoAlimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_comida_id", nullable = false)
    private Long itemComidaId;

    @Column(name = "alimento_reemplazo_id", nullable = false)
    private Long alimentoReemplazoId;

    @Column(nullable = false, precision = 7, scale = 1)
    private BigDecimal gramos;

    @Column(name = "cantidad_casera", length = 60)
    private String cantidadCasera;

    @Column(name = "paciente_id", nullable = false)
    private Long pacienteId;

    @Column(name = "fecha_desde", nullable = false)
    private LocalDate fechaDesde;

    @Column(nullable = false)
    private boolean activo = true;

    protected ReemplazoAlimento() {
    }

    public ReemplazoAlimento(Long itemComidaId, Long alimentoReemplazoId, BigDecimal gramos, String cantidadCasera,
                             Long pacienteId, LocalDate fechaDesde) {
        this.itemComidaId = itemComidaId;
        this.alimentoReemplazoId = alimentoReemplazoId;
        this.gramos = gramos;
        this.cantidadCasera = cantidadCasera;
        this.pacienteId = pacienteId;
        this.fechaDesde = fechaDesde;
    }

    public Long getId() { return id; }
    public Long getItemComidaId() { return itemComidaId; }
    public Long getAlimentoReemplazoId() { return alimentoReemplazoId; }
    public BigDecimal getGramos() { return gramos; }
    public String getCantidadCasera() { return cantidadCasera; }
    public Long getPacienteId() { return pacienteId; }
    public LocalDate getFechaDesde() { return fechaDesde; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
