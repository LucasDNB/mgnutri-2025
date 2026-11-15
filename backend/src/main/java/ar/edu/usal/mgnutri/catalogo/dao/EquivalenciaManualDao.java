package ar.edu.usal.mgnutri.catalogo.dao;

import ar.edu.usal.mgnutri.catalogo.domain.EquivalenciaManual;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquivalenciaManualDao extends JpaRepository<EquivalenciaManual, Long> {
    List<EquivalenciaManual> findByAlimentoId(Long alimentoId);
    void deleteByAlimentoId(Long alimentoId);
}
