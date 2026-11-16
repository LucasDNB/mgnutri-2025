package ar.edu.usal.mgnutri.entrenamiento.dao;

import ar.edu.usal.mgnutri.entrenamiento.domain.RegistroEntrenamiento;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistroEntrenamientoDao extends JpaRepository<RegistroEntrenamiento, Long> {
    Optional<RegistroEntrenamiento> findByPacienteIdAndEjercicioRutinaIdAndFecha(Long pacienteId, Long ejercicioRutinaId, LocalDate fecha);
    List<RegistroEntrenamiento> findByPacienteIdAndFechaBetweenOrderByFechaAsc(Long pacienteId, LocalDate desde, LocalDate hasta);
    Optional<RegistroEntrenamiento> findFirstByPacienteIdAndEjercicioIdAndCargaKgIsNotNullOrderByFechaDesc(Long pacienteId, Long ejercicioId);
}
