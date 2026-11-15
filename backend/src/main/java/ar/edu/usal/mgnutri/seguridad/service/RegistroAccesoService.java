package ar.edu.usal.mgnutri.seguridad.service;

import ar.edu.usal.mgnutri.common.Fechas;
import ar.edu.usal.mgnutri.common.PageResponse;
import ar.edu.usal.mgnutri.common.security.CurrentUser;
import ar.edu.usal.mgnutri.seguridad.dao.RegistroAccesoDao;
import ar.edu.usal.mgnutri.seguridad.domain.RegistroAcceso;
import ar.edu.usal.mgnutri.seguridad.dto.SeguridadDtos.AccesoResponse;
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
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/** Registro de accesos al sistema (RF-16), escrito en transacción independiente. */
@Service
public class RegistroAccesoService {

    private static final Logger log = LoggerFactory.getLogger(RegistroAccesoService.class);
    private final RegistroAccesoDao dao;
    private final Clock clock;

    public RegistroAccesoService(RegistroAccesoDao dao, Clock clock) {
        this.dao = dao;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(RegistroAcceso.Accion accion, Long usuarioId, String username, String roles, String detalle) {
        try {
            dao.save(new RegistroAcceso(clock.instant().truncatedTo(ChronoUnit.SECONDS), usuarioId,
                    username == null ? null : username.substring(0, Math.min(40, username.length())), roles,
                    CurrentUser.ip(), CurrentUser.userAgent(), accion, detalle));
        } catch (RuntimeException e) {
            log.error("No se pudo registrar el acceso {}: {}", accion, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<AccesoResponse> buscar(String usuario, String accion, LocalDate desde, LocalDate hasta, String ip,
                                               Pageable pageable) {
        return PageResponse.de(dao.findAll(spec(usuario, accion, desde, hasta, ip), pageable), RegistroAccesoService::aDto);
    }

    @Transactional(readOnly = true)
    public List<AccesoResponse> exportar(String usuario, String accion, LocalDate desde, LocalDate hasta, String ip) {
        return dao.findAll(spec(usuario, accion, desde, hasta, ip), Sort.by(Sort.Direction.DESC, "fechaHora")).stream()
                .limit(50_000).map(RegistroAccesoService::aDto).toList();
    }

    private static Specification<RegistroAcceso> spec(String usuario, String accion, LocalDate desde, LocalDate hasta, String ip) {
        return (root, q, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (usuario != null && !usuario.isBlank()) {
                p.add(cb.like(cb.lower(root.get("usernameIntentado")), "%" + usuario.toLowerCase() + "%"));
            }
            if (accion != null && !accion.isBlank()) {
                p.add(cb.equal(root.get("accion"), RegistroAcceso.Accion.valueOf(accion)));
            }
            if (desde != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("fechaHora"), Fechas.inicioDelDia(desde)));
            }
            if (hasta != null) {
                p.add(cb.lessThan(root.get("fechaHora"), Fechas.inicioDelDia(hasta.plusDays(1))));
            }
            if (ip != null && !ip.isBlank()) {
                p.add(cb.like(root.get("ip"), "%" + ip + "%"));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }

    static AccesoResponse aDto(RegistroAcceso r) {
        return new AccesoResponse(r.getId(), r.getFechaHora(), r.getUsuarioId(), r.getUsernameIntentado(), r.getRoles(),
                r.getIp(), r.getUserAgent(), r.getAccion().name(), r.getDetalle());
    }
}
