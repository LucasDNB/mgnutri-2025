package ar.edu.usal.mgnutri.bitacora.service;

import ar.edu.usal.mgnutri.bitacora.dao.BitacoraDao;
import ar.edu.usal.mgnutri.bitacora.domain.BitacoraEvento;
import ar.edu.usal.mgnutri.bitacora.domain.Modulo;
import ar.edu.usal.mgnutri.bitacora.domain.TipoEvento;
import ar.edu.usal.mgnutri.bitacora.dto.BitacoraDtos.EventoResponse;
import ar.edu.usal.mgnutri.bitacora.dto.BitacoraDtos.Filtro;
import ar.edu.usal.mgnutri.common.Fechas;
import ar.edu.usal.mgnutri.common.PageResponse;
import ar.edu.usal.mgnutri.common.security.CurrentUser;
import ar.edu.usal.mgnutri.common.security.UsuarioPrincipal;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Registro y consulta de la bitácora (CU-35 / CU-36). */
@Service
public class BitacoraService {

    private static final Logger log = LoggerFactory.getLogger(BitacoraService.class);

    private final BitacoraDao dao;
    private final Clock clock;
    private final ObjectMapper mapper;

    public BitacoraService(BitacoraDao dao, Clock clock, ObjectMapper mapper) {
        this.dao = dao;
        this.clock = clock;
        this.mapper = mapper;
    }

    /**
     * Registra un evento en una transacción independiente. Un fallo al escribir se loguea pero no
     * interrumpe la operación del usuario (CA-14.1.3).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(Modulo modulo, TipoEvento tipo, String entidad, Object entidadId, String descripcion,
                          BitacoraEvento.Resultado resultado, Map<String, Object> detalle) {
        try {
            Optional<UsuarioPrincipal> usuario = CurrentUser.opcional();
            String detalleJson = detalle == null || detalle.isEmpty() ? null : mapper.writeValueAsString(detalle);
            dao.save(new BitacoraEvento(clock.instant().truncatedTo(ChronoUnit.SECONDS),
                    usuario.map(UsuarioPrincipal::id).orElse(null),
                    usuario.map(UsuarioPrincipal::username).orElse("SISTEMA"),
                    usuario.map(UsuarioPrincipal::rolesTexto).orElse(null),
                    modulo, tipo, entidad, entidadId == null ? null : String.valueOf(entidadId), descripcion, resultado,
                    CurrentUser.ip(), detalleJson));
        } catch (JsonProcessingException | RuntimeException e) {
            log.error("No se pudo registrar el evento de bitácora {}/{} {}: {}", modulo, tipo, entidad, e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ok(Modulo modulo, TipoEvento tipo, String entidad, Object entidadId, String descripcion) {
        registrar(modulo, tipo, entidad, entidadId, descripcion, BitacoraEvento.Resultado.OK, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ok(Modulo modulo, TipoEvento tipo, String entidad, Object entidadId, String descripcion,
                   Map<String, Object> detalle) {
        registrar(modulo, tipo, entidad, entidadId, descripcion, BitacoraEvento.Resultado.OK, detalle);
    }

    @Transactional(readOnly = true)
    public PageResponse<EventoResponse> buscar(Filtro f, Pageable pageable) {
        return PageResponse.de(dao.findAll(especificacion(f), pageable), BitacoraService::aDto);
    }

    @Transactional(readOnly = true)
    public List<EventoResponse> exportar(Filtro f) {
        return dao.findAll(especificacion(f), Sort.by(Sort.Direction.DESC, "fechaHora")).stream()
                .limit(50_000).map(BitacoraService::aDto).toList();
    }

    @Transactional(readOnly = true)
    public Optional<EventoResponse> obtener(Long id) {
        return dao.findById(id).map(BitacoraService::aDto);
    }

    static Specification<BitacoraEvento> especificacion(Filtro f) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (f.desde() != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("fechaHora"), Fechas.inicioDelDia(f.desde())));
            }
            if (f.hasta() != null) {
                p.add(cb.lessThan(root.get("fechaHora"), Fechas.inicioDelDia(f.hasta().plusDays(1))));
            }
            if (f.usuarioId() != null) {
                p.add(cb.equal(root.get("usuarioId"), f.usuarioId()));
            }
            if (f.usuario() != null && !f.usuario().isBlank()) {
                p.add(cb.like(cb.lower(root.get("username")), "%" + f.usuario().toLowerCase() + "%"));
            }
            if (f.modulo() != null) {
                p.add(cb.equal(root.get("modulo"), f.modulo()));
            }
            if (f.tipo() != null) {
                p.add(cb.equal(root.get("tipoEvento"), f.tipo()));
            }
            if (f.entidad() != null && !f.entidad().isBlank()) {
                p.add(cb.equal(root.get("entidad"), f.entidad()));
            }
            if (f.entidadId() != null && !f.entidadId().isBlank()) {
                p.add(cb.equal(root.get("entidadId"), f.entidadId()));
            }
            if (f.resultado() != null) {
                p.add(cb.equal(root.get("resultado"), f.resultado()));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }

    static EventoResponse aDto(BitacoraEvento e) {
        return new EventoResponse(e.getId(), e.getFechaHora(), e.getUsuarioId(), e.getUsername(), e.getRoles(),
                e.getModulo(), e.getTipoEvento(), e.getEntidad(), e.getEntidadId(), e.getDescripcion(),
                e.getResultado(), e.getIp(), e.getDetalle());
    }
}
