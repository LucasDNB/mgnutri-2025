package ar.edu.usal.mgnutri.configuracion.service;

import ar.edu.usal.mgnutri.bitacora.domain.Modulo;
import ar.edu.usal.mgnutri.bitacora.domain.TipoEvento;
import ar.edu.usal.mgnutri.bitacora.service.Auditable;
import ar.edu.usal.mgnutri.bitacora.service.Auditoria;
import ar.edu.usal.mgnutri.common.Textos;
import ar.edu.usal.mgnutri.common.error.BusinessException;
import ar.edu.usal.mgnutri.common.security.CurrentUser;
import ar.edu.usal.mgnutri.configuracion.dao.ConfiguracionDao;
import ar.edu.usal.mgnutri.configuracion.dao.TerminosDao;
import ar.edu.usal.mgnutri.configuracion.domain.ConfiguracionSistema;
import ar.edu.usal.mgnutri.configuracion.domain.TerminosCondiciones;
import ar.edu.usal.mgnutri.configuracion.dto.ConfiguracionDtos.ContactoCentral;
import ar.edu.usal.mgnutri.configuracion.dto.ConfiguracionDtos.ParametroResponse;
import ar.edu.usal.mgnutri.configuracion.dto.ConfiguracionDtos.TerminosRequest;
import ar.edu.usal.mgnutri.configuracion.dto.ConfiguracionDtos.TerminosResponse;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Parámetros del sistema editables por el admin (configuracion_sistema) y términos y condiciones. */
@Service
public class ConfiguracionService {

    public static final String VERSION_TERMINOS = "terminos.version_vigente";
    private static final Set<String> SOLO_SISTEMA = Set.of(VERSION_TERMINOS);

    private final ConfiguracionDao dao;
    private final TerminosDao terminosDao;
    private final Clock clock;

    public ConfiguracionService(ConfiguracionDao dao, TerminosDao terminosDao, Clock clock) {
        this.dao = dao;
        this.terminosDao = terminosDao;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public String valor(String clave, String porDefecto) {
        return dao.findById(clave).map(ConfiguracionSistema::getValor).filter(v -> !v.isBlank()).orElse(porDefecto);
    }

    @Transactional(readOnly = true)
    public Optional<String> valor(String clave) {
        return dao.findById(clave).map(ConfiguracionSistema::getValor).filter(v -> !v.isBlank());
    }

    @Transactional(readOnly = true)
    public List<ParametroResponse> listar() {
        return dao.findAll().stream()
                .sorted((a, b) -> a.getClave().compareTo(b.getClave()))
                .map(c -> new ParametroResponse(c.getClave(), c.getValor(), c.getDescripcion(), c.getUpdatedAt(),
                        c.getUpdatedBy(), !SOLO_SISTEMA.contains(c.getClave())))
                .toList();
    }

    @Transactional
    @Auditable(modulo = Modulo.CONFIGURACION, tipo = TipoEvento.CONFIGURACION, entidad = "ConfiguracionSistema",
            descripcion = "Cambio de parámetros de configuración")
    public List<ParametroResponse> actualizar(Map<String, String> valores) {
        valores.forEach((clave, valor) -> {
            if (SOLO_SISTEMA.contains(clave)) {
                throw BusinessException.validacion(clave, "Este parámetro se modifica desde Términos y condiciones.");
            }
            ConfiguracionSistema c = dao.findById(clave)
                    .orElseThrow(() -> BusinessException.validacion(clave, "Parámetro desconocido: " + clave));
            validar(clave, valor);
            Auditoria.detalle(clave, Map.of("anterior", String.valueOf(c.getValor()), "nuevo", String.valueOf(valor)));
            guardar(c, valor);
        });
        return listar();
    }

    @Transactional
    public void establecer(String clave, String valor) {
        ConfiguracionSistema c = dao.findById(clave).orElseGet(() -> new ConfiguracionSistema(clave, valor, null));
        guardar(c, valor);
    }

    private void guardar(ConfiguracionSistema c, String valor) {
        c.setValor(valor == null ? "" : valor.trim());
        c.setUpdatedAt(clock.instant().truncatedTo(ChronoUnit.SECONDS));
        c.setUpdatedBy(CurrentUser.opcional().map(u -> u.username()).orElse("SISTEMA"));
        dao.save(c);
    }

    private void validar(String clave, String valor) {
        String v = valor == null ? "" : valor.trim();
        if (clave.endsWith("_cron") && !v.isEmpty() && !CronExpression.isValidExpression(v)) {
            throw BusinessException.validacion(clave, "Expresión de programación inválida.");
        }
        if ((clave.equals("turnos.recordatorio_hora") || clave.equals("backup.hora")) && !v.isEmpty()) {
            try {
                LocalTime.parse(v);
            } catch (DateTimeParseException e) {
                throw BusinessException.validacion(clave, "La hora debe tener el formato HH:mm.");
            }
        }
        if (clave.equals("centro.telefono_whatsapp") && !v.isEmpty() && Textos.telefonoE164(v) == null) {
            throw BusinessException.validacion(clave, "Teléfono inválido.");
        }
        if (clave.equals("pagos.link") && !v.isEmpty() && !v.startsWith("https://")) {
            throw BusinessException.validacion(clave, "El enlace de pago debe comenzar con https://");
        }
    }

    // ---- Contacto público (RF-12) ----

    @Transactional(readOnly = true)
    public ContactoCentral contactoCentral() {
        String tel = valor("centro.telefono_whatsapp", "");
        String digitos = Textos.digitos(tel);
        return new ContactoCentral(valor("centro.nombre", "MG Nutrición y Entrenamiento"),
                valor("centro.direccion", ""), valor("centro.horario", ""), tel,
                digitos == null || digitos.isEmpty() ? null
                        : "https://wa.me/" + digitos + "?text=" + "Hola%2C%20quisiera%20hacer%20una%20consulta",
                valor("mantenimiento.mensaje", ""));
    }

    // ---- Términos y condiciones (D-25) ----

    @Transactional(readOnly = true)
    public String versionTerminosVigente() {
        return valor(VERSION_TERMINOS, "1.0");
    }

    @Transactional(readOnly = true)
    public TerminosResponse terminosVigentes() {
        String version = versionTerminosVigente();
        TerminosCondiciones t = terminosDao.findById(version).orElseThrow(() -> BusinessException.noEncontrado("Términos"));
        return new TerminosResponse(t.getVersion(), t.getTexto(), t.getVigenteDesde());
    }

    @Transactional(readOnly = true)
    public List<TerminosResponse> historialTerminos() {
        return terminosDao.findAllByOrderByVigenteDesdeDesc().stream()
                .map(t -> new TerminosResponse(t.getVersion(), t.getTexto(), t.getVigenteDesde())).toList();
    }

    @Transactional
    @Auditable(modulo = Modulo.SEGURIDAD, tipo = TipoEvento.CONFIGURACION, entidad = "TerminosCondiciones",
            entidadId = "#req.version()", descripcion = "Nueva versión de términos y condiciones #{#req.version()}")
    public TerminosResponse publicarTerminos(TerminosRequest req) {
        if (terminosDao.existsById(req.version())) {
            throw BusinessException.validacion("version", "Ya existe esa versión de términos.");
        }
        TerminosCondiciones t = terminosDao.save(new TerminosCondiciones(req.version(), req.texto(),
                clock.instant().truncatedTo(ChronoUnit.SECONDS)));
        establecer(VERSION_TERMINOS, req.version());
        return new TerminosResponse(t.getVersion(), t.getTexto(), t.getVigenteDesde());
    }
}
