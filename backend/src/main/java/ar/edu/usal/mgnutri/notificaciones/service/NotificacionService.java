package ar.edu.usal.mgnutri.notificaciones.service;

import ar.edu.usal.mgnutri.bitacora.domain.Modulo;
import ar.edu.usal.mgnutri.bitacora.domain.TipoEvento;
import ar.edu.usal.mgnutri.bitacora.service.BitacoraService;
import ar.edu.usal.mgnutri.common.Fechas;
import ar.edu.usal.mgnutri.common.PageResponse;
import ar.edu.usal.mgnutri.common.Textos;
import ar.edu.usal.mgnutri.common.error.BusinessException;
import ar.edu.usal.mgnutri.notificaciones.dao.NotificacionDao;
import ar.edu.usal.mgnutri.notificaciones.domain.Alerta;
import ar.edu.usal.mgnutri.notificaciones.domain.Notificacion;
import ar.edu.usal.mgnutri.notificaciones.domain.TipoNotificacion;
import ar.edu.usal.mgnutri.notificaciones.dto.NotificacionDtos.NotificacionResponse;
import ar.edu.usal.mgnutri.notificaciones.dto.NotificacionDtos.ResumenNotificaciones;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Outbox de notificaciones (D-35): encolado transaccional, despacho con reintentos y alertas. */
@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);
    static final int MAX_INTENTOS = 3;
    static final Duration[] BACKOFF = {Duration.ofMinutes(1), Duration.ofMinutes(5), Duration.ofMinutes(15)};

    private final NotificacionDao dao;
    private final MensajeriaPort mensajeria;
    private final AlertaService alertas;
    private final BitacoraService bitacora;
    private final ObjectMapper mapper;
    private final Clock clock;
    private final Optional<TelefonoUsuarioResolver> resolver;

    public NotificacionService(NotificacionDao dao, MensajeriaPort mensajeria, AlertaService alertas,
                               BitacoraService bitacora, ObjectMapper mapper, Clock clock,
                               Optional<TelefonoUsuarioResolver> resolver) {
        this.dao = dao;
        this.mensajeria = mensajeria;
        this.alertas = alertas;
        this.bitacora = bitacora;
        this.mapper = mapper;
        this.clock = clock;
        this.resolver = resolver;
    }

    /**
     * Inserta la notificación dentro de la transacción del evento de negocio. Si el destinatario no
     * tiene un WhatsApp válido queda OMITIDA y se avisa al administrador (CA-11.5.1).
     */
    @Transactional
    public Notificacion encolar(TipoNotificacion tipo, Long usuarioId, String destinatario, String telefono,
                                List<String> parametros, String refTipo, Long refId) {
        Instant ahora = ahora();
        Notificacion n = new Notificacion(tipo, usuarioId, destinatario, telefono, json(parametros), refTipo, refId, ahora);
        if (!Textos.telefonoValido(telefono)) {
            n.setEstado(Notificacion.Estado.OMITIDA);
            n.setError("El destinatario no tiene un número de WhatsApp válido registrado");
            n.setParametros("[\"***\"]");
            n.setProximoIntentoAt(null);
            dao.save(n);
            alertas.paraAdmins(Alerta.Tipo.NOTIFICACION_FALLIDA, "No se envió " + tipo.name() + " a " + destinatario
                    + ": no tiene WhatsApp válido registrado.", "Notificacion", n.getId());
            bitacora.ok(Modulo.NOTIFICACIONES, TipoEvento.NOTIFICACION, "Notificacion", n.getId(),
                    tipo.name() + " omitida (sin teléfono válido)");
            return n;
        }
        return dao.save(n);
    }

    /** Despacha hasta 50 notificaciones pendientes (job cada minuto). */
    @Transactional
    public int despacharPendientes() {
        List<Notificacion> pendientes = dao.tomarPendientes(ahora());
        for (Notificacion n : pendientes) {
            despachar(n);
        }
        return pendientes.size();
    }

    void despachar(Notificacion n) {
        List<String> params = parametros(n.getParametros());
        MensajeriaPort.ResultadoEnvio r;
        try {
            r = mensajeria.enviar(n.getPlantilla(), n.getTelefono(), params);
        } catch (RuntimeException e) {
            r = MensajeriaPort.ResultadoEnvio.falla(true, e.getMessage());
        }
        n.setIntentos(n.getIntentos() + 1);
        if (r.ok()) {
            n.setEstado(Notificacion.Estado.ENVIADA);
            n.setWaMessageId(r.messageId());
            n.setEnviadaAt(ahora());
            n.setError(null);
            n.setParametros("[\"***\"]");
            n.setProximoIntentoAt(null);
            bitacora.ok(Modulo.NOTIFICACIONES, TipoEvento.NOTIFICACION, "Notificacion", n.getId(),
                    n.getTipo().name() + " enviada a " + n.getDestinatario());
            return;
        }
        n.setError(r.error());
        if (!r.reintentable() || n.getIntentos() >= MAX_INTENTOS) {
            n.setEstado(Notificacion.Estado.FALLIDA);
            n.setParametros("[\"***\"]");
            n.setProximoIntentoAt(null);
            alertas.paraAdmins(Alerta.Tipo.NOTIFICACION_FALLIDA, "Falló el envío de " + n.getTipo().name() + " a "
                    + n.getDestinatario() + ". Revisá Notificaciones para reintentar.", "Notificacion", n.getId());
            bitacora.registrar(Modulo.NOTIFICACIONES, TipoEvento.NOTIFICACION, "Notificacion", n.getId(),
                    n.getTipo().name() + " fallida", ar.edu.usal.mgnutri.bitacora.domain.BitacoraEvento.Resultado.ERROR, null);
            log.warn("Notificación {} fallida: {}", n.getId(), r.error());
        } else {
            n.setProximoIntentoAt(ahora().plus(BACKOFF[Math.min(n.getIntentos() - 1, BACKOFF.length - 1)]));
        }
    }

    /**
     * Reintento manual (CA-11.6.1): relee el teléfono actual del usuario y vuelve a PENDIENTE.
     * Las credenciales no se pueden reenviar (la contraseña ya fue borrada): se usa "Reenviar credenciales".
     */
    @Transactional
    public NotificacionResponse reintentar(Long id) {
        Notificacion n = dao.findById(id).orElseThrow(() -> BusinessException.noEncontrado("Notificación"));
        if (n.getEstado() != Notificacion.Estado.FALLIDA && n.getEstado() != Notificacion.Estado.OMITIDA) {
            throw new BusinessException(ar.edu.usal.mgnutri.common.error.ErrorCode.REGLA_NEGOCIO,
                    "Sólo se pueden reintentar notificaciones fallidas u omitidas.");
        }
        if ("[\"***\"]".equals(n.getParametros())) {
            throw new BusinessException(ar.edu.usal.mgnutri.common.error.ErrorCode.REGLA_NEGOCIO,
                    "El contenido del mensaje ya no está disponible por seguridad. "
                            + (n.getTipo() == TipoNotificacion.CREDENCIALES ? "Usá «Reenviar credenciales» en la ficha."
                            : "Generá nuevamente la acción que lo originó."));
        }
        if (n.getUsuarioId() != null) {
            resolver.flatMap(r -> r.telefonoDe(n.getUsuarioId())).ifPresent(n::setTelefono);
        }
        if (!Textos.telefonoValido(n.getTelefono())) {
            throw BusinessException.validacion("telefono", "El destinatario sigue sin un WhatsApp válido. Corregí el teléfono primero.");
        }
        n.setEstado(Notificacion.Estado.PENDIENTE);
        n.setIntentos(0);
        n.setError(null);
        n.setProximoIntentoAt(ahora());
        bitacora.ok(Modulo.NOTIFICACIONES, TipoEvento.NOTIFICACION, "Notificacion", n.getId(), "Reintento manual de " + n.getTipo());
        return aDto(n);
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificacionResponse> buscar(String tipo, String estado, LocalDate desde, LocalDate hasta, Pageable p) {
        Specification<Notificacion> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (tipo != null && !tipo.isBlank()) {
                ps.add(cb.equal(root.get("tipo"), TipoNotificacion.valueOf(tipo)));
            }
            if (estado != null && !estado.isBlank()) {
                ps.add(cb.equal(root.get("estado"), Notificacion.Estado.valueOf(estado)));
            }
            if (desde != null) {
                ps.add(cb.greaterThanOrEqualTo(root.get("createdAt"), Fechas.inicioDelDia(desde)));
            }
            if (hasta != null) {
                ps.add(cb.lessThan(root.get("createdAt"), Fechas.inicioDelDia(hasta.plusDays(1))));
            }
            return cb.and(ps.toArray(Predicate[]::new));
        };
        return PageResponse.de(dao.findAll(spec, p), NotificacionService::aDto);
    }

    @Transactional(readOnly = true)
    public NotificacionResponse obtener(Long id) {
        return dao.findById(id).map(NotificacionService::aDto).orElseThrow(() -> BusinessException.noEncontrado("Notificación"));
    }

    @Transactional(readOnly = true)
    public ResumenNotificaciones resumen() {
        return new ResumenNotificaciones(dao.countByEstado(Notificacion.Estado.PENDIENTE),
                dao.countByEstado(Notificacion.Estado.FALLIDA), dao.countByEstado(Notificacion.Estado.OMITIDA));
    }

    @Transactional(readOnly = true)
    public boolean recibioRecientemente(Long usuarioId, TipoNotificacion tipo, Duration ventana) {
        return dao.existsByUsuarioIdAndTipoAndCreatedAtAfterAndEstadoIn(usuarioId, tipo, ahora().minus(ventana),
                List.of(Notificacion.Estado.PENDIENTE, Notificacion.Estado.ENVIADA));
    }

    static NotificacionResponse aDto(Notificacion n) {
        String tel = n.getTelefono() == null ? null : "••" + Textos.ultimosDigitos(n.getTelefono(), 4);
        return new NotificacionResponse(n.getId(), n.getTipo().name(), n.getUsuarioId(), n.getDestinatario(), tel,
                n.getPlantilla(), n.getReferenciaTipo(), n.getReferenciaId(), n.getEstado().name(), n.getIntentos(),
                n.getProximoIntentoAt(), n.getError(), n.getWaMessageId(), n.getCreatedAt(), n.getEnviadaAt());
    }

    private String json(List<String> parametros) {
        try {
            return mapper.writeValueAsString(parametros);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private List<String> parametros(String json) {
        try {
            return json == null ? List.of() : mapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private Instant ahora() {
        return clock.instant().truncatedTo(ChronoUnit.SECONDS);
    }
}
