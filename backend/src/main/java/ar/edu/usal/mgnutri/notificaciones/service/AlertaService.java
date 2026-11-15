package ar.edu.usal.mgnutri.notificaciones.service;

import ar.edu.usal.mgnutri.common.error.BusinessException;
import ar.edu.usal.mgnutri.common.security.CurrentUser;
import ar.edu.usal.mgnutri.common.security.UsuarioPrincipal;
import ar.edu.usal.mgnutri.notificaciones.dao.AlertaDao;
import ar.edu.usal.mgnutri.notificaciones.domain.Alerta;
import ar.edu.usal.mgnutri.notificaciones.dto.NotificacionDtos.AlertaResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Avisos internos para administradores y profesionales. */
@Service
public class AlertaService {

    private final AlertaDao dao;
    private final Clock clock;

    public AlertaService(AlertaDao dao, Clock clock) {
        this.dao = dao;
        this.clock = clock;
    }

    /** Crea un aviso para los administradores (destinatario null). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void paraAdmins(Alerta.Tipo tipo, String mensaje, String refTipo, Long refId) {
        dao.save(new Alerta(null, tipo, mensaje, false, refTipo, refId, ahora()));
    }

    @Transactional
    public void paraUsuario(Long usuarioId, Alerta.Tipo tipo, String mensaje, boolean prioritaria, String refTipo, Long refId) {
        dao.save(new Alerta(usuarioId, tipo, mensaje, prioritaria, refTipo, refId, ahora()));
    }

    @Transactional(readOnly = true)
    public List<AlertaResponse> mias(boolean soloNoLeidas) {
        UsuarioPrincipal u = CurrentUser.get();
        return dao.paraUsuario(u.id(), u.esAdmin(), soloNoLeidas).stream().limit(200).map(AlertaService::aDto).toList();
    }

    @Transactional
    public void marcarLeida(Long id) {
        UsuarioPrincipal u = CurrentUser.get();
        Alerta a = dao.findById(id).orElseThrow(() -> BusinessException.noEncontrado("Aviso"));
        boolean propia = u.id().equals(a.getDestinatarioUsuarioId()) || (a.getDestinatarioUsuarioId() == null && u.esAdmin());
        if (!propia) {
            throw BusinessException.noEncontrado("Aviso");
        }
        a.setLeidaAt(ahora());
    }

    private java.time.Instant ahora() {
        return clock.instant().truncatedTo(ChronoUnit.SECONDS);
    }

    static AlertaResponse aDto(Alerta a) {
        return new AlertaResponse(a.getId(), a.getTipo().name(), a.getMensaje(), a.isPrioritaria(),
                a.getReferenciaTipo(), a.getReferenciaId(), a.getCreatedAt(), a.getLeidaAt());
    }
}
