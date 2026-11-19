package ar.edu.usal.mgnutri.seguridad.service;

import ar.edu.usal.mgnutri.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Lectura del costo de un hash BCrypt, que decide si se rehashea en el login. */
class CostoBcryptTest {

    @Test
    void leeElCostoDelHash() {
        assertEquals(12, AuthService.costo("$2a$12$C6UzMDM.H6dfI/f/IKcEeO7FHq9aBp8.0NUIyRf4tTOR8f4CRAg8a"));
        assertEquals(SecurityConfig.BCRYPT_COSTO,
                AuthService.costo(new BCryptPasswordEncoder(SecurityConfig.BCRYPT_COSTO).encode("x")));
    }

    @Test
    void hashSinFormatoBcryptDevuelveMenosUno() {
        assertEquals(-1, AuthService.costo("texto"));
        assertEquals(-1, AuthService.costo(""));
    }
}
