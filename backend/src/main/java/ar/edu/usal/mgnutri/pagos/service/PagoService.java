package ar.edu.usal.mgnutri.pagos.service;

import ar.edu.usal.mgnutri.bitacora.domain.Modulo;
import ar.edu.usal.mgnutri.bitacora.domain.TipoEvento;
import ar.edu.usal.mgnutri.bitacora.service.Auditable;
import ar.edu.usal.mgnutri.bitacora.service.Auditoria;
import ar.edu.usal.mgnutri.common.Fechas;
import ar.edu.usal.mgnutri.common.Textos;
import ar.edu.usal.mgnutri.common.error.BusinessException;
import ar.edu.usal.mgnutri.common.error.ErrorCode;
import ar.edu.usal.mgnutri.configuracion.service.ConfiguracionService;
import ar.edu.usal.mgnutri.notificaciones.service.Notificador;
import ar.edu.usal.mgnutri.pacientes.dao.PacienteDao;
import ar.edu.usal.mgnutri.pacientes.domain.Paciente;
import ar.edu.usal.mgnutri.pagos.dao.PagoDao;
import ar.edu.usal.mgnutri.pagos.dao.SaldoDao;
import ar.edu.usal.mgnutri.pagos.domain.Pago;
import ar.edu.usal.mgnutri.pagos.dto.PagoDtos.PagoRequest;
import ar.edu.usal.mgnutri.pagos.dto.PagoDtos.PagoResponse;
import ar.edu.usal.mgnutri.reportes.service.PdfLayout;
import com.lowagie.text.Element;
import com.lowagie.text.pdf.PdfPTable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Registro, corrección y anulación de pagos (CU-44, CU-46, SDP 7.1.2/7.1.3). */
@Service
public class PagoService {

    private final PagoDao dao;
    private final PacienteDao pacienteDao;
    private final SaldoDao saldoDao;
    private final ConfiguracionService config;
    private final Clock clock;

    public PagoService(PagoDao dao, PacienteDao pacienteDao, SaldoDao saldoDao, ConfiguracionService config, Clock clock) {
        this.dao = dao;
        this.pacienteDao = pacienteDao;
        this.saldoDao = saldoDao;
        this.config = config;
        this.clock = clock;
    }

    @Transactional
    @Auditable(modulo = Modulo.PAGOS, tipo = TipoEvento.ALTA, entidad = "Pago", entidadId = "#result.id()",
            descripcion = "Pago de #{#req.monto()} (comprobante #{#result.nroComprobante()})")
    public PagoResponse registrar(Long pacienteId, PagoRequest req) {
        Paciente p = pacienteDao.findById(pacienteId).orElseThrow(() -> BusinessException.noEncontrado("Paciente"));
        LocalDate fecha = req.fecha() == null ? LocalDate.now(clock) : req.fecha();
        validarFecha(fecha);
        Pago pago = new Pago();
        pago.setPacienteId(pacienteId);
        aplicar(pago, req, fecha);
        pago.setNroComprobante(dao.siguienteComprobante());
        dao.saveAndFlush(pago);
        BigDecimal saldo = saldoDao.saldo(pacienteId);
        String mensaje = saldo.signum() < 0 ? "El paciente queda con saldo a favor de " + Notificador.moneda(saldo.negate()) : null;
        return respuesta(pago, p, saldo, mensaje);
    }

    @Transactional
    @Auditable(modulo = Modulo.PAGOS, tipo = TipoEvento.MODIFICACION, entidad = "Pago", entidadId = "#id",
            descripcion = "Corrección de pago: #{#req.motivo()}")
    public PagoResponse modificar(Long id, PagoRequest req) {
        Pago pago = dao.findById(id).orElseThrow(() -> BusinessException.noEncontrado("Pago"));
        Textos.verificarVersion(pago.getVersion(), req.version());
        if (pago.isAnulado()) {
            throw new BusinessException(ErrorCode.REGLA_NEGOCIO, "No se puede modificar un pago anulado.");
        }
        if (req.motivo() == null || req.motivo().isBlank()) {
            throw BusinessException.validacion("motivo", "Indicá el motivo de la corrección.");
        }
        LocalDate fecha = req.fecha() == null ? pago.getFecha() : req.fecha();
        validarFecha(fecha);
        if (pago.getMonto().compareTo(req.monto()) != 0) {
            Auditoria.detalle("monto", Map.of("anterior", pago.getMonto().toPlainString(), "nuevo", req.monto().toPlainString()));
        }
        if (!Objects.equals(pago.getFecha(), fecha)) {
            Auditoria.detalle("fecha", Map.of("anterior", pago.getFecha().toString(), "nuevo", fecha.toString()));
        }
        aplicar(pago, req, fecha);
        dao.saveAndFlush(pago);
        Paciente p = pacienteDao.findById(pago.getPacienteId()).orElseThrow();
        return respuesta(pago, p, saldoDao.saldo(p.getId()), null);
    }

