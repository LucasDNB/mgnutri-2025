package ar.edu.usal.mgnutri.profesionales.dao;

import ar.edu.usal.mgnutri.profesionales.domain.Disponibilidad;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DisponibilidadDao extends JpaRepository<Disponibilidad, Long> {
    List<Disponibilidad> findByProfesionalIdAndActivoTrueOrderByDiaSemanaAscHoraDesdeAsc(Long profesionalId);
    boolean existsByProfesionalIdAndActivoTrue(Long profesionalId);
}
