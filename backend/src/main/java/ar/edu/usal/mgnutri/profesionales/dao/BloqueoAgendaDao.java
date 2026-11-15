package ar.edu.usal.mgnutri.profesionales.dao;

import ar.edu.usal.mgnutri.profesionales.domain.BloqueoAgenda;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BloqueoAgendaDao extends JpaRepository<BloqueoAgenda, Long> {
    List<BloqueoAgenda> findByProfesionalIdOrderByDesdeDesc(Long profesionalId);
    @Query("select b from BloqueoAgenda b where b.profesionalId = :profesionalId and b.desde < :hasta and b.hasta > :desde")
    List<BloqueoAgenda> solapados(@Param("profesionalId") Long profesionalId, @Param("desde") Instant desde, @Param("hasta") Instant hasta);
}
