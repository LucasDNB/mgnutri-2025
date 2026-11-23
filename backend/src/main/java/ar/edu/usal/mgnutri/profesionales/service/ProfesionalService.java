package ar.edu.usal.mgnutri.profesionales.service;

import ar.edu.usal.mgnutri.bitacora.domain.Modulo;
import ar.edu.usal.mgnutri.bitacora.domain.TipoEvento;
import ar.edu.usal.mgnutri.bitacora.service.Auditable;
import ar.edu.usal.mgnutri.bitacora.service.Auditoria;
import ar.edu.usal.mgnutri.common.Area;
import ar.edu.usal.mgnutri.common.PageResponse;
import ar.edu.usal.mgnutri.common.Textos;
import ar.edu.usal.mgnutri.common.error.BusinessException;
import ar.edu.usal.mgnutri.common.error.ErrorCode;
import ar.edu.usal.mgnutri.common.eventos.Eventos;
import ar.edu.usal.mgnutri.common.security.CurrentUser;
import ar.edu.usal.mgnutri.common.security.UsuarioPrincipal;
import ar.edu.usal.mgnutri.notificaciones.domain.Alerta;
import ar.edu.usal.mgnutri.notificaciones.service.AlertaService;
import ar.edu.usal.mgnutri.pacientes.dao.PacienteDao;
import ar.edu.usal.mgnutri.pacientes.dao.PacienteProfesionalDao;
import ar.edu.usal.mgnutri.pacientes.domain.PacienteProfesional;
import ar.edu.usal.mgnutri.pacientes.dto.PacienteDtos.CredencialesResponse;
import ar.edu.usal.mgnutri.pacientes.dto.PacienteDtos.NotificacionInfo;
import ar.edu.usal.mgnutri.pacientes.dto.PacienteDtos.AltaResponse;
import ar.edu.usal.mgnutri.pacientes.service.AsignacionService;
import ar.edu.usal.mgnutri.profesionales.dao.DisponibilidadDao;
import ar.edu.usal.mgnutri.profesionales.dao.ProfesionalDao;
import ar.edu.usal.mgnutri.profesionales.domain.Disponibilidad;
import ar.edu.usal.mgnutri.profesionales.domain.Profesional;
import ar.edu.usal.mgnutri.profesionales.dto.ProfesionalDtos.ModificacionResponse;
import ar.edu.usal.mgnutri.profesionales.dto.ProfesionalDtos.ProfesionalDisponible;
import ar.edu.usal.mgnutri.profesionales.dto.ProfesionalDtos.ProfesionalListItem;
import ar.edu.usal.mgnutri.profesionales.dto.ProfesionalDtos.ProfesionalRequest;
import ar.edu.usal.mgnutri.profesionales.dto.ProfesionalDtos.ProfesionalResponse;
import ar.edu.usal.mgnutri.seguridad.dto.SeguridadDtos.Credenciales;
import ar.edu.usal.mgnutri.seguridad.service.EnvioCredencialesService;
import ar.edu.usal.mgnutri.seguridad.service.UsuarioService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** ABM de profesionales (CU-04/05/06). */
@Service
public class ProfesionalService {

    private final ProfesionalDao dao;
    private final PacienteDao pacienteDao;
    private final PacienteProfesionalDao asignacionDao;
    private final DisponibilidadDao disponibilidadDao;
    private final AsignacionService asignaciones;
    private final UsuarioService usuarios;
    private final EnvioCredencialesService credenciales;
    private final AlertaService alertas;
    private final List<PlanesActivosConsulta> planesActivos;
    private final ApplicationEventPublisher eventos;
    private final Clock clock;

    public ProfesionalService(ProfesionalDao dao, PacienteDao pacienteDao, PacienteProfesionalDao asignacionDao,
                              DisponibilidadDao disponibilidadDao, AsignacionService asignaciones, UsuarioService usuarios,
                              EnvioCredencialesService credenciales, AlertaService alertas,
                              List<PlanesActivosConsulta> planesActivos, ApplicationEventPublisher eventos, Clock clock) {
        this.dao = dao;
        this.pacienteDao = pacienteDao;
        this.asignacionDao = asignacionDao;
        this.disponibilidadDao = disponibilidadDao;
        this.asignaciones = asignaciones;
        this.usuarios = usuarios;
        this.credenciales = credenciales;
        this.alertas = alertas;
        this.planesActivos = planesActivos;
        this.eventos = eventos;
        this.clock = clock;
    }

