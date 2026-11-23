package ar.edu.usal.mgnutri.pacientes.service;

import ar.edu.usal.mgnutri.bitacora.domain.Modulo;
import ar.edu.usal.mgnutri.bitacora.domain.TipoEvento;
import ar.edu.usal.mgnutri.bitacora.service.Auditable;
import ar.edu.usal.mgnutri.bitacora.service.Auditoria;
import ar.edu.usal.mgnutri.bitacora.service.BitacoraService;
import ar.edu.usal.mgnutri.common.PageResponse;
import ar.edu.usal.mgnutri.common.Rol;
import ar.edu.usal.mgnutri.common.Textos;
import ar.edu.usal.mgnutri.common.error.BusinessException;
import ar.edu.usal.mgnutri.common.error.ErrorCode;
import ar.edu.usal.mgnutri.common.eventos.Eventos;
import ar.edu.usal.mgnutri.common.security.AccesoPacienteGuard;
import ar.edu.usal.mgnutri.common.security.CurrentUser;
import ar.edu.usal.mgnutri.common.security.UsuarioPrincipal;
import ar.edu.usal.mgnutri.pacientes.dao.PacienteDao;
import ar.edu.usal.mgnutri.pacientes.dao.PacienteProfesionalDao;
import ar.edu.usal.mgnutri.pacientes.domain.Paciente;
import ar.edu.usal.mgnutri.pacientes.dto.PacienteDtos.AltaResponse;
import ar.edu.usal.mgnutri.pacientes.dto.PacienteDtos.AsignacionResponse;
import ar.edu.usal.mgnutri.pacientes.dto.PacienteDtos.BajaRequest;
import ar.edu.usal.mgnutri.pacientes.dto.PacienteDtos.CredencialesResponse;
import ar.edu.usal.mgnutri.pacientes.dto.PacienteDtos.NotificacionInfo;
import ar.edu.usal.mgnutri.pacientes.dto.PacienteDtos.PacienteListItem;
import ar.edu.usal.mgnutri.pacientes.dto.PacienteDtos.PacienteRequest;
import ar.edu.usal.mgnutri.pacientes.dto.PacienteDtos.PacienteResponse;
import ar.edu.usal.mgnutri.pacientes.dto.PacienteDtos.PerfilRequest;
import ar.edu.usal.mgnutri.pagos.dao.SaldoDao;
import ar.edu.usal.mgnutri.profesionales.dao.ProfesionalDao;
import ar.edu.usal.mgnutri.seguridad.dto.SeguridadDtos.Credenciales;
import ar.edu.usal.mgnutri.seguridad.service.EnvioCredencialesService;
import ar.edu.usal.mgnutri.seguridad.service.UsuarioService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Alta, modificación, baja y consulta de pacientes (CU-01/02/03). */
@Service
public class PacienteService {

    private final PacienteDao dao;
    private final ProfesionalDao profesionalDao;
    private final PacienteProfesionalDao asignacionDao;
    private final AsignacionService asignaciones;
    private final UsuarioService usuarios;
    private final EnvioCredencialesService credenciales;
    private final AccesoPacienteGuard guard;
    private final SaldoDao saldos;
    private final BitacoraService bitacora;
    private final ApplicationEventPublisher eventos;
    private final Clock clock;

    public PacienteService(PacienteDao dao, ProfesionalDao profesionalDao, PacienteProfesionalDao asignacionDao,
                           AsignacionService asignaciones, UsuarioService usuarios, EnvioCredencialesService credenciales,
                           AccesoPacienteGuard guard, SaldoDao saldos, BitacoraService bitacora,
                           ApplicationEventPublisher eventos, Clock clock) {
        this.dao = dao;
        this.profesionalDao = profesionalDao;
        this.asignacionDao = asignacionDao;
        this.asignaciones = asignaciones;
        this.usuarios = usuarios;
        this.credenciales = credenciales;
        this.guard = guard;
        this.saldos = saldos;
        this.bitacora = bitacora;
        this.eventos = eventos;
        this.clock = clock;
    }

