package ar.edu.usal.mgnutri.nutricion.dao;

import ar.edu.usal.mgnutri.nutricion.domain.ItemComida;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemComidaDao extends JpaRepository<ItemComida, Long> {
}
