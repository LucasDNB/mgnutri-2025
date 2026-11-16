package ar.edu.usal.mgnutri.backup.controller;

import ar.edu.usal.mgnutri.backup.domain.BackupRegistro;
import ar.edu.usal.mgnutri.backup.dto.BackupDtos.BackupResponse;
import ar.edu.usal.mgnutri.backup.dto.BackupDtos.ProgramacionDto;
import ar.edu.usal.mgnutri.backup.service.BackupProvider;
import ar.edu.usal.mgnutri.backup.service.BackupService;
import ar.edu.usal.mgnutri.common.PageResponse;
import ar.edu.usal.mgnutri.reportes.service.ReporteExporterFactory;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Backups (spec 15). Sólo ADMIN. */
@RestController
@RequestMapping("/api/v1/backups")
@PreAuthorize("hasRole('ADMIN')")
public class BackupController {

    private final BackupService service;

    public BackupController(BackupService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<BackupResponse> listar(@RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        return service.listar(PageResponse.pageable(page, size, Sort.by(Sort.Direction.DESC, "iniciadoAt")));
    }

    @PostMapping
    public ResponseEntity<BackupResponse> iniciar() {
        return ResponseEntity.accepted().body(service.iniciar(BackupRegistro.Tipo.MANUAL));
    }

    @GetMapping("/{id}")
    public BackupResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    /** Devuelve {url} (enlace SAS temporal, Azure) o el archivo directamente (local). */
    @GetMapping("/{id}/descarga")
    public ResponseEntity<?> descargar(@PathVariable Long id) {
        BackupProvider.Descarga d = service.descargar(id);
        if (d.url() != null) {
            return ResponseEntity.ok(Map.of("url", d.url()));
        }
        return ReporteExporterFactory.archivo(d.contenido(), "application/octet-stream", service.archivo(id));
    }

    @GetMapping("/programacion")
    public ProgramacionDto programacion() {
        return service.programacion();
    }

    @PutMapping("/programacion")
    public ProgramacionDto guardar(@Valid @RequestBody ProgramacionDto req) {
        return service.guardarProgramacion(req);
    }
}
