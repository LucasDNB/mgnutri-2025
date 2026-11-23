package ar.edu.usal.mgnutri.pacientes.controller;

import ar.edu.usal.mgnutri.common.PageResponse;
import ar.edu.usal.mgnutri.pacientes.dto.PacienteDtos.AltaResponse;
import ar.edu.usal.mgnutri.pacientes.dto.PacienteDtos.AsignacionRequest;
import ar.edu.usal.mgnutri.pacientes.dto.PacienteDtos.AsignacionResponse;
import ar.edu.usal.mgnutri.pacientes.dto.PacienteDtos.BajaRequest;
import ar.edu.usal.mgnutri.pacientes.dto.PacienteDtos.CredencialesResponse;
import ar.edu.usal.mgnutri.pacientes.dto.PacienteDtos.PacienteListItem;
import ar.edu.usal.mgnutri.pacientes.dto.PacienteDtos.PacienteRequest;
import ar.edu.usal.mgnutri.pacientes.dto.PacienteDtos.PacienteResponse;
import ar.edu.usal.mgnutri.pacientes.dto.PacienteDtos.PerfilRequest;
import ar.edu.usal.mgnutri.pacientes.service.AsignacionService;
import ar.edu.usal.mgnutri.pacientes.service.PacienteService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/** Pacientes (spec 01). */
@RestController
@RequestMapping("/api/v1")
public class PacienteController {

    private final PacienteService service;
    private final AsignacionService asignaciones;

    public PacienteController(PacienteService service, AsignacionService asignaciones) {
        this.service = service;
        this.asignaciones = asignaciones;
    }

    @GetMapping("/pacientes")
    @PreAuthorize("hasAnyRole('ADMIN','NUTRICIONISTA','ENTRENADOR')")
    public PageResponse<PacienteListItem> listar(@RequestParam(required = false) String q,
                                                 @RequestParam(required = false) Long profesionalId,
                                                 @RequestParam(defaultValue = "false") boolean incluirBajas,
                                                 @RequestParam(required = false) Integer page,
                                                 @RequestParam(required = false) Integer size) {
        return service.buscar(q, profesionalId, incluirBajas, PageResponse.pageable(page, size, Sort.by("apellido", "nombre")));
    }

    @GetMapping("/pacientes/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','NUTRICIONISTA','ENTRENADOR')")
    public PacienteResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping("/pacientes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AltaResponse> crear(@Valid @RequestBody PacienteRequest req) {
        AltaResponse r = service.crear(req);
        return ResponseEntity.created(URI.create("/api/v1/pacientes/" + r.id())).body(r);
    }

    @PutMapping("/pacientes/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public PacienteResponse modificar(@PathVariable Long id, @Valid @RequestBody PacienteRequest req) {
        return service.modificar(id, req);
    }

    @DeleteMapping("/pacientes/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> baja(@PathVariable Long id, @RequestBody(required = false) BajaRequest req) {
        service.darDeBaja(id, req);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/pacientes/{id}/baja")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> bajaPost(@PathVariable Long id, @RequestBody(required = false) BajaRequest req) {
        service.darDeBaja(id, req);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/pacientes/{id}/reactivar")
    @PreAuthorize("hasRole('ADMIN')")
    public CredencialesResponse reactivar(@PathVariable Long id) {
        return service.reactivar(id);
    }

    @PostMapping("/pacientes/{id}/reenviar-credenciales")
    @PreAuthorize("hasRole('ADMIN')")
    public CredencialesResponse reenviar(@PathVariable Long id) {
        return service.reenviarCredenciales(id);
    }

    @GetMapping("/pacientes/{id}/profesionales")
    @PreAuthorize("hasAnyRole('ADMIN','NUTRICIONISTA','ENTRENADOR')")
    public List<AsignacionResponse> asignaciones(@PathVariable Long id) {
        return asignaciones.listar(id);
    }

    @PostMapping("/pacientes/{id}/profesionales")
    @PreAuthorize("hasRole('ADMIN')")
    public List<AsignacionResponse> asignar(@PathVariable Long id, @Valid @RequestBody AsignacionRequest req) {
        return asignaciones.asignar(id, req.asignaciones());
    }

    @DeleteMapping("/pacientes/{id}/profesionales/{asignacionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> desasignar(@PathVariable Long id, @PathVariable Long asignacionId) {
        asignaciones.desasignar(id, asignacionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/perfil")
    @PreAuthorize("hasRole('PACIENTE')")
    public PacienteResponse miPerfil() {
        return service.miPerfil();
    }

    @PatchMapping("/me/perfil")
    @PreAuthorize("hasRole('PACIENTE')")
    public PacienteResponse actualizarPerfil(@Valid @RequestBody PerfilRequest req) {
        return service.actualizarPerfil(req);
    }
}
