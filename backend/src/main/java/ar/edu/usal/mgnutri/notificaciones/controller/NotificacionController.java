package ar.edu.usal.mgnutri.notificaciones.controller;

import ar.edu.usal.mgnutri.common.PageResponse;
import ar.edu.usal.mgnutri.notificaciones.dto.NotificacionDtos.AlertaResponse;
import ar.edu.usal.mgnutri.notificaciones.dto.NotificacionDtos.NotificacionResponse;
import ar.edu.usal.mgnutri.notificaciones.dto.NotificacionDtos.ResultadoJob;
import ar.edu.usal.mgnutri.notificaciones.dto.NotificacionDtos.ResumenNotificaciones;
import ar.edu.usal.mgnutri.notificaciones.service.AlertaService;
import ar.edu.usal.mgnutri.notificaciones.service.NotificacionService;
import ar.edu.usal.mgnutri.notificaciones.service.RecordatoriosService;
import ar.edu.usal.mgnutri.common.error.BusinessException;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Panel de notificaciones (US-11.6), alertas internas y ejecución manual de jobs. */
@RestController
@RequestMapping("/api/v1")
public class NotificacionController {

    private final NotificacionService service;
    private final AlertaService alertas;
    private final RecordatoriosService recordatorios;

    public NotificacionController(NotificacionService service, AlertaService alertas, RecordatoriosService recordatorios) {
        this.service = service;
        this.alertas = alertas;
        this.recordatorios = recordatorios;
    }

    @GetMapping("/notificaciones")
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<NotificacionResponse> listar(@RequestParam(required = false) String tipo,
                                                     @RequestParam(required = false) String estado,
                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                                     @RequestParam(required = false) Integer page,
                                                     @RequestParam(required = false) Integer size) {
        return service.buscar(tipo, estado, desde, hasta, PageResponse.pageable(page, size, Sort.by(Sort.Direction.DESC, "id")));
    }

    @GetMapping("/notificaciones/resumen")
    @PreAuthorize("hasRole('ADMIN')")
    public ResumenNotificaciones resumen() {
        return service.resumen();
    }

    @GetMapping("/notificaciones/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public NotificacionResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping("/notificaciones/{id}/reintentar")
    @PreAuthorize("hasRole('ADMIN')")
    public NotificacionResponse reintentar(@PathVariable Long id) {
        return service.reintentar(id);
    }

    @PostMapping("/admin/jobs/{job}/ejecutar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResultadoJob ejecutarJob(@PathVariable String job) {
        return switch (job) {
            case "recordatorio-turnos" -> new ResultadoJob(job, recordatorios.recordarTurnosDeManiana());
            case "recordatorio-deuda" -> new ResultadoJob(job, recordatorios.recordarDeudas());
            case "despacho" -> new ResultadoJob(job, service.despacharPendientes());
            default -> throw BusinessException.noEncontrado("Proceso");
        };
    }

    @GetMapping("/alertas")
    public List<AlertaResponse> alertas(@RequestParam(defaultValue = "false") boolean leidas) {
        return alertas.mias(!leidas);
    }

    @PostMapping("/alertas/{id}/leer")
    public ResponseEntity<Void> leer(@PathVariable Long id) {
        alertas.marcarLeida(id);
        return ResponseEntity.noContent().build();
    }
}
