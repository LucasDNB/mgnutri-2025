package ar.edu.usal.mgnutri.turnos.controller;

import ar.edu.usal.mgnutri.common.Area;
import ar.edu.usal.mgnutri.turnos.dto.TurnoDtos.CambiarEstadoRequest;
import ar.edu.usal.mgnutri.turnos.dto.TurnoDtos.CancelarRequest;
import ar.edu.usal.mgnutri.turnos.dto.TurnoDtos.CrearTurnoRequest;
import ar.edu.usal.mgnutri.turnos.dto.TurnoDtos.HistorialResponse;
import ar.edu.usal.mgnutri.turnos.dto.TurnoDtos.HuecoResponse;
import ar.edu.usal.mgnutri.turnos.dto.TurnoDtos.ReprogramarRequest;
import ar.edu.usal.mgnutri.turnos.dto.TurnoDtos.SolicitarTurnoRequest;
import ar.edu.usal.mgnutri.turnos.dto.TurnoDtos.TurnoResponse;
import ar.edu.usal.mgnutri.turnos.service.TurnoService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

/** Turnos (spec 03). */
@RestController
@RequestMapping("/api/v1")
public class TurnoController {

    private final TurnoService service;

    public TurnoController(TurnoService service) {
        this.service = service;
    }

    @GetMapping("/profesionales/{id}/huecos")
    public List<HuecoResponse> huecos(@PathVariable Long id,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return service.huecos(id, desde, hasta);
    }

    // ---- paciente

    @GetMapping("/me/turnos")
    @PreAuthorize("hasRole('PACIENTE')")
    public List<TurnoResponse> misTurnos(@RequestParam(required = false) String estado,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return service.misTurnos(estado, desde, hasta);
    }

    @PostMapping("/me/turnos")
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<TurnoResponse> solicitar(@Valid @RequestBody SolicitarTurnoRequest req) {
        TurnoResponse t = service.solicitar(req.profesionalId(), req.inicio(), req.area());
        return ResponseEntity.created(URI.create("/api/v1/turnos/" + t.id())).body(t);
    }

    @PostMapping("/me/turnos/{id}/reprogramar")
    @PreAuthorize("hasRole('PACIENTE')")
    public TurnoResponse reprogramarPropio(@PathVariable Long id, @Valid @RequestBody ReprogramarRequest req) {
        return service.reprogramarPorPaciente(id, req.inicio(), req.version());
    }

    @PostMapping("/me/turnos/{id}/cancelar")
    @PreAuthorize("hasRole('PACIENTE')")
    public TurnoResponse cancelarPropio(@PathVariable Long id, @RequestBody(required = false) CancelarRequest req) {
        return service.cancelarPorPaciente(id, req == null ? null : req.version());
    }

    // ---- staff

    @GetMapping("/turnos")
    @PreAuthorize("hasAnyRole('ADMIN','NUTRICIONISTA','ENTRENADOR')")
    public List<TurnoResponse> listar(@RequestParam(required = false) Long profesionalId,
                                      @RequestParam(required = false) Long pacienteId,
                                      @RequestParam(required = false) Area area,
                                      @RequestParam(required = false) String estado,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                      @RequestParam(defaultValue = "false") boolean incluirCancelados) {
        return service.listar(profesionalId, pacienteId, area, estado, desde, hasta, incluirCancelados);
    }

    @GetMapping("/turnos/{id}")
    public TurnoResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @GetMapping("/turnos/{id}/historial")
    public List<HistorialResponse> historial(@PathVariable Long id) {
        return service.historial(id);
    }

    @PostMapping("/turnos")
    @PreAuthorize("hasAnyRole('ADMIN','NUTRICIONISTA','ENTRENADOR')")
    public ResponseEntity<TurnoResponse> crear(@Valid @RequestBody CrearTurnoRequest req) {
        TurnoResponse t = service.crearPorStaff(req);
        return ResponseEntity.created(URI.create("/api/v1/turnos/" + t.id())).body(t);
    }

    @PostMapping("/turnos/{id}/reprogramar")
    @PreAuthorize("hasAnyRole('ADMIN','NUTRICIONISTA','ENTRENADOR')")
    public TurnoResponse reprogramar(@PathVariable Long id, @Valid @RequestBody ReprogramarRequest req) {
        return service.reprogramarPorStaff(id, req.inicio(), req.motivo(), req.version());
    }

    @PostMapping("/turnos/{id}/cancelar")
    @PreAuthorize("hasAnyRole('ADMIN','NUTRICIONISTA','ENTRENADOR')")
    public TurnoResponse cancelar(@PathVariable Long id, @Valid @RequestBody CancelarRequest req) {
        return service.cancelarPorStaff(id, req.motivo(), req.version());
    }

    @PostMapping("/turnos/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMIN','NUTRICIONISTA','ENTRENADOR')")
    public TurnoResponse estado(@PathVariable Long id, @Valid @RequestBody CambiarEstadoRequest req) {
        return service.cambiarEstado(id, req.estado(), req.version());
    }
}
