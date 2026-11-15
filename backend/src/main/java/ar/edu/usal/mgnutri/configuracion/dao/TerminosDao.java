package ar.edu.usal.mgnutri.configuracion.dao;

import ar.edu.usal.mgnutri.configuracion.domain.TerminosCondiciones;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TerminosDao extends JpaRepository<TerminosCondiciones, String> {
    List<TerminosCondiciones> findAllByOrderByVigenteDesdeDesc();
}
