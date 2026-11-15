package ar.edu.usal.mgnutri.bitacora.dao;

import ar.edu.usal.mgnutri.bitacora.domain.BitacoraEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BitacoraDao extends JpaRepository<BitacoraEvento, Long>, JpaSpecificationExecutor<BitacoraEvento> {
}