    @Transactional
    @Auditable(modulo = Modulo.PACIENTES, tipo = TipoEvento.ALTA, entidad = "Paciente", entidadId = "#result.id()",
            descripcion = "Alta de paciente #{#req.apellido()}, #{#req.nombre()}")
    public AltaResponse crear(PacienteRequest req) {
        String doc = req.nroDocumento().trim().toUpperCase();
        validarDocumentoUnico(doc, null);
        String telefono = validarComunes(req);
        Credenciales cred = usuarios.crear(doc, EnumSet.of(Rol.PACIENTE));
        Paciente p = new Paciente();
        p.setUsuarioId(cred.usuarioId());
        aplicar(p, req, doc, telefono);
        dao.save(p);
        asignaciones.crearAsignaciones(p.getId(), req.asignaciones());
        EnvioCredencialesService.Resultado envio = credenciales.enviar(cred, p.getNombre(), telefono);
        return new AltaResponse(p.getId(), cred.username(), new NotificacionInfo(envio.notificacionId(), envio.estado()),
                envio.passwordVisible());
    }

    @Transactional
    @Auditable(modulo = Modulo.PACIENTES, tipo = TipoEvento.MODIFICACION, entidad = "Paciente", entidadId = "#id",
            descripcion = "Modificación de paciente #{#req.apellido()}, #{#req.nombre()}")
    public PacienteResponse modificar(Long id, PacienteRequest req) {
        Paciente p = dao.findById(id).filter(Paciente::activo).orElseThrow(() -> BusinessException.noEncontrado("Paciente"));
        Textos.verificarVersion(p.getVersion(), req.version());
        String doc = req.nroDocumento().trim().toUpperCase();
        if (!doc.equals(p.getNroDocumento())) {
            validarDocumentoUnico(doc, id);
            usuarios.cambiarUsername(p.getUsuarioId(), doc);
        }
        String telefono = validarComunes(req);
        registrarCambios(p, req, doc, telefono);
        aplicar(p, req, doc, telefono);
        dao.saveAndFlush(p);
        return detalle(p, false);
    }

    @Transactional
    @Auditable(modulo = Modulo.PACIENTES, tipo = TipoEvento.BAJA, entidad = "Paciente", entidadId = "#id",
            descripcion = "Baja de paciente #{#id}")
    public void darDeBaja(Long id, BajaRequest req) {
        Paciente p = dao.findById(id).filter(Paciente::activo).orElseThrow(() -> BusinessException.noEncontrado("Paciente"));
        p.setEstado(Paciente.Estado.BAJA);
        p.setFechaBaja(LocalDate.now(clock));
        p.setMotivoBaja(Textos.vacioANull(req == null ? null : req.motivo()));
        usuarios.darDeBaja(p.getUsuarioId());
        asignaciones.desactivarDePaciente(id);
        boolean notificar = req != null && Boolean.TRUE.equals(req.notificarCancelaciones());
        eventos.publishEvent(new Eventos.PacienteDadoDeBaja(id, p.getMotivoBaja(), notificar));
    }

    /**
     * Reactivación (D-37): vuelve a ACTIVO con credenciales nuevas enviadas por WhatsApp. Conserva el historial;
     * no restaura asignaciones ni turnos cancelados (el admin lo vuelve a asignar).
     */
    @Transactional
    @Auditable(modulo = Modulo.PACIENTES, tipo = TipoEvento.ALTA, entidad = "Paciente", entidadId = "#id",
            descripcion = "Reactivación de paciente #{#id}")
    public CredencialesResponse reactivar(Long id) {
        Paciente p = dao.findById(id).orElseThrow(() -> BusinessException.noEncontrado("Paciente"));
        if (p.activo()) {
            throw new BusinessException(ErrorCode.REGLA_NEGOCIO, "El paciente ya está activo.");
        }
        p.setEstado(Paciente.Estado.ACTIVO);
        p.setFechaBaja(null);
        p.setMotivoBaja(null);
        Credenciales c = usuarios.reactivar(p.getUsuarioId());
        EnvioCredencialesService.Resultado envio = credenciales.enviar(c, p.getNombre(), p.getTelefonoWhatsapp());
        return new CredencialesResponse(c.username(), new NotificacionInfo(envio.notificacionId(), envio.estado()),
                envio.passwordVisible());
    }

    @Transactional
    @Auditable(modulo = Modulo.SEGURIDAD, tipo = TipoEvento.CAMBIO_PASSWORD, entidad = "Usuario",
            descripcion = "Reenvío de credenciales al paciente #{#id}")
    public CredencialesResponse reenviarCredenciales(Long id) {
        Paciente p = dao.findById(id).filter(Paciente::activo).orElseThrow(() -> BusinessException.noEncontrado("Paciente"));
        Auditoria.entidadId(p.getUsuarioId());
        Credenciales c = usuarios.regenerarCredenciales(p.getUsuarioId());
        EnvioCredencialesService.Resultado envio = credenciales.enviar(c, p.getNombre(), p.getTelefonoWhatsapp());
        return new CredencialesResponse(c.username(), new NotificacionInfo(envio.notificacionId(), envio.estado()),
                envio.passwordVisible());
    }

