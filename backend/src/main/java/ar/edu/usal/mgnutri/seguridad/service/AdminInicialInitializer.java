package ar.edu.usal.mgnutri.seguridad.service;

import ar.edu.usal.mgnutri.common.Rol;
import ar.edu.usal.mgnutri.config.MgnutriProperties;
import ar.edu.usal.mgnutri.seguridad.dao.AdministradorDao;
import ar.edu.usal.mgnutri.seguridad.dao.UsuarioDao;
import ar.edu.usal.mgnutri.seguridad.domain.Administrador;
import ar.edu.usal.mgnutri.seguridad.domain.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;

/**
 * Crea el administrador inicial si no existe ninguno (docs/03-modelo-de-datos.md §2).
 * La contraseña temporal se toma de ADMIN_PASSWORD_INICIAL y debe cambiarse en el primer ingreso.
 */
@Component
@Order(1)
public class AdminInicialInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInicialInitializer.class);

    private final UsuarioDao usuarioDao;
    private final AdministradorDao administradorDao;
    private final PasswordEncoder encoder;
    private final MgnutriProperties props;

    public AdminInicialInitializer(UsuarioDao usuarioDao, AdministradorDao administradorDao, PasswordEncoder encoder,
                                   MgnutriProperties props) {
        this.usuarioDao = usuarioDao;
        this.administradorDao = administradorDao;
        this.encoder = encoder;
        this.props = props;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usuarioDao.contarActivosConRol(Rol.ADMIN) > 0) {
            return;
        }
        String password = props.adminInicial() == null ? null : props.adminInicial().password();
        if (password == null || password.isBlank()) {
            log.warn("No hay administradores y no se definió ADMIN_PASSWORD_INICIAL: no se crea el administrador inicial.");
            return;
        }
        String username = props.adminInicial().username() == null ? "admin" : props.adminInicial().username();
        Usuario u = usuarioDao.findByUsername(username).orElseGet(Usuario::new);
        u.setUsername(username);
        u.setPasswordHash(encoder.encode(password));
        u.setEstado(Usuario.Estado.ACTIVO);
        u.setDebeCambiarPassword(true);
        u.reemplazarRoles(EnumSet.of(Rol.ADMIN));
        usuarioDao.save(u);
        if (administradorDao.findByUsuarioId(u.getId()).isEmpty()) {
            administradorDao.save(new Administrador(u.getId(), "Administrador", "General"));
        }
        log.info("Administrador inicial '{}' creado. Debe cambiar la contraseña en el primer ingreso.", username);
    }
}
