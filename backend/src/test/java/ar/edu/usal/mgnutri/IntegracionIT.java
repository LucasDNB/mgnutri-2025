package ar.edu.usal.mgnutri;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integración contra SQL Server real (Testcontainers + Flyway). Se omiten si no hay Docker.
 * Cubren CA-00.1.2, CA-13.1.x, CA-13.6.1 (matriz de permisos), CA-01.1.x, CA-14.1.5 (bitácora inmutable).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IntegracionIT {

    @ServiceConnection
    static final MSSQLServerContainer<?> SQL = new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();

    static {
        // Se inicia antes que el contexto de Spring (la clase usa ciclo de vida PER_CLASS); Ryuk lo elimina al terminar.
        if (org.testcontainers.DockerClientFactory.instance().isDockerAvailable()) {
            SQL.start();
        }
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;

    String admin;

    String json(Object o) throws Exception {
        return mapper.writeValueAsString(o);
    }

    JsonNode cuerpo(MvcResult r) throws Exception {
        return mapper.readTree(r.getResponse().getContentAsString());
    }

    String login(String u, String p) throws Exception {
        return cuerpo(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(json(Map.of("username", u, "password", p))))
                .andExpect(status().isOk()).andReturn()).get("token").asText();
    }

    String primerIngreso(String u, String temporal, String nueva) throws Exception {
        String t = login(u, temporal);
        t = cuerpo(mvc.perform(post("/api/v1/auth/cambiar-password").header("Authorization", "Bearer " + t).contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("actual", temporal, "nueva", nueva)))).andExpect(status().isOk()).andReturn()).get("token").asText();
        mvc.perform(post("/api/v1/terminos/aceptar").header("Authorization", "Bearer " + t).contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("version", "1.0")))).andExpect(status().isOk());
        return t;
    }

    @BeforeAll
    void preparar() throws Exception {
        String t = login("admin", "Admin2025!");
        mvc.perform(get("/api/v1/pacientes").header("Authorization", "Bearer " + t)).andExpect(status().isPreconditionRequired());
        admin = primerIngreso("admin", "Admin2025!", "AdminTest2025");
    }

    @Test
    @Order(1)
    void salud() throws Exception {
        mvc.perform(get("/api/v1/salud")).andExpect(status().isOk()).andExpect(jsonPath("$.bd").value("OK"));
    }

    @Test
    @Order(2)
    void credencialesInvalidas_y_bloqueo() throws Exception {
        for (int i = 0; i < 4; i++) {
            mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(json(Map.of("username", "admin", "password", "mala"))))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("CREDENCIALES_INVALIDAS"));
        }
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(json(Map.of("username", "admin", "password", "mala"))))
                .andExpect(status().isLocked()).andExpect(jsonPath("$.code").value("USUARIO_BLOQUEADO"));
        jdbc.update("UPDATE usuario SET bloqueado_hasta = NULL, estado = 'ACTIVO', intentos_fallidos = 0 WHERE username = 'admin'");
    }

    @Test
    @Order(3)
    void altaDePaciente_permisos_y_bitacora() throws Exception {
        JsonNode prof = cuerpo(mvc.perform(post("/api/v1/profesionales").header("Authorization", "Bearer " + admin).contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("nombre", "Laura", "apellido", "López", "tipoDocumento", "DNI", "nroDocumento", "30111222", "tipo", "NUTRICIONISTA",
                        "matricula", "MN 1", "telefonoWhatsapp", "1134567890")))).andExpect(status().isCreated()).andReturn());
        JsonNode pac = cuerpo(mvc.perform(post("/api/v1/pacientes").header("Authorization", "Bearer " + admin).contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("nombre", "Ana", "apellido", "Gómez", "tipoDocumento", "DNI", "nroDocumento", "40123456", "fechaNacimiento", "1997-03-14",
                        "sexo", "F", "telefonoWhatsapp", "1145678901")))).andExpect(status().isCreated()).andReturn());
        // documento duplicado (también contra profesionales)
        mvc.perform(post("/api/v1/pacientes").header("Authorization", "Bearer " + admin).contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("nombre", "Otra", "apellido", "Persona", "tipoDocumento", "DNI", "nroDocumento", "30111222", "fechaNacimiento", "1990-01-01",
                        "sexo", "F", "telefonoWhatsapp", "1145678902")))).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("DUPLICADO"));
        // campos obligatorios faltantes → 400 con errors[]
        mvc.perform(post("/api/v1/pacientes").header("Authorization", "Bearer " + admin).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors").isArray());

        String nutri = primerIngreso("30111222", prof.get("passwordTemporal").asText(), "Nutri2025ok");
        // nutricionista no asignado → 403 en la ficha
        mvc.perform(get("/api/v1/pacientes/" + pac.get("id").asLong()).header("Authorization", "Bearer " + nutri)).andExpect(status().isForbidden());
        // matriz de permisos: nutricionista no accede a bitácora, pagos ni backups
        mvc.perform(get("/api/v1/bitacora").header("Authorization", "Bearer " + nutri)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/deudores").header("Authorization", "Bearer " + nutri)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/backups").header("Authorization", "Bearer " + nutri)).andExpect(status().isForbidden());
        // sin token → 401
        mvc.perform(get("/api/v1/pacientes")).andExpect(status().isUnauthorized());

        Integer eventos = jdbc.queryForObject("SELECT COUNT(*) FROM bitacora_evento WHERE modulo = 'PACIENTES' AND tipo_evento = 'ALTA'", Integer.class);
        assertThat(eventos).isGreaterThanOrEqualTo(1);
        Integer notif = jdbc.queryForObject("SELECT COUNT(*) FROM notificacion WHERE tipo = 'CREDENCIALES'", Integer.class);
        assertThat(notif).isGreaterThanOrEqualTo(2);

        // D-37: baja → sólo el admin lo ve con incluirBajas → reactivación con credenciales nuevas
        long pacId = pac.get("id").asLong();
        mvc.perform(post("/api/v1/pacientes/" + pacId + "/baja").header("Authorization", "Bearer " + admin).contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("motivo", "prueba")))).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/pacientes?q=40123456").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(0));
        mvc.perform(get("/api/v1/pacientes?q=40123456&incluirBajas=true").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].estado").value("BAJA"));
        mvc.perform(post("/api/v1/pacientes/" + pacId + "/reactivar").header("Authorization", "Bearer " + nutri)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/pacientes/" + pacId + "/reactivar").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.username").value("40123456"));
        assertThat(jdbc.queryForObject("SELECT p.estado + '/' + u.estado + '/' + CAST(u.debe_cambiar_password AS VARCHAR) FROM paciente p "
                + "JOIN usuario u ON u.id = p.usuario_id WHERE p.id = ?", String.class, pacId)).isEqualTo("ACTIVO/ACTIVO/1");
        assertThat(jdbc.queryForObject("SELECT fecha_baja FROM paciente WHERE id = ?", Object.class, pacId)).isNull();
        mvc.perform(post("/api/v1/pacientes/" + pacId + "/reactivar").header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.code").value("REGLA_NEGOCIO"));
    }

    @Test
    @Order(4)
    void bitacoraYAccesosSonInmutables() {
        assertThatThrownBy(() -> jdbc.update("UPDATE bitacora_evento SET descripcion = 'x'")).hasMessageContaining("solo lectura");
        assertThatThrownBy(() -> jdbc.update("DELETE FROM registro_acceso")).hasMessageContaining("solo lectura");
    }

    @Test
    @Order(5)
    void logoutRevocaElToken() throws Exception {
        String t = login("admin", "AdminTest2025");
        mvc.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + t)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + t)).andExpect(status().isUnauthorized());
    }
}