    @Transactional(readOnly = true)
    public PageResponse<PacienteListItem> buscar(String q, Long profesionalId, boolean incluirBajas, Pageable pageable) {
        UsuarioPrincipal u = CurrentUser.get();
        // Sólo el admin puede ver los pacientes dados de baja
        String estado = incluirBajas && u.esAdmin() ? null : Paciente.Estado.ACTIVO.name();
        Long filtroProf = profesionalId;
        if (!u.esAdmin()) {
            filtroProf = u.profesionalId(); // CA-01.5.2: sólo asignados
            if (filtroProf == null) {
                throw BusinessException.sinPermiso();
            }
        }
        String texto = q == null || q.isBlank() ? null : q.trim();
        Page<Paciente> pagina = dao.buscar(texto, estado, filtroProf, pageable);
        Map<Long, BigDecimal> saldoMap = u.esAdmin() ? saldos.saldos(pagina.getContent().stream().map(Paciente::getId).toList()) : Map.of();
        LocalDate hoy = LocalDate.now(clock);
        return PageResponse.de(pagina, p -> new PacienteListItem(p.getId(), p.getApellido(), p.getNombre(), p.getNroDocumento(),
                p.getTelefonoWhatsapp(), asignaciones.respuestas(asignacionDao.findByPacienteIdAndActivoTrue(p.getId())).stream()
                .map(a -> a.profesional() + " · " + a.area().etiqueta()).toList(),
                u.esAdmin() ? saldoMap.getOrDefault(p.getId(), BigDecimal.ZERO) : null, p.getEstado().name(), p.edad(hoy),
                p.getObjetivo()));
    }

    /** Ficha completa: consulta sensible auditada (bitácora CONSULTA_SENSIBLE). */
    @Transactional(readOnly = true)
    public PacienteResponse obtener(Long id) {
        Paciente p = guard.assertPuedeVer(id);
        UsuarioPrincipal u = CurrentUser.get();
        if (!u.esPaciente()) {
            bitacora.ok(Modulo.PACIENTES, TipoEvento.CONSULTA_SENSIBLE, "Paciente", id, "Consulta de ficha de " + p.nombreCompleto());
        }
        return detalle(p, u.esAdmin());
    }

    @Transactional(readOnly = true)
    public PacienteResponse miPerfil() {
        Paciente p = dao.findById(CurrentUser.pacienteId()).orElseThrow(() -> BusinessException.noEncontrado("Paciente"));
        return detalle(p, false);
    }

    /** US-01.5.3: el paciente sólo edita email y domicilio. */
    @Transactional
    @Auditable(modulo = Modulo.PACIENTES, tipo = TipoEvento.MODIFICACION, entidad = "Paciente",
            entidadId = "#result.id()", descripcion = "El paciente actualizó su perfil")
    public PacienteResponse actualizarPerfil(PerfilRequest req) {
        Paciente p = dao.findById(CurrentUser.pacienteId()).orElseThrow(() -> BusinessException.noEncontrado("Paciente"));
        if (!Objects.equals(p.getEmail(), Textos.vacioANull(req.email()))) {
            Auditoria.detalle("email", "(modificado)");
        }
        if (!Objects.equals(p.getDomicilio(), Textos.vacioANull(req.domicilio()))) {
            Auditoria.detalle("domicilio", "(modificado)");
        }
        p.setEmail(Textos.vacioANull(req.email()));
        p.setDomicilio(Textos.vacioANull(req.domicilio()));
        return detalle(p, false);
    }

    PacienteResponse detalle(Paciente p, boolean conSaldo) {
        List<AsignacionResponse> asign = asignaciones.respuestas(asignacionDao.findByPacienteIdAndActivoTrue(p.getId()));
        String username = usuarios.obtener(p.getUsuarioId()).getUsername();
        return new PacienteResponse(p.getId(), p.getUsuarioId(), username, p.getNombre(), p.getApellido(), p.getTipoDocumento(),
                p.getNroDocumento(), p.getFechaNacimiento(), p.edad(LocalDate.now(clock)), p.getSexo(), p.getTelefonoWhatsapp(),
                p.getEmail(), p.getDomicilio(), p.getObjetivo(), p.getAntecedentesSalud(), p.getObservaciones(),
                p.getEstado().name(), p.getFechaBaja(), p.getMotivoBaja(), asign,
                conSaldo ? saldos.saldo(p.getId()) : null, p.getVersion());
    }

