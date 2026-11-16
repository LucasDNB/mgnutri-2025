package ar.edu.usal.mgnutri.backup.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Registro de cada ejecución de backup (CU-51/52). */
@Entity
@Table(name = "backup_registro")
public class BackupRegistro {

    public enum Tipo { MANUAL, AUTOMATICO }

    public enum Estado { EN_CURSO, OK, ERROR }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Tipo tipo;
    @Column(name = "iniciado_at", nullable = false)
    private Instant iniciadoAt;
    @Column(name = "finalizado_at")
    private Instant finalizadoAt;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Estado estado;
    @Column(length = 200)
    private String archivo;
    @Column(name = "tamano_bytes")
    private Long tamanoBytes;
    @Column(length = 400)
    private String ubicacion;
    @Column(name = "solicitado_por", length = 40)
    private String solicitadoPor;
    @Column(length = 1000)
    private String error;
    @Column(nullable = false)
    private boolean expirado;

    protected BackupRegistro() {
    }

    public BackupRegistro(Tipo tipo, Instant iniciadoAt, String archivo, String solicitadoPor) {
        this.tipo = tipo;
        this.iniciadoAt = iniciadoAt;
        this.archivo = archivo;
        this.solicitadoPor = solicitadoPor;
        this.estado = Estado.EN_CURSO;
    }

    public Long getId() { return id; }
    public Tipo getTipo() { return tipo; }
    public Instant getIniciadoAt() { return iniciadoAt; }
    public Instant getFinalizadoAt() { return finalizadoAt; }
    public void setFinalizadoAt(Instant v) { this.finalizadoAt = v; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado v) { this.estado = v; }
    public String getArchivo() { return archivo; }
    public Long getTamanoBytes() { return tamanoBytes; }
    public void setTamanoBytes(Long v) { this.tamanoBytes = v; }
    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String v) { this.ubicacion = v; }
    public String getSolicitadoPor() { return solicitadoPor; }
    public String getError() { return error; }
    public void setError(String v) { this.error = v == null ? null : v.substring(0, Math.min(1000, v.length())); }
    public boolean isExpirado() { return expirado; }
    public void setExpirado(boolean v) { this.expirado = v; }
}
