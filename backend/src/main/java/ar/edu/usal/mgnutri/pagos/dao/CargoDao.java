package ar.edu.usal.mgnutri.pagos.dao;

import ar.edu.usal.mgnutri.pagos.domain.Cargo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CargoDao extends JpaRepository<Cargo, Long> {
    List<Cargo> findByPacienteIdOrderByFechaAscIdAsc(Long pacienteId);
    List<Cargo> findByPacienteIdAndAnuladoFalseOrderByVencimientoAscIdAsc(Long pacienteId);
    @Query("select count(c) > 0 from Cargo c where c.pacienteId = :pacienteId and c.concepto = :concepto and "
            + "((:periodo is null and c.periodo is null) or c.periodo = :periodo) and c.monto = :monto and c.anulado = false and (:excluirId is null or c.id <> :excluirId)")
    boolean existeDuplicado(@Param("pacienteId") Long pacienteId, @Param("concepto") String concepto, @Param("periodo") String periodo, @Param("monto") BigDecimal monto, @Param("excluirId") Long excluirId);
    List<Cargo> findByAnuladoFalseAndFechaLessThanEqual(LocalDate fecha);
}
