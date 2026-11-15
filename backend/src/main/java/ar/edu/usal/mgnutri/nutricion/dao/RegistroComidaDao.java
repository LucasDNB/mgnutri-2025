package ar.edu.usal.mgnutri.nutricion.dao;

import ar.edu.usal.mgnutri.nutricion.domain.RegistroComida;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistroComidaDao extends JpaRepository<RegistroComida, Long> {
    Optional<RegistroComida> findByPacienteIdAndComidaPlanIdAndFecha(Long pacienteId, Long comidaPlanId, LocalDate fecha);
    List<RegistroComida> findByPacienteIdAndFechaBetweenOrderByFechaAsc(Long pacienteId, LocalDate desde, LocalDate hasta);
    List<RegistroComida> findByComidaPlanIdIn(Collection<Long> comidaIds);
}