    @Transactional
    @Auditable(modulo = Modulo.PROFESIONALES, tipo = TipoEvento.ALTA, entidad = "Profesional", entidadId = "#result.id()",
            descripcion = "Alta de profesional #{#req.apellido()}, #{#req.nombre()} (#{#req.tipo()})")
    public AltaResponse crear(ProfesionalRequest req) {
        String doc = req.nroDocumento().trim().toUpperCase();
        validarDocumentoUnico(doc, null);
        String tel = validar(req);
        Credenciales cred = usuarios.crear(doc, req.tipo().roles());
        Profesional p = new Profesional();
        p.setUsuarioId(cred.usuarioId());
        aplicar(p, req, doc, tel);
        dao.save(p);
        EnvioCredencialesService.Resultado envio = credenciales.enviar(cred, p.getNombre(), tel);
        return new AltaResponse(p.getId(), cred.username(), new NotificacionInfo(envio.notificacionId(), envio.estado()),
                envio.passwordVisible());
    }

    @Transactional
    @Auditable(modulo = Modulo.PROFESIONALES, tipo = TipoEvento.MODIFICACION, entidad = "Profesional", entidadId = "#id",
            descripcion = "Modificación de profesional #{#req.apellido()}, #{#req.nombre()}")
    public ModificacionResponse modificar(Long id, ProfesionalRequest req) {
        Profesional p = dao.findById(id).filter(Profesional::activo).orElseThrow(() -> BusinessException.noEncontrado("Profesional"));
        Textos.verificarVersion(p.getVersion(), req.version());
        String doc = req.nroDocumento().trim().toUpperCase();
        if (!doc.equals(p.getNroDocumento())) {
            validarDocumentoUnico(doc, id);
            usuarios.cambiarUsername(p.getUsuarioId(), doc);
        }
        String tel = validar(req);
        List<String> advertencias = new ArrayList<>();
        if (p.getTipo() != req.tipo()) {
            // CA-02.2.4: recalcular roles y desactivar asignaciones de áreas que ya no atiende
            Auditoria.detalle("tipo", Map.of("anterior", p.getTipo().name(), "nuevo", req.tipo().name()));
            usuarios.reemplazarRoles(p.getUsuarioId(), req.tipo().roles());
            for (Area area : Area.values()) {
                if (p.getTipo().atiende(area) && !req.tipo().atiende(area)) {
                    long activos = planesActivos.stream().mapToLong(c -> c.activosDe(id, area)).sum();
                    if (activos > 0) {
                        advertencias.add("El profesional tiene " + activos + " " + (area == Area.NUTRICION ? "plan(es) alimentario(s)" : "rutina(s)")
                                + " activo(s) que deberán reasignarse.");
                    }
                    int n = asignaciones.desactivarDeProfesional(id, area);
                    if (n > 0) {
                        advertencias.add("Se desactivaron " + n + " asignación(es) del área " + area.etiqueta() + ".");
                    }
                }
            }
        }
        cambio("nombre", p.getNombre(), req.nombre().trim());
        cambio("apellido", p.getApellido(), req.apellido().trim());
        cambio("matricula", p.getMatricula(), Textos.vacioANull(req.matricula()));
        cambio("duracionTurnoMin", String.valueOf(p.getDuracionTurnoMin()), String.valueOf(req.duracionTurnoMin() == null ? 30 : req.duracionTurnoMin()));
        if (!Objects.equals(p.getTelefonoWhatsapp(), tel)) {
            Auditoria.detalle("telefonoWhatsapp", "(modificado)");
        }
        aplicar(p, req, doc, tel);
        dao.saveAndFlush(p);
        return new ModificacionResponse(detalle(p), advertencias);
    }

