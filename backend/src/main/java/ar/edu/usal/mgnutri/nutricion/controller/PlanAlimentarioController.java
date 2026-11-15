package ar.edu.usal.mgnutri.nutricion.controller;

import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.ActivarRequest;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.AlternativasResponse;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.BorradorGenerado;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.GenerarRequest;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.ObservacionRequest;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.ObservacionResponse;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.PlanListItem;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.PlanRequest;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.PlanResponse;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.ReemplazoRequest;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.RegistroComidaRequest;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.RegistroComidaResponse;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.RegistrosNutricion;
import ar.edu.usal.mgnutri.nutricion.service.ActividadNutricionalService;
import ar.edu.usal.mgnutri.nutricion.service.GeneradorPlanService;
import ar.edu.usal.mgnutri.nutricion.service.PlanAlimentarioService;
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
import java.util.List;

/** Planes alimentarios (spec 04) y consulta/actividad del paciente (spec 08). */
@RestController
@RequestMapping("/api/v1")
public class PlanAlimentarioController {

    private final PlanAlimentarioService planes;
    private final GeneradorPlanService generador;
    private final ActividadNutricionalService actividad;

    public PlanAlimentarioController(PlanAlimentarioService planes, GeneradorPlanService generador,
                                     ActividadNutricionalService actividad) {
        this.planes = planes;
        this.generador = generador;
        this.actividad = actividad;
    }

    @GetMapping("/pacientes/{pid}/planes-alimentarios")
    @PreAuthorize("hasAnyRole('ADMIN','NUTRICIONISTA','ENTRENADOR')")
    public List<PlanListItem> listar(@PathVariable Long pid, @RequestParam(required = false) String estado) {
        return planes.listar(pid, estado);
    }

    @GetMapping("/planes-alimentarios/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','NUTRICIONISTA','ENTRENADOR')")
    public PlanResponse obtener(@PathVariable Long id) {
        return planes.obtener(id);
    }

    @PostMapping("/pacientes/{pid}/planes-alimentarios")
    @PreAuthorize("hasRole('NUTRICIONISTA')")
    public ResponseEntity<PlanResponse> crear(@PathVariable Long pid, @Valid @RequestBody PlanRequest req) {
        PlanResponse r = planes.crear(pid, req);
        return ResponseEntity.created(URI.create("/api/v1/planes-alimentarios/" + r.id())).body(r);
    }

    @PutMapping("/planes-alimentarios/{id}")
    @PreAuthorize("hasRole('NUTRICIONISTA')")
    public PlanResponse actualizar(@PathVariable Long id, @Valid @RequestBody PlanRequest req) {
        return planes.actualizar(id, req);
    }

    @PostMapping("/planes-alimentarios/{id}/activar")
    @PreAuthorize("hasRole('NUTRICIONISTA')")
    public PlanResponse activar(@PathVariable Long id, @RequestBody(required = false) ActivarRequest req) {
        return planes.activar(id, req != null && Boolean.TRUE.equals(req.finalizarActual()));
    }

    @DeleteMapping("/planes-alimentarios/{id}")
    @PreAuthorize("hasRole('NUTRICIONISTA')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        planes.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/planes-alimentarios/{id}/duplicar")
    @PreAuthorize("hasRole('NUTRICIONISTA')")
    public PlanResponse duplicar(@PathVariable Long id) {
        return planes.duplicar(id);
    }

    @PostMapping("/planes-alimentarios/generar")
    @PreAuthorize("hasRole('NUTRICIONISTA')")
    public BorradorGenerado generar(@Valid @RequestBody GenerarRequest req) {
        return generador.generar(req);
    }

    @GetMapping("/planes-alimentarios/{id}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN','NUTRICIONISTA','ENTRENADOR')")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        return ReporteExporterFactory.archivo(actividad.pdfDe(id), "application/pdf", "plan-alimentario-" + id + ".pdf");
    }

    @GetMapping("/pacientes/{pid}/registros-nutricion")
    @PreAuthorize("hasAnyRole('ADMIN','NUTRICIONISTA','ENTRENADOR')")
    public RegistrosNutricion registros(@PathVariable Long pid,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return actividad.registrosDePaciente(pid, desde, hasta);
    }

    // ---- paciente

    @GetMapping("/me/plan-alimentario")
    @PreAuthorize("hasRole('PACIENTE')")
    public PlanResponse miPlan() {
        return actividad.miPlan();
    }

    @GetMapping("/me/plan-alimentario/pdf")
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<byte[]> miPdf() {
        return ReporteExporterFactory.archivo(actividad.miPdf(), "application/pdf", "mi-plan-alimentario.pdf");
    }

    @GetMapping("/me/registro-comidas")
    @PreAuthorize("hasRole('PACIENTE')")
    public List<RegistroComidaResponse> misRegistros(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return actividad.misRegistros(desde, hasta);
    }

    @PutMapping("/me/registro-comidas")
    @PreAuthorize("hasRole('PACIENTE')")
    public RegistroComidaResponse registrar(@Valid @RequestBody RegistroComidaRequest req) {
        return actividad.registrar(req);
    }

    @GetMapping("/me/observaciones-diarias")
    @PreAuthorize("hasRole('PACIENTE')")
    public List<ObservacionResponse> misObservaciones(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return actividad.misObservaciones(desde, hasta);
    }

    @PutMapping("/me/observaciones-diarias")
    @PreAuthorize("hasRole('PACIENTE')")
    public ObservacionResponse guardarObservacion(@Valid @RequestBody ObservacionRequest req) {
        return actividad.guardarObservacion(req);
    }

    @GetMapping("/me/plan-alimentario/items/{itemId}/alternativas")
    @PreAuthorize("hasRole('PACIENTE')")
    public AlternativasResponse alternativas(@PathVariable Long itemId) {
        return actividad.alternativas(itemId);
    }

    @PutMapping("/me/plan-alimentario/items/{itemId}/reemplazo")
    @PreAuthorize("hasRole('PACIENTE')")
    public PlanResponse reemplazar(@PathVariable Long itemId, @Valid @RequestBody ReemplazoRequest req) {
        return actividad.reemplazar(itemId, req.alimentoId());
    }

    @DeleteMapping("/me/plan-alimentario/items/{itemId}/reemplazo")
    @PreAuthorize("hasRole('PACIENTE')")
    public PlanResponse volverAlOriginal(@PathVariable Long itemId) {
        return actividad.volverAlOriginal(itemId);
    }
}
