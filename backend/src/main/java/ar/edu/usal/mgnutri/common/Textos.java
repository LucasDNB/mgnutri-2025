package ar.edu.usal.mgnutri.common;

import ar.edu.usal.mgnutri.common.error.BusinessException;
import ar.edu.usal.mgnutri.common.error.ErrorCode;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;

/** Utilidades de texto, teléfonos y concurrencia. */
public final class Textos {

    private Textos() {
    }

    /** Minúsculas sin acentos ni espacios repetidos (búsquedas y unicidad). */
    public static String normalizar(String s) {
        if (s == null) {
            return "";
        }
        String sinAcentos = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinAcentos.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }

    public static boolean contiene(String texto, String busqueda) {
        return normalizar(texto).contains(normalizar(busqueda));
    }

    public static String vacioANull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    /**
     * Normaliza un teléfono argentino a E.164 (+549 + área + número, 13 dígitos).
     * Acepta formatos locales: 011 15-3456-7890, 11 3456 7890, +54 9 11 3456-7890.
     * Devuelve null si no es válido.
     */
    public static String telefonoE164(String entrada) {
        if (entrada == null || entrada.isBlank()) {
            return null;
        }
        String d = entrada.replaceAll("\\D", "");
        if (d.startsWith("549") && d.length() == 13) {
            return "+" + d;
        }
        if (d.startsWith("54") && d.length() == 12) {
            return "+549" + d.substring(2);
        }
        if (d.startsWith("0")) {
            d = d.substring(1);
        }
        // área + 15 + número (p. ej. 11 15 34567890)
        if (d.length() == 12) {
            for (int area = 2; area <= 4; area++) {
                if (d.startsWith("15", area)) {
                    String sin15 = d.substring(0, area) + d.substring(area + 2);
                    if (sin15.length() == 10) {
                        return "+549" + sin15;
                    }
                }
            }
        }
        if (d.length() == 10) {
            return "+549" + d;
        }
        return null;
    }

    public static boolean telefonoValido(String e164) {
        return e164 != null && e164.matches("\\+549\\d{10}");
    }

    /** Sólo dígitos, para wa.me y la API de WhatsApp. */
    public static String digitos(String telefono) {
        return telefono == null ? null : telefono.replaceAll("\\D", "");
    }

    public static String ultimosDigitos(String telefono, int n) {
        String d = digitos(telefono);
        return d == null || d.length() < n ? "" : d.substring(d.length() - n);
    }

    /** Control de concurrencia optimista con la versión reenviada por el cliente. */
    public static void verificarVersion(Integer actual, Integer enviada) {
        if (enviada != null && !Objects.equals(actual, enviada)) {
            throw new BusinessException(ErrorCode.CONCURRENCIA,
                    "Otro usuario modificó estos datos mientras los editabas. Recargá la página e intentá de nuevo.");
        }
    }
}
