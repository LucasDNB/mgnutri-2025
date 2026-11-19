package ar.edu.usal.mgnutri.seguridad.service;

import ar.edu.usal.mgnutri.bitacora.domain.Modulo;
import ar.edu.usal.mgnutri.bitacora.domain.TipoEvento;
import ar.edu.usal.mgnutri.bitacora.service.BitacoraService;
import ar.edu.usal.mgnutri.common.Fechas;
import ar.edu.usal.mgnutri.common.error.BusinessException;
import ar.edu.usal.mgnutri.common.error.ErrorCode;
import ar.edu.usal.mgnutri.common.security.CurrentUser;
import ar.edu.usal.mgnutri.common.security.UsuarioPrincipal;
import ar.edu.usal.mgnutri.config.MgnutriProperties;
import ar.edu.usal.mgnutri.configuracion.service.ConfiguracionService;
import ar.edu.usal.mgnutri.pacientes.dao.PacienteDao;
import ar.edu.usal.mgnutri.profesionales.dao.ProfesionalDao;
import ar.edu.usal.mgnutri.seguridad.dao.UsuarioDao;
import ar.edu.usal.mgnutri.seguridad.domain.RegistroAcceso;
import ar.edu.usal.mgnutri.seguridad.domain.Usuario;
import ar.edu.usal.mgnutri.seguridad.dto.SeguridadDtos.CambiarPasswordRequest;
import ar.edu.usal.mgnutri.seguridad.dto.SeguridadDtos.LoginRequest;
import ar.edu.usal.mgnutri.seguridad.dto.SeguridadDtos.LoginResponse;
import ar.edu.usal.mgnutri.seguridad.dto.SeguridadDtos.UsuarioActual;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

/** Autenticación (CU-47), cambio de contraseña (CU-49), cierre de sesión (CU-50) y T&C (D-25). */
@Service
public class AuthService {

    static final String MSJ_CREDENCIALES = "Usuario o contraseña incorrectos";

    private final UsuarioDao usuarioDao;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final TokenRevocacionService revocacion;
    private final RegistroAccesoService accesos;
    private final PerfilUsuarioService perfiles;
    private final PasswordPolicy policy;
    private final BitacoraService bitacora;
    private final ConfiguracionService config;
    private final PacienteDao pacienteDao;
    private final ProfesionalDao profesionalDao;
    private final MgnutriProperties.Seguridad seg;
    private final Clock clock;
    private final TransactionTemplate tx;

    public AuthService(UsuarioDao usuarioDao, PasswordEncoder encoder, JwtService jwt, TokenRevocacionService revocacion,
                       RegistroAccesoService accesos, PerfilUsuarioService perfiles, PasswordPolicy policy,
                       BitacoraService bitacora, ConfiguracionService config, PacienteDao pacienteDao,
                       ProfesionalDao profesionalDao, MgnutriProperties props, Clock clock, TransactionTemplate tx) {
        this.usuarioDao = usuarioDao;
        this.encoder = encoder;
        this.jwt = jwt;
        this.revocacion = revocacion;
        this.accesos = accesos;
        this.perfiles = perfiles;
        this.policy = policy;
        this.bitacora = bitacora;
        this.config = config;
        this.pacienteDao = pacienteDao;
        this.profesionalDao = profesionalDao;
        this.seg = props.seguridad();
        this.clock = clock;
        this.tx = tx;
    }

    /**
     * Login con contador de intentos: 5 fallos consecutivos bloquean 15 minutos (CA-13.1.4).
     * Usuario inexistente o dado de baja reciben el mismo mensaje (CA-13.1.3/5).
     */
    public LoginResponse login(LoginRequest req) {
        String username = req.username().trim();
        // BCrypt (costo 12) se calcula fuera de la transacción: adentro retenía una conexión del pool durante todo el
        // cálculo y con 50 ingresos simultáneos el pool se agotaba (prueba k6 en Azure, 19/11/2026).
        String hash = usuarioDao.findByUsername(username).map(Usuario::getPasswordHash).orElse(null);
        boolean coincide = encoder.matches(req.password(), hash != null ? hash : HASH_TIEMPO_CONSTANTE) && hash != null;
        // La actualización de intentos debe persistir aunque el login falle: se usa una transacción explícita.
        Resultado r = tx.execute(status -> intentar(username, hash, coincide));
        if (r.error != null) {
            throw r.error;
        }
        return r.respuesta;
    }

    /** Hash de una contraseña cualquiera: se verifica igual cuando el usuario no existe, para no delatarlo por el tiempo. */
    private static final String HASH_TIEMPO_CONSTANTE = "$2a$12$C6UzMDM.H6dfI/f/IKcEeO7FHq9aBp8.0NUIyRf4tTOR8f4CRAg8a";

