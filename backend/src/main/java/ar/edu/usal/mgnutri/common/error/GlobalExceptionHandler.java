package ar.edu.usal.mgnutri.common.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.util.List;
import java.util.Locale;

/** Traduce excepciones a Problem Details (RFC 7807) con mensajes en español. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public static ProblemDetail problema(ErrorCode code, String detalle, String instancia,
                                         List<BusinessException.CampoError> errores) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(code.status(), detalle);
        pd.setType(URI.create("https://mgnutri/errores/" + code.name().toLowerCase(Locale.ROOT).replace('_', '-')));
        pd.setTitle(code.titulo());
        pd.setProperty("code", code.name());
        if (instancia != null) {
            pd.setInstance(URI.create(instancia));
        }
        if (errores != null && !errores.isEmpty()) {
            pd.setProperty("errors", errores);
        }
        return pd;
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> negocio(BusinessException ex, HttpServletRequest req) {
        ProblemDetail pd = problema(ex.getCode(), ex.getMessage(), req.getRequestURI(), ex.getErrores());
        ex.getExtras().forEach(pd::setProperty);
        return ResponseEntity.status(ex.getCode().status()).body(pd);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> validacion(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<BusinessException.CampoError> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> new BusinessException.CampoError(f.getField(), f.getDefaultMessage()))
                .toList();
        String detalle = errores.isEmpty() ? "Datos inválidos." : "Revisá los datos ingresados: "
                + String.join("; ", errores.stream().map(e -> e.campo() + ": " + e.mensaje()).toList());
        return ResponseEntity.badRequest().body(problema(ErrorCode.VALIDACION, detalle, req.getRequestURI(), errores));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> restricciones(ConstraintViolationException ex, HttpServletRequest req) {
        List<BusinessException.CampoError> errores = ex.getConstraintViolations().stream()
                .map(v -> new BusinessException.CampoError(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return ResponseEntity.badRequest().body(problema(ErrorCode.VALIDACION, "Revisá los datos ingresados.", req.getRequestURI(), errores));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<ProblemDetail> malformado(Exception ex, HttpServletRequest req) {
        return ResponseEntity.badRequest().body(problema(ErrorCode.VALIDACION,
                "La solicitud tiene un formato inválido.", req.getRequestURI(), null));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> concurrencia(ObjectOptimisticLockingFailureException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problema(ErrorCode.CONCURRENCIA,
                "Otro usuario modificó estos datos mientras los editabas. Recargá la página e intentá de nuevo.",
                req.getRequestURI(), null));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> integridad(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("Violación de integridad en {}: {}", req.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problema(ErrorCode.DUPLICADO,
                "La operación entra en conflicto con datos existentes.", req.getRequestURI(), null));
    }

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<ProblemDetail> acceso(Exception ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problema(ErrorCode.SIN_PERMISO,
                "No tenés permiso para realizar esta acción.", req.getRequestURI(), null));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> metodo(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        ProblemDetail pd = problema(ErrorCode.VALIDACION, "Operación no soportada.", req.getRequestURI(), null);
        pd.setStatus(HttpStatus.METHOD_NOT_ALLOWED.value());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(pd);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> sinRecurso(NoResourceFoundException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problema(ErrorCode.NO_ENCONTRADO,
                "Recurso no encontrado.", req.getRequestURI(), null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> inesperado(Exception ex, HttpServletRequest req) {
        log.error("Error inesperado en {}", req.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problema(ErrorCode.ERROR_INTERNO,
                "Ocurrió un error inesperado. Intentá nuevamente en unos minutos.", req.getRequestURI(), null));
    }
}
