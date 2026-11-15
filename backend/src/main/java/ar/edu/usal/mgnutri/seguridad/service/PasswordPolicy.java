package ar.edu.usal.mgnutri.seguridad.service;

import ar.edu.usal.mgnutri.common.error.BusinessException;
import ar.edu.usal.mgnutri.common.error.ErrorCode;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/** Política de contraseñas (CA-13.3.3) y generación de contraseñas temporales (D-04). */
@Component
public class PasswordPolicy {

    private static final String LETRAS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz";
    private static final String NUMEROS = "23456789";
    private static final String TODOS = LETRAS + NUMEROS;
    private final SecureRandom random = new SecureRandom();

    /** Valida: ≥ 8 caracteres, al menos una letra y un número, distinta del documento. */
    public void validar(String nueva, String documento) {
        if (nueva == null || nueva.length() < 8) {
            throw new BusinessException(ErrorCode.PASSWORD_DEBIL, "La contraseña debe tener al menos 8 caracteres.");
        }
        if (nueva.length() > 64) {
            throw new BusinessException(ErrorCode.PASSWORD_DEBIL, "La contraseña no puede superar los 64 caracteres.");
        }
        if (!nueva.matches(".*[A-Za-zÁÉÍÓÚáéíóúÑñ].*") || !nueva.matches(".*\\d.*")) {
            throw new BusinessException(ErrorCode.PASSWORD_DEBIL, "La contraseña debe incluir al menos una letra y un número.");
        }
        if (documento != null && nueva.contains(documento)) {
            throw new BusinessException(ErrorCode.PASSWORD_DEBIL, "La contraseña no puede contener tu número de documento.");
        }
    }

    /** Contraseña temporal aleatoria de 12 caracteres con letras y números. */
    public String temporal() {
        StringBuilder sb = new StringBuilder(12);
        sb.append(LETRAS.charAt(random.nextInt(LETRAS.length())));
        sb.append(NUMEROS.charAt(random.nextInt(NUMEROS.length())));
        for (int i = 0; i < 10; i++) {
            sb.append(TODOS.charAt(random.nextInt(TODOS.length())));
        }
        return sb.toString();
    }
}
