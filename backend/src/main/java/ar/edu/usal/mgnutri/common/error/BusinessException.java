package ar.edu.usal.mgnutri.common.error;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Excepción de negocio: lleva un código estable y un mensaje apto para mostrar al usuario. */
public class BusinessException extends RuntimeException {

    private final ErrorCode code;
    private final Map<String, Object> extras = new LinkedHashMap<>();
    private final List<CampoError> errores;

    public BusinessException(ErrorCode code, String detalle) {
        this(code, detalle, List.of());
    }

    public BusinessException(ErrorCode code, String detalle, List<CampoError> errores) {
        super(detalle);
        this.code = code;
        this.errores = errores == null ? List.of() : errores;
    }

    public static BusinessException noEncontrado(String que) {
        return new BusinessException(ErrorCode.NO_ENCONTRADO, que + " no encontrado.");
    }

    public static BusinessException validacion(String campo, String mensaje) {
        return new BusinessException(ErrorCode.VALIDACION, mensaje, List.of(new CampoError(campo, mensaje)));
    }

    public static BusinessException sinPermiso() {
        return new BusinessException(ErrorCode.SIN_PERMISO, "No tenés permiso para realizar esta acción.");
    }

    public BusinessException con(String clave, Object valor) {
        extras.put(clave, valor);
        return this;
    }

    public ErrorCode getCode() {
        return code;
    }

    public Map<String, Object> getExtras() {
        return Collections.unmodifiableMap(extras);
    }

    public List<CampoError> getErrores() {
        return errores;
    }

    public record CampoError(String campo, String mensaje) {}
}
