package ar.edu.usal.mgnutri.seguridad.dao;

import ar.edu.usal.mgnutri.seguridad.domain.RegistroAcceso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RegistroAccesoDao extends JpaRepository<RegistroAcceso, Long>, JpaSpecificationExecutor<RegistroAcceso> {
}
