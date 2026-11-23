package ar.edu.usal.mgnutri.entrenamiento.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "ejercicio_rutina")
public class EjercicioRutina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dia_rutina_id")
    private DiaRutina dia;
    @Column(name = "ejercicio_id", nullable = false)
    private Long ejercicioId;
    @Column(nullable = false)
    private int orden;
    private Integer series;
    @Column(length = 20)
    private String repeticiones;
    /** Bloque opcional («Entrada en calor», «WOD») y esquema dentro del bloque («3 rondas», «21-15-9»). */
    @Column(length = 60)
    private String bloque;
    @Column(length = 60)
    private String esquema;
    @Column(name = "descanso_seg")
    private Integer descansoSeg;
    @Column(name = "carga_sugerida", length = 30)
    private String cargaSugerida;
    @Column(length = 300)
    private String notas;
    @Column(nullable = false)
    private boolean activo = true;

    protected EjercicioRutina() {
    }

    public EjercicioRutina(DiaRutina dia, Long ejercicioId, int orden) {
        this.dia = dia;
        this.ejercicioId = ejercicioId;
        this.orden = orden;
    }

    public Long getId() { return id; }
    public DiaRutina getDia() { return dia; }
    public Long getEjercicioId() { return ejercicioId; }
    public void setEjercicioId(Long v) { this.ejercicioId = v; }
    public int getOrden() { return orden; }
    public void setOrden(int v) { this.orden = v; }
    public Integer getSeries() { return series; }
    public void setSeries(Integer v) { this.series = v; }
    public String getBloque() { return bloque; }
    public void setBloque(String v) { this.bloque = v; }
    public String getEsquema() { return esquema; }
    public void setEsquema(String v) { this.esquema = v; }
    public String getRepeticiones() { return repeticiones; }
    public void setRepeticiones(String v) { this.repeticiones = v; }
    public Integer getDescansoSeg() { return descansoSeg; }
    public void setDescansoSeg(Integer v) { this.descansoSeg = v; }
    public String getCargaSugerida() { return cargaSugerida; }
    public void setCargaSugerida(String v) { this.cargaSugerida = v; }
    public String getNotas() { return notas; }
    public void setNotas(String v) { this.notas = v; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean v) { this.activo = v; }
}
