package ar.edu.usal.mgnutri.seguridad.service;

import ar.edu.usal.mgnutri.bitacora.domain.Modulo;
import ar.edu.usal.mgnutri.bitacora.domain.TipoEvento;
import ar.edu.usal.mgnutri.bitacora.service.BitacoraService;
import ar.edu.usal.mgnutri.common.Textos;
import ar.edu.usal.mgnutri.common.error.BusinessException;
import ar.edu.usal.mgnutri.common.error.ErrorCode;
import ar.edu.usal.mgnutri.config.MgnutriProperties;
import ar.edu.usal.mgnutri.configuracion.service.ConfiguracionService;
import ar.edu.usal.mgnutri.notificaciones.service.Notificador;
import ar.edu.usal.mgnutri.pacientes.dao.PacienteDao;
import ar.edu.usal.mgnutri.pacientes.domain.Paciente;
import ar.edu.usal.mgnutri.profesionales.dao.ProfesionalDao;
import ar.edu.usal.mgnutri.profesionales.domain.Profesional;
import ar.edu.usal.mgnutri.seguridad.dao.TokenRecuperoDao;
import ar.edu.usal.mgnutri.seguridad.dao.UsuarioDao;
import ar.edu.usal.mgnutri.seguridad.domain.RegistroAcceso;
import ar.edu.usal.mgnutri.seguridad.domain.TokenRecupero;
import ar.edu.usal.mgnutri.seguridad.domain.Usuario;
import ar.edu.usal.mgnutri.seguridad.dto.SeguridadDtos.RecuperoResponse;
import ar.edu.usal.mgnutri.seguridad.dto.SeguridadDtos.TokenValidoResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/** Recupero de contraseña por WhatsApp con token de un solo uso (CU-48, D-26). */
@Service
public class RecuperoService {

    private final UsuarioDao usuarioDao;
    private final TokenRecuperoDao tokenDao;
    private final PacienteDao pacienteDao;
    private final ProfesionalDao profesionalDao;
    private final Notificador notificador;
    private final RegistroAccesoService accesos;
    private final BitacoraService bitacora;
    private final PasswordEncoder encoder;
    private final PasswordPolicy policy;
    private final ConfiguracionService config;
    private final MgnutriProperties.Seguridad seg;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public RecuperoService(UsuarioDao usuarioDao, TokenRecuperoDao tokenDao, PacienteDao pacienteDao,
                           ProfesionalDao profesionalDao, Notificador notificador, RegistroAccesoService accesos,
                           BitacoraService bitacora, PasswordEncoder encoder, PasswordPolicy policy,
                           ConfiguracionService config, MgnutriProperties props, Clock clock) {
        this.usuarioDao = usuarioDao;
        this.tokenDao = tokenDao;
        this.pacienteDao = pacienteDao;
        this.profesionalDao = profesionalDao;
        this.notificador = notificador;
        this.accesos = accesos;
        this.bitacora = bitacora;
        this.encoder = encoder;
        this.policy = policy;
        this.config = config;
        this.seg = props.seguridad();
        this.clock = clock;
    }

    @Transactional
    public RecuperoResponse solicitar(String documentoOTelefono) {
        String dato = documentoOTelefono.trim();
        Optional<Destinatario> dest = buscar(dato);
        if (dest.isEmpty()) {
            accesos.registrar(RegistroAcceso.Accion.RECUPERO_SOLICITADO, null, dato, null, "Dato inexistente");
            if (seg.recuperoMensajeNeutro()) {
                return new RecuperoResponse("Si el dato corresponde a un usuario, te enviamos un enlace a tu WhatsApp registrado.");
            }
            throw new BusinessException(ErrorCode.NO_ENCONTRADO, "No encontramos un usuario con ese dato");
        }
        Destinatario d = dest.get();
        Instant ahora = ahora();
        if (tokenDao.countByUsuarioIdAndCreatedAtAfter(d.usuario.getId(), ahora.minus(Duration.ofHours(1))) >= seg.recuperoMaxPorHora()) {
            throw new BusinessException(ErrorCode.DEMASIADAS_SOLICITUDES,
                    "Ya pediste varios enlaces en la última hora. Esperá un rato e intentá de nuevo.");
        }
        if (!Textos.telefonoValido(d.telefono)) {
            throw new BusinessException(ErrorCode.REGLA_NEGOCIO,
                    "Tu usuario no tiene un WhatsApp registrado. Comunicate con la central para recuperar el acceso.");
        }
        String token = nuevoToken();
        tokenDao.save(new TokenRecupero(d.usuario.getId(), sha256(token), ahora.plus(Duration.ofHours(seg.recuperoHoras())),
                "WHATSAPP", ahora));
        String enlace = config.valor("centro.url_sistema", "") + "/recupero-confirmar.html?token=" + token;
        notificador.recuperoPassword(d.usuario.getId(), d.nombre, d.telefono, enlace);
        accesos.registrar(RegistroAcceso.Accion.RECUPERO_SOLICITADO, d.usuario.getId(), d.usuario.getUsername(), null, null);
        return new RecuperoResponse("Te enviamos un enlace a tu WhatsApp registrado terminado en ••"
                + Textos.ultimosDigitos(d.telefono, 2));
    }

