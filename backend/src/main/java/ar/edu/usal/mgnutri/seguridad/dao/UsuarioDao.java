package ar.edu.usal.mgnutri.seguridad.dao;

import ar.edu.usal.mgnutri.common.Rol;
import ar.edu.usal.mgnutri.seguridad.domain.Usuario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioDao extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsername(String username);
    boolean existsByUsername(String username);
    @Query("select count(u) from Usuario u join u.roles r where r = :rol and u.estado <> ar.edu.usal.mgnutri.seguridad.domain.Usuario.Estado.BAJA")
    long contarActivosConRol(@Param("rol") Rol rol);
    @Query("select u from Usuario u join u.roles r where r = :rol and u.estado = ar.edu.usal.mgnutri.seguridad.domain.Usuario.Estado.ACTIVO")
    List<Usuario> activosConRol(@Param("rol") Rol rol);
}
