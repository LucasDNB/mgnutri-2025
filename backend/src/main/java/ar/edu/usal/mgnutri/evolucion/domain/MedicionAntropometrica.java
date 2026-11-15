package ar.edu.usal.mgnutri.evolucion.domain;

import ar.edu.usal.mgnutri.common.EntidadAuditada;
import ar.edu.usal.mgnutri.common.crypto.CryptoConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Medición antropométrica (spec 06). */
@Entity
@Table(name = "medicion_antropometrica")
public class MedicionAntropometrica extends EntidadAuditada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "paciente_id", nullable = false)
    private Long pacienteId;
    @Column(name = "profesional_id", nullable = false)
    private Long profesionalId;
    @Column(nullable = false)
    private LocalDate fecha;
    @Column(name = "peso_kg", nullable = false, precision = 5, scale = 2)
    private BigDecimal pesoKg;
    @Column(name = "talla_cm", precision = 5, scale = 1)
    private BigDecimal tallaCm;
    @Column(precision = 4, scale = 1)
    private BigDecimal imc;
    @Column(name = "pct_grasa", precision = 4, scale = 1)
    private BigDecimal pctGrasa;
    @Column(name = "pct_musculo", precision = 4, scale = 1)
    private BigDecimal pctMusculo;
    @Column(name = "circ_cintura_cm", precision = 5, scale = 1)
    private BigDecimal circCintura;
    @Column(name = "circ_cadera_cm", precision = 5, scale = 1)
    private BigDecimal circCadera;
    @Column(name = "circ_brazo_cm", precision = 5, scale = 1)
    private BigDecimal circBrazo;
    @Column(name = "circ_muslo_cm", precision = 5, scale = 1)
    private BigDecimal circMuslo;
    @Column(name = "circ_pecho_cm", precision = 5, scale = 1)
    private BigDecimal circPecho;
    @Column(name = "circ_pantorrilla_cm", precision = 5, scale = 1)
    private BigDecimal circPantorrilla;
    @Convert(converter = CryptoConverter.class)
    @Column(length = 4000)
    private String observaciones;

    public Long getId() { return id; }
    public Long getPacienteId() { return pacienteId; }
    public void setPacienteId(Long v) { this.pacienteId = v; }
    public Long getProfesionalId() { return profesionalId; }
    public void setProfesionalId(Long v) { this.profesionalId = v; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate v) { this.fecha = v; }
    public BigDecimal getPesoKg() { return pesoKg; }
    public void setPesoKg(BigDecimal v) { this.pesoKg = v; }
    public BigDecimal getTallaCm() { return tallaCm; }
    public void setTallaCm(BigDecimal v) { this.tallaCm = v; }
    public BigDecimal getImc() { return imc; }
    public void setImc(BigDecimal v) { this.imc = v; }
    public BigDecimal getPctGrasa() { return pctGrasa; }
    public void setPctGrasa(BigDecimal v) { this.pctGrasa = v; }
    public BigDecimal getPctMusculo() { return pctMusculo; }
    public void setPctMusculo(BigDecimal v) { this.pctMusculo = v; }
    public BigDecimal getCircCintura() { return circCintura; }
    public void setCircCintura(BigDecimal v) { this.circCintura = v; }
    public BigDecimal getCircCadera() { return circCadera; }
    public void setCircCadera(BigDecimal v) { this.circCadera = v; }
    public BigDecimal getCircBrazo() { return circBrazo; }
    public void setCircBrazo(BigDecimal v) { this.circBrazo = v; }
    public BigDecimal getCircMuslo() { return circMuslo; }
    public void setCircMuslo(BigDecimal v) { this.circMuslo = v; }
    public BigDecimal getCircPecho() { return circPecho; }
    public void setCircPecho(BigDecimal v) { this.circPecho = v; }
    public BigDecimal getCircPantorrilla() { return circPantorrilla; }
    public void setCircPantorrilla(BigDecimal v) { this.circPantorrilla = v; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String v) { this.observaciones = v; }
}