    /** Genera un token de activación de un solo uso (modo ENLACE de credenciales). */
    @Transactional
    public String tokenActivacion(Long usuarioId) {
        String token = nuevoToken();
        Instant ahora = ahora();
        tokenDao.save(new TokenRecupero(usuarioId, sha256(token), ahora.plus(Duration.ofHours(72)), "ACTIVACION", ahora));
        return config.valor("centro.url_sistema", "") + "/recupero-confirmar.html?token=" + token;
    }

    @Transactional(readOnly = true)
    public TokenValidoResponse validar(String token) {
        return tokenVigente(token).map(t -> new TokenValidoResponse(true,
                        usuarioDao.findById(t.getUsuarioId()).map(Usuario::getUsername).orElse(null)))
                .orElse(new TokenValidoResponse(false, null));
    }

    @Transactional
    public void confirmar(String token, String nueva) {
        TokenRecupero t = tokenVigente(token).orElseThrow(() ->
                new BusinessException(ErrorCode.TOKEN_EXPIRADO, "El enlace ya no es válido. Pedí uno nuevo."));
        Usuario u = usuarioDao.findById(t.getUsuarioId()).orElseThrow(() -> BusinessException.noEncontrado("Usuario"));
        if (u.getEstado() == Usuario.Estado.BAJA) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRADO, "El enlace ya no es válido. Pedí uno nuevo.");
        }
        policy.validar(nueva, u.getUsername());
        Instant ahora = ahora();
        u.setPasswordHash(encoder.encode(nueva));
        u.setDebeCambiarPassword(false);
        u.setIntentosFallidos(0);
        u.setBloqueadoHasta(null);
        u.setEstado(Usuario.Estado.ACTIVO);
        u.setTokensValidosDesde(ahora.plusSeconds(1));
        t.setUsadoAt(ahora);
        tokenDao.findByUsuarioIdAndUsadoAtIsNull(u.getId()).forEach(otro -> otro.setUsadoAt(ahora));
        accesos.registrar(RegistroAcceso.Accion.PASSWORD_RESTABLECIDA, u.getId(), u.getUsername(), null, null);
        bitacora.ok(Modulo.SEGURIDAD, TipoEvento.CAMBIO_PASSWORD, "Usuario", u.getId(), "Contraseña restablecida por recupero");
    }

    private Optional<TokenRecupero> tokenVigente(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return tokenDao.findByTokenHash(sha256(token))
                .filter(t -> t.getUsadoAt() == null && t.getExpiraAt().isAfter(clock.instant()));
    }

    private Optional<Destinatario> buscar(String dato) {
        Optional<Usuario> porDoc = usuarioDao.findByUsername(dato).filter(u -> u.getEstado() != Usuario.Estado.BAJA);
        if (porDoc.isPresent()) {
            return destinatario(porDoc.get());
        }
        String tel = Textos.telefonoE164(dato);
        if (tel == null) {
            return Optional.empty();
        }
        for (Paciente p : pacienteDao.findByEstado(Paciente.Estado.ACTIVO)) {
            if (tel.equals(p.getTelefonoWhatsapp())) {
                return usuarioDao.findById(p.getUsuarioId()).flatMap(this::destinatario);
            }
        }
        for (Profesional p : profesionalDao.findByEstado(Profesional.Estado.ACTIVO)) {
            if (tel.equals(p.getTelefonoWhatsapp())) {
                return usuarioDao.findById(p.getUsuarioId()).flatMap(this::destinatario);
            }
        }
        return Optional.empty();
    }

    private Optional<Destinatario> destinatario(Usuario u) {
        Optional<Paciente> pac = pacienteDao.findByUsuarioId(u.getId());
        if (pac.isPresent()) {
            return Optional.of(new Destinatario(u, pac.get().getNombre(), pac.get().getTelefonoWhatsapp()));
        }
        Optional<Profesional> prof = profesionalDao.findByUsuarioId(u.getId());
        if (prof.isPresent()) {
            return Optional.of(new Destinatario(u, prof.get().getNombre(), prof.get().getTelefonoWhatsapp()));
        }
        return Optional.of(new Destinatario(u, u.getUsername(), null));
    }

    private String nuevoToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String sha256(String texto) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private Instant ahora() {
        return clock.instant().truncatedTo(ChronoUnit.SECONDS);
    }

    private record Destinatario(Usuario usuario, String nombre, String telefono) {}
}
