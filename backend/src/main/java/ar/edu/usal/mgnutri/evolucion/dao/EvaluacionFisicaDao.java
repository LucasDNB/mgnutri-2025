package ar.edu.usal.mgnutri.evolucion.dao;

import ar.edu.usal.mgnutri.evolucion.domain.EvaluacionFisica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface EvaluacionFisicaDao extends JpaRepository<EvaluacionFisica, Long> {
    List<EvaluacionFisica> findByPacienteIdAndFechaBetweenOrderByFechaAsc(Long pacienteId, LocalDate desde, LocalDate hasta);
}
