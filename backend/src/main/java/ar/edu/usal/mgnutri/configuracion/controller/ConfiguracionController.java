package ar.edu.usal.mgnutri.configuracion.controller;

import ar.edu.usal.mgnutri.configuracion.dto.ConfiguracionDtos.ContactoCentral;
import ar.edu.usal.mgnutri.configuracion.dto.ConfiguracionDtos.ParametroResponse;
import ar.edu.usal.mgnutri.configuracion.service.ConfiguracionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Parámetros del sistema (Opciones avanzadas › Configuración) y contacto público (RF-12). */
@RestController
@RequestMapping("/api/v1")
public class ConfiguracionController {

    private final ConfiguracionService service;

    public ConfiguracionController(ConfiguracionService service) {
        this.service = service;
    }

    @GetMapping("/publico/contacto")
    public ContactoCentral contactoPublico() {
        return service.contactoCentral();
    }

    @GetMapping("/configuracion")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ParametroResponse> listar() {
        return service.listar();
    }

    @PutMapping("/configuracion")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ParametroResponse> actualizar(@RequestBody Map<String, String> valores) {
        return service.actualizar(valores);
    }
}
