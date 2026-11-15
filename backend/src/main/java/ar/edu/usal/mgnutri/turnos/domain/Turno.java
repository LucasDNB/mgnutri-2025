package ar.edu.usal.mgnutri.turnos.domain;

import ar.edu.usal.mgnutri.common.Area;
import ar.edu.usal.mgnutri.common.EntidadAuditada;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Turno (D-09): RESERVADO → ATENDIDO | AUSENTE | CANCELADO. */
@Entity
@Table(name = "turno")
public class Turno extends EntidadAuditada {

    public enum Estado { RESERVADO, CANCELADO, ATENDIDO, AUSENTE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "paciente_id", nullable = false)
    private Long pacienteId;

    @Column(name = "profesional_id", nullable = false)
    private Long profesionalId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Area area;

    @Column(nullable = false)
    private Instant inicio;

    @Column(nullable = false)
    private Instant fin;

    @Column(length = 120)
    private String ubicacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Estado estado = Estado.RESERVADO;

    @Column(name = "motivo_cancelacion", length = 200)
    private String motivoCancelacion;

    @Column(name = "cancelado_por_usuario_id")
    private Long canceladoPorUsuarioId;

    @Column(name = "cancelado_por_rol", length = 20)
    private String canceladoPorRol;

    @Column(name = "recordatorio_enviado_at")
    private Instant recordatorioEnviadoAt;

    @Column(name = "creado_por_usuario_id")
    private Long creadoPorUsuarioId;

    @Column(name = "fuera_de_agenda", nullable = false)
    private boolean fueraDeAgenda;

    public boolean solapa(Instant i, Instant f) {
        return inicio.isBefore(f) && fin.isAfter(i);
    }

    public Long getId() { return id; }
    public Long getPacienteId() { return pacienteId; }
    public void setPacienteId(Long pacienteId) { this.pacienteId = pacienteId; }
    public Long getProfesionalId() { return profesionalId; }
    public void setProfesionalId(Long profesionalId) { this.profesionalId = profesionalId; }
    public Area getArea() { return area; }
    public void setArea(Area area) { this.area = area; }
    public Instant getInicio() { return inicio; }
    public void setInicio(Instant inicio) { this.inicio = inicio; }
    public Instant getFin() { return fin; }
    public void setFin(Instant fin) { this.fin = fin; }
    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
    public String getMotivoCancelacion() { return motivoCancelacion; }
    public void setMotivoCancelacion(String motivoCancelacion) { this.motivoCancelacion = motivoCancelacion; }
    public Long getCanceladoPorUsuarioId() { return canceladoPorUsuarioId; }
    public void setCanceladoPorUsuarioId(Long v) { this.canceladoPorUsuarioId = v; }
    public String getCanceladoPorRol() { return canceladoPorRol; }
    public void setCanceladoPorRol(String canceladoPorRol) { this.canceladoPorRol = canceladoPorRol; }
    public Instant getRecordatorioEnviadoAt() { return recordatorioEnviadoAt; }
    public void setRecordatorioEnviadoAt(Instant v) { this.recordatorioEnviadoAt = v; }
    public Long getCreadoPorUsuarioId() { return creadoPorUsuarioId; }
    public void setCreadoPorUsuarioId(Long v) { this.creadoPorUsuarioId = v; }
    public boolean isFueraDeAgenda() { return fueraDeAgenda; }
    public void setFueraDeAgenda(boolean fueraDeAgenda) { this.fueraDeAgenda = fueraDeAgenda; }
}
