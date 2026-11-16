package ar.edu.usal.mgnutri.entrenamiento.dao;

import ar.edu.usal.mgnutri.entrenamiento.domain.ReemplazoEjercicio;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReemplazoEjercicioDao extends JpaRepository<ReemplazoEjercicio, Long> {
    Optional<ReemplazoEjercicio> findByEjercicioRutinaIdAndActivoTrue(Long ejercicioRutinaId);
    List<ReemplazoEjercicio> findByPacienteIdAndActivoTrue(Long pacienteId);
    List<ReemplazoEjercicio> findByPacienteIdAndFechaBetween(Long pacienteId, LocalDate desde, LocalDate hasta);
}
