package ar.edu.usal.mgnutri.catalogo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Grupo de intercambio con su macronutriente de referencia (D-15). */
@Entity
@Table(name = "grupo_alimento")
public class GrupoAlimento {

    public enum Macro { HC, PROTEINAS, GRASAS, KCAL }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "macro_referencia", nullable = false, length = 12)
    private Macro macroReferencia;

    @Column(length = 200)
    private String descripcion;

    protected GrupoAlimento() {
    }

    public GrupoAlimento(String nombre, Macro macro) {
        this.nombre = nombre;
        this.macroReferencia = macro;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public Macro getMacroReferencia() { return macroReferencia; }
    public String getDescripcion() { return descripcion; }
}
