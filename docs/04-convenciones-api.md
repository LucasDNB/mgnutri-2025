# 04 · Convenciones de la API REST

## Generales
- Base: `https://<app>.azurewebsites.net/api/v1`.
- JSON UTF-8, `camelCase`. Fechas `YYYY-MM-DD`, fecha-hora ISO-8601 con offset (`2025-08-20T14:30:00-03:00`), dinero como número con 2 decimales.
- Recursos en plural y español: `/pacientes`, `/profesionales`, `/turnos`, `/planes-alimentarios`, `/rutinas`, `/alimentos`, `/ejercicios`, `/pagos`, `/cargos`, `/reportes`, `/bitacora`, `/accesos`, `/backups`.
- Recursos del usuario autenticado bajo `/me/...` (p. ej. `/me/plan-alimentario`, `/me/turnos`). Evita exponer IDs ajenos y simplifica la autorización del paciente.
- Documentación OpenAPI con springdoc en `/swagger-ui.html` (sólo `local` y `dev`).

## Métodos y códigos
| Operación | Método | Éxito | Errores típicos |
|---|---|---|---|
| Listar | GET colección | 200 | 400 filtros inválidos |
| Obtener | GET `/{id}` | 200 | 404 |
| Crear | POST | 201 + `Location` | 400, 409 duplicado, 422 regla de negocio |
| Modificar | PUT `/{id}` (completo) / PATCH (parcial) | 200 | 400, 404, 409 versión (concurrencia), 422 |
| Baja | DELETE `/{id}` | 204 | 404, 422 |
| Acción | POST `/{id}/<accion>` (p. ej. `/turnos/{id}/cancelar`) | 200 | 422 |
| Archivo | GET `.../pdf` o `?formato=pdf|xlsx` | 200 `application/pdf` / `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` + `Content-Disposition: attachment` | |

401 sin token o token inválido/revocado · 403 rol o pertenencia insuficiente · 423 usuario bloqueado · 428 debe cambiar contraseña / aceptar términos.

## Paginación, orden y filtros
`?page=0&size=20&sort=apellido,asc&q=texto&desde=2025-08-01&hasta=2025-08-31`.
Respuesta paginada:
```json
{ "content": [ ... ], "page": 0, "size": 20, "totalElements": 135, "totalPages": 7 }
```
`size` máximo 100.

## Errores (RFC 7807)
```json
{
  "type": "https://mgnutri/errores/turno-proximo-existente",
  "title": "Regla de negocio",
  "status": 422,
  "detail": "Ya tenés un turno con este profesional el 22/08 a las 10:00. Podés modificarlo o cancelarlo.",
  "code": "TURNO_PROXIMO_EXISTENTE",
  "instance": "/api/v1/me/turnos",
  "errors": [ { "campo": "inicio", "mensaje": "..." } ]
}
```
`code` es estable y lo usa el frontend para acciones (p. ej. redirigir a "Modificar/Cancelar turnos" en CU-07 alterno). `detail` se muestra tal cual al usuario.

Catálogo de códigos (se amplía en cada spec): `VALIDACION`, `NO_ENCONTRADO`, `DUPLICADO`, `CONCURRENCIA`, `SIN_PERMISO`, `CREDENCIALES_INVALIDAS`, `USUARIO_BLOQUEADO`, `DEBE_CAMBIAR_PASSWORD`, `DEBE_ACEPTAR_TERMINOS`, `TOKEN_EXPIRADO`, `TURNO_PROXIMO_EXISTENTE`, `TURNO_DENTRO_24H`, `HORARIO_NO_DISPONIBLE`, `PLAN_ACTIVO_EXISTENTE`, `PLAN_MIN_4_COMIDAS`, `PACIENTE_NO_ASIGNADO`, `SIN_PLAN_ASIGNADO`, `SIN_RUTINA_ASIGNADA`, `VALOR_FUERA_DE_RANGO`, `DEUDA_DUPLICADA`, `SIN_DATOS`, `BACKUP_EN_CURSO`.

## Concurrencia
Entidades editables exponen `version`; el cliente la reenvía en PUT/PATCH. Conflicto → 409 `CONCURRENCIA`.

## Seguridad
- `Authorization: Bearer <jwt>` en todas las rutas salvo `POST /auth/login`, `POST /auth/recupero`, `POST /auth/recupero/confirmar`, `GET /publico/contacto`, `GET /terminos/vigentes`.
- CORS: sólo el origen del frontend.
- Rate limit simple en `/auth/**` (10 req/min por IP) con filtro en memoria.

## Auditoría
Toda operación de escritura y toda generación de reporte o descarga queda en bitácora (ver spec 14). Los controllers no auditan: lo hace el servicio.

## Colección Postman
`postman/mgnutri.postman_collection.json` con una carpeta por spec y tests (`pm.test`) para cada criterio de aceptación de API. Se corre en CI con Newman contra `local` (SQL Server en contenedor).
