package ar.edu.usal.mgnutri.evolucion.controller;

import ar.edu.usal.mgnutri.evolucion.dto.EvolucionDtos.EvaluacionRequest;
import ar.edu.usal.mgnutri.evolucion.dto.EvolucionDtos.EvaluacionResponse;
import ar.edu.usal.mgnutri.evolucion.dto.EvolucionDtos.EvaluacionUpdateRequest;
import ar.edu.usal.mgnutri.evolucion.dto.EvolucionDtos.EvolucionResponse;
import ar.edu.usal.mgnutri.evolucion.dto.EvolucionDtos.MedicionRequest;
import ar.edu.usal.mgnutri.evolucion.dto.EvolucionDtos.MedicionResponse;
import ar.edu.usal.mgnutri.evolucion.service.EvolucionService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.List;

/** Evolución (spec 06). */
@RestController
@RequestMapping("/api/v1")
public class EvolucionController {

    private final EvolucionService service;

    public EvolucionController(EvolucionService service) {
        this.service = service;
    }

    @GetMapping("/pacientes/{pid}/antropometria")
    @PreAuthorize("hasAnyRole('ADMIN','NUTRICIONISTA','ENTRENADOR')")
    public List<MedicionResponse> mediciones(@PathVariable Long pid,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return service.mediciones(pid, desde, hasta);
    }

    @PostMapping("/pacientes/{pid}/antropometria")
    @PreAuthorize("hasRole('NUTRICIONISTA')")
    public MedicionResponse crearMedicion(@PathVariable Long pid, @Valid @RequestBody MedicionRequest req) {
        return service.crearMedicion(pid, req);
    }

    @PutMapping("/antropometria/{id}")
    @PreAuthorize("hasRole('NUTRICIONISTA')")
    public MedicionResponse modificarMedicion(@PathVariable Long id, @Valid @RequestBody MedicionRequest req) {
        return service.modificarMedicion(id, req);
    }

    @DeleteMapping("/antropometria/{id}")
    @PreAuthorize("hasRole('NUTRICIONISTA')")
    public ResponseEntity<Void> eliminarMedicion(@PathVariable Long id) {
        service.eliminarMedicion(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pacientes/{pid}/evaluaciones-fisicas")
    @PreAuthorize("hasAnyRole('ADMIN','NUTRICIONISTA','ENTRENADOR')")
    public List<EvaluacionResponse> evaluaciones(@PathVariable Long pid,
                                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                                 @RequestParam(required = false) String prueba) {
        return service.evaluaciones(pid, desde, hasta, prueba);
    }

    @PostMapping("/pacientes/{pid}/evaluaciones-fisicas")
    @PreAuthorize("hasRole('ENTRENADOR')")
    public List<EvaluacionResponse> crearEvaluaciones(@PathVariable Long pid, @Valid @RequestBody EvaluacionRequest req) {
        return service.crearEvaluaciones(pid, req);
    }

    @PutMapping("/evaluaciones-fisicas/{id}")
    @PreAuthorize("hasRole('ENTRENADOR')")
    public EvaluacionResponse modificarEvaluacion(@PathVariable Long id, @Valid @RequestBody EvaluacionUpdateRequest req) {
        return service.modificarEvaluacion(id, req);
    }

    @DeleteMapping("/evaluaciones-fisicas/{id}")
    @PreAuthorize("hasRole('ENTRENADOR')")
    public ResponseEntity<Void> eliminarEvaluacion(@PathVariable Long id) {
        service.eliminarEvaluacion(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pacientes/{pid}/evolucion")
    @PreAuthorize("hasAnyRole('ADMIN','NUTRICIONISTA','ENTRENADOR')")
    public EvolucionResponse evolucion(@PathVariable Long pid,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return service.evolucion(pid, desde, hasta);
    }

    @GetMapping("/me/evolucion")
    @PreAuthorize("hasRole('PACIENTE')")
    public EvolucionResponse miEvolucion(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return service.miEvolucion(desde, hasta);
    }
}
