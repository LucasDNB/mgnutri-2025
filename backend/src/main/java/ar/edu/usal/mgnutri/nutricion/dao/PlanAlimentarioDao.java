package ar.edu.usal.mgnutri.nutricion.dao;

import ar.edu.usal.mgnutri.nutricion.domain.PlanAlimentario;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlanAlimentarioDao extends JpaRepository<PlanAlimentario, Long> {
    Optional<PlanAlimentario> findFirstByPacienteIdAndEstado(Long pacienteId, PlanAlimentario.Estado estado);
    List<PlanAlimentario> findByPacienteIdOrderByFechaInicioDescIdDesc(Long pacienteId);
    List<PlanAlimentario> findByEstadoAndFechaFinBefore(PlanAlimentario.Estado estado, LocalDate fecha);
    long countByNutricionistaIdAndEstado(Long nutricionistaId, PlanAlimentario.Estado estado);
    @Query("select p from PlanAlimentario p where p.createdAt >= :desde and p.createdAt < :hasta order by p.createdAt")
    List<PlanAlimentario> creadosEntre(@Param("desde") Instant desde, @Param("hasta") Instant hasta);
}
