package ar.edu.usal.mgnutri.pacientes.service;

import ar.edu.usal.mgnutri.bitacora.domain.Modulo;
import ar.edu.usal.mgnutri.bitacora.domain.TipoEvento;
import ar.edu.usal.mgnutri.bitacora.service.Auditable;
import ar.edu.usal.mgnutri.bitacora.service.Auditoria;
import ar.edu.usal.mgnutri.common.Area;
import ar.edu.usal.mgnutri.common.error.BusinessException;
import ar.edu.usal.mgnutri.common.error.ErrorCode;
import ar.edu.usal.mgnutri.common.security.AccesoPacienteGuard;
import ar.edu.usal.mgnutri.pacientes.dao.PacienteDao;
import ar.edu.usal.mgnutri.pacientes.dao.PacienteProfesionalDao;
import ar.edu.usal.mgnutri.pacientes.domain.Paciente;
import ar.edu.usal.mgnutri.pacientes.domain.PacienteProfesional;
import ar.edu.usal.mgnutri.pacientes.dto.PacienteDtos.AsignacionItem;
import ar.edu.usal.mgnutri.pacientes.dto.PacienteDtos.AsignacionResponse;
import ar.edu.usal.mgnutri.profesionales.dao.ProfesionalDao;
import ar.edu.usal.mgnutri.profesionales.domain.Profesional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Asignación de pacientes a profesionales por área (CU-29). */
@Service
public class AsignacionService {

    private final PacienteProfesionalDao dao;
    private final PacienteDao pacienteDao;
    private final ProfesionalDao profesionalDao;
    private final AccesoPacienteGuard guard;
    private final Clock clock;

    public AsignacionService(PacienteProfesionalDao dao, PacienteDao pacienteDao, ProfesionalDao profesionalDao,
                             AccesoPacienteGuard guard, Clock clock) {
        this.dao = dao;
        this.pacienteDao = pacienteDao;
        this.profesionalDao = profesionalDao;
        this.guard = guard;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<AsignacionResponse> listar(Long pacienteId) {
        guard.assertPuedeVer(pacienteId);
        return respuestas(dao.findByPacienteIdAndActivoTrue(pacienteId));
    }

    @Transactional
    @Auditable(modulo = Modulo.PACIENTES, tipo = TipoEvento.MODIFICACION, entidad = "PacienteProfesional",
            entidadId = "#pacienteId", descripcion = "Asignación de profesionales al paciente #{#pacienteId}")
    public List<AsignacionResponse> asignar(Long pacienteId, List<AsignacionItem> items) {
        Paciente p = pacienteDao.findById(pacienteId).orElseThrow(() -> BusinessException.noEncontrado("Paciente"));
        if (!p.activo()) {
            throw new BusinessException(ErrorCode.REGLA_NEGOCIO, "El paciente está dado de baja.");
        }
        crearAsignaciones(pacienteId, items);
        return respuestas(dao.findByPacienteIdAndActivoTrue(pacienteId));
    }

    /** Usado también por el alta de paciente (CA-01.1.7). */
    @Transactional
    public void crearAsignaciones(Long pacienteId, List<AsignacionItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        if (profesionalDao.findByEstado(Profesional.Estado.ACTIVO).isEmpty()) {
            throw new BusinessException(ErrorCode.SIN_PROFESIONALES, "No hay profesionales disponibles");
        }
        for (AsignacionItem item : items) {
            Profesional prof = profesionalDao.findById(item.profesionalId())
                    .filter(Profesional::activo)
                    .orElseThrow(() -> BusinessException.validacion("profesionalId", "El profesional no existe o está dado de baja."));
            if (!prof.getTipo().atiende(item.area())) {
                throw BusinessException.validacion("area", prof.nombreVisible() + " no atiende el área de "
                        + item.area().etiqueta().toLowerCase() + ".");
            }
            if (dao.existsByPacienteIdAndProfesionalIdAndAreaAndActivoTrue(pacienteId, prof.getId(), item.area())) {
                throw new BusinessException(ErrorCode.DUPLICADO, "El paciente ya está asignado a ese profesional");
            }
            dao.save(new PacienteProfesional(pacienteId, prof.getId(), item.area(), LocalDate.now(clock)));
            Auditoria.detalle("asignado_" + prof.getId(), item.area().name());
        }
    }

    @Transactional
    @Auditable(modulo = Modulo.PACIENTES, tipo = TipoEvento.MODIFICACION, entidad = "PacienteProfesional",
            entidadId = "#asignacionId", descripcion = "Desasignación de profesional (asignación #{#asignacionId})")
    public void desasignar(Long pacienteId, Long asignacionId) {
        PacienteProfesional pp = dao.findById(asignacionId)
                .filter(a -> a.getPacienteId().equals(pacienteId) && a.isActivo())
                .orElseThrow(() -> BusinessException.noEncontrado("Asignación"));
        pp.setActivo(false);
    }

    /** Desactiva las asignaciones de un área de un profesional (cambio de tipo o baja). */
    @Transactional
    public int desactivarDeProfesional(Long profesionalId, Area area) {
        List<PacienteProfesional> lista = area == null ? dao.findByProfesionalIdAndActivoTrue(profesionalId)
                : dao.findByProfesionalIdAndAreaAndActivoTrue(profesionalId, area);
        lista.forEach(a -> a.setActivo(false));
        return lista.size();
    }

    @Transactional
    public void desactivarDePaciente(Long pacienteId) {
        dao.findByPacienteIdAndActivoTrue(pacienteId).forEach(a -> a.setActivo(false));
    }

    List<AsignacionResponse> respuestas(List<PacienteProfesional> lista) {
        Map<Long, Profesional> profs = profesionalDao.findAllById(lista.stream().map(PacienteProfesional::getProfesionalId).toList())
                .stream().collect(Collectors.toMap(Profesional::getId, Function.identity()));
        return lista.stream()
                .sorted(Comparator.comparing(PacienteProfesional::getArea))
                .map(a -> {
                    Profesional pr = profs.get(a.getProfesionalId());
                    return new AsignacionResponse(a.getId(), a.getProfesionalId(), pr == null ? "" : pr.nombreVisible() + " (" + pr.nombreCompleto() + ")",
                            pr == null ? null : pr.getTipo().name(), a.getArea(), a.getFechaAsignacion(),
                            pr == null ? null : pr.getTelefonoWhatsapp());
                }).toList();
    }
}
