package ar.edu.usal.mgnutri.pagos.service;

import ar.edu.usal.mgnutri.bitacora.domain.Modulo;
import ar.edu.usal.mgnutri.bitacora.domain.TipoEvento;
import ar.edu.usal.mgnutri.bitacora.service.Auditable;
import ar.edu.usal.mgnutri.bitacora.service.Auditoria;
import ar.edu.usal.mgnutri.common.Textos;
import ar.edu.usal.mgnutri.common.error.BusinessException;
import ar.edu.usal.mgnutri.common.error.ErrorCode;
import ar.edu.usal.mgnutri.notificaciones.domain.TipoNotificacion;
import ar.edu.usal.mgnutri.notificaciones.service.NotificacionService;
import ar.edu.usal.mgnutri.notificaciones.service.Notificador;
import ar.edu.usal.mgnutri.pacientes.dao.PacienteDao;
import ar.edu.usal.mgnutri.pacientes.domain.Paciente;
import ar.edu.usal.mgnutri.pagos.dao.ArancelDao;
import ar.edu.usal.mgnutri.pagos.dao.CargoDao;
import ar.edu.usal.mgnutri.pagos.dao.SaldoDao;
import ar.edu.usal.mgnutri.pagos.domain.Arancel;
import ar.edu.usal.mgnutri.pagos.domain.Cargo;
import ar.edu.usal.mgnutri.pagos.dto.PagoDtos.CargoRequest;
import ar.edu.usal.mgnutri.pagos.dto.PagoDtos.CargoResponse;
import ar.edu.usal.mgnutri.pagos.dto.PagoDtos.GeneracionMasivaRequest;
import ar.edu.usal.mgnutri.pagos.dto.PagoDtos.GeneracionMasivaResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Deudas (cargos) del paciente (CU-45, US-07.3, US-07.4). */
@Service
public class CargoService {

    static final Duration VENTANA_RECORDATORIO = Duration.ofDays(6);

    private final CargoDao dao;
    private final ArancelDao arancelDao;
    private final PacienteDao pacienteDao;
    private final SaldoDao saldoDao;
    private final Notificador notificador;
    private final NotificacionService notificaciones;
    private final Clock clock;

    public CargoService(CargoDao dao, ArancelDao arancelDao, PacienteDao pacienteDao, SaldoDao saldoDao, Notificador notificador,
                        NotificacionService notificaciones, Clock clock) {
        this.dao = dao;
        this.arancelDao = arancelDao;
        this.pacienteDao = pacienteDao;
        this.saldoDao = saldoDao;
        this.notificador = notificador;
        this.notificaciones = notificaciones;
        this.clock = clock;
    }

    @Transactional
    @Auditable(modulo = Modulo.PAGOS, tipo = TipoEvento.ALTA, entidad = "Cargo", entidadId = "#result.id()",
            descripcion = "Deuda registrada: #{#req.concepto()} #{#req.monto()}")
    public CargoResponse registrar(Long pacienteId, CargoRequest req) {
        Paciente p = pacienteDao.findById(pacienteId).orElseThrow(() -> BusinessException.noEncontrado("Paciente"));
        String periodo = Textos.vacioANull(req.periodo());
        BigDecimal monto = req.monto().setScale(2, RoundingMode.HALF_EVEN);
        if (dao.existeDuplicado(pacienteId, req.concepto().trim(), periodo, monto, null)) {
            throw new BusinessException(ErrorCode.DEUDA_DUPLICADA, "El paciente ya tiene esta deuda registrada");
        }
        Cargo c = new Cargo();
        c.setPacienteId(pacienteId);
        aplicar(c, req, periodo, monto);
        dao.saveAndFlush(c);
        String notif = null;
        if (Boolean.TRUE.equals(req.notificarAhora())) {
            notif = recordarDeuda(p) ? "ENCOLADA" : "OMITIDA_RECIENTE";
        }
        return respuesta(c, notif);
    }

    @Transactional
    @Auditable(modulo = Modulo.PAGOS, tipo = TipoEvento.MODIFICACION, entidad = "Cargo", entidadId = "#id",
            descripcion = "Corrección de deuda: #{#req.motivo()}")
    public CargoResponse modificar(Long id, CargoRequest req) {
        Cargo c = dao.findById(id).orElseThrow(() -> BusinessException.noEncontrado("Deuda"));
        Textos.verificarVersion(c.getVersion(), req.version());
        if (c.isAnulado()) {
            throw new BusinessException(ErrorCode.REGLA_NEGOCIO, "No se puede modificar una deuda anulada.");
        }
        if (req.motivo() == null || req.motivo().isBlank()) {
            throw BusinessException.validacion("motivo", "Indicá el motivo de la corrección.");
        }
        String periodo = Textos.vacioANull(req.periodo());
        BigDecimal monto = req.monto().setScale(2, RoundingMode.HALF_EVEN);
        if (dao.existeDuplicado(c.getPacienteId(), req.concepto().trim(), periodo, monto, id)) {
            throw new BusinessException(ErrorCode.DEUDA_DUPLICADA, "El paciente ya tiene esta deuda registrada");
        }
        if (c.getMonto().compareTo(monto) != 0) {
            Auditoria.detalle("monto", Map.of("anterior", c.getMonto().toPlainString(), "nuevo", monto.toPlainString()));
        }
        aplicar(c, req, periodo, monto);
        dao.saveAndFlush(c);
        return respuesta(c, null);
    }

