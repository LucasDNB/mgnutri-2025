package ar.edu.usal.mgnutri.pagos.domain;

import ar.edu.usal.mgnutri.common.EntidadAuditada;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Pago registrado por el administrador (CU-44). */
@Entity
@Table(name = "pago")
public class Pago extends EntidadAuditada {

    public enum Medio {
        EFECTIVO("Efectivo"), TRANSFERENCIA("Transferencia"), DEBITO("Débito"), CREDITO("Crédito"),
        MERCADO_PAGO("Mercado Pago"), OTRO("Otro");

        private final String etiqueta;

        Medio(String etiqueta) {
            this.etiqueta = etiqueta;
        }

        public String etiqueta() {
            return etiqueta;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "paciente_id", nullable = false)
    private Long pacienteId;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Medio medio;

    @Column(length = 60)
    private String referencia;

    @Column(length = 300)
    private String observaciones;

    @Column(name = "nro_comprobante", nullable = false, unique = true)
    private Long nroComprobante;

    @Column(nullable = false)
    private boolean anulado;

    @Column(name = "motivo_anulacion", length = 200)
    private String motivoAnulacion;

    public Long getId() { return id; }
    public Long getPacienteId() { return pacienteId; }
    public void setPacienteId(Long pacienteId) { this.pacienteId = pacienteId; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }
    public Medio getMedio() { return medio; }
    public void setMedio(Medio medio) { this.medio = medio; }
    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public Long getNroComprobante() { return nroComprobante; }
    public void setNroComprobante(Long nroComprobante) { this.nroComprobante = nroComprobante; }
    public boolean isAnulado() { return anulado; }
    public void setAnulado(boolean anulado) { this.anulado = anulado; }
    public String getMotivoAnulacion() { return motivoAnulacion; }
    public void setMotivoAnulacion(String motivoAnulacion) { this.motivoAnulacion = motivoAnulacion; }
}
