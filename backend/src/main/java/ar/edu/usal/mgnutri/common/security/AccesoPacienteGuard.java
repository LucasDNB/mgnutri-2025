package ar.edu.usal.mgnutri.common.security;

import ar.edu.usal.mgnutri.common.Area;
import ar.edu.usal.mgnutri.common.error.BusinessException;
import ar.edu.usal.mgnutri.common.error.ErrorCode;
import ar.edu.usal.mgnutri.pacientes.dao.PacienteDao;
import ar.edu.usal.mgnutri.pacientes.dao.PacienteProfesionalDao;
import ar.edu.usal.mgnutri.pacientes.domain.Paciente;
import ar.edu.usal.mgnutri.pacientes.domain.PacienteProfesional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Autorización por pertenencia (constitución art. V.2): un profesional sólo accede a sus pacientes
 * asignados y un paciente sólo a sus propios datos. Lo usan todos los servicios con datos de pacientes.
 */
@Component
public class AccesoPacienteGuard {

    private final PacienteDao pacienteDao;
    private final PacienteProfesionalDao asignaciones;

    public AccesoPacienteGuard(PacienteDao pacienteDao, PacienteProfesionalDao asignaciones) {
        this.pacienteDao = pacienteDao;
        this.asignaciones = asignaciones;
    }

    /** Lectura: admin, el propio paciente o un profesional asignado (en cualquier área). */
    @Transactional(readOnly = true)
    public Paciente assertPuedeVer(Long pacienteId) {
        UsuarioPrincipal u = CurrentUser.get();
        Paciente p = pacienteDao.findById(pacienteId).orElseThrow(() -> BusinessException.noEncontrado("Paciente"));
        if (u.esAdmin()) {
            return p;
        }
        if (u.esPaciente()) {
            if (!pacienteId.equals(u.pacienteId())) {
                throw BusinessException.noEncontrado("Paciente"); // 404 para no revelar existencia
            }
            return p;
        }
        if (u.profesionalId() != null && asignaciones.existsByPacienteIdAndProfesionalIdAndActivoTrue(pacienteId, u.profesionalId())) {
            return p;
        }
        throw new BusinessException(ErrorCode.SIN_PERMISO, "No tenés asignado a este paciente.");
    }

    /** Edición: el profesional con el rol del área y asignado al paciente en esa área (D-07). */
    @Transactional(readOnly = true)
    public Paciente assertPuedeEditar(Long pacienteId, Area area) {
        UsuarioPrincipal u = CurrentUser.get();
        Paciente p = pacienteDao.findById(pacienteId).orElseThrow(() -> BusinessException.noEncontrado("Paciente"));
        if (!u.tiene(area.rolRequerido()) || u.profesionalId() == null) {
            throw BusinessException.sinPermiso();
        }
        if (!asignaciones.existsByPacienteIdAndProfesionalIdAndAreaAndActivoTrue(pacienteId, u.profesionalId(), area)) {
            throw new BusinessException(ErrorCode.PACIENTE_NO_ASIGNADO,
                    "No estás asignado a este paciente en el área de " + area.etiqueta().toLowerCase() + ".");
        }
        if (!p.activo()) {
            throw new BusinessException(ErrorCode.REGLA_NEGOCIO, "El paciente está dado de baja.");
        }
        return p;
    }

    /** Ids de pacientes visibles por el profesional actual (null = sin restricción, admin). */
    @Transactional(readOnly = true)
    public Set<Long> pacientesVisibles() {
        UsuarioPrincipal u = CurrentUser.get();
        if (u.esAdmin()) {
            return null;
        }
        if (u.esPaciente()) {
            return Set.of(u.pacienteId());
        }
        if (u.profesionalId() == null) {
            return Set.of();
        }
        List<PacienteProfesional> lista = asignaciones.findByProfesionalIdAndActivoTrue(u.profesionalId());
        return lista.stream().map(PacienteProfesional::getPacienteId).collect(Collectors.toSet());
    }
}
