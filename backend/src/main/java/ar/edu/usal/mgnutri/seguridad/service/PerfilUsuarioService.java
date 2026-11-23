package ar.edu.usal.mgnutri.seguridad.service;

import ar.edu.usal.mgnutri.common.Rol;
import ar.edu.usal.mgnutri.configuracion.service.ConfiguracionService;
import ar.edu.usal.mgnutri.pacientes.dao.PacienteDao;
import ar.edu.usal.mgnutri.pacientes.domain.Paciente;
import ar.edu.usal.mgnutri.profesionales.dao.DisponibilidadDao;
import ar.edu.usal.mgnutri.profesionales.dao.ProfesionalDao;
import ar.edu.usal.mgnutri.profesionales.domain.Profesional;
import ar.edu.usal.mgnutri.seguridad.dao.AdministradorDao;
import ar.edu.usal.mgnutri.seguridad.domain.Usuario;
import ar.edu.usal.mgnutri.seguridad.dto.SeguridadDtos.UsuarioActual;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.stream.Collectors;

/** Arma la vista del usuario autenticado (nombre, home, ids de paciente/profesional). */
@Service
public class PerfilUsuarioService {

    private final PacienteDao pacienteDao;
    private final ProfesionalDao profesionalDao;
    private final AdministradorDao administradorDao;
    private final DisponibilidadDao disponibilidadDao;
    private final ConfiguracionService config;

    public PerfilUsuarioService(PacienteDao pacienteDao, ProfesionalDao profesionalDao, AdministradorDao administradorDao,
                                DisponibilidadDao disponibilidadDao, ConfiguracionService config) {
        this.pacienteDao = pacienteDao;
        this.profesionalDao = profesionalDao;
        this.administradorDao = administradorDao;
        this.disponibilidadDao = disponibilidadDao;
        this.config = config;
    }

    @Transactional(readOnly = true)
    public Optional<Long> pacienteId(Long usuarioId) {
        return pacienteDao.findByUsuarioId(usuarioId).map(Paciente::getId);
    }

    @Transactional(readOnly = true)
    public Optional<Long> profesionalId(Long usuarioId) {
        return profesionalDao.findByUsuarioId(usuarioId).map(Profesional::getId);
    }

    public boolean debeAceptarTerminos(Usuario u) {
        return !config.versionTerminosVigente().equals(u.getTerminosVersionAceptada());
    }

    @Transactional(readOnly = true)
    public UsuarioActual vista(Usuario u) {
        String nombre = u.getUsername();
        Long pacienteId = null;
        Long profesionalId = null;
        String tipo = null;
        boolean agenda = false;
        Optional<Paciente> pac = pacienteDao.findByUsuarioId(u.getId());
        Optional<Profesional> prof = profesionalDao.findByUsuarioId(u.getId());
        if (pac.isPresent()) {
            nombre = pac.get().nombreCompleto();
            pacienteId = pac.get().getId();
        } else if (prof.isPresent()) {
            nombre = prof.get().nombreVisible();
            profesionalId = prof.get().getId();
            tipo = prof.get().getTipo().name();
            agenda = prof.get().getTipo().usaAgenda() && disponibilidadDao.existsByProfesionalIdAndActivoTrue(profesionalId);
        } else {
            nombre = administradorDao.findByUsuarioId(u.getId()).map(a -> a.getNombre() + " " + a.getApellido()).orElse("Administrador");
        }
        String home;
        if (u.tieneRol(Rol.ADMIN)) {
            home = "/admin/home.html";
        } else if (u.tieneRol(Rol.PACIENTE)) {
            home = "/paciente/home.html";
        } else {
            home = "/profesional/home.html";
        }
        return new UsuarioActual(u.getId(), u.getUsername(), nombre,
                u.getRoles().stream().map(Enum::name).collect(Collectors.toSet()), home, u.isDebeCambiarPassword(),
                debeAceptarTerminos(u), pacienteId, profesionalId, tipo, agenda);
    }
}
