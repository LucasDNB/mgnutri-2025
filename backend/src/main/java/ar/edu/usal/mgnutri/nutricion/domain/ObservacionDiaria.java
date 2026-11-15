package ar.edu.usal.mgnutri.nutricion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/** Observación diaria de hambre y saciedad (CU-43). Una por paciente y fecha. */
@Entity
@Table(name = "observacion_diaria")
public class ObservacionDiaria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "paciente_id", nullable = false)
    private Long pacienteId;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "nivel_hambre", nullable = false)
    private int nivelHambre;

    @Column(name = "nivel_saciedad", nullable = false)
    private int nivelSaciedad;

    @Column(name = "estado_animo")
    private Integer estadoAnimo;

    @Column(length = 300)
    private String comentario;

    protected ObservacionDiaria() {
    }

    public ObservacionDiaria(Long pacienteId, LocalDate fecha) {
        this.pacienteId = pacienteId;
        this.fecha = fecha;
    }

    public Long getId() { return id; }
    public Long getPacienteId() { return pacienteId; }
    public LocalDate getFecha() { return fecha; }
    public int getNivelHambre() { return nivelHambre; }
    public void setNivelHambre(int nivelHambre) { this.nivelHambre = nivelHambre; }
    public int getNivelSaciedad() { return nivelSaciedad; }
    public void setNivelSaciedad(int nivelSaciedad) { this.nivelSaciedad = nivelSaciedad; }
    public Integer getEstadoAnimo() { return estadoAnimo; }
    public void setEstadoAnimo(Integer estadoAnimo) { this.estadoAnimo = estadoAnimo; }
    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }
}
