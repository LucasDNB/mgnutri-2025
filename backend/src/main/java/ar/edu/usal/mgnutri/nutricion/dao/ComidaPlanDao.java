package ar.edu.usal.mgnutri.nutricion.dao;

import ar.edu.usal.mgnutri.nutricion.domain.ComidaPlan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComidaPlanDao extends JpaRepository<ComidaPlan, Long> {
}
