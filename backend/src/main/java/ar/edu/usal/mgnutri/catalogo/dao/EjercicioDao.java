package ar.edu.usal.mgnutri.catalogo.dao;

import ar.edu.usal.mgnutri.catalogo.domain.Ejercicio;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EjercicioDao extends JpaRepository<Ejercicio, Long>, JpaSpecificationExecutor<Ejercicio> {
    Optional<Ejercicio> findByNombreNormalizado(String nombreNormalizado);
    List<Ejercicio> findByActivoTrueAndGrupoPrincipalIdAndIdNot(Long grupoId, Long id);
}