    private void validarDocumentoUnico(String doc, Long idActual) {
        boolean enPacientes = dao.findByNroDocumento(doc).filter(x -> !x.getId().equals(idActual)).isPresent();
        if (enPacientes || profesionalDao.existsByNroDocumento(doc)) {
            throw new BusinessException(ErrorCode.DUPLICADO, "Ya existe una persona registrada con ese documento",
                    List.of(new BusinessException.CampoError("nroDocumento", "Documento ya registrado")));
        }
    }

    /** RN-01.2 / RN-01.3: edad 5–110 años y teléfono E.164 argentino. */
    private String validarComunes(PacienteRequest req) {
        List<BusinessException.CampoError> errores = new ArrayList<>();
        LocalDate hoy = LocalDate.now(clock);
        if (req.fechaNacimiento() != null) {
            int edad = java.time.Period.between(req.fechaNacimiento(), hoy).getYears();
            if (edad < 5 || edad > 110) {
                errores.add(new BusinessException.CampoError("fechaNacimiento", "La edad debe estar entre 5 y 110 años"));
            }
        }
        String telefono = Textos.telefonoE164(req.telefonoWhatsapp());
        if (telefono == null) {
            errores.add(new BusinessException.CampoError("telefonoWhatsapp",
                    "Teléfono inválido. Ingresá código de área y número, por ejemplo 11 3456-7890"));
        }
        if (!errores.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDACION, "Revisá los datos ingresados: "
                    + String.join("; ", errores.stream().map(BusinessException.CampoError::mensaje).toList()), errores);
        }
        return telefono;
    }

    private void aplicar(Paciente p, PacienteRequest r, String doc, String telefono) {
        p.setNombre(r.nombre().trim());
        p.setApellido(r.apellido().trim());
        p.setTipoDocumento(r.tipoDocumento().trim().toUpperCase());
        p.setNroDocumento(doc);
        p.setFechaNacimiento(r.fechaNacimiento());
        p.setSexo(r.sexo());
        p.setTelefonoWhatsapp(telefono);
        p.setEmail(Textos.vacioANull(r.email()));
        p.setDomicilio(Textos.vacioANull(r.domicilio()));
        p.setObjetivo(Textos.vacioANull(r.objetivo()));
        p.setAntecedentesSalud(Textos.vacioANull(r.antecedentesSalud()));
        p.setObservaciones(Textos.vacioANull(r.observaciones()));
    }

    /** CA-01.2.2: campos cambiados; los sensibles sólo por nombre. */
    private void registrarCambios(Paciente p, PacienteRequest r, String doc, String tel) {
        cambio("nombre", p.getNombre(), r.nombre().trim(), false);
        cambio("apellido", p.getApellido(), r.apellido().trim(), false);
        cambio("nroDocumento", p.getNroDocumento(), doc, false);
        cambio("fechaNacimiento", String.valueOf(p.getFechaNacimiento()), String.valueOf(r.fechaNacimiento()), false);
        cambio("sexo", p.getSexo(), r.sexo(), false);
        cambio("objetivo", p.getObjetivo(), Textos.vacioANull(r.objetivo()), false);
        cambio("telefonoWhatsapp", p.getTelefonoWhatsapp(), tel, true);
        cambio("email", p.getEmail(), Textos.vacioANull(r.email()), true);
        cambio("domicilio", p.getDomicilio(), Textos.vacioANull(r.domicilio()), true);
        cambio("antecedentesSalud", p.getAntecedentesSalud(), Textos.vacioANull(r.antecedentesSalud()), true);
        cambio("observaciones", p.getObservaciones(), Textos.vacioANull(r.observaciones()), true);
    }

    private static void cambio(String campo, String antes, String despues, boolean sensible) {
        if (!Objects.equals(antes, despues)) {
            Auditoria.detalle(campo, sensible ? "(modificado)" : Map.of("anterior", String.valueOf(antes), "nuevo", String.valueOf(despues)));
        }
    }
}
