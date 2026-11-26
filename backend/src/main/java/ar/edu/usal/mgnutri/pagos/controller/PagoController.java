package ar.edu.usal.mgnutri.pagos.controller;

import ar.edu.usal.mgnutri.bitacora.domain.Modulo;
import ar.edu.usal.mgnutri.bitacora.domain.TipoEvento;
import ar.edu.usal.mgnutri.bitacora.service.BitacoraService;
import ar.edu.usal.mgnutri.pagos.dto.PagoDtos.AnularRequest;
import ar.edu.usal.mgnutri.pagos.dto.PagoDtos.ArancelRequest;
import ar.edu.usal.mgnutri.pagos.dto.PagoDtos.ArancelResponse;
import ar.edu.usal.mgnutri.pagos.dto.PagoDtos.CargoRequest;
import ar.edu.usal.mgnutri.pagos.dto.PagoDtos.CargoResponse;
import ar.edu.usal.mgnutri.pagos.dto.PagoDtos.CuentaResponse;
import ar.edu.usal.mgnutri.pagos.dto.PagoDtos.DeudorResponse;
import ar.edu.usal.mgnutri.pagos.dto.PagoDtos.GeneracionMasivaRequest;
import ar.edu.usal.mgnutri.pagos.dto.PagoDtos.GeneracionMasivaResponse;
import ar.edu.usal.mgnutri.pagos.dto.PagoDtos.PagoRequest;
import ar.edu.usal.mgnutri.pagos.dto.PagoDtos.PagoResponse;
import ar.edu.usal.mgnutri.pagos.service.ArancelService;
import ar.edu.usal.mgnutri.pagos.service.CargoService;
import ar.edu.usal.mgnutri.pagos.service.CuentaCorrienteService;
import ar.edu.usal.mgnutri.pagos.service.PagoService;
import ar.edu.usal.mgnutri.reportes.dto.ReporteDto;
import ar.edu.usal.mgnutri.reportes.service.ReporteExporterFactory;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import ar.edu.usal.mgnutri.notificaciones.service.RecordatoriosService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

/** Pagos, deudas, deudores y aranceles (spec 07). Gestión exclusiva del administrador (RN-07.1). */
@RestController
@RequestMapping("/api/v1")
public class PagoController {

    private final CuentaCorrienteService cuentas;
    private final PagoService pagos;
    private final CargoService cargos;
    private final ArancelService aranceles;
    private final ReporteExporterFactory exportadores;
    private final BitacoraService bitacora;
    private final RecordatoriosService recordatorios;

    public PagoController(CuentaCorrienteService cuentas, PagoService pagos, CargoService cargos, ArancelService aranceles,
                          ReporteExporterFactory exportadores, BitacoraService bitacora, RecordatoriosService recordatorios) {
        this.cuentas = cuentas;
        this.pagos = pagos;
        this.cargos = cargos;
        this.aranceles = aranceles;
        this.exportadores = exportadores;
        this.bitacora = bitacora;
        this.recordatorios = recordatorios;
    }

    @GetMapping("/pacientes/{pid}/cuenta")
    @PreAuthorize("hasRole('ADMIN')")
    public CuentaResponse cuenta(@PathVariable Long pid,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return cuentas.cuenta(pid, desde, hasta);
    }

    @GetMapping("/me/cuenta")
    @PreAuthorize("hasRole('PACIENTE')")
    public CuentaResponse miCuenta() {
        return cuentas.miCuenta();
    }

    @PostMapping("/pacientes/{pid}/pagos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagoResponse> registrarPago(@PathVariable Long pid, @Valid @RequestBody PagoRequest req) {
        PagoResponse r = pagos.registrar(pid, req);
        return ResponseEntity.created(URI.create("/api/v1/pagos/" + r.id())).body(r);
    }

    @GetMapping("/pacientes/{pid}/pagos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> historial(@PathVariable Long pid,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                       @RequestParam(required = false) String estado,
                                       @RequestParam(defaultValue = "json") String formato) {
        List<PagoResponse> lista = pagos.historial(pid, desde, hasta, estado);
        if ("json".equalsIgnoreCase(formato)) {
            return ResponseEntity.ok(lista);
        }
        ReporteDto r = ReporteDto.nuevo("historial-pagos", "Historial de pagos" + (lista.isEmpty() ? "" : " · " + lista.get(0).paciente()));
        if (desde != null) {
            r.filtros().put("Desde", desde.toString());
        }
        if (hasta != null) {
            r.filtros().put("Hasta", hasta.toString());
        }
        r.tabla().con("Fecha", "Comprobante", "Medio", "Monto", "Referencia", "Estado");
        lista.forEach(p -> r.tabla().fila(p.fecha(), p.nroComprobante(), p.medio(), p.monto(), p.referencia(),
                p.anulado() ? "Anulado: " + p.motivoAnulacion() : "Vigente"));
        bitacora.ok(Modulo.PAGOS, TipoEvento.DESCARGA, "Pago", pid, "Exportación de historial de pagos (" + formato + ")");
        return exportadores.descargar(r, formato, "historial-pagos-" + pid);
    }

