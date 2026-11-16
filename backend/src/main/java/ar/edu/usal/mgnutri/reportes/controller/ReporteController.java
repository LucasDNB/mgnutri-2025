package ar.edu.usal.mgnutri.reportes.controller;

import ar.edu.usal.mgnutri.bitacora.domain.Modulo;
import ar.edu.usal.mgnutri.bitacora.domain.TipoEvento;
import ar.edu.usal.mgnutri.bitacora.service.BitacoraService;
import ar.edu.usal.mgnutri.common.Area;
import ar.edu.usal.mgnutri.reportes.dto.ReporteDto;
import ar.edu.usal.mgnutri.reportes.service.ReporteExporterFactory;
import ar.edu.usal.mgnutri.reportes.service.ReporteService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;
import java.util.function.Supplier;

/** Reportes (spec 12): formato=json|pdf|xlsx. Cada generación queda en bitácora (CA-12.0.8). */
@RestController
@RequestMapping("/api/v1")
public class ReporteController {

    private final ReporteService service;
    private final ReporteExporterFactory exportadores;
    private final BitacoraService bitacora;

    public ReporteController(ReporteService service, ReporteExporterFactory exportadores, BitacoraService bitacora) {
        this.service = service;
        this.exportadores = exportadores;
        this.bitacora = bitacora;
    }

    private ResponseEntity<?> responder(String tipo, String formato, Supplier<ReporteDto> generar) {
        ReporteDto r = generar.get();
        boolean json = formato == null || "json".equalsIgnoreCase(formato);
        bitacora.ok(Modulo.REPORTES, json ? TipoEvento.REPORTE : TipoEvento.DESCARGA, "Reporte", tipo,
                "Reporte de " + tipo + (json ? "" : " (" + formato + ")"), Map.of("filtros", r.filtros()));
        if (json) {
            return ResponseEntity.ok(Map.of("tipo", r.tipo(), "titulo", r.titulo(), "filtros", r.filtros(), "resumen", r.resumen(),
                    "tabla", r.tabla(), "secciones", r.secciones(), "graficos", r.graficos(), "vacio", r.vacio()));
        }
        return exportadores.descargar(r, formato, "reporte-" + tipo + "-" + LocalDate.now());
    }

    @GetMapping("/reportes/turnos")
    @PreAuthorize("hasAnyRole('ADMIN','NUTRICIONISTA','ENTRENADOR')")
    public ResponseEntity<?> turnos(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                    @RequestParam(required = false) Long profesionalId, @RequestParam(required = false) Long pacienteId,
                                    @RequestParam(required = false) Area area, @RequestParam(required = false) String estado,
                                    @RequestParam(defaultValue = "json") String formato) {
        return responder("turnos", formato, () -> service.turnos(desde, hasta, profesionalId, pacienteId, area, estado));
    }

    @GetMapping("/reportes/pagos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> pagos(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                   @RequestParam(required = false) Long pacienteId, @RequestParam(required = false) String estado,
                                   @RequestParam(required = false) String medio, @RequestParam(defaultValue = "json") String formato) {
        return responder("pagos", formato, () -> service.pagos(desde, hasta, pacienteId, estado, medio));
    }

    @GetMapping("/reportes/evolucion/{pacienteId}")
    @PreAuthorize("hasAnyRole('ADMIN','NUTRICIONISTA','ENTRENADOR')")
    public ResponseEntity<?> evolucion(@PathVariable Long pacienteId,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                       @RequestParam(defaultValue = "json") String formato) {
        return responder("evolucion", formato, () -> service.evolucion(pacienteId, desde, hasta));
    }

    @GetMapping("/me/reportes/evolucion")
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<?> miEvolucion(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                         @RequestParam(defaultValue = "pdf") String formato) {
        return responder("evolucion", formato, () -> service.miEvolucion(desde, hasta));
    }

    @GetMapping("/reportes/rutinas")
    @PreAuthorize("hasAnyRole('ADMIN','ENTRENADOR')")
    public ResponseEntity<?> rutinas(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                     @RequestParam(required = false) Long pacienteId, @RequestParam(required = false) Long entrenadorId,
                                     @RequestParam(required = false) String estado, @RequestParam(defaultValue = "MES") String agrupar,
                                     @RequestParam(defaultValue = "json") String formato) {
        return responder("rutinas", formato, () -> service.rutinas(desde, hasta, pacienteId, entrenadorId, estado, agrupar));
    }

    @GetMapping("/reportes/planes")
    @PreAuthorize("hasAnyRole('ADMIN','NUTRICIONISTA')")
    public ResponseEntity<?> planes(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                    @RequestParam(required = false) Long pacienteId, @RequestParam(required = false) Long nutricionistaId,
                                    @RequestParam(required = false) String estado, @RequestParam(required = false) String modo,
                                    @RequestParam(defaultValue = "json") String formato) {
        return responder("planes", formato, () -> service.planes(desde, hasta, pacienteId, nutricionistaId, estado, modo));
    }
}
