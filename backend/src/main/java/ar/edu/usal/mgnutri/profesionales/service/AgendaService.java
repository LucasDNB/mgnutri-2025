package ar.edu.usal.mgnutri.profesionales.service;

import ar.edu.usal.mgnutri.bitacora.domain.Modulo;
import ar.edu.usal.mgnutri.bitacora.domain.TipoEvento;
import ar.edu.usal.mgnutri.bitacora.service.Auditable;
import ar.edu.usal.mgnutri.bitacora.service.Auditoria;
import ar.edu.usal.mgnutri.common.error.BusinessException;
import ar.edu.usal.mgnutri.common.error.ErrorCode;
import ar.edu.usal.mgnutri.common.security.CurrentUser;
import ar.edu.usal.mgnutri.common.security.UsuarioPrincipal;
import ar.edu.usal.mgnutri.profesionales.dao.BloqueoAgendaDao;
import ar.edu.usal.mgnutri.profesionales.dao.DisponibilidadDao;
import ar.edu.usal.mgnutri.profesionales.dao.ProfesionalDao;
import ar.edu.usal.mgnutri.profesionales.domain.BloqueoAgenda;
import ar.edu.usal.mgnutri.profesionales.domain.Disponibilidad;
import ar.edu.usal.mgnutri.profesionales.domain.Profesional;
import ar.edu.usal.mgnutri.profesionales.dto.ProfesionalDtos.BloqueoRequest;
import ar.edu.usal.mgnutri.profesionales.dto.ProfesionalDtos.BloqueoResponse;
import ar.edu.usal.mgnutri.profesionales.dto.ProfesionalDtos.FranjaDto;
import ar.edu.usal.mgnutri.profesionales.dto.ProfesionalDtos.FranjaResponse;
import ar.edu.usal.mgnutri.turnos.service.TurnoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

/** Disponibilidad semanal y bloqueos de agenda (US-02.4). */
@Service
public class AgendaService {

    private final ProfesionalDao profesionalDao;
    private final DisponibilidadDao disponibilidadDao;
    private final BloqueoAgendaDao bloqueoDao;
    private final TurnoService turnos;

    public AgendaService(ProfesionalDao profesionalDao, DisponibilidadDao disponibilidadDao, BloqueoAgendaDao bloqueoDao,
                         TurnoService turnos) {
        this.profesionalDao = profesionalDao;
        this.disponibilidadDao = disponibilidadDao;
        this.bloqueoDao = bloqueoDao;
        this.turnos = turnos;
    }

    @Transactional(readOnly = true)
    public List<FranjaResponse> franjas(Long profesionalId) {
        autorizar(profesionalId);
        return disponibilidadDao.findByProfesionalIdAndActivoTrueOrderByDiaSemanaAscHoraDesdeAsc(profesionalId).stream()
                .map(d -> new FranjaResponse(d.getId(), d.getDiaSemana(), d.getHoraDesde(), d.getHoraHasta(), d.getUbicacion()))
                .toList();
    }

    /** Reemplazo completo de franjas con validación RN-02.4 (sin superposición, duración ≥ turno). */
    @Transactional
    @Auditable(modulo = Modulo.PROFESIONALES, tipo = TipoEvento.MODIFICACION, entidad = "Disponibilidad",
            entidadId = "#profesionalId", descripcion = "Actualización de agenda semanal del profesional #{#profesionalId}")
    public List<FranjaResponse> reemplazarFranjas(Long profesionalId, List<FranjaDto> franjas) {
        Profesional p = autorizar(profesionalId);
        validarFranjas(franjas, p.getDuracionTurnoMin());
        disponibilidadDao.findByProfesionalIdAndActivoTrueOrderByDiaSemanaAscHoraDesdeAsc(profesionalId).forEach(d -> d.setActivo(false));
        disponibilidadDao.flush();
        for (FranjaDto f : franjas) {
            disponibilidadDao.save(new Disponibilidad(profesionalId, f.diaSemana(), f.horaDesde(), f.horaHasta(),
                    f.ubicacion() == null || f.ubicacion().isBlank() ? null : f.ubicacion().trim()));
        }
        Auditoria.detalle("franjas", franjas.size());
        return franjas(profesionalId);
    }

