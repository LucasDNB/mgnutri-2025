package ar.edu.usal.mgnutri.common.eventos;

/**
 * Eventos de dominio publicados con ApplicationEventPublisher y consumidos de forma sincrónica
 * dentro de la misma transacción (docs/02-arquitectura.md §3).
 */
public final class Eventos {

    private Eventos() {
    }

    /** Baja de paciente: cancelar turnos futuros, finalizar plan y rutina (CA-01.3.2). */
    public record PacienteDadoDeBaja(Long pacienteId, String motivo, boolean notificarCancelaciones) {}

    /** Baja de profesional: cancelar turnos futuros notificando a los pacientes (CA-02.3.2). */
    public record ProfesionalDadoDeBaja(Long profesionalId, String motivo) {}
}