    @Transactional
    @Auditable(modulo = Modulo.PROFESIONALES, tipo = TipoEvento.BAJA, entidad = "Profesional", entidadId = "#id",
            descripcion = "Baja de profesional #{#id}")
    public void darDeBaja(Long id, String motivo) {
        Profesional p = dao.findById(id).filter(Profesional::activo).orElseThrow(() -> BusinessException.noEncontrado("Profesional"));
        p.setEstado(Profesional.Estado.BAJA);
        p.setFechaBaja(LocalDate.now(clock));
        usuarios.darDeBaja(p.getUsuarioId());
        eventos.publishEvent(new Eventos.ProfesionalDadoDeBaja(id, Textos.vacioANull(motivo)));
        asignaciones.desactivarDeProfesional(id, null);
        disponibilidadDao.findByProfesionalIdAndActivoTrueOrderByDiaSemanaAscHoraDesdeAsc(id).forEach(d -> d.setActivo(false));
        long activos = planesActivos.stream().mapToLong(c -> c.activosDe(id, Area.NUTRICION) + c.activosDe(id, Area.ENTRENAMIENTO)).sum();
        if (activos > 0) {
            alertas.paraAdmins(Alerta.Tipo.PROFESIONAL_BAJA, "Pacientes con plan/rutina de un profesional dado de baja ("
                    + p.nombreCompleto() + "): " + activos + ". Reasigná un profesional a esos pacientes.", "Profesional", id);
        }
    }

    @Transactional
    @Auditable(modulo = Modulo.SEGURIDAD, tipo = TipoEvento.CAMBIO_PASSWORD, entidad = "Usuario",
            descripcion = "Reenvío de credenciales al profesional #{#id}")
    public CredencialesResponse reenviarCredenciales(Long id) {
        Profesional p = dao.findById(id).filter(Profesional::activo).orElseThrow(() -> BusinessException.noEncontrado("Profesional"));
        Auditoria.entidadId(p.getUsuarioId());
        Credenciales c = usuarios.regenerarCredenciales(p.getUsuarioId());
        EnvioCredencialesService.Resultado envio = credenciales.enviar(c, p.getNombre(), p.getTelefonoWhatsapp());
        return new CredencialesResponse(c.username(), new NotificacionInfo(envio.notificacionId(), envio.estado()), envio.passwordVisible());
    }

    @Transactional(readOnly = true)
    public PageResponse<ProfesionalListItem> buscar(String q, Profesional.Tipo tipo, Profesional.Estado estado, Pageable pageable) {
        String texto = q == null || q.isBlank() ? null : q.trim();
        return PageResponse.de(dao.buscar(texto, tipo, estado == null ? Profesional.Estado.ACTIVO : estado, pageable),
                p -> new ProfesionalListItem(p.getId(), p.getApellido(), p.getNombre(), p.getNroDocumento(), p.getTipo(),
                        p.getEspecialidad(), p.getMatricula(), p.getTelefonoWhatsapp(), p.getEstado().name(),
                        disponibilidadDao.existsByProfesionalIdAndActivoTrue(p.getId()),
                        asignacionDao.findByProfesionalIdAndActivoTrue(p.getId()).size()));
    }

    @Transactional(readOnly = true)
    public ProfesionalResponse obtener(Long id) {
        UsuarioPrincipal u = CurrentUser.get();
        if (!u.esAdmin() && !id.equals(u.profesionalId())) {
            throw BusinessException.sinPermiso();
        }
        return detalle(dao.findById(id).orElseThrow(() -> BusinessException.noEncontrado("Profesional")));
    }

    @Transactional(readOnly = true)
    public ProfesionalResponse yo() {
        return detalle(dao.findById(CurrentUser.profesionalId()).orElseThrow(() -> BusinessException.noEncontrado("Profesional")));
    }

    /** Profesionales con agenda del área; para el paciente, primero los asignados (CA-03.1.1). */
    @Transactional(readOnly = true)
    public List<ProfesionalDisponible> disponibles(Area area) {
        if (area == Area.ENTRENAMIENTO) {
            return List.of();
        }
        UsuarioPrincipal u = CurrentUser.get();
        Set<Long> asignados = u.pacienteId() == null ? Set.of()
                : asignacionDao.findByPacienteIdAndActivoTrue(u.pacienteId()).stream()
                .filter(a -> area == null || a.getArea() == area)
                .map(PacienteProfesional::getProfesionalId).collect(Collectors.toSet());
        return dao.findByEstado(Profesional.Estado.ACTIVO).stream()
                .filter(p -> p.getTipo().usaAgenda() && (area == null || p.getTipo().atiende(area)))
                .filter(p -> disponibilidadDao.existsByProfesionalIdAndActivoTrue(p.getId()))
                .map(p -> new ProfesionalDisponible(p.getId(), p.nombreVisible() + " (" + p.nombreCompleto() + ")",
                        p.getEspecialidad(), p.getTipo(), asignados.contains(p.getId()), p.getDuracionTurnoMin()))
                .sorted(Comparator.comparing((ProfesionalDisponible d) -> !d.asignado()).thenComparing(ProfesionalDisponible::nombre))
                .toList();
    }

