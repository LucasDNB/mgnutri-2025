package ar.edu.usal.mgnutri.seguridad.domain;

import ar.edu.usal.mgnutri.common.EntidadAuditada;
import ar.edu.usal.mgnutri.common.Rol;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "usuario")
public class Usuario extends EntidadAuditada {

    public enum Estado { ACTIVO, BLOQUEADO, BAJA }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Estado estado = Estado.ACTIVO;

    @Column(name = "debe_cambiar_password", nullable = false)
    private boolean debeCambiarPassword = true;

    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos;

    @Column(name = "bloqueado_hasta")
    private Instant bloqueadoHasta;

    @Column(name = "terminos_version_aceptada", length = 10)
    private String terminosVersionAceptada;

    @Column(name = "terminos_aceptados_at")
    private Instant terminosAceptadosAt;

    @Column(name = "ultimo_acceso_at")
    private Instant ultimoAccesoAt;

    /** Los tokens emitidos antes de este instante quedan revocados (cambio de contraseña, baja). */
    @Column(name = "tokens_validos_desde")
    private Instant tokensValidosDesde;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "usuario_rol", joinColumns = @JoinColumn(name = "usuario_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "rol", length = 20)
    private Set<Rol> roles = new HashSet<>();

    public boolean tieneRol(Rol rol) {
        return roles.contains(rol);
    }

    public void reemplazarRoles(Set<Rol> nuevos) {
        roles.clear();
        roles.addAll(nuevos.isEmpty() ? EnumSet.noneOf(Rol.class) : nuevos);
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
    public boolean isDebeCambiarPassword() { return debeCambiarPassword; }
    public void setDebeCambiarPassword(boolean v) { this.debeCambiarPassword = v; }
    public int getIntentosFallidos() { return intentosFallidos; }
    public void setIntentosFallidos(int intentosFallidos) { this.intentosFallidos = intentosFallidos; }
    public Instant getBloqueadoHasta() { return bloqueadoHasta; }
    public void setBloqueadoHasta(Instant bloqueadoHasta) { this.bloqueadoHasta = bloqueadoHasta; }
    public String getTerminosVersionAceptada() { return terminosVersionAceptada; }
    public void setTerminosVersionAceptada(String v) { this.terminosVersionAceptada = v; }
    public Instant getTerminosAceptadosAt() { return terminosAceptadosAt; }
    public void setTerminosAceptadosAt(Instant v) { this.terminosAceptadosAt = v; }
    public Instant getUltimoAccesoAt() { return ultimoAccesoAt; }
    public void setUltimoAccesoAt(Instant v) { this.ultimoAccesoAt = v; }
    public Instant getTokensValidosDesde() { return tokensValidosDesde; }
    public void setTokensValidosDesde(Instant v) { this.tokensValidosDesde = v; }
    public Set<Rol> getRoles() { return roles; }
}
