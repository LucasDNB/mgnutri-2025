package ar.edu.usal.mgnutri.configuracion.controller;

import ar.edu.usal.mgnutri.config.MgnutriProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** CA-00.1.2: estado del servicio y de la base de datos. */
@RestController
@RequestMapping("/api/v1/salud")
public class SaludController {

    private final JdbcTemplate jdbc;
    private final MgnutriProperties props;

    public SaludController(JdbcTemplate jdbc, MgnutriProperties props) {
        this.jdbc = jdbc;
        this.props = props;
    }

    @GetMapping
    public ResponseEntity<Map<String, String>> salud() {
        String bd;
        try {
            jdbc.queryForObject("SELECT 1", Integer.class);
            bd = "OK";
        } catch (RuntimeException e) {
            bd = "ERROR";
        }
        Map<String, String> r = new LinkedHashMap<>();
        r.put("estado", "OK".equals(bd) ? "OK" : "DEGRADADO");
        r.put("version", props.version());
        r.put("bd", bd);
        return ResponseEntity.status("OK".equals(bd) ? 200 : 503).body(r);
    }
}
