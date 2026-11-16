package ar.edu.usal.mgnutri.entrenamiento.controller;

import ar.edu.usal.mgnutri.entrenamiento.dto.RutinaDtos.AlternativasEjercicioResponse;
import ar.edu.usal.mgnutri.entrenamiento.dto.RutinaDtos.DiaCalendario;
import ar.edu.usal.mgnutri.entrenamiento.dto.RutinaDtos.ReemplazoEjercicioRequest;
import ar.edu.usal.mgnutri.entrenamiento.dto.RutinaDtos.RegistroRequest;
import ar.edu.usal.mgnutri.entrenamiento.dto.RutinaDtos.RegistroResponse;
import ar.edu.usal.mgnutri.entrenamiento.dto.RutinaDtos.RegistrosEntrenamiento;
import ar.edu.usal.mgnutri.entrenamiento.dto.RutinaDtos.RutinaListItem;
import ar.edu.usal.mgnutri.entrenamiento.dto.RutinaDtos.RutinaRequest;
import ar.edu.usal.mgnutri.entrenamiento.dto.RutinaDtos.RutinaResponse;
import ar.edu.usal.mgnutri.entrenamiento.service.ActividadFisicaService;
import ar.edu.usal.mgnutri.entrenamiento.service.RutinaService;
import ar.edu.usal.mgnutri.reportes.service.ReporteExporterFactory;
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

import java.net.URI;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/** Rutinas (spec 05) y consulta/actividad del paciente (spec 09). */
@RestController
@RequestMapping("/api/v1")
public class RutinaController {

    private final RutinaService rutinas;
    private final ActividadFisicaService actividad;

    public RutinaController(RutinaService rutinas, ActividadFisicaService actividad) {
        this.rutinas = rutinas;
        this.actividad = actividad;
    }

    @GetMapping("/pacientes/{pid}/rutinas")
    @PreAuthorize("hasAnyRole('ADMIN','NUTRICIONISTA','ENTRENADOR')")
    public List<RutinaListItem> listar(@PathVariable Long pid, @RequestParam(required = false) String estado) {
        return rutinas.listar(pid, estado);
    }

    @GetMapping("/rutinas/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','NUTRICIONISTA','ENTRENADOR')")
    public RutinaResponse obtener(@PathVariable Long id) {
        return rutinas.obtener(id);
    }

    @PostMapping("/pacientes/{pid}/rutinas")
    @PreAuthorize("hasRole('ENTRENADOR')")
    public ResponseEntity<RutinaResponse> crear(@PathVariable Long pid, @Valid @RequestBody RutinaRequest req) {
        RutinaResponse r = rutinas.crear(pid, req);
        return ResponseEntity.created(URI.create("/api/v1/rutinas/" + r.id())).body(r);
    }

    @PutMapping("/rutinas/{id}")
    @PreAuthorize("hasRole('ENTRENADOR')")
    public RutinaResponse actualizar(@PathVariable Long id, @Valid @RequestBody RutinaRequest req) {
        return rutinas.actualizar(id, req);
    }

    @DeleteMapping("/rutinas/{id}")
    @PreAuthorize("hasRole('ENTRENADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        rutinas.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/rutinas/{id}/duplicar")
    @PreAuthorize("hasRole('ENTRENADOR')")
    public RutinaResponse duplicar(@PathVariable Long id) {
        return rutinas.duplicar(id);
    }

    @GetMapping("/rutinas/{id}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN','NUTRICIONISTA','ENTRENADOR')")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        return ReporteExporterFactory.archivo(actividad.pdfDe(id), "application/pdf", "rutina-" + id + ".pdf");
    }

    @GetMapping("/pacientes/{pid}/registros-entrenamiento")
    @PreAuthorize("hasAnyRole('ADMIN','NUTRICIONISTA','ENTRENADOR')")
    public RegistrosEntrenamiento registros(@PathVariable Long pid,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return actividad.registrosDePaciente(pid, desde, hasta);
    }

    // ---- paciente

    @GetMapping("/me/rutina")
    @PreAuthorize("hasRole('PACIENTE')")
    public RutinaResponse miRutina() {
        return actividad.miRutina();
    }

    @GetMapping("/me/rutina/pdf")
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<byte[]> miPdf() {
        return ReporteExporterFactory.archivo(actividad.miPdf(), "application/pdf", "mi-rutina.pdf");
    }

    @GetMapping("/me/rutina/calendario")
    @PreAuthorize("hasRole('PACIENTE')")
    public List<DiaCalendario> calendario(@RequestParam(required = false) String mes) {
        return actividad.calendario(mes == null || mes.isBlank() ? YearMonth.now() : YearMonth.parse(mes));
    }

    @GetMapping("/me/registro-entrenamiento")
    @PreAuthorize("hasRole('PACIENTE')")
    public List<RegistroResponse> misRegistros(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return actividad.misRegistros(desde, hasta);
    }

    @PutMapping("/me/registro-entrenamiento")
    @PreAuthorize("hasRole('PACIENTE')")
    public RegistroResponse registrar(@Valid @RequestBody RegistroRequest req) {
        return actividad.registrar(req);
    }

    @GetMapping("/me/rutina/ejercicios/{id}/alternativas")
    @PreAuthorize("hasRole('PACIENTE')")
    public AlternativasEjercicioResponse alternativas(@PathVariable Long id) {
        return actividad.alternativas(id);
    }

    @PutMapping("/me/rutina/ejercicios/{id}/reemplazo")
    @PreAuthorize("hasRole('PACIENTE')")
    public RutinaResponse reemplazar(@PathVariable Long id, @Valid @RequestBody ReemplazoEjercicioRequest req) {
        return actividad.reemplazar(id, req.ejercicioId());
    }

    @DeleteMapping("/me/rutina/ejercicios/{id}/reemplazo")
    @PreAuthorize("hasRole('PACIENTE')")
    public RutinaResponse volver(@PathVariable Long id) {
        return actividad.volverAlOriginal(id);
    }
}
