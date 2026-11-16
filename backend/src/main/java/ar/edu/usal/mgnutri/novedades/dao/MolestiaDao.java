package ar.edu.usal.mgnutri.novedades.dao;

import ar.edu.usal.mgnutri.novedades.domain.Molestia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface MolestiaDao extends JpaRepository<Molestia, Long> {
    List<Molestia> findByPacienteIdOrderByCreatedAtDesc(Long pacienteId);

    List<Molestia> findByPacienteIdAndFechaBetweenOrderByCreatedAtDesc(Long pacienteId, LocalDate desde, LocalDate hasta);

    List<Molestia> findByPacienteIdInOrderByCreatedAtDesc(Collection<Long> pacientes);
}