    public static void validarFranjas(List<FranjaDto> franjas, int duracionTurno) {
        for (FranjaDto f : franjas) {
            if (!f.horaDesde().isBefore(f.horaHasta())) {
                throw BusinessException.validacion("franjas", "En cada franja la hora desde debe ser anterior a la hora hasta.");
            }
            if (Duration.between(f.horaDesde(), f.horaHasta()).toMinutes() < duracionTurno) {
                throw BusinessException.validacion("franjas", "Cada franja debe durar al menos un turno (" + duracionTurno + " min).");
            }
        }
        List<FranjaDto> ordenadas = franjas.stream()
                .sorted(Comparator.comparing(FranjaDto::diaSemana).thenComparing(FranjaDto::horaDesde)).toList();
        for (int i = 1; i < ordenadas.size(); i++) {
            FranjaDto a = ordenadas.get(i - 1);
            FranjaDto b = ordenadas.get(i);
            if (a.diaSemana().equals(b.diaSemana()) && b.horaDesde().isBefore(a.horaHasta())) {
                throw BusinessException.validacion("franjas", "Hay franjas superpuestas el mismo día.");
            }
        }
    }

    @Transactional(readOnly = true)
    public List<BloqueoResponse> bloqueos(Long profesionalId) {
        autorizar(profesionalId);
        return bloqueoDao.findByProfesionalIdOrderByDesdeDesc(profesionalId).stream()
                .map(b -> new BloqueoResponse(b.getId(), b.getDesde(), b.getHasta(), b.getMotivo(), 0)).toList();
    }

    /** CA-02.4.3: si pisa turnos reservados, devuelve 409 TURNOS_AFECTADOS salvo confirmación. */
    @Transactional
    @Auditable(modulo = Modulo.PROFESIONALES, tipo = TipoEvento.ALTA, entidad = "BloqueoAgenda", entidadId = "#result.id()",
            descripcion = "Bloqueo de agenda del profesional #{#profesionalId}")
    public BloqueoResponse crearBloqueo(Long profesionalId, BloqueoRequest req) {
        autorizar(profesionalId);
        if (!req.desde().isBefore(req.hasta())) {
            throw BusinessException.validacion("hasta", "La fecha hasta debe ser posterior a la fecha desde.");
        }
        int afectados = turnos.afectadosPorBloqueo(profesionalId, req.desde(), req.hasta()).size();
        if (afectados > 0 && !Boolean.TRUE.equals(req.confirmarCancelacion())) {
            throw new BusinessException(ErrorCode.TURNOS_AFECTADOS, "El bloqueo afecta " + afectados
                    + " turno(s) reservado(s). Si confirmás, se cancelarán y se avisará a los pacientes.").con("turnos", afectados);
        }
        BloqueoAgenda b = bloqueoDao.save(new BloqueoAgenda(profesionalId, req.desde(), req.hasta(),
                req.motivo() == null || req.motivo().isBlank() ? null : req.motivo().trim()));
        int cancelados = afectados > 0 ? turnos.cancelarPorBloqueo(profesionalId, req.desde(), req.hasta(), req.motivo()) : 0;
        return new BloqueoResponse(b.getId(), b.getDesde(), b.getHasta(), b.getMotivo(), cancelados);
    }

    @Transactional
    @Auditable(modulo = Modulo.PROFESIONALES, tipo = TipoEvento.BAJA, entidad = "BloqueoAgenda", entidadId = "#bloqueoId",
            descripcion = "Eliminación de bloqueo de agenda")
    public void eliminarBloqueo(Long profesionalId, Long bloqueoId) {
        autorizar(profesionalId);
        BloqueoAgenda b = bloqueoDao.findById(bloqueoId).filter(x -> x.getProfesionalId().equals(profesionalId))
                .orElseThrow(() -> BusinessException.noEncontrado("Bloqueo"));
        bloqueoDao.delete(b);
    }

    /** CA-02.4.4: el profesional sólo edita su propia agenda; el admin la de todos. */
    private Profesional autorizar(Long profesionalId) {
        UsuarioPrincipal u = CurrentUser.get();
        if (!u.esAdmin() && !profesionalId.equals(u.profesionalId())) {
            throw BusinessException.sinPermiso();
        }
        return profesionalDao.findById(profesionalId).filter(Profesional::activo)
                .orElseThrow(() -> BusinessException.noEncontrado("Profesional"));
    }

    static boolean dentro(LocalTime t, LocalTime desde, LocalTime hasta) {
        return !t.isBefore(desde) && !t.isAfter(hasta);
    }
}
