package ar.edu.usal.mgnutri.common.error;

import org.springframework.http.HttpStatus;

/** Catálogo de códigos de error estables usados por el frontend (docs/04-convenciones-api.md). */
public enum ErrorCode {
    VALIDACION(HttpStatus.BAD_REQUEST, "Datos inválidos"),
    NO_ENCONTRADO(HttpStatus.NOT_FOUND, "No encontrado"),
    DUPLICADO(HttpStatus.CONFLICT, "Registro duplicado"),
    CONCURRENCIA(HttpStatus.CONFLICT, "Conflicto de edición"),
    SIN_PERMISO(HttpStatus.FORBIDDEN, "Sin permiso"),
    NO_AUTENTICADO(HttpStatus.UNAUTHORIZED, "No autenticado"),
    CREDENCIALES_INVALIDAS(HttpStatus.UNAUTHORIZED, "Credenciales inválidas"),
    USUARIO_BLOQUEADO(HttpStatus.LOCKED, "Usuario bloqueado"),
    DEBE_CAMBIAR_PASSWORD(HttpStatus.PRECONDITION_REQUIRED, "Debe cambiar la contraseña"),
    DEBE_ACEPTAR_TERMINOS(HttpStatus.PRECONDITION_REQUIRED, "Debe aceptar los términos"),
    TOKEN_EXPIRADO(HttpStatus.BAD_REQUEST, "Enlace vencido"),
    PASSWORD_DEBIL(HttpStatus.BAD_REQUEST, "Contraseña débil"),
    DEMASIADAS_SOLICITUDES(HttpStatus.TOO_MANY_REQUESTS, "Demasiadas solicitudes"),
    TURNO_PROXIMO_EXISTENTE(HttpStatus.UNPROCESSABLE_ENTITY, "Regla de negocio"),
    TURNO_DENTRO_24H(HttpStatus.UNPROCESSABLE_ENTITY, "Regla de negocio"),
    HORARIO_NO_DISPONIBLE(HttpStatus.CONFLICT, "Horario no disponible"),
    TURNOS_AFECTADOS(HttpStatus.CONFLICT, "Turnos afectados"),
    PLAN_ACTIVO_EXISTENTE(HttpStatus.CONFLICT, "Plan activo existente"),
    RUTINA_ACTIVA_EXISTENTE(HttpStatus.CONFLICT, "Rutina activa existente"),
    PLAN_MIN_4_COMIDAS(HttpStatus.UNPROCESSABLE_ENTITY, "Regla de negocio"),
    PACIENTE_NO_ASIGNADO(HttpStatus.FORBIDDEN, "Paciente no asignado"),
    SIN_PLAN_ASIGNADO(HttpStatus.UNPROCESSABLE_ENTITY, "Sin plan asignado"),
    SIN_RUTINA_ASIGNADA(HttpStatus.UNPROCESSABLE_ENTITY, "Sin rutina asignada"),
    VALOR_FUERA_DE_RANGO(HttpStatus.UNPROCESSABLE_ENTITY, "Valor fuera de rango"),
    DEUDA_DUPLICADA(HttpStatus.CONFLICT, "Deuda duplicada"),
    SIN_DATOS(HttpStatus.UNPROCESSABLE_ENTITY, "Sin datos"),
    SIN_PROFESIONALES(HttpStatus.UNPROCESSABLE_ENTITY, "Sin profesionales"),
    KCAL_INCOHERENTES(HttpStatus.UNPROCESSABLE_ENTITY, "Calorías incoherentes"),
    BACKUP_EN_CURSO(HttpStatus.CONFLICT, "Backup en curso"),
    REGLA_NEGOCIO(HttpStatus.UNPROCESSABLE_ENTITY, "Regla de negocio"),
    ERROR_INTERNO(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno");

    private final HttpStatus status;
    private final String titulo;

    ErrorCode(HttpStatus status, String titulo) {
        this.status = status;
        this.titulo = titulo;
    }

    public HttpStatus status() {
        return status;
    }

    public String titulo() {
        return titulo;
    }
}
