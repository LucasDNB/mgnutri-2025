package ar.edu.usal.mgnutri.profesionales.controller;

import ar.edu.usal.mgnutri.common.Area;
import ar.edu.usal.mgnutri.common.PageResponse;
import ar.edu.usal.mgnutri.pacientes.dto.PacienteDtos.AltaResponse;
import ar.edu.usal.mgnutri.pacientes.dto.PacienteDtos.BajaRequest;
import ar.edu.usal.mgnutri.pacientes.dto.PacienteDtos.CredencialesResponse;
import ar.edu.usal.mgnutri.profesionales.domain.Profesional;
import ar.edu.usal.mgnutri.profesionales.dto.ProfesionalDtos.BloqueoRequest;
import ar.edu.usal.mgnutri.profesionales.dto.ProfesionalDtos.BloqueoResponse;
import ar.edu.usal.mgnutri.profesionales.dto.ProfesionalDtos.FranjaResponse;
import ar.edu.usal.mgnutri.profesionales.dto.ProfesionalDtos.FranjasRequest;
import ar.edu.usal.mgnutri.profesionales.dto.ProfesionalDtos.ModificacionResponse;
import ar.edu.usal.mgnutri.profesionales.dto.ProfesionalDtos.ProfesionalDisponible;
import ar.edu.usal.mgnutri.profesionales.dto.ProfesionalDtos.ProfesionalListItem;
import ar.edu.usal.mgnutri.profesionales.dto.ProfesionalDtos.ProfesionalRequest;
import ar.edu.usal.mgnutri.profesionales.dto.ProfesionalDtos.ProfesionalResponse;
import ar.edu.usal.mgnutri.profesionales.service.AgendaService;
import ar.edu.usal.mgnutri.profesionales.service.ProfesionalService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/** Profesionales y agenda (spec 02). */
@RestController
@RequestMapping("/api/v1")
public class ProfesionalController {

    private final ProfesionalService service;
    private final AgendaService agenda;

    public ProfesionalController(ProfesionalService service, AgendaService agenda) {
        this.service = service;
        this.agenda = agenda;
    }

    @GetMapping("/profesionales")
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<ProfesionalListItem> listar(@RequestParam(required = false) String q,
                                                    @RequestParam(required = false) Profesional.Tipo tipo,
                                                    @RequestParam(required = false) Profesional.Estado estado,
                                                    @RequestParam(required = false) Integer page,
                                                    @RequestParam(required = false) Integer size) {
        return service.buscar(q, tipo, estado, PageResponse.pageable(page, size, Sort.by("apellido", "nombre")));
    }

    @GetMapping("/profesionales/disponibles")
    public List<ProfesionalDisponible> disponibles(@RequestParam(required = false) Area area) {
        return service.disponibles(area);
    }

    @GetMapping("/profesionales/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','NUTRICIONISTA','ENTRENADOR')")
    public ProfesionalResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @GetMapping("/me/profesional")
    @PreAuthorize("hasAnyRole('NUTRICIONISTA','ENTRENADOR')")
    public ProfesionalResponse yo() {
        return service.yo();
    }

    @PostMapping("/profesionales")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AltaResponse> crear(@Valid @RequestBody ProfesionalRequest req) {
        AltaResponse r = service.crear(req);
        return ResponseEntity.created(URI.create("/api/v1/profesionales/" + r.id())).body(r);
    }

    @PutMapping("/profesionales/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ModificacionResponse modificar(@PathVariable Long id, @Valid @RequestBody ProfesionalRequest req) {
        return service.modificar(id, req);
    }

    @DeleteMapping("/profesionales/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> baja(@PathVariable Long id, @RequestBody(required = false) BajaRequest req) {
        service.darDeBaja(id, req == null ? null : req.motivo());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/profesionales/{id}/baja")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> bajaPost(@PathVariable Long id, @RequestBody(required = false) BajaRequest req) {
        service.darDeBaja(id, req == null ? null : req.motivo());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/profesionales/{id}/reenviar-credenciales")
    @PreAuthorize("hasRole('ADMIN')")
    public CredencialesResponse reenviar(@PathVariable Long id) {
        return service.reenviarCredenciales(id);
    }

    @GetMapping("/profesionales/{id}/disponibilidad")
    @PreAuthorize("hasAnyRole('ADMIN','NUTRICIONISTA','ENTRENADOR')")
    public List<FranjaResponse> franjas(@PathVariable Long id) {
        return agenda.franjas(id);
    }

    @PutMapping("/profesionales/{id}/disponibilidad")
    @PreAuthorize("hasAnyRole('ADMIN','NUTRICIONISTA','ENTRENADOR')")
    public List<FranjaResponse> reemplazarFranjas(@PathVariable Long id, @Valid @RequestBody FranjasRequest req) {
        return agenda.reemplazarFranjas(id, req.franjas());
    }

    @GetMapping("/profesionales/{id}/bloqueos")
    @PreAuthorize("hasAnyRole('ADMIN','NUTRICIONISTA','ENTRENADOR')")
    public List<BloqueoResponse> bloqueos(@PathVariable Long id) {
        return agenda.bloqueos(id);
    }

    @PostMapping("/profesionales/{id}/bloqueos")
    @PreAuthorize("hasAnyRole('ADMIN','NUTRICIONISTA','ENTRENADOR')")
    public BloqueoResponse crearBloqueo(@PathVariable Long id, @Valid @RequestBody BloqueoRequest req) {
        return agenda.crearBloqueo(id, req);
    }

    @DeleteMapping("/profesionales/{id}/bloqueos/{bloqueoId}")
    @PreAuthorize("hasAnyRole('ADMIN','NUTRICIONISTA','ENTRENADOR')")
    public ResponseEntity<Void> eliminarBloqueo(@PathVariable Long id, @PathVariable Long bloqueoId) {
        agenda.eliminarBloqueo(id, bloqueoId);
        return ResponseEntity.noContent().build();
    }
}
