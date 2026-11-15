package ar.edu.usal.mgnutri.bitacora.controller;

import ar.edu.usal.mgnutri.bitacora.domain.BitacoraEvento;
import ar.edu.usal.mgnutri.bitacora.domain.Modulo;
import ar.edu.usal.mgnutri.bitacora.domain.TipoEvento;
import ar.edu.usal.mgnutri.bitacora.dto.BitacoraDtos.EventoResponse;
import ar.edu.usal.mgnutri.bitacora.dto.BitacoraDtos.Filtro;
import ar.edu.usal.mgnutri.bitacora.service.BitacoraService;
import ar.edu.usal.mgnutri.common.PageResponse;
import ar.edu.usal.mgnutri.common.error.BusinessException;
import ar.edu.usal.mgnutri.reportes.dto.ReporteDto;
import ar.edu.usal.mgnutri.reportes.service.ReporteExporterFactory;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** Consulta de bitácora (CU-36). No existen operaciones de modificación ni eliminación. */
@RestController
@RequestMapping("/api/v1/bitacora")
@PreAuthorize("hasRole('ADMIN')")
public class BitacoraController {

    private final BitacoraService service;
    private final ReporteExporterFactory exportadores;

    public BitacoraController(BitacoraService service, ReporteExporterFactory exportadores) {
        this.service = service;
        this.exportadores = exportadores;
    }

    @GetMapping
    public PageResponse<EventoResponse> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long usuarioId, @RequestParam(required = false) String usuario,
            @RequestParam(required = false) Modulo modulo, @RequestParam(required = false) TipoEvento tipo,
            @RequestParam(required = false) String entidad, @RequestParam(required = false) String entidadId,
            @RequestParam(required = false) BitacoraEvento.Resultado resultado,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        return service.buscar(new Filtro(desde, hasta, usuarioId, usuario, modulo, tipo, entidad, entidadId, resultado),
                PageResponse.pageable(page, size, Sort.by(Sort.Direction.DESC, "fechaHora").and(Sort.by(Sort.Direction.DESC, "id"))));
    }

    @GetMapping("/{id}")
    public EventoResponse obtener(@PathVariable Long id) {
        return service.obtener(id).orElseThrow(() -> BusinessException.noEncontrado("Evento"));
    }

    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long usuarioId, @RequestParam(required = false) String usuario,
            @RequestParam(required = false) Modulo modulo, @RequestParam(required = false) TipoEvento tipo,
            @RequestParam(required = false) String entidad, @RequestParam(required = false) String entidadId,
            @RequestParam(required = false) BitacoraEvento.Resultado resultado) {
        ReporteDto r = ReporteDto.nuevo("bitacora", "Bitácora del sistema");
        r.tabla().con("Fecha y hora", "Usuario", "Roles", "Módulo", "Tipo", "Entidad", "Id", "Descripción", "Resultado", "IP");
        for (EventoResponse e : service.exportar(new Filtro(desde, hasta, usuarioId, usuario, modulo, tipo, entidad, entidadId, resultado))) {
            r.tabla().fila(e.fechaHora(), e.username(), e.roles(), e.modulo().name(), e.tipoEvento().name(), e.entidad(),
                    e.entidadId(), e.descripcion(), e.resultado().name(), e.ip());
        }
        service.ok(Modulo.REPORTES, TipoEvento.REPORTE, "BitacoraEvento", null, "Exportación de bitácora a Excel");
        return exportadores.descargar(r, "xlsx", "bitacora");
    }
}
