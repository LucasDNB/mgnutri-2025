package ar.edu.usal.mgnutri.pagos.service;

import ar.edu.usal.mgnutri.bitacora.domain.Modulo;
import ar.edu.usal.mgnutri.bitacora.domain.TipoEvento;
import ar.edu.usal.mgnutri.bitacora.service.Auditable;
import ar.edu.usal.mgnutri.bitacora.service.Auditoria;
import ar.edu.usal.mgnutri.common.Textos;
import ar.edu.usal.mgnutri.common.error.BusinessException;
import ar.edu.usal.mgnutri.notificaciones.service.Notificador;
import ar.edu.usal.mgnutri.pacientes.dao.PacienteDao;
import ar.edu.usal.mgnutri.pacientes.domain.Paciente;
import ar.edu.usal.mgnutri.pagos.dao.ArancelDao;
import ar.edu.usal.mgnutri.pagos.domain.Arancel;
import ar.edu.usal.mgnutri.pagos.dto.PagoDtos.ArancelRequest;
import ar.edu.usal.mgnutri.pagos.dto.PagoDtos.ArancelResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/** Aranceles (D-20) con notificación opcional del cambio de monto (US-11.4). */
@Service
public class ArancelService {

    private final ArancelDao dao;
    private final PacienteDao pacienteDao;
    private final Notificador notificador;

    public ArancelService(ArancelDao dao, PacienteDao pacienteDao, Notificador notificador) {
        this.dao = dao;
        this.pacienteDao = pacienteDao;
        this.notificador = notificador;
    }

    @Transactional(readOnly = true)
    public List<ArancelResponse> listar() {
        return dao.findAllByOrderByActivoDescConceptoAsc().stream().map(a -> respuesta(a, null)).toList();
    }

    @Transactional
    @Auditable(modulo = Modulo.PAGOS, tipo = TipoEvento.ALTA, entidad = "Arancel", entidadId = "#result.id()",
            descripcion = "Alta de arancel #{#req.concepto()}")
    public ArancelResponse crear(ArancelRequest req) {
        Arancel a = new Arancel();
        a.setConcepto(req.concepto().trim());
        a.setMonto(req.monto().setScale(2, RoundingMode.HALF_EVEN));
        a.setVigenteDesde(req.vigenteDesde());
        a.setActivo(req.activo() == null || req.activo());
        dao.saveAndFlush(a);
        return respuesta(a, null);
    }

    @Transactional
    @Auditable(modulo = Modulo.PAGOS, tipo = TipoEvento.MODIFICACION, entidad = "Arancel", entidadId = "#id",
            descripcion = "Modificación de arancel #{#req.concepto()}")
    public ArancelResponse modificar(Long id, ArancelRequest req) {
        Arancel a = dao.findById(id).orElseThrow(() -> BusinessException.noEncontrado("Arancel"));
        Textos.verificarVersion(a.getVersion(), req.version());
        boolean cambioMonto = a.getMonto().compareTo(req.monto()) != 0;
        if (cambioMonto) {
            Auditoria.detalle("monto", Map.of("anterior", a.getMonto().toPlainString(), "nuevo", req.monto().toPlainString()));
        }
        a.setConcepto(req.concepto().trim());
        a.setMonto(req.monto().setScale(2, RoundingMode.HALF_EVEN));
        a.setVigenteDesde(req.vigenteDesde());
        a.setActivo(req.activo() == null || req.activo());
        dao.saveAndFlush(a);
        Integer notificados = null;
        if (cambioMonto && Boolean.TRUE.equals(req.notificarPacientes())) {
            int n = 0;
            for (Paciente p : pacienteDao.findByEstado(Paciente.Estado.ACTIVO)) {
                if (p.getTelefonoWhatsapp() != null) {
                    notificador.cambioArancel(p.getUsuarioId(), p.getNombre(), p.getTelefonoWhatsapp(), a.getConcepto(),
                            a.getMonto(), a.getVigenteDesde(), a.getId());
                    n++;
                }
            }
            notificados = n;
            Auditoria.detalle("pacientesNotificados", n);
        }
        return respuesta(a, notificados);
    }

    static ArancelResponse respuesta(Arancel a, Integer notificados) {
        return new ArancelResponse(a.getId(), a.getConcepto(), a.getMonto(), a.getVigenteDesde(), a.isActivo(), a.getVersion(), notificados);
    }
}
