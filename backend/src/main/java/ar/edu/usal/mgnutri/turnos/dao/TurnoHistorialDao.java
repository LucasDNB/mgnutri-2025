package ar.edu.usal.mgnutri.turnos.dao;

import ar.edu.usal.mgnutri.turnos.domain.TurnoHistorial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TurnoHistorialDao extends JpaRepository<TurnoHistorial, Long> {
    List<TurnoHistorial> findByTurnoIdOrderByCambiadoAtDesc(Long turnoId);
}