    @Transactional
    @Auditable(modulo = Modulo.PAGOS, tipo = TipoEvento.BAJA, entidad = "Pago", entidadId = "#id",
            descripcion = "Anulación de pago: #{#motivo}")
    public PagoResponse anular(Long id, String motivo) {
        Pago pago = dao.findById(id).orElseThrow(() -> BusinessException.noEncontrado("Pago"));
        if (pago.isAnulado()) {
            throw new BusinessException(ErrorCode.REGLA_NEGOCIO, "El pago ya está anulado.");
        }
        pago.setAnulado(true);
        pago.setMotivoAnulacion(motivo.trim());
        dao.saveAndFlush(pago);
        Paciente p = pacienteDao.findById(pago.getPacienteId()).orElseThrow();
        return respuesta(pago, p, saldoDao.saldo(p.getId()), null);
    }

    @Transactional(readOnly = true)
    public List<PagoResponse> historial(Long pacienteId, LocalDate desde, LocalDate hasta, String estado) {
        Paciente p = pacienteDao.findById(pacienteId).orElseThrow(() -> BusinessException.noEncontrado("Paciente"));
        return dao.findByPacienteIdOrderByFechaAscIdAsc(pacienteId).stream()
                .filter(x -> desde == null || !x.getFecha().isBefore(desde))
                .filter(x -> hasta == null || !x.getFecha().isAfter(hasta))
                .filter(x -> estado == null || estado.isBlank() || ("ANULADO".equals(estado) == x.isAnulado()))
                .sorted((a, b) -> b.getFecha().compareTo(a.getFecha()))
                .map(x -> respuesta(x, p, null, null)).toList();
    }

    /** Comprobante interno de pago (requerimiento especial CU-44). */
    @Transactional(readOnly = true)
    @Auditable(modulo = Modulo.PAGOS, tipo = TipoEvento.DESCARGA, entidad = "Pago", entidadId = "#id",
            descripcion = "Descarga de comprobante de pago")
    public byte[] comprobante(Long id) {
        Pago pago = dao.findById(id).orElseThrow(() -> BusinessException.noEncontrado("Pago"));
        Paciente p = pacienteDao.findById(pago.getPacienteId()).orElseThrow();
        String centro = config.valor("centro.nombre", "MG Nutrición y Entrenamiento");
        return PdfLayout.documento(false, centro, "Comprobante de pago N.º " + String.format("%08d", pago.getNroComprobante()), null, doc -> {
            PdfLayout.agregar(doc, PdfLayout.parrafo("Comprobante de pago N.º " + String.format("%08d", pago.getNroComprobante())
                    + (pago.isAnulado() ? "  — ANULADO" : ""), PdfLayout.TITULO));
            PdfLayout.agregar(doc, PdfLayout.parrafo(config.valor("centro.direccion", "") + " · " + config.valor("centro.telefono_whatsapp", ""), PdfLayout.CHICA));
            List<List<Object>> filas = new ArrayList<>();
            filas.add(new ArrayList<>(List.of("Paciente", p.nombreCompleto())));
            filas.add(new ArrayList<>(List.of("Documento", p.getTipoDocumento() + " " + p.getNroDocumento())));
            filas.add(new ArrayList<>(List.of("Fecha de pago", Fechas.fecha(pago.getFecha()))));
            filas.add(new ArrayList<>(List.of("Importe", Notificador.moneda(pago.getMonto()))));
            filas.add(new ArrayList<>(List.of("Medio de pago", pago.getMedio().etiqueta())));
            if (pago.getReferencia() != null) {
                filas.add(new ArrayList<>(List.of("Referencia", pago.getReferencia())));
            }
            if (pago.getObservaciones() != null) {
                filas.add(new ArrayList<>(List.of("Observaciones", pago.getObservaciones())));
            }
            PdfPTable t = PdfLayout.tabla(List.of("Concepto", "Detalle"), filas);
            t.setWidthPercentage(80);
            t.setHorizontalAlignment(Element.ALIGN_LEFT);
            PdfLayout.agregar(doc, t);
            PdfLayout.agregar(doc, PdfLayout.parrafo("Documento no válido como factura. Comprobante interno de la cuenta corriente del paciente.",
                    PdfLayout.CHICA));
        });
    }

    private void validarFecha(LocalDate fecha) {
        if (fecha.isAfter(LocalDate.now(clock))) {
            throw BusinessException.validacion("fecha", "La fecha del pago no puede ser futura.");
        }
    }

    private void aplicar(Pago pago, PagoRequest req, LocalDate fecha) {
        pago.setMonto(req.monto().setScale(2, RoundingMode.HALF_EVEN));
        pago.setFecha(fecha);
        pago.setMedio(req.medio());
        pago.setReferencia(Textos.vacioANull(req.referencia()));
        pago.setObservaciones(Textos.vacioANull(req.observaciones()));
    }

    static PagoResponse respuesta(Pago x, Paciente p, BigDecimal saldo, String mensaje) {
        return new PagoResponse(x.getId(), x.getPacienteId(), p.nombreCompleto(), x.getFecha(), x.getMonto(), x.getMedio().name(),
                x.getReferencia(), x.getObservaciones(), x.getNroComprobante(), x.isAnulado(), x.getMotivoAnulacion(),
                x.getVersion(), saldo, mensaje);
    }
}
