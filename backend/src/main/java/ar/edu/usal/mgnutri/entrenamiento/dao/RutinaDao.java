package ar.edu.usal.mgnutri.entrenamiento.dao;

import ar.edu.usal.mgnutri.entrenamiento.domain.Rutina;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RutinaDao extends JpaRepository<Rutina, Long> {
    Optional<Rutina> findFirstByPacienteIdAndEstado(Long pacienteId, Rutina.Estado estado);
    List<Rutina> findByPacienteIdOrderByFechaInicioDescIdDesc(Long pacienteId);
    List<Rutina> findByEstadoAndFechaFinBefore(Rutina.Estado estado, LocalDate fecha);
    long countByEntrenadorIdAndEstado(Long entrenadorId, Rutina.Estado estado);
}
