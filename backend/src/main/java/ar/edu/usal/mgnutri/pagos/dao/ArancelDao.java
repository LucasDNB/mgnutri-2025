package ar.edu.usal.mgnutri.pagos.dao;

import ar.edu.usal.mgnutri.pagos.domain.Arancel;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArancelDao extends JpaRepository<Arancel, Long> {
    List<Arancel> findAllByOrderByActivoDescConceptoAsc();
}
