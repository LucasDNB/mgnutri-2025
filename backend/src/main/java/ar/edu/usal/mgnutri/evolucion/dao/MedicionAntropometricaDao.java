package ar.edu.usal.mgnutri.evolucion.dao;

import ar.edu.usal.mgnutri.evolucion.domain.MedicionAntropometrica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MedicionAntropometricaDao extends JpaRepository<MedicionAntropometrica, Long> {
    Optional<MedicionAntropometrica> findFirstByPacienteIdOrderByFechaDescIdDesc(Long pacienteId);

    List<MedicionAntropometrica> findByPacienteIdAndFechaBetweenOrderByFechaAsc(Long pacienteId, LocalDate desde, LocalDate hasta);
}