    @PutMapping("/pagos/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public PagoResponse modificarPago(@PathVariable Long id, @Valid @RequestBody PagoRequest req) {
        return pagos.modificar(id, req);
    }

    @PostMapping("/pagos/{id}/anular")
    @PreAuthorize("hasRole('ADMIN')")
    public PagoResponse anularPago(@PathVariable Long id, @Valid @RequestBody AnularRequest req) {
        return pagos.anular(id, req.motivo());
    }

    @GetMapping("/pagos/{id}/comprobante.pdf")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> comprobante(@PathVariable Long id) {
        return ReporteExporterFactory.archivo(pagos.comprobante(id), "application/pdf", "comprobante-" + id + ".pdf");
    }

    @PostMapping("/pacientes/{pid}/cargos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CargoResponse> registrarCargo(@PathVariable Long pid, @Valid @RequestBody CargoRequest req) {
        CargoResponse r = cargos.registrar(pid, req);
        return ResponseEntity.created(URI.create("/api/v1/cargos/" + r.id())).body(r);
    }

    @PutMapping("/cargos/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CargoResponse modificarCargo(@PathVariable Long id, @Valid @RequestBody CargoRequest req) {
        return cargos.modificar(id, req);
    }

    @PostMapping("/cargos/{id}/anular")
    @PreAuthorize("hasRole('ADMIN')")
    public CargoResponse anularCargo(@PathVariable Long id, @Valid @RequestBody AnularRequest req) {
        return cargos.anular(id, req.motivo());
    }

    @PostMapping("/cargos/generacion-masiva")
    @PreAuthorize("hasRole('ADMIN')")
    public GeneracionMasivaResponse generacionMasiva(@Valid @RequestBody GeneracionMasivaRequest req) {
        return cargos.generarMasivo(req);
    }

    @GetMapping("/deudores")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deudores(@RequestParam(defaultValue = "monto") String orden,
                                      @RequestParam(defaultValue = "0") long diasMinimos,
                                      @RequestParam(defaultValue = "json") String formato) {
        List<DeudorResponse> lista = cuentas.deudores(orden, diasMinimos);
        if ("json".equalsIgnoreCase(formato)) {
            return ResponseEntity.ok(lista);
        }
        ReporteDto r = ReporteDto.nuevo("deudores", "Listado de deudores");
        r.filtros().put("Orden", orden);
        if (diasMinimos > 0) {
            r.filtros().put("Días de atraso (mínimo)", String.valueOf(diasMinimos));
        }
        r.tabla().con("Paciente", "Documento", "Teléfono", "Saldo", "Fecha de deuda", "Días de atraso");
        lista.forEach(d -> r.tabla().fila(d.paciente(), d.documento(), d.telefono(), d.saldo(), d.fechaDeuda(), d.diasAtraso()));
        r.resumen().put("Pacientes deudores", lista.size());
        r.resumen().put("Total adeudado", lista.stream().map(DeudorResponse::saldo).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
        bitacora.ok(Modulo.REPORTES, TipoEvento.REPORTE, "Deudores", null, "Listado de deudores (" + formato + ")");
        return exportadores.descargar(r, formato, "deudores");
    }

    /** Envía el recordatorio de deuda a los deudores con al menos {@code diasMinimos} días de atraso (D-40). */
    @PostMapping("/deudores/recordatorios")
    @PreAuthorize("hasRole('ADMIN')")
    public RecordatoriosService.EnvioDeudas recordarDeudores(@RequestParam(defaultValue = "0") long diasMinimos) {
        return recordatorios.recordarDeudas(diasMinimos);
    }

    @GetMapping("/aranceles")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ArancelResponse> aranceles() {
        return aranceles.listar();
    }

    @PostMapping("/aranceles")
    @PreAuthorize("hasRole('ADMIN')")
    public ArancelResponse crearArancel(@Valid @RequestBody ArancelRequest req) {
        return aranceles.crear(req);
    }

    @PutMapping("/aranceles/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ArancelResponse modificarArancel(@PathVariable Long id, @Valid @RequestBody ArancelRequest req) {
        return aranceles.modificar(id, req);
    }
}