    /** @param hashVerificado hash contra el que se calculó {@code coincide}; si cambió mientras tanto, cuenta como fallo. */
    private Resultado intentar(String username, String hashVerificado, boolean coincide) {
        Instant ahora = ahora();
        Usuario u = usuarioDao.findByUsername(username).orElse(null);
        if (u == null || u.getEstado() == Usuario.Estado.BAJA) {
            accesos.registrar(RegistroAcceso.Accion.LOGIN_FALLIDO, u == null ? null : u.getId(), username, null,
                    u == null ? "Usuario inexistente" : "Usuario dado de baja");
            return Resultado.error(new BusinessException(ErrorCode.CREDENCIALES_INVALIDAS, MSJ_CREDENCIALES));
        }
        String roles = u.getRoles().stream().map(Enum::name).sorted().collect(Collectors.joining(","));
        if (u.getBloqueadoHasta() != null && u.getBloqueadoHasta().isAfter(ahora)) {
            accesos.registrar(RegistroAcceso.Accion.LOGIN_FALLIDO, u.getId(), username, roles, "Cuenta bloqueada");
            return Resultado.error(bloqueado(u.getBloqueadoHasta()));
        }
        if (!coincide || !u.getPasswordHash().equals(hashVerificado)) {
            int intentos = u.getIntentosFallidos() + 1;
            u.setIntentosFallidos(intentos);
            accesos.registrar(RegistroAcceso.Accion.LOGIN_FALLIDO, u.getId(), username, roles, "Contraseña incorrecta (" + intentos + ")");
            if (intentos >= seg.maxIntentos()) {
                u.setBloqueadoHasta(ahora.plus(Duration.ofMinutes(seg.bloqueoMinutos())));
                u.setIntentosFallidos(0);
                u.setEstado(Usuario.Estado.BLOQUEADO);
                accesos.registrar(RegistroAcceso.Accion.BLOQUEO, u.getId(), username, roles,
                        "Bloqueo por " + seg.maxIntentos() + " intentos fallidos");
                return Resultado.error(bloqueado(u.getBloqueadoHasta()));
            }
            return Resultado.error(new BusinessException(ErrorCode.CREDENCIALES_INVALIDAS, MSJ_CREDENCIALES));
        }
        u.setIntentosFallidos(0);
        u.setBloqueadoHasta(null);
        u.setEstado(Usuario.Estado.ACTIVO);
        u.setUltimoAccesoAt(ahora);
        accesos.registrar(RegistroAcceso.Accion.LOGIN_OK, u.getId(), username, roles, null);
        return Resultado.ok(emitir(u));
    }

    private BusinessException bloqueado(Instant hasta) {
        return new BusinessException(ErrorCode.USUARIO_BLOQUEADO, "Tu cuenta quedó bloqueada temporalmente. Probá de nuevo en "
                + seg.bloqueoMinutos() + " minutos o usá ¿Olvidaste tu contraseña?").con("bloqueadoHasta", Fechas.offset(hasta).toString());
    }

    LoginResponse emitir(Usuario u) {
        Long pacienteId = perfiles.pacienteId(u.getId()).orElse(null);
        Long profesionalId = perfiles.profesionalId(u.getId()).orElse(null);
        JwtService.TokenEmitido t = jwt.emitir(u.getId(), u.getRoles().stream().map(Enum::name).toList(), pacienteId, profesionalId);
        return new LoginResponse(t.token(), t.expira(), perfiles.vista(u));
    }

    @Transactional
    public void logout() {
        UsuarioPrincipal p = CurrentUser.get();
        revocacion.revocar(p.jti(), ahora().plus(Duration.ofHours(24)));
        accesos.registrar(RegistroAcceso.Accion.LOGOUT, p.id(), p.username(), p.rolesTexto(), null);
    }

    @Transactional(readOnly = true)
    public UsuarioActual me() {
        UsuarioPrincipal p = CurrentUser.get();
        return perfiles.vista(usuarioDao.findById(p.id()).orElseThrow(() -> BusinessException.noEncontrado("Usuario")));
    }

    /** CU-49: verifica la contraseña actual, aplica la política y revoca las demás sesiones. */
    @Transactional
    public LoginResponse cambiarPassword(CambiarPasswordRequest req) {
        UsuarioPrincipal p = CurrentUser.get();
        Usuario u = usuarioDao.findById(p.id()).orElseThrow(() -> BusinessException.noEncontrado("Usuario"));
        if (!encoder.matches(req.actual(), u.getPasswordHash())) {
            throw BusinessException.validacion("actual", "La contraseña actual es incorrecta.");
        }
        if (req.actual().equals(req.nueva())) {
            throw new BusinessException(ErrorCode.PASSWORD_DEBIL, "La nueva contraseña debe ser distinta de la actual.");
        }
        policy.validar(req.nueva(), u.getUsername());
        u.setPasswordHash(encoder.encode(req.nueva()));
        u.setDebeCambiarPassword(false);
        u.setTokensValidosDesde(ahora());
        accesos.registrar(RegistroAcceso.Accion.PASSWORD_CAMBIADA, u.getId(), u.getUsername(), p.rolesTexto(), null);
        bitacora.ok(Modulo.SEGURIDAD, TipoEvento.CAMBIO_PASSWORD, "Usuario", u.getId(), "Cambio de contraseña");
        return emitir(u);
    }

    @Transactional
    public UsuarioActual aceptarTerminos(String version) {
        UsuarioPrincipal p = CurrentUser.get();
        String vigente = config.versionTerminosVigente();
        if (!vigente.equals(version)) {
            throw BusinessException.validacion("version", "La versión aceptada no es la vigente. Recargá la página.");
        }
        Usuario u = usuarioDao.findById(p.id()).orElseThrow(() -> BusinessException.noEncontrado("Usuario"));
        u.setTerminosVersionAceptada(vigente);
        u.setTerminosAceptadosAt(ahora());
        return perfiles.vista(u);
    }

    private Instant ahora() {
        return clock.instant().truncatedTo(ChronoUnit.SECONDS);
    }

    private record Resultado(LoginResponse respuesta, BusinessException error) {
        static Resultado ok(LoginResponse r) {
            return new Resultado(r, null);
        }

        static Resultado error(BusinessException e) {
            return new Resultado(null, e);
        }
    }
}
