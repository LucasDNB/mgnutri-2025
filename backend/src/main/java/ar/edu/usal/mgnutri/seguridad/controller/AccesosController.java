package ar.edu.usal.mgnutri.seguridad.controller;

import ar.edu.usal.mgnutri.bitacora.domain.Modulo;
import ar.edu.usal.mgnutri.bitacora.domain.TipoEvento;
import ar.edu.usal.mgnutri.bitacora.service.BitacoraService;
import ar.edu.usal.mgnutri.common.PageResponse;
import ar.edu.usal.mgnutri.reportes.dto.ReporteDto;
import ar.edu.usal.mgnutri.reportes.service.ReporteExporterFactory;
import ar.edu.usal.mgnutri.seguridad.dto.SeguridadDtos.AccesoResponse;
import ar.edu.usal.mgnutri.seguridad.service.RegistroAccesoService;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** Consulta del registro de accesos (RF-16, CA-13.7.3). Sólo lectura. */
@RestController
@RequestMapping("/api/v1/accesos")
@PreAuthorize("hasRole('ADMIN')")
public class AccesosController {

    private final RegistroAccesoService service;
    private final ReporteExporterFactory exportadores;
    private final BitacoraService bitacora;

    public AccesosController(RegistroAccesoService service, ReporteExporterFactory exportadores, BitacoraService bitacora) {
        this.service = service;
        this.exportadores = exportadores;
        this.bitacora = bitacora;
    }

    @GetMapping
    public PageResponse<AccesoResponse> listar(@RequestParam(required = false) String usuario,
                                               @RequestParam(required = false) String accion,
                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                               @RequestParam(required = false) String ip,
                                               @RequestParam(required = false) Integer page,
                                               @RequestParam(required = false) Integer size) {
        return service.buscar(usuario, accion, desde, hasta, ip,
                PageResponse.pageable(page, size, Sort.by(Sort.Direction.DESC, "fechaHora")));
    }

    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportar(@RequestParam(required = false) String usuario,
                                           @RequestParam(required = false) String accion,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                           @RequestParam(required = false) String ip) {
        ReporteDto r = ReporteDto.nuevo("accesos", "Registro de accesos");
        r.tabla().con("Fecha y hora", "Usuario", "Roles", "Acción", "IP", "Navegador", "Detalle");
        for (AccesoResponse a : service.exportar(usuario, accion, desde, hasta, ip)) {
            r.tabla().fila(a.fechaHora(), a.usuario(), a.roles(), a.accion(), a.ip(), a.userAgent(), a.detalle());
        }
        bitacora.ok(Modulo.SEGURIDAD, TipoEvento.REPORTE, "RegistroAcceso", null, "Exportación del registro de accesos");
        return exportadores.descargar(r, "xlsx", "accesos");
    }
}