    @Transactional
    @Auditable(modulo = Modulo.PAGOS, tipo = TipoEvento.BAJA, entidad = "Cargo", entidadId = "#id",
            descripcion = "Anulación de deuda: #{#motivo}")
    public CargoResponse anular(Long id, String motivo) {
        Cargo c = dao.findById(id).orElseThrow(() -> BusinessException.noEncontrado("Deuda"));
        if (c.isAnulado()) {
            throw new BusinessException(ErrorCode.REGLA_NEGOCIO, "La deuda ya está anulada.");
        }
        c.setAnulado(true);
        c.setMotivoAnulacion(motivo.trim());
        dao.saveAndFlush(c);
        return respuesta(c, null);
    }

    /** CA-07.4.5: genera la cuota de un arancel para los pacientes elegidos, omitiendo duplicados (idempotente). */
    @Transactional
    @Auditable(modulo = Modulo.PAGOS, tipo = TipoEvento.ALTA, entidad = "Cargo",
            descripcion = "Generación masiva de cuotas #{#req.periodo()}")
    public GeneracionMasivaResponse generarMasivo(GeneracionMasivaRequest req) {
        Arancel a = arancelDao.findById(req.arancelId()).filter(Arancel::isActivo)
                .orElseThrow(() -> BusinessException.validacion("arancelId", "Arancel inexistente o inactivo."));
        List<Paciente> pacientes = Boolean.TRUE.equals(req.todos()) || req.pacienteIds() == null || req.pacienteIds().isEmpty()
                ? pacienteDao.findByEstado(Paciente.Estado.ACTIVO)
                : pacienteDao.findAllById(req.pacienteIds()).stream().filter(Paciente::activo).toList();
        LocalDate hoy = LocalDate.now(clock);
        int creados = 0;
        int omitidos = 0;
        for (Paciente p : pacientes) {
            if (dao.existeDuplicado(p.getId(), a.getConcepto(), req.periodo(), a.getMonto(), null)) {
                omitidos++;
                continue;
            }
            Cargo c = new Cargo();
            c.setPacienteId(p.getId());
            c.setConcepto(a.getConcepto());
            c.setPeriodo(req.periodo());
            c.setMonto(a.getMonto());
            c.setFecha(hoy);
            c.setVencimiento(req.vencimiento() == null ? hoy.plusDays(10) : req.vencimiento());
            c.setArancelId(a.getId());
            dao.save(c);
            creados++;
        }
        Auditoria.detalle("creados", creados);
        Auditoria.detalle("omitidosDuplicados", omitidos);
        return new GeneracionMasivaResponse(creados, omitidos);
    }

    /**
     * Encola RECORDATORIO_DEUDA si el saldo es deudor y no recibió uno en los últimos 6 días (CA-11.3.3).
     */
    @Transactional
    public boolean recordarDeuda(Paciente p) {
        BigDecimal saldo = saldoDao.saldo(p.getId());
        if (saldo.signum() <= 0 || notificaciones.recibioRecientemente(p.getUsuarioId(), TipoNotificacion.RECORDATORIO_DEUDA, VENTANA_RECORDATORIO)) {
            return false;
        }
        notificador.recordatorioDeuda(p.getUsuarioId(), p.getNombre(), p.getTelefonoWhatsapp(), saldo, p.getId());
        return true;
    }

    private void aplicar(Cargo c, CargoRequest req, String periodo, BigDecimal monto) {
        LocalDate fecha = req.fecha() == null ? LocalDate.now(clock) : req.fecha();
        LocalDate venc = req.vencimiento() == null ? fecha.plusDays(10) : req.vencimiento();
        if (venc.isBefore(fecha)) {
            throw BusinessException.validacion("vencimiento", "El vencimiento no puede ser anterior a la fecha.");
        }
        if (req.arancelId() != null && !arancelDao.existsById(req.arancelId())) {
            throw BusinessException.validacion("arancelId", "Arancel inexistente.");
        }
        c.setConcepto(req.concepto().trim());
        c.setPeriodo(periodo);
        c.setMonto(monto);
        c.setFecha(fecha);
        c.setVencimiento(venc);
        c.setArancelId(req.arancelId());
    }

    static CargoResponse respuesta(Cargo c, String notificacion) {
        return new CargoResponse(c.getId(), c.getPacienteId(), c.getConcepto(), c.getPeriodo(), c.getMonto(), c.getFecha(),
                c.getVencimiento(), c.getArancelId(), c.isAnulado(), c.getMotivoAnulacion(), c.getVersion(), notificacion);
    }
}
