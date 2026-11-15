package ar.edu.usal.mgnutri.nutricion.dao;

import ar.edu.usal.mgnutri.nutricion.domain.ReemplazoAlimento;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReemplazoAlimentoDao extends JpaRepository<ReemplazoAlimento, Long> {
    Optional<ReemplazoAlimento> findByItemComidaIdAndActivoTrue(Long itemComidaId);
    List<ReemplazoAlimento> findByPacienteIdAndActivoTrue(Long pacienteId);
    List<ReemplazoAlimento> findByItemComidaIdIn(Collection<Long> itemIds);
}
