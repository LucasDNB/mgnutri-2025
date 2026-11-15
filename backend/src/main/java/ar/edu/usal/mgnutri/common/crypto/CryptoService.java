package ar.edu.usal.mgnutri.common.crypto;

import ar.edu.usal.mgnutri.config.MgnutriProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/** Cifrado AES-256-GCM de datos personales sensibles en columna (D-30). */
@Component
public class CryptoService {

    private static final String PREFIJO = "v1:";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static volatile CryptoService instancia;

    private final SecureRandom random = new SecureRandom();
    private final SecretKeySpec clave;

    public CryptoService(MgnutriProperties props) {
        String base64 = props.crypto() == null ? null : props.crypto().key();
        if (base64 == null || base64.isBlank()) {
            throw new IllegalStateException("Falta la clave de cifrado DATA_KEY_BASE64 (32 bytes en Base64)");
        }
        byte[] bytes = Base64.getDecoder().decode(base64.trim());
        if (bytes.length != 32) {
            throw new IllegalStateException("DATA_KEY_BASE64 debe decodificar a 32 bytes (AES-256)");
        }
        this.clave = new SecretKeySpec(bytes, "AES");
    }

    @PostConstruct
    void registrar() {
        instancia = this;
    }

    static CryptoService instancia() {
        if (instancia == null) {
            throw new IllegalStateException("CryptoService no inicializado");
        }
        return instancia;
    }

    public String cifrar(String plano) {
        if (plano == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, clave, new GCMParameterSpec(TAG_BITS, iv));
            byte[] cifrado = cipher.doFinal(plano.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buf = ByteBuffer.allocate(iv.length + cifrado.length).put(iv).put(cifrado);
            return PREFIJO + Base64.getEncoder().encodeToString(buf.array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Error al cifrar", e);
        }
    }

    public String descifrar(String valor) {
        if (valor == null) {
            return null;
        }
        if (!valor.startsWith(PREFIJO)) {
            return valor; // dato heredado sin cifrar
        }
        try {
            byte[] todo = Base64.getDecoder().decode(valor.substring(PREFIJO.length()));
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, clave, new GCMParameterSpec(TAG_BITS, todo, 0, IV_BYTES));
            byte[] plano = cipher.doFinal(todo, IV_BYTES, todo.length - IV_BYTES);
            return new String(plano, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Error al descifrar", e);
        }
    }
}
