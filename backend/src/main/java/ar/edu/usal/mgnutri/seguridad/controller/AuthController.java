package ar.edu.usal.mgnutri.seguridad.controller;

import ar.edu.usal.mgnutri.configuracion.dto.ConfiguracionDtos.AceptarTerminosRequest;
import ar.edu.usal.mgnutri.configuracion.dto.ConfiguracionDtos.TerminosRequest;
import ar.edu.usal.mgnutri.configuracion.dto.ConfiguracionDtos.TerminosResponse;
import ar.edu.usal.mgnutri.configuracion.service.ConfiguracionService;
import ar.edu.usal.mgnutri.seguridad.dto.SeguridadDtos.CambiarPasswordRequest;
import ar.edu.usal.mgnutri.seguridad.dto.SeguridadDtos.LoginRequest;
import ar.edu.usal.mgnutri.seguridad.dto.SeguridadDtos.LoginResponse;
import ar.edu.usal.mgnutri.seguridad.dto.SeguridadDtos.RecuperoConfirmarRequest;
import ar.edu.usal.mgnutri.seguridad.dto.SeguridadDtos.RecuperoRequest;
import ar.edu.usal.mgnutri.seguridad.dto.SeguridadDtos.RecuperoResponse;
import ar.edu.usal.mgnutri.seguridad.dto.SeguridadDtos.TokenValidoResponse;
import ar.edu.usal.mgnutri.seguridad.dto.SeguridadDtos.UsuarioActual;
import ar.edu.usal.mgnutri.seguridad.service.AuthService;
import ar.edu.usal.mgnutri.seguridad.service.RecuperoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Autenticación, recupero y cambio de contraseña, T&C (spec 13). */
@RestController
@RequestMapping("/api/v1")
public class AuthController {

    private final AuthService auth;
    private final RecuperoService recupero;
    private final ConfiguracionService config;

    public AuthController(AuthService auth, RecuperoService recupero, ConfiguracionService config) {
        this.auth = auth;
        this.recupero = recupero;
        this.config = config;
    }

    @PostMapping("/auth/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req);
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout() {
        auth.logout();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/auth/recupero")
    public RecuperoResponse solicitarRecupero(@Valid @RequestBody RecuperoRequest req) {
        return recupero.solicitar(req.documentoOTelefono());
    }

    @GetMapping("/auth/recupero/{token}")
    public TokenValidoResponse validarRecupero(@PathVariable String token) {
        return recupero.validar(token);
    }

    @PostMapping("/auth/recupero/confirmar")
    public ResponseEntity<Void> confirmarRecupero(@Valid @RequestBody RecuperoConfirmarRequest req) {
        recupero.confirmar(req.token(), req.nuevaPassword());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/auth/cambiar-password")
    public LoginResponse cambiarPassword(@Valid @RequestBody CambiarPasswordRequest req) {
        return auth.cambiarPassword(req);
    }

    @GetMapping("/me")
    public UsuarioActual me() {
        return auth.me();
    }

    @GetMapping("/terminos/vigentes")
    public TerminosResponse terminosVigentes() {
        return config.terminosVigentes();
    }

    @PostMapping("/terminos/aceptar")
    public UsuarioActual aceptarTerminos(@Valid @RequestBody AceptarTerminosRequest req) {
        return auth.aceptarTerminos(req.version());
    }

    @GetMapping("/terminos")
    @PreAuthorize("hasRole('ADMIN')")
    public List<TerminosResponse> historialTerminos() {
        return config.historialTerminos();
    }

    @PostMapping("/terminos")
    @PreAuthorize("hasRole('ADMIN')")
    public TerminosResponse publicarTerminos(@Valid @RequestBody TerminosRequest req) {
        return config.publicarTerminos(req);
    }
}
