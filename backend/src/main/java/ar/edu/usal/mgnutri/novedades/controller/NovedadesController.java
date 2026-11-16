package ar.edu.usal.mgnutri.novedades.controller;

import ar.edu.usal.mgnutri.configuracion.dto.ConfiguracionDtos.MisContactos;
import ar.edu.usal.mgnutri.novedades.dto.NovedadesDtos.ComentarioRequest;
import ar.edu.usal.mgnutri.novedades.dto.NovedadesDtos.ComentarioResponse;
import ar.edu.usal.mgnutri.novedades.dto.NovedadesDtos.HomePaciente;
import ar.edu.usal.mgnutri.novedades.dto.NovedadesDtos.MolestiaRequest;
import ar.edu.usal.mgnutri.novedades.dto.NovedadesDtos.MolestiaResponse;
import ar.edu.usal.mgnutri.novedades.dto.NovedadesDtos.Novedad;
import ar.edu.usal.mgnutri.novedades.service.NovedadesService;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Comentarios, molestias, novedades y home del paciente (specs 08/09). */
@RestController
@RequestMapping("/api/v1")
public class NovedadesController {

    private final NovedadesService service;

    public NovedadesController(NovedadesService service) {
        this.service = service;
    }

    @GetMapping("/me/home")
    @PreAuthorize("hasRole('PACIENTE')")
    public HomePaciente home() {
        return service.home();
    }

    @GetMapping("/me/contactos")
    @PreAuthorize("hasRole('PACIENTE')")
    public MisContactos contactos() {
        return service.contactos();
    }

    @PostMapping("/me/comentarios")
    @PreAuthorize("hasRole('PACIENTE')")
    public ComentarioResponse comentar(@Valid @RequestBody ComentarioRequest req) {
        return service.comentar(req);
    }

    @GetMapping("/me/comentarios")
    @PreAuthorize("hasRole('PACIENTE')")
    public List<ComentarioResponse> misComentarios() {
        return service.misComentarios();
    }

    @PostMapping("/me/molestias")
    @PreAuthorize("hasRole('PACIENTE')")
    public MolestiaResponse molestia(@Valid @RequestBody MolestiaRequest req) {
        return service.registrarMolestia(req);
    }

    @GetMapping("/me/molestias")
    @PreAuthorize("hasRole('PACIENTE')")
    public List<MolestiaResponse> misMolestias() {
        return service.misMolestias();
    }

    @GetMapping("/novedades")
    @PreAuthorize("hasAnyRole('ADMIN','NUTRICIONISTA','ENTRENADOR')")
    public List<Novedad> novedades(@RequestParam(defaultValue = "false") boolean leidas) {
        return service.novedades(leidas);
    }

    @GetMapping("/novedades/contador")
    @PreAuthorize("hasAnyRole('ADMIN','NUTRICIONISTA','ENTRENADOR')")
    public Map<String, Long> contador() {
        return Map.of("noLeidas", service.noLeidas());
    }

    @PostMapping("/novedades/{tipo}/{id}/leer")
    @PreAuthorize("hasAnyRole('ADMIN','NUTRICIONISTA','ENTRENADOR')")
    public ResponseEntity<Void> leer(@PathVariable String tipo, @PathVariable Long id) {
        service.marcarLeida(tipo, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/molestias/{id}/revisar")
    @PreAuthorize("hasAnyRole('ADMIN','NUTRICIONISTA','ENTRENADOR')")
    public ResponseEntity<Void> revisar(@PathVariable Long id) {
        service.marcarLeida("MOLESTIA", id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pacientes/{pid}/comentarios")
    @PreAuthorize("hasAnyRole('ADMIN','NUTRICIONISTA','ENTRENADOR')")
    public List<ComentarioResponse> comentarios(@PathVariable Long pid,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return service.comentariosDe(pid, desde, hasta);
    }

    @GetMapping("/pacientes/{pid}/molestias")
    @PreAuthorize("hasAnyRole('ADMIN','NUTRICIONISTA','ENTRENADOR')")
    public List<MolestiaResponse> molestias(@PathVariable Long pid,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return service.molestiasDe(pid, desde, hasta);
    }
}
