package ar.edu.usal.mgnutri.seguridad.service;

import ar.edu.usal.mgnutri.seguridad.dao.TokenRecuperoDao;
import ar.edu.usal.mgnutri.seguridad.dao.TokenRevocadoDao;
import ar.edu.usal.mgnutri.seguridad.domain.TokenRevocado;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Revocación de tokens por jti (logout, CU-50) con caché en memoria. */
@Service
public class TokenRevocacionService {

    private final TokenRevocadoDao dao;
    private final TokenRecuperoDao recuperoDao;
    private final Clock clock;
    private final Map<String, Instant> cache = new ConcurrentHashMap<>();

    public TokenRevocacionService(TokenRevocadoDao dao, TokenRecuperoDao recuperoDao, Clock clock) {
        this.dao = dao;
        this.recuperoDao = recuperoDao;
        this.clock = clock;
    }

    @Transactional
    public void revocar(String jti, Instant expira) {
        if (jti == null) {
            return;
        }
        cache.put(jti, expira);
        if (!dao.existsById(jti)) {
            dao.save(new TokenRevocado(jti, expira));
        }
    }

    @Transactional(readOnly = true)
    public boolean revocado(String jti) {
        if (jti == null) {
            return false;
        }
        if (cache.containsKey(jti)) {
            return true;
        }
        boolean enBd = dao.existsById(jti);
        if (enBd) {
            cache.put(jti, clock.instant().plusSeconds(3600));
        }
        return enBd;
    }

    /** Limpieza diaria de tokens revocados y de recupero expirados (03:00). */
    @Scheduled(cron = "${mgnutri.jobs.limpieza-tokens-cron}", zone = "America/Argentina/Buenos_Aires")
    @Transactional
    public void limpiar() {
        Instant ahora = clock.instant();
        dao.eliminarExpirados(ahora);
        recuperoDao.eliminarExpirados(ahora.minusSeconds(7 * 24 * 3600L));
        cache.entrySet().removeIf(e -> e.getValue().isBefore(ahora));
    }
}
