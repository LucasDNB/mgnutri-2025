package ar.edu.usal.mgnutri.catalogo.dao;

import ar.edu.usal.mgnutri.catalogo.domain.GrupoMuscular;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GrupoMuscularDao extends JpaRepository<GrupoMuscular, Long> {
    List<GrupoMuscular> findAllByOrderByNombreAsc();
}
