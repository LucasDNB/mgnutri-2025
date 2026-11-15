package ar.edu.usal.mgnutri.notificaciones.dao;

import ar.edu.usal.mgnutri.notificaciones.domain.Notificacion;
import ar.edu.usal.mgnutri.notificaciones.domain.TipoNotificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface NotificacionDao extends JpaRepository<Notificacion, Long>, JpaSpecificationExecutor<Notificacion> {

    /** Toma pendientes con bloqueo, salteando filas tomadas por otra instancia (UPDLOCK, READPAST). */
    @Query(value = "SELECT TOP 50 * FROM notificacion WITH (UPDLOCK, READPAST) "
            + "WHERE estado = 'PENDIENTE' AND proximo_intento_at <= :ahora ORDER BY id", nativeQuery = true)
    List<Notificacion> tomarPendientes(@Param("ahora") Instant ahora);

    long countByEstado(Notificacion.Estado estado);

    boolean existsByUsuarioIdAndTipoAndCreatedAtAfterAndEstadoIn(Long usuarioId, TipoNotificacion tipo, Instant desde,
                                                                  List<Notificacion.Estado> estados);
}
