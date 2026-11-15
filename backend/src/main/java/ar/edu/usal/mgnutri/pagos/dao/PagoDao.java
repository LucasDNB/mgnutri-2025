package ar.edu.usal.mgnutri.pagos.dao;

import ar.edu.usal.mgnutri.pagos.domain.Pago;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PagoDao extends JpaRepository<Pago, Long> {
    List<Pago> findByPacienteIdOrderByFechaAscIdAsc(Long pacienteId);
    @Query(value = "SELECT NEXT VALUE FOR seq_comprobante", nativeQuery = true)
    Long siguienteComprobante();
    @Query("select p from Pago p where p.fecha >= :desde and p.fecha <= :hasta and (:pacienteId is null or p.pacienteId = :pacienteId) order by p.fecha, p.id")
    List<Pago> enRango(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta, @Param("pacienteId") Long pacienteId);
    List<Pago> findByAnuladoFalseAndFechaLessThanEqual(LocalDate fecha);
}