    ProfesionalResponse detalle(Profesional p) {
        String username = usuarios.obtener(p.getUsuarioId()).getUsername();
        return new ProfesionalResponse(p.getId(), p.getUsuarioId(), username, p.getNombre(), p.getApellido(), p.nombreVisible(),
                p.getTipoDocumento(), p.getNroDocumento(), p.getMatricula(), p.getTipo(), p.getEspecialidad(),
                p.getTelefonoWhatsapp(), p.getEmail(), p.getDuracionTurnoMin(), p.getEstado().name(), p.getFechaBaja(),
                p.getTipo().usaAgenda() && disponibilidadDao.existsByProfesionalIdAndActivoTrue(p.getId()),
                asignacionDao.findByProfesionalIdAndActivoTrue(p.getId()).size(), p.getVersion());
    }

    private void validarDocumentoUnico(String doc, Long idActual) {
        boolean enProf = dao.findByNroDocumento(doc).filter(x -> !x.getId().equals(idActual)).isPresent();
        if (enProf || pacienteDao.existsByNroDocumento(doc)) {
            throw new BusinessException(ErrorCode.DUPLICADO, "Ya existe una persona registrada con ese documento",
                    List.of(new BusinessException.CampoError("nroDocumento", "Documento ya registrado")));
        }
    }

    /** RN-02.2 matrícula condicional, RN-02.3 duración 15–120 múltiplo de 5, teléfono E.164. */
    private String validar(ProfesionalRequest req) {
        List<BusinessException.CampoError> errores = new ArrayList<>();
        if (req.tipo().requiereMatricula() && (req.matricula() == null || req.matricula().isBlank())) {
            errores.add(new BusinessException.CampoError("matricula", "La matrícula es obligatoria para nutricionistas e integrales"));
        }
        int dur = req.duracionTurnoMin() == null ? 30 : req.duracionTurnoMin();
        if (dur % 5 != 0) {
            errores.add(new BusinessException.CampoError("duracionTurnoMin", "La duración debe ser múltiplo de 5"));
        }
        String tel = null;
        if (req.telefonoWhatsapp() != null && !req.telefonoWhatsapp().isBlank()) {
            tel = Textos.telefonoE164(req.telefonoWhatsapp());
            if (tel == null) {
                errores.add(new BusinessException.CampoError("telefonoWhatsapp", "Teléfono inválido"));
            }
        }
        if (!errores.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDACION, "Revisá los datos ingresados: "
                    + String.join("; ", errores.stream().map(BusinessException.CampoError::mensaje).toList()), errores);
        }
        return tel;
    }

    private void aplicar(Profesional p, ProfesionalRequest r, String doc, String tel) {
        p.setNombre(r.nombre().trim());
        p.setApellido(r.apellido().trim());
        p.setTipoDocumento(r.tipoDocumento().trim().toUpperCase());
        p.setNroDocumento(doc);
        p.setTipo(r.tipo());
        p.setMatricula(Textos.vacioANull(r.matricula()));
        p.setEspecialidad(Textos.vacioANull(r.especialidad()));
        p.setTelefonoWhatsapp(tel);
        p.setEmail(Textos.vacioANull(r.email()));
        p.setDuracionTurnoMin(r.duracionTurnoMin() == null ? 30 : r.duracionTurnoMin());
    }

    private static void cambio(String campo, String antes, String despues) {
        if (!Objects.equals(antes, despues)) {
            Auditoria.detalle(campo, Map.of("anterior", String.valueOf(antes), "nuevo", String.valueOf(despues)));
        }
    }

    /** Usado por el editor de agenda. */
    @Transactional(readOnly = true)
    public Profesional entidad(Long id) {
        return dao.findById(id).orElseThrow(() -> BusinessException.noEncontrado("Profesional"));
    }

    @Transactional(readOnly = true)
    public List<Disponibilidad> franjas(Long id) {
        return disponibilidadDao.findByProfesionalIdAndActivoTrueOrderByDiaSemanaAscHoraDesdeAsc(id);
    }
}
