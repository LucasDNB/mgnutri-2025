package ar.edu.usal.mgnutri.configuracion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "terminos_condiciones")
public class TerminosCondiciones {

    @Id
    @Column(length = 10)
    private String version;

    @Column(nullable = false, columnDefinition = "nvarchar(max)")
    private String texto;

    @Column(name = "vigente_desde", nullable = false)
    private Instant vigenteDesde;

    protected TerminosCondiciones() {
    }

    public TerminosCondiciones(String version, String texto, Instant vigenteDesde) {
        this.version = version;
        this.texto = texto;
        this.vigenteDesde = vigenteDesde;
    }

    public String getVersion() { return version; }
    public String getTexto() { return texto; }
    public Instant getVigenteDesde() { return vigenteDesde; }
}
