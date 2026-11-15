package ar.edu.usal.mgnutri.catalogo.dao;

import ar.edu.usal.mgnutri.catalogo.domain.Alimento;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AlimentoDao extends JpaRepository<Alimento, Long>, JpaSpecificationExecutor<Alimento> {
    Optional<Alimento> findByNombreNormalizado(String nombreNormalizado);
    List<Alimento> findByActivoTrue();
    List<Alimento> findByActivoTrueAndGrupoIdAndIdNot(Long grupoId, Long id);
}
