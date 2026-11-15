package ar.edu.usal.mgnutri.configuracion.dao;

import ar.edu.usal.mgnutri.configuracion.domain.ConfiguracionSistema;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracionDao extends JpaRepository<ConfiguracionSistema, String> {
}
