package ar.edu.usal.mgnutri.seguridad.dao;

import ar.edu.usal.mgnutri.seguridad.domain.TokenRevocado;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TokenRevocadoDao extends JpaRepository<TokenRevocado, String> {
    @Modifying
    @Query("delete from TokenRevocado t where t.expiraAt < :ahora")
    int eliminarExpirados(@Param("ahora") Instant ahora);
}
