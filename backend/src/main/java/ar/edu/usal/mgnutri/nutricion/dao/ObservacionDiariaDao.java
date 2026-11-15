package ar.edu.usal.mgnutri.nutricion.dao;

import ar.edu.usal.mgnutri.nutricion.domain.ObservacionDiaria;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ObservacionDiariaDao extends JpaRepository<ObservacionDiaria, Long> {
    Optional<ObservacionDiaria> findByPacienteIdAndFecha(Long pacienteId, LocalDate fecha);
    List<ObservacionDiaria> findByPacienteIdAndFechaBetweenOrderByFechaAsc(Long pacienteId, LocalDate desde, LocalDate hasta);
}
