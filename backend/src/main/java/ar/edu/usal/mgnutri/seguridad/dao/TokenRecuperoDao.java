package ar.edu.usal.mgnutri.seguridad.dao;

import ar.edu.usal.mgnutri.seguridad.domain.TokenRecupero;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TokenRecuperoDao extends JpaRepository<TokenRecupero, Long> {
    Optional<TokenRecupero> findByTokenHash(String tokenHash);
    long countByUsuarioIdAndCreatedAtAfter(Long usuarioId, Instant desde);
    List<TokenRecupero> findByUsuarioIdAndUsadoAtIsNull(Long usuarioId);
    @Modifying
    @Query("delete from TokenRecupero t where t.expiraAt < :limite")
    int eliminarExpirados(@Param("limite") Instant limite);
}
