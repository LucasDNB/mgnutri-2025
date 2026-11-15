package ar.edu.usal.mgnutri.notificaciones.dao;

import ar.edu.usal.mgnutri.notificaciones.domain.Alerta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AlertaDao extends JpaRepository<Alerta, Long> {

    @Query("select a from Alerta a where (a.destinatarioUsuarioId = :usuarioId or (:admin = true and a.destinatarioUsuarioId is null)) "
            + "and (:soloNoLeidas = false or a.leidaAt is null) order by a.prioritaria desc, a.createdAt desc")
    List<Alerta> paraUsuario(@Param("usuarioId") Long usuarioId, @Param("admin") boolean admin,
                             @Param("soloNoLeidas") boolean soloNoLeidas);
}
