package ar.edu.usal.mgnutri.bitacora.service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Contexto por hilo para que un servicio agregue información al evento que registrará el aspecto
 * (campos cambiados, id de entidad, descripción). Nunca se agregan datos sensibles.
 */
public final class Auditoria {

    private static final ThreadLocal<Datos> ACTUAL = ThreadLocal.withInitial(Datos::new);

    private Auditoria() {
    }

    public static void detalle(String clave, Object valor) {
        ACTUAL.get().detalle.put(clave, valor);
    }

    public static void entidadId(Object id) {
        ACTUAL.get().entidadId = id == null ? null : String.valueOf(id);
    }

    public static void descripcion(String descripcion) {
        ACTUAL.get().descripcion = descripcion;
    }

    static Datos tomar() {
        Datos d = ACTUAL.get();
        ACTUAL.remove();
        return d;
    }

    static void limpiar() {
        ACTUAL.remove();
    }

    static final class Datos {
        final Map<String, Object> detalle = new LinkedHashMap<>();
        String entidadId;
        String descripcion;
    }
}
