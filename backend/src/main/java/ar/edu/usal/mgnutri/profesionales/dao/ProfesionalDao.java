package ar.edu.usal.mgnutri.profesionales.dao;

import ar.edu.usal.mgnutri.profesionales.domain.Profesional;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProfesionalDao extends JpaRepository<Profesional, Long> {
    Optional<Profesional> findByUsuarioId(Long usuarioId);
    boolean existsByNroDocumento(String nroDocumento);
    Optional<Profesional> findByNroDocumento(String nroDocumento);
    List<Profesional> findByEstado(Profesional.Estado estado);
    @Query("select p from Profesional p where (:estado is null or p.estado = :estado) and (:tipo is null or p.tipo = :tipo) and (:q is null or lower(p.apellido) like lower(concat('%', :q, '%')) "
            + "or lower(p.nombre) like lower(concat('%', :q, '%')) or p.nroDocumento like concat('%', :q, '%'))")
    Page<Profesional> buscar(@Param("q") String q, @Param("tipo") Profesional.Tipo tipo, @Param("estado") Profesional.Estado estado, Pageable pageable);
}
