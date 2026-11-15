package ar.edu.usal.mgnutri.catalogo.service;

import ar.edu.usal.mgnutri.bitacora.domain.Modulo;
import ar.edu.usal.mgnutri.bitacora.domain.TipoEvento;
import ar.edu.usal.mgnutri.bitacora.service.Auditable;
import ar.edu.usal.mgnutri.catalogo.dao.AlimentoDao;
import ar.edu.usal.mgnutri.catalogo.dao.EquivalenciaManualDao;
import ar.edu.usal.mgnutri.catalogo.dao.GrupoAlimentoDao;
import ar.edu.usal.mgnutri.catalogo.domain.Alimento;
import ar.edu.usal.mgnutri.catalogo.domain.EquivalenciaManual;
import ar.edu.usal.mgnutri.catalogo.domain.GrupoAlimento;
import ar.edu.usal.mgnutri.catalogo.dto.CatalogoDtos.AlimentoRequest;
import ar.edu.usal.mgnutri.catalogo.dto.CatalogoDtos.AlimentoResponse;
import ar.edu.usal.mgnutri.catalogo.dto.CatalogoDtos.EquivalenciaDto;
import ar.edu.usal.mgnutri.catalogo.dto.CatalogoDtos.GrupoAlimentoResponse;
import ar.edu.usal.mgnutri.common.PageResponse;
import ar.edu.usal.mgnutri.common.Textos;
import ar.edu.usal.mgnutri.common.error.BusinessException;
import ar.edu.usal.mgnutri.common.error.ErrorCode;
import jakarta.persistence.criteria.Predicate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/** ABM de alimentos y equivalencias manuales (CU-53A/B/C). */
@Service
public class AlimentoService {

    private final AlimentoDao dao;
    private final GrupoAlimentoDao grupoDao;
    private final EquivalenciaManualDao equivDao;

    public AlimentoService(AlimentoDao dao, GrupoAlimentoDao grupoDao, EquivalenciaManualDao equivDao) {
        this.dao = dao;
        this.grupoDao = grupoDao;
        this.equivDao = equivDao;
    }

