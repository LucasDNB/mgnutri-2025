package ar.edu.usal.mgnutri.catalogo.service;

import ar.edu.usal.mgnutri.bitacora.domain.Modulo;
import ar.edu.usal.mgnutri.bitacora.domain.TipoEvento;
import ar.edu.usal.mgnutri.bitacora.service.Auditable;
import ar.edu.usal.mgnutri.catalogo.dao.EjercicioDao;
import ar.edu.usal.mgnutri.catalogo.dao.GrupoMuscularDao;
import ar.edu.usal.mgnutri.catalogo.domain.Ejercicio;
import ar.edu.usal.mgnutri.catalogo.domain.GrupoMuscular;
import ar.edu.usal.mgnutri.catalogo.dto.CatalogoDtos.EjercicioRequest;
import ar.edu.usal.mgnutri.catalogo.dto.CatalogoDtos.EjercicioResponse;
import ar.edu.usal.mgnutri.catalogo.dto.CatalogoDtos.GrupoMuscularResponse;
import ar.edu.usal.mgnutri.common.PageResponse;
import ar.edu.usal.mgnutri.common.Textos;
import ar.edu.usal.mgnutri.common.error.BusinessException;
import ar.edu.usal.mgnutri.common.error.ErrorCode;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** ABM de ejercicios (CU-54A/B/C). */
@Service
public class EjercicioService {

    private final EjercicioDao dao;
    private final GrupoMuscularDao grupoDao;

    public EjercicioService(EjercicioDao dao, GrupoMuscularDao grupoDao) {
        this.dao = dao;
        this.grupoDao = grupoDao;
    }

    @Transactional(readOnly = true)
    public List<GrupoMuscularResponse> grupos() {
        return grupoDao.findAllByOrderByNombreAsc().stream().map(g -> new GrupoMuscularResponse(g.getId(), g.getNombre())).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<EjercicioResponse> buscar(String q, Long grupoId, Ejercicio.Dificultad dificultad, Boolean activo,
                                                  Pageable pageable) {
        Specification<Ejercicio> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                p.add(cb.like(root.get("nombreNormalizado"), "%" + Textos.normalizar(q) + "%"));
            }
            if (grupoId != null) {
                p.add(cb.equal(root.get("grupoPrincipal").get("id"), grupoId));
            }
            if (dificultad != null) {
                p.add(cb.equal(root.get("dificultad"), dificultad));
            }
            if (activo != null) {
                p.add(cb.equal(root.get("activo"), activo));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
        return PageResponse.de(dao.findAll(spec, pageable), EjercicioService::respuesta);
    }

    @Transactional(readOnly = true)
    public EjercicioResponse obtener(Long id) {
        return respuesta(dao.findById(id).orElseThrow(() -> BusinessException.noEncontrado("Ejercicio")));
    }

    /** D-18: mismo grupo muscular principal, activos, excluye el actual, orden por cercanía de dificultad. */
    @Transactional(readOnly = true)
    public List<Ejercicio> alternativas(Ejercicio actual, int max) {
        int nivel = actual.getDificultad().nivel();
        return dao.findByActivoTrueAndGrupoPrincipalIdAndIdNot(actual.getGrupoPrincipal().getId(), actual.getId()).stream()
                .sorted(Comparator.comparingInt((Ejercicio e) -> Math.abs(e.getDificultad().nivel() - nivel)).thenComparing(Ejercicio::getNombre))
                .limit(max).toList();
    }

    @Transactional
    @Auditable(modulo = Modulo.CATALOGO, tipo = TipoEvento.ALTA, entidad = "Ejercicio", entidadId = "#result.id()",
            descripcion = "Alta de ejercicio #{#req.nombre()}")
    public EjercicioResponse crear(EjercicioRequest req) {
        Ejercicio e = new Ejercicio();
        aplicar(e, req, null);
        dao.saveAndFlush(e);
        return respuesta(e);
    }

    @Transactional
    @Auditable(modulo = Modulo.CATALOGO, tipo = TipoEvento.MODIFICACION, entidad = "Ejercicio", entidadId = "#id",
            descripcion = "Modificación de ejercicio #{#req.nombre()}")
    public EjercicioResponse modificar(Long id, EjercicioRequest req) {
        Ejercicio e = dao.findById(id).orElseThrow(() -> BusinessException.noEncontrado("Ejercicio"));
        Textos.verificarVersion(e.getVersion(), req.version());
        aplicar(e, req, id);
        dao.saveAndFlush(e);
        return respuesta(e);
    }

    @Transactional
    @Auditable(modulo = Modulo.CATALOGO, tipo = TipoEvento.BAJA, entidad = "Ejercicio", entidadId = "#id",
            descripcion = "Baja de ejercicio #{#id}")
    public void darDeBaja(Long id) {
        dao.findById(id).orElseThrow(() -> BusinessException.noEncontrado("Ejercicio")).setActivo(false);
    }

    @Transactional
    @Auditable(modulo = Modulo.CATALOGO, tipo = TipoEvento.MODIFICACION, entidad = "Ejercicio", entidadId = "#id",
            descripcion = "Reactivación de ejercicio #{#id}")
    public EjercicioResponse reactivar(Long id) {
        Ejercicio e = dao.findById(id).orElseThrow(() -> BusinessException.noEncontrado("Ejercicio"));
        e.setActivo(true);
        return respuesta(e);
    }

    private void aplicar(Ejercicio e, EjercicioRequest r, Long idActual) {
        String norm = Textos.normalizar(r.nombre());
        dao.findByNombreNormalizado(norm).filter(x -> !x.getId().equals(idActual)).ifPresent(x -> {
            throw new BusinessException(ErrorCode.DUPLICADO, "Ya existe un ejercicio con ese nombre.",
                    List.of(new BusinessException.CampoError("nombre", "Nombre repetido")));
        });
        String video = Textos.vacioANull(r.videoUrl());
        if (video != null && !urlHttpsValida(video)) {
            throw BusinessException.validacion("videoUrl", "El enlace de video debe ser una URL https:// válida.");
        }
        GrupoMuscular g = grupoDao.findById(r.grupoMuscularId())
                .orElseThrow(() -> BusinessException.validacion("grupoMuscularId", "Grupo muscular inexistente."));
        e.setNombre(r.nombre().trim());
        e.setNombreNormalizado(norm);
        e.setDescripcion(Textos.vacioANull(r.descripcion()));
        e.setGrupoPrincipal(g);
        e.getGruposSecundarios().clear();
        if (r.gruposSecundariosIds() != null) {
            grupoDao.findAllById(r.gruposSecundariosIds()).stream().filter(x -> !x.getId().equals(g.getId()))
                    .forEach(e.getGruposSecundarios()::add);
        }
        e.setDificultad(r.dificultad());
        e.setEquipamiento(Textos.vacioANull(r.equipamiento()));
        e.setVideoUrl(video);
    }

    static boolean urlHttpsValida(String url) {
        try {
            URI u = URI.create(url);
            return "https".equalsIgnoreCase(u.getScheme()) && u.getHost() != null && !u.getHost().isBlank();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static EjercicioResponse respuesta(Ejercicio e) {
        return new EjercicioResponse(e.getId(), e.getNombre(), e.getDescripcion(), e.getGrupoPrincipal().getId(),
                e.getGrupoPrincipal().getNombre(),
                e.getGruposSecundarios().stream().map(g -> new GrupoMuscularResponse(g.getId(), g.getNombre())).toList(),
                e.getDificultad().name(), e.getEquipamiento(), e.getVideoUrl(), e.isActivo(), e.getVersion());
    }
}
