package ar.edu.usal.mgnutri.seguridad.service;

import ar.edu.usal.mgnutri.common.Rol;
import ar.edu.usal.mgnutri.common.error.BusinessException;
import ar.edu.usal.mgnutri.common.error.ErrorCode;
import ar.edu.usal.mgnutri.seguridad.dao.UsuarioDao;
import ar.edu.usal.mgnutri.seguridad.domain.Usuario;
import ar.edu.usal.mgnutri.seguridad.dto.SeguridadDtos.Credenciales;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;

/** Gestión de cuentas de usuario usada por pacientes y profesionales (D-04, D-06). */
@Service
public class UsuarioService {

    private final UsuarioDao dao;
    private final PasswordEncoder encoder;
    private final PasswordPolicy policy;
    private final Clock clock;

    public UsuarioService(UsuarioDao dao, PasswordEncoder encoder, PasswordPolicy policy, Clock clock) {
        this.dao = dao;
        this.encoder = encoder;
        this.policy = policy;
        this.clock = clock;
    }

    /** Crea el usuario con contraseña temporal; username = número de documento. */
    @Transactional
    public Credenciales crear(String username, Set<Rol> roles) {
        if (dao.existsByUsername(username)) {
            throw new BusinessException(ErrorCode.DUPLICADO, "Ya existe una persona registrada con ese documento.");
        }
        String temporal = policy.temporal();
        Usuario u = new Usuario();
        u.setUsername(username);
        u.setPasswordHash(encoder.encode(temporal));
        u.setDebeCambiarPassword(true);
        u.setEstado(Usuario.Estado.ACTIVO);
        u.reemplazarRoles(roles);
        dao.save(u);
        return new Credenciales(u.getId(), username, temporal);
    }

    @Transactional
    public void cambiarUsername(Long usuarioId, String nuevo) {
        Usuario u = obtener(usuarioId);
        if (u.getUsername().equals(nuevo)) {
            return;
        }
        if (dao.existsByUsername(nuevo)) {
            throw new BusinessException(ErrorCode.DUPLICADO, "Ya existe una persona registrada con ese documento.");
        }
        u.setUsername(nuevo);
    }

    @Transactional
    public void reemplazarRoles(Long usuarioId, Set<Rol> roles) {
        Usuario u = obtener(usuarioId);
        u.reemplazarRoles(roles);
        u.setTokensValidosDesde(ahora());
    }

    /** Nueva contraseña temporal; fuerza el cambio en el próximo ingreso (CA-01.2.6). */
    @Transactional
    public Credenciales regenerarCredenciales(Long usuarioId) {
        Usuario u = obtener(usuarioId);
        if (u.getEstado() == Usuario.Estado.BAJA) {
            throw new BusinessException(ErrorCode.REGLA_NEGOCIO, "El usuario está dado de baja.");
        }
        String temporal = policy.temporal();
        u.setPasswordHash(encoder.encode(temporal));
        u.setDebeCambiarPassword(true);
        u.setIntentosFallidos(0);
        u.setBloqueadoHasta(null);
        u.setEstado(Usuario.Estado.ACTIVO);
        return new Credenciales(u.getId(), u.getUsername(), temporal);
    }

    /** Reactivación de un usuario dado de baja: vuelve a ACTIVO con una contraseña temporal nueva. */
    @Transactional
    public Credenciales reactivar(Long usuarioId) {
        Usuario u = obtener(usuarioId);
        u.setEstado(Usuario.Estado.ACTIVO);
        return regenerarCredenciales(usuarioId);
    }

    /** Baja: usuario BAJA, contraseña invalidada y sesiones revocadas (CA-01.3.2). */
    @Transactional
    public void darDeBaja(Long usuarioId) {
        Usuario u = obtener(usuarioId);
        u.setEstado(Usuario.Estado.BAJA);
        u.setPasswordHash(encoder.encode(UUID.randomUUID().toString()));
        u.setTokensValidosDesde(ahora().plusSeconds(1));
    }

    @Transactional(readOnly = true)
    public Usuario obtener(Long id) {
        return dao.findById(id).orElseThrow(() -> BusinessException.noEncontrado("Usuario"));
    }

    private Instant ahora() {
        return clock.instant().truncatedTo(ChronoUnit.SECONDS);
    }
}
