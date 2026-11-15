package ar.edu.usal.mgnutri.catalogo.domain;

import ar.edu.usal.mgnutri.common.EntidadAuditada;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;

/** Ejercicio del catálogo (spec 10). */
@Entity
@Table(name = "ejercicio")
public class Ejercicio extends EntidadAuditada {

    public enum Dificultad {
        BAJA(1), MEDIA(2), ALTA(3);

        private final int nivel;

        Dificultad(int nivel) {
            this.nivel = nivel;
        }

        public int nivel() {
            return nivel;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String nombre;

    @Column(name = "nombre_normalizado", nullable = false, unique = true, length = 120)
    private String nombreNormalizado;

    @Column(length = 500)
    private String descripcion;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "grupo_muscular_principal_id")
    private GrupoMuscular grupoPrincipal;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "ejercicio_grupo_secundario", joinColumns = @JoinColumn(name = "ejercicio_id"),
            inverseJoinColumns = @JoinColumn(name = "grupo_muscular_id"))
    private Set<GrupoMuscular> gruposSecundarios = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Dificultad dificultad;

    @Column(length = 100)
    private String equipamiento;

    @Column(name = "video_url", length = 300)
    private String videoUrl;

    @Column(nullable = false)
    private boolean activo = true;

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getNombreNormalizado() { return nombreNormalizado; }
    public void setNombreNormalizado(String v) { this.nombreNormalizado = v; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public GrupoMuscular getGrupoPrincipal() { return grupoPrincipal; }
    public void setGrupoPrincipal(GrupoMuscular g) { this.grupoPrincipal = g; }
    public Set<GrupoMuscular> getGruposSecundarios() { return gruposSecundarios; }
    public Dificultad getDificultad() { return dificultad; }
    public void setDificultad(Dificultad dificultad) { this.dificultad = dificultad; }
    public String getEquipamiento() { return equipamiento; }
    public void setEquipamiento(String equipamiento) { this.equipamiento = equipamiento; }
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
