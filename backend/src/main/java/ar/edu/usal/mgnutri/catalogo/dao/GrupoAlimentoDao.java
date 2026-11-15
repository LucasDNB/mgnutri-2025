package ar.edu.usal.mgnutri.catalogo.dao;

import ar.edu.usal.mgnutri.catalogo.domain.GrupoAlimento;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GrupoAlimentoDao extends JpaRepository<GrupoAlimento, Long> {
    List<GrupoAlimento> findAllByOrderByNombreAsc();
}
