package ar.edu.usal.mgnutri.pacientes.dao;

import ar.edu.usal.mgnutri.common.Area;
import ar.edu.usal.mgnutri.pacientes.domain.PacienteProfesional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PacienteProfesionalDao extends JpaRepository<PacienteProfesional, Long> {
    List<PacienteProfesional> findByPacienteIdAndActivoTrue(Long pacienteId);
    List<PacienteProfesional> findByProfesionalIdAndActivoTrue(Long profesionalId);
    List<PacienteProfesional> findByProfesionalIdAndAreaAndActivoTrue(Long profesionalId, Area area);
    List<PacienteProfesional> findByPacienteIdAndAreaAndActivoTrue(Long pacienteId, Area area);
    boolean existsByPacienteIdAndProfesionalIdAndActivoTrue(Long pacienteId, Long profesionalId);
    boolean existsByPacienteIdAndProfesionalIdAndAreaAndActivoTrue(Long pacienteId, Long profesionalId, Area area);
}
