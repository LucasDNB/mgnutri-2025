package ar.edu.usal.mgnutri.profesionales.domain;

import ar.edu.usal.mgnutri.common.Area;
import ar.edu.usal.mgnutri.common.EntidadAuditada;
import ar.edu.usal.mgnutri.common.Rol;
import ar.edu.usal.mgnutri.common.crypto.CryptoConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

@Entity
@Table(name = "profesional")
public class Profesional extends EntidadAuditada {

    public enum Estado { ACTIVO, BAJA }

    public enum Tipo {
        NUTRICIONISTA, ENTRENADOR, INTEGRAL;

        /** RN-02.5: roles por tipo de profesional. */
        public Set<Rol> roles() {
            return switch (this) {
                case NUTRICIONISTA -> EnumSet.of(Rol.NUTRICIONISTA);
                case ENTRENADOR -> EnumSet.of(Rol.ENTRENADOR);
                case INTEGRAL -> EnumSet.of(Rol.NUTRICIONISTA, Rol.ENTRENADOR);
            };
        }

        public boolean atiende(Area area) {
            return this == INTEGRAL || (area == Area.NUTRICION ? this == NUTRICIONISTA : this == ENTRENADOR);
        }

        public boolean requiereMatricula() {
            return this != ENTRENADOR;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false, unique = true)
    private Long usuarioId;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false, length = 80)
    private String apellido;

    @Column(name = "tipo_documento", nullable = false, length = 10)
    private String tipoDocumento;

    @Column(name = "nro_documento", nullable = false, unique = true, length = 20)
    private String nroDocumento;

    @Column(length = 30)
    private String matricula;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Tipo tipo;

    @Column(length = 100)
    private String especialidad;

    @Convert(converter = CryptoConverter.class)
    @Column(name = "telefono_whatsapp", length = 1024)
    private String telefonoWhatsapp;

    @Convert(converter = CryptoConverter.class)
    @Column(length = 1024)
    private String email;

    @Column(name = "duracion_turno_min", nullable = false)
    private int duracionTurnoMin = 30;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Estado estado = Estado.ACTIVO;

    @Column(name = "fecha_baja")
    private LocalDate fechaBaja;

    public String nombreCompleto() {
        return nombre + " " + apellido;
    }

    /** "Lic. López" para nutricionistas/integrales, "Prof. López" para entrenadores. */
    public String nombreVisible() {
        return (tipo == Tipo.ENTRENADOR ? "Prof. " : "Lic. ") + apellido;
    }

    public boolean activo() {
        return estado == Estado.ACTIVO;
    }

    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }
    public String getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }
    public String getNroDocumento() { return nroDocumento; }
    public void setNroDocumento(String nroDocumento) { this.nroDocumento = nroDocumento; }
    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }
    public Tipo getTipo() { return tipo; }
    public void setTipo(Tipo tipo) { this.tipo = tipo; }
    public String getEspecialidad() { return especialidad; }
    public void setEspecialidad(String especialidad) { this.especialidad = especialidad; }
    public String getTelefonoWhatsapp() { return telefonoWhatsapp; }
    public void setTelefonoWhatsapp(String telefonoWhatsapp) { this.telefonoWhatsapp = telefonoWhatsapp; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public int getDuracionTurnoMin() { return duracionTurnoMin; }
    public void setDuracionTurnoMin(int duracionTurnoMin) { this.duracionTurnoMin = duracionTurnoMin; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
    public LocalDate getFechaBaja() { return fechaBaja; }
    public void setFechaBaja(LocalDate fechaBaja) { this.fechaBaja = fechaBaja; }
}
