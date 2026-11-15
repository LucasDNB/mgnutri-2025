package ar.edu.usal.mgnutri.pacientes.dao;

import ar.edu.usal.mgnutri.pacientes.domain.Paciente;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PacienteDao extends JpaRepository<Paciente, Long> {
    Optional<Paciente> findByUsuarioId(Long usuarioId);
    boolean existsByNroDocumento(String nroDocumento);
    Optional<Paciente> findByNroDocumento(String nroDocumento);
    List<Paciente> findByEstado(Paciente.Estado estado);
    /** Búsqueda parcial sin distinguir mayúsculas ni acentos (CA-01.2.1). */
    @Query(value = "SELECT p.* FROM paciente p WHERE p.estado = :estado AND (:q IS NULL "
            + "OR p.apellido COLLATE Latin1_General_CI_AI LIKE '%' + :q + '%' "
            + "OR p.nombre COLLATE Latin1_General_CI_AI LIKE '%' + :q + '%' "
            + "OR (p.nombre + ' ' + p.apellido) COLLATE Latin1_General_CI_AI LIKE '%' + :q + '%' "
            + "OR (p.apellido + ' ' + p.nombre) COLLATE Latin1_General_CI_AI LIKE '%' + :q + '%' "
            + "OR p.nro_documento LIKE '%' + :q + '%') "
            + "AND (:profesionalId IS NULL OR EXISTS (SELECT 1 FROM paciente_profesional pp WHERE pp.paciente_id = p.id "
            + "AND pp.profesional_id = :profesionalId AND pp.activo = 1))",
            countQuery = "SELECT COUNT(*) FROM paciente p WHERE p.estado = :estado AND (:q IS NULL "
            + "OR p.apellido COLLATE Latin1_General_CI_AI LIKE '%' + :q + '%' "
            + "OR p.nombre COLLATE Latin1_General_CI_AI LIKE '%' + :q + '%' "
            + "OR (p.nombre + ' ' + p.apellido) COLLATE Latin1_General_CI_AI LIKE '%' + :q + '%' "
            + "OR (p.apellido + ' ' + p.nombre) COLLATE Latin1_General_CI_AI LIKE '%' + :q + '%' "
            + "OR p.nro_documento LIKE '%' + :q + '%') "
            + "AND (:profesionalId IS NULL OR EXISTS (SELECT 1 FROM paciente_profesional pp WHERE pp.paciente_id = p.id "
            + "AND pp.profesional_id = :profesionalId AND pp.activo = 1))",
            nativeQuery = true)
    Page<Paciente> buscar(@Param("q") String q, @Param("estado") String estado, @Param("profesionalId") Long profesionalId, Pageable pageable);
}
