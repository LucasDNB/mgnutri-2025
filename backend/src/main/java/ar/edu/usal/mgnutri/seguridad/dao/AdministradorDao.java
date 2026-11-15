package ar.edu.usal.mgnutri.seguridad.dao;

import ar.edu.usal.mgnutri.seguridad.domain.Administrador;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdministradorDao extends JpaRepository<Administrador, Long> {
    Optional<Administrador> findByUsuarioId(Long usuarioId);
}
