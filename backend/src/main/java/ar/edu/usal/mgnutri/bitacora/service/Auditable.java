package ar.edu.usal.mgnutri.bitacora.service;

import ar.edu.usal.mgnutri.bitacora.domain.Modulo;
import ar.edu.usal.mgnutri.bitacora.domain.TipoEvento;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca un método de servicio cuya ejecución se registra en la bitácora (CU-35).
 * {@code entidadId} y {@code descripcion} admiten SpEL: argumentos por nombre y {@code #result}.
 * La descripción es una plantilla: {@code "Alta de paciente #{#result.id}"}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {
    Modulo modulo();

    TipoEvento tipo();

    String entidad();

    String entidadId() default "";

    String descripcion() default "";
}