    @Transactional(readOnly = true)
    public List<GrupoAlimentoResponse> grupos() {
        return grupoDao.findAllByOrderByNombreAsc().stream()
                .map(g -> new GrupoAlimentoResponse(g.getId(), g.getNombre(), g.getMacroReferencia().name(), g.getDescripcion()))
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<AlimentoResponse> buscar(String q, Long grupoId, Boolean activo, String aptoPara, Pageable pageable) {
        Specification<Alimento> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                p.add(cb.like(root.get("nombreNormalizado"), "%" + Textos.normalizar(q) + "%"));
            }
            if (grupoId != null) {
                p.add(cb.equal(root.get("grupo").get("id"), grupoId));
            }
            if (activo != null) {
                p.add(cb.equal(root.get("activo"), activo));
            }
            if (aptoPara != null && !aptoPara.isBlank()) {
                String campo = switch (aptoPara.toUpperCase()) {
                    case "DESAYUNO", "MERIENDA" -> "aptoDesayuno";
                    case "COLACION", "COLACION_MANANA", "COLACION_TARDE" -> "aptoColacion";
                    default -> "aptoAlmuerzo";
                };
                p.add(cb.isTrue(root.get(campo)));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
        return PageResponse.de(dao.findAll(spec, pageable), a -> respuesta(a, false));
    }

    @Transactional(readOnly = true)
    public AlimentoResponse obtener(Long id) {
        return respuesta(dao.findById(id).orElseThrow(() -> BusinessException.noEncontrado("Alimento")), true);
    }

    /** Catálogo activo cacheado para el generador y las equivalencias. */
    @Cacheable("catalogo-alimentos")
    @Transactional(readOnly = true)
    public List<Alimento> activos() {
        return dao.findByActivoTrue();
    }

    @Transactional
    @CacheEvict(value = "catalogo-alimentos", allEntries = true)
    @Auditable(modulo = Modulo.CATALOGO, tipo = TipoEvento.ALTA, entidad = "Alimento", entidadId = "#result.id()",
            descripcion = "Alta de alimento #{#req.nombre()}")
    public AlimentoResponse crear(AlimentoRequest req) {
        validar(req, null);
        Alimento a = new Alimento();
        aplicar(a, req);
        dao.saveAndFlush(a);
        guardarEquivalencias(a.getId(), req.equivalencias());
        return respuesta(a, true);
    }

    @Transactional
    @CacheEvict(value = "catalogo-alimentos", allEntries = true)
    @Auditable(modulo = Modulo.CATALOGO, tipo = TipoEvento.MODIFICACION, entidad = "Alimento", entidadId = "#id",
            descripcion = "Modificación de alimento #{#req.nombre()}")
    public AlimentoResponse modificar(Long id, AlimentoRequest req) {
        Alimento a = dao.findById(id).orElseThrow(() -> BusinessException.noEncontrado("Alimento"));
        Textos.verificarVersion(a.getVersion(), req.version());
        validar(req, id);
        aplicar(a, req);
        dao.saveAndFlush(a);
        if (req.equivalencias() != null) {
            equivDao.deleteByAlimentoId(id);
            equivDao.flush();
            guardarEquivalencias(id, req.equivalencias());
        }
        return respuesta(a, true);
    }

    @Transactional
    @CacheEvict(value = "catalogo-alimentos", allEntries = true)
    @Auditable(modulo = Modulo.CATALOGO, tipo = TipoEvento.BAJA, entidad = "Alimento", entidadId = "#id",
            descripcion = "Baja de alimento #{#id}")
    public void darDeBaja(Long id) {
        dao.findById(id).orElseThrow(() -> BusinessException.noEncontrado("Alimento")).setActivo(false);
    }

    @Transactional
    @CacheEvict(value = "catalogo-alimentos", allEntries = true)
    @Auditable(modulo = Modulo.CATALOGO, tipo = TipoEvento.MODIFICACION, entidad = "Alimento", entidadId = "#id",
            descripcion = "Reactivación de alimento #{#id}")
    public AlimentoResponse reactivar(Long id) {
        Alimento a = dao.findById(id).orElseThrow(() -> BusinessException.noEncontrado("Alimento"));
        a.setActivo(true);
        return respuesta(a, true);
    }

    /** CA-10.1.4 / CA-10.1.5: rangos, suma de macros, coherencia de kcal (advertencia) y nombre único. */
    void validar(AlimentoRequest r, Long idActual) {
        BigDecimal fibra = r.fibra() == null ? BigDecimal.ZERO : r.fibra();
        BigDecimal suma = r.hc().add(r.proteinas()).add(r.grasas()).add(fibra);
        if (suma.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw BusinessException.validacion("hc", "La suma de HC, proteínas, grasas y fibra no puede superar 100 g cada 100 g.");
        }
        String norm = Textos.normalizar(r.nombre());
        dao.findByNombreNormalizado(norm).filter(a -> !a.getId().equals(idActual)).ifPresent(a -> {
            throw new BusinessException(ErrorCode.DUPLICADO, "Ya existe un alimento con ese nombre.",
                    List.of(new BusinessException.CampoError("nombre", "Nombre repetido")));
        });
        double calc = kcalCalculadas(r.hc(), r.proteinas(), r.grasas());
        double kcal = r.kcal().doubleValue();
        boolean incoherente = kcal > 0 ? Math.abs(calc - kcal) / kcal > 0.15 : calc > 5;
        if (incoherente && !Boolean.TRUE.equals(r.confirmarAdvertencias())) {
            throw new BusinessException(ErrorCode.KCAL_INCOHERENTES, "Las calorías no coinciden con los macronutrientes ("
                    + Math.round(calc) + " kcal calculadas). Revisá los valores o confirmá para guardar igual.")
                    .con("kcalCalculadas", Math.round(calc));
        }
    }

    public static double kcalCalculadas(BigDecimal hc, BigDecimal prot, BigDecimal grasas) {
        return 4 * hc.doubleValue() + 4 * prot.doubleValue() + 9 * grasas.doubleValue();
    }

    private void aplicar(Alimento a, AlimentoRequest r) {
        GrupoAlimento g = grupoDao.findById(r.grupoId()).orElseThrow(() -> BusinessException.validacion("grupoId", "Grupo inexistente."));
        a.setNombre(r.nombre().trim());
        a.setNombreNormalizado(Textos.normalizar(r.nombre()));
        a.setGrupo(g);
        a.setKcal(r.kcal().setScale(2, RoundingMode.HALF_EVEN));
        a.setHc(r.hc());
        a.setProteinas(r.proteinas());
        a.setGrasas(r.grasas());
        a.setFibra(r.fibra() == null ? BigDecimal.ZERO : r.fibra());
        a.setSodioMg(r.sodioMg());
        a.setUnidadCasera(Textos.vacioANull(r.unidadCasera()));
        a.setGramosUnidadCasera(r.gramosUnidadCasera());
        a.setAptoDesayuno(r.aptoDesayuno());
        a.setAptoColacion(r.aptoColacion());
        a.setAptoAlmuerzo(r.aptoAlmuerzo());
    }

    private void guardarEquivalencias(Long alimentoId, List<EquivalenciaDto> equivalencias) {
        if (equivalencias == null) {
            return;
        }
        for (EquivalenciaDto e : equivalencias) {
            if (e.alimentoEquivalenteId().equals(alimentoId)) {
                throw BusinessException.validacion("equivalencias", "Un alimento no puede ser equivalente de sí mismo.");
            }
            if (!dao.existsById(e.alimentoEquivalenteId())) {
                throw BusinessException.validacion("equivalencias", "Alimento equivalente inexistente.");
            }
            equivDao.save(new EquivalenciaManual(alimentoId, e.alimentoEquivalenteId(), e.gramosOrigen(), e.gramosEquivalente(),
                    Textos.vacioANull(e.nota())));
        }
    }

    AlimentoResponse respuesta(Alimento a, boolean conEquivalencias) {
        List<EquivalenciaDto> eq = conEquivalencias ? equivDao.findByAlimentoId(a.getId()).stream()
                .map(e -> new EquivalenciaDto(e.getAlimentoEquivalenteId(),
                        dao.findById(e.getAlimentoEquivalenteId()).map(Alimento::getNombre).orElse(""),
                        e.getGramosOrigen(), e.getGramosEquivalente(), e.getNota())).toList() : null;
        return new AlimentoResponse(a.getId(), a.getNombre(), a.getGrupo().getId(), a.getGrupo().getNombre(),
                a.getGrupo().getMacroReferencia().name(), a.getKcal(), a.getHc(), a.getProteinas(), a.getGrasas(), a.getFibra(),
                a.getSodioMg(), a.getUnidadCasera(), a.getGramosUnidadCasera(), a.isAptoDesayuno(), a.isAptoColacion(),
                a.isAptoAlmuerzo(), a.isActivo(), eq, a.getVersion());
    }
}
