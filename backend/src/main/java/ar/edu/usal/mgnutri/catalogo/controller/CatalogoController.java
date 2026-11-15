package ar.edu.usal.mgnutri.catalogo.controller;

import ar.edu.usal.mgnutri.catalogo.domain.Ejercicio;
import ar.edu.usal.mgnutri.catalogo.dto.CatalogoDtos.AlimentoRequest;
import ar.edu.usal.mgnutri.catalogo.dto.CatalogoDtos.AlimentoResponse;
import ar.edu.usal.mgnutri.catalogo.dto.CatalogoDtos.EjercicioRequest;
import ar.edu.usal.mgnutri.catalogo.dto.CatalogoDtos.EjercicioResponse;
import ar.edu.usal.mgnutri.catalogo.dto.CatalogoDtos.GrupoAlimentoResponse;
import ar.edu.usal.mgnutri.catalogo.dto.CatalogoDtos.GrupoMuscularResponse;
import ar.edu.usal.mgnutri.catalogo.service.AlimentoService;
import ar.edu.usal.mgnutri.catalogo.service.EjercicioService;
import ar.edu.usal.mgnutri.common.PageResponse;
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

/** Base de datos de alimentos y ejercicios (spec 10). Permisos según tabla de la spec. */
@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasAnyRole('ADMIN','NUTRICIONISTA','ENTRENADOR')")
public class CatalogoController {

    private final AlimentoService alimentos;
    private final EjercicioService ejercicios;

    public CatalogoController(AlimentoService alimentos, EjercicioService ejercicios) {
        this.alimentos = alimentos;
        this.ejercicios = ejercicios;
    }

    @GetMapping("/grupos-alimento")
    public List<GrupoAlimentoResponse> gruposAlimento() {
        return alimentos.grupos();
    }

    @GetMapping("/alimentos")
    public PageResponse<AlimentoResponse> alimentos(@RequestParam(required = false) String q,
                                                    @RequestParam(required = false) Long grupoId,
                                                    @RequestParam(required = false) Boolean activo,
                                                    @RequestParam(required = false) String aptoPara,
                                                    @RequestParam(required = false) Integer page,
                                                    @RequestParam(required = false) Integer size) {
        return alimentos.buscar(q, grupoId, activo, aptoPara, PageResponse.pageable(page, size, Sort.by("nombre")));
    }

    @GetMapping("/alimentos/{id}")
    public AlimentoResponse alimento(@PathVariable Long id) {
        return alimentos.obtener(id);
    }

    @PostMapping("/alimentos")
    @PreAuthorize("hasAnyRole('ADMIN','NUTRICIONISTA')")
    public ResponseEntity<AlimentoResponse> crearAlimento(@Valid @RequestBody AlimentoRequest req) {
        AlimentoResponse r = alimentos.crear(req);
        return ResponseEntity.created(URI.create("/api/v1/alimentos/" + r.id())).body(r);
    }

    @PutMapping("/alimentos/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','NUTRICIONISTA')")
    public AlimentoResponse modificarAlimento(@PathVariable Long id, @Valid @RequestBody AlimentoRequest req) {
        return alimentos.modificar(id, req);
    }

    @DeleteMapping("/alimentos/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','NUTRICIONISTA')")
    public ResponseEntity<Void> bajaAlimento(@PathVariable Long id) {
        alimentos.darDeBaja(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/alimentos/{id}/reactivar")
    @PreAuthorize("hasAnyRole('ADMIN','NUTRICIONISTA')")
    public AlimentoResponse reactivarAlimento(@PathVariable Long id) {
        return alimentos.reactivar(id);
    }

    @GetMapping("/grupos-musculares")
    public List<GrupoMuscularResponse> gruposMusculares() {
        return ejercicios.grupos();
    }

    @GetMapping("/ejercicios")
    public PageResponse<EjercicioResponse> ejercicios(@RequestParam(required = false) String q,
                                                      @RequestParam(required = false) Long grupoMuscularId,
                                                      @RequestParam(required = false) Ejercicio.Dificultad dificultad,
                                                      @RequestParam(required = false) Boolean activo,
                                                      @RequestParam(required = false) Integer page,
                                                      @RequestParam(required = false) Integer size) {
        return ejercicios.buscar(q, grupoMuscularId, dificultad, activo, PageResponse.pageable(page, size, Sort.by("nombre")));
    }

    @GetMapping("/ejercicios/{id}")
    public EjercicioResponse ejercicio(@PathVariable Long id) {
        return ejercicios.obtener(id);
    }

    @PostMapping("/ejercicios")
    @PreAuthorize("hasAnyRole('ADMIN','ENTRENADOR')")
    public ResponseEntity<EjercicioResponse> crearEjercicio(@Valid @RequestBody EjercicioRequest req) {
        EjercicioResponse r = ejercicios.crear(req);
        return ResponseEntity.created(URI.create("/api/v1/ejercicios/" + r.id())).body(r);
    }

    @PutMapping("/ejercicios/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ENTRENADOR')")
    public EjercicioResponse modificarEjercicio(@PathVariable Long id, @Valid @RequestBody EjercicioRequest req) {
        return ejercicios.modificar(id, req);
    }

    @DeleteMapping("/ejercicios/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ENTRENADOR')")
    public ResponseEntity<Void> bajaEjercicio(@PathVariable Long id) {
        ejercicios.darDeBaja(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/ejercicios/{id}/reactivar")
    @PreAuthorize("hasAnyRole('ADMIN','ENTRENADOR')")
    public EjercicioResponse reactivarEjercicio(@PathVariable Long id) {
        return ejercicios.reactivar(id);
    }
}
