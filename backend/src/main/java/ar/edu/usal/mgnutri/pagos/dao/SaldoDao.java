package ar.edu.usal.mgnutri.pagos.dao;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Lectura de la vista v_saldo_paciente (D-19). */
@Repository
public class SaldoDao {

    private final NamedParameterJdbcTemplate jdbc;

    public SaldoDao(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public BigDecimal saldo(Long pacienteId) {
        List<BigDecimal> r = jdbc.queryForList("SELECT saldo FROM v_saldo_paciente WHERE paciente_id = :id",
                new MapSqlParameterSource("id", pacienteId), BigDecimal.class);
        return r.isEmpty() || r.get(0) == null ? BigDecimal.ZERO : r.get(0);
    }

    public Map<Long, BigDecimal> saldos(Collection<Long> ids) {
        Map<Long, BigDecimal> m = new HashMap<>();
        if (ids == null || ids.isEmpty()) {
            return m;
        }
        jdbc.query("SELECT paciente_id, saldo FROM v_saldo_paciente WHERE paciente_id IN (:ids)",
                new MapSqlParameterSource("ids", ids),
                rs -> {
                    m.put(rs.getLong(1), rs.getBigDecimal(2));
                });
        return m;
    }

    /** Pacientes con saldo deudor (> 0). */
    public Map<Long, BigDecimal> deudores() {
        Map<Long, BigDecimal> m = new HashMap<>();
        jdbc.query("SELECT paciente_id, saldo FROM v_saldo_paciente WHERE saldo > 0", new MapSqlParameterSource(),
                rs -> {
                    m.put(rs.getLong(1), rs.getBigDecimal(2));
                });
        return m;
    }
}
