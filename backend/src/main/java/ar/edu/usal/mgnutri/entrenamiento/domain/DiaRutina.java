package ar.edu.usal.mgnutri.entrenamiento.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "dia_rutina")
public class DiaRutina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rutina_id")
    private Rutina rutina;
    @Column(name = "dia_semana", nullable = false)
    private int diaSemana;
    @Column(length = 80)
    private String nombre;
    @Column(nullable = false)
    private int orden;
    @Column(nullable = false)
    private boolean activo = true;
    @OneToMany(mappedBy = "dia", cascade = CascadeType.ALL)
    @OrderBy("orden ASC")
    private List<EjercicioRutina> ejercicios = new ArrayList<>();

    protected DiaRutina() {
    }

    public DiaRutina(Rutina rutina, int diaSemana, String nombre, int orden) {
        this.rutina = rutina;
        this.diaSemana = diaSemana;
        this.nombre = nombre;
        this.orden = orden;
    }

    public List<EjercicioRutina> ejerciciosActivos() {
        return ejercicios.stream().filter(EjercicioRutina::isActivo).toList();
    }

    public Long getId() { return id; }
    public Rutina getRutina() { return rutina; }
    public int getDiaSemana() { return diaSemana; }
    public void setDiaSemana(int v) { this.diaSemana = v; }
    public String getNombre() { return nombre; }
    public void setNombre(String v) { this.nombre = v; }
    public int getOrden() { return orden; }
    public void setOrden(int v) { this.orden = v; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean v) { this.activo = v; }
    public List<EjercicioRutina> getEjercicios() { return ejercicios; }
}
