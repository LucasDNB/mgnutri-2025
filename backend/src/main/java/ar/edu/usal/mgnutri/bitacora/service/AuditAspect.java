package ar.edu.usal.mgnutri.bitacora.service;

import ar.edu.usal.mgnutri.bitacora.domain.BitacoraEvento;
import ar.edu.usal.mgnutri.common.error.BusinessException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registra en bitácora cada método anotado con {@link Auditable}. Se ejecuta por fuera de la
 * transacción del servicio (orden máximo), de modo que un fallo al confirmar se registra como ERROR.
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);
    private final ExpressionParser parser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer nombres = new DefaultParameterNameDiscoverer();
    private final BitacoraService bitacora;

    public AuditAspect(BitacoraService bitacora) {
        this.bitacora = bitacora;
    }

    @Around("@annotation(auditable)")
    public Object auditar(ProceedingJoinPoint pjp, Auditable auditable) throws Throwable {
        Auditoria.limpiar();
        Object resultado;
        try {
            resultado = pjp.proceed();
        } catch (Throwable ex) {
            Auditoria.Datos datos = Auditoria.tomar();
            Map<String, Object> detalle = new LinkedHashMap<>(datos.detalle);
            String codigo = ex instanceof BusinessException be ? be.getCode().name() : ex.getClass().getSimpleName();
            detalle.put("error", codigo);
            String id = datos.entidadId != null ? datos.entidadId : evaluar(pjp, auditable.entidadId(), null, false);
            String desc = datos.descripcion != null ? datos.descripcion : evaluar(pjp, auditable.descripcion(), null, true);
            bitacora.registrar(auditable.modulo(), auditable.tipo(), auditable.entidad(), id,
                    (desc == null ? auditable.tipo().name() + " " + auditable.entidad() : desc) + " (error: " + codigo + ")",
                    BitacoraEvento.Resultado.ERROR, detalle);
            throw ex;
        }
        Auditoria.Datos datos = Auditoria.tomar();
        String id = datos.entidadId != null ? datos.entidadId : evaluar(pjp, auditable.entidadId(), resultado, false);
        String desc = datos.descripcion != null ? datos.descripcion : evaluar(pjp, auditable.descripcion(), resultado, true);
        bitacora.registrar(auditable.modulo(), auditable.tipo(), auditable.entidad(), id,
                desc == null ? auditable.tipo().name() + " " + auditable.entidad() : desc,
                BitacoraEvento.Resultado.OK, datos.detalle);
        return resultado;
    }

    private String evaluar(ProceedingJoinPoint pjp, String expr, Object resultado, boolean plantilla) {
        if (expr == null || expr.isBlank()) {
            return null;
        }
        try {
            MethodSignature firma = (MethodSignature) pjp.getSignature();
            MethodBasedEvaluationContext ctx = new MethodBasedEvaluationContext(pjp.getTarget(), firma.getMethod(),
                    pjp.getArgs(), nombres);
            ctx.setVariable("result", resultado);
            Object v = plantilla
                    ? parser.parseExpression(expr, ParserContext.TEMPLATE_EXPRESSION).getValue(ctx)
                    : parser.parseExpression(expr).getValue(ctx);
            return v == null ? null : String.valueOf(v);
        } catch (RuntimeException e) {
            log.debug("No se pudo evaluar la expresión de auditoría '{}': {}", expr, e.getMessage());
            return null;
        }
    }
}
