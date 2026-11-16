package ar.edu.usal.mgnutri.novedades.dao;

import ar.edu.usal.mgnutri.novedades.domain.Comentario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface ComentarioDao extends JpaRepository<Comentario, Long> {
    List<Comentario> findByPacienteIdOrderByCreatedAtDesc(Long pacienteId);

    List<Comentario> findByPacienteIdAndFechaBetweenOrderByCreatedAtDesc(Long pacienteId, LocalDate desde, LocalDate hasta);

    List<Comentario> findByPacienteIdInAndTipoInOrderByCreatedAtDesc(Collection<Long> pacientes, Collection<Comentario.Tipo> tipos);
}
