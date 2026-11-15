# Plan 01 · Gestión de pacientes

## Datos
Tablas: `paciente`, `usuario`, `usuario_rol`, `paciente_profesional` (ver `docs/03-modelo-de-datos.md §2`). Migración V3.

## API
| Método | Ruta | Rol | Descripción | CU |
|---|---|---|---|---|
| GET | `/pacientes?q=&estado=ACTIVO&profesionalId=&page=&size=&sort=` | ADMIN, NUTRI, ENTR (sólo asignados) | Listado paginado | CU-02/03/29 |
| GET | `/pacientes/{id}` | ADMIN; NUTRI/ENTR asignados | Ficha | CU-02 |
| POST | `/pacientes` | ADMIN | Alta | CU-01 |
| PUT | `/pacientes/{id}` | ADMIN | Modificación (con `version`) | CU-02 |
| DELETE | `/pacientes/{id}` body `{motivo, notificarCancelaciones}` | ADMIN | Baja lógica | CU-03 |
| POST | `/pacientes/{id}/reenviar-credenciales` | ADMIN | D-05 | CU-37 |
| GET | `/pacientes/{id}/profesionales` | ADMIN, asignados | Asignaciones | CU-29 |
| POST | `/pacientes/{id}/profesionales` body `{asignaciones:[{profesionalId, area}]}` | ADMIN | Asignar | CU-29 |
| DELETE | `/pacientes/{id}/profesionales/{asignacionId}` | ADMIN | Desasignar | CU-29 |
| GET / PATCH | `/me/perfil` | PACIENTE | Ver / editar email y domicilio | US-01.5 |

### Ejemplo alta
```http
POST /api/v1/pacientes
{
  "nombre": "Ana", "apellido": "Gómez",
  "tipoDocumento": "DNI", "nroDocumento": "40123456",
  "fechaNacimiento": "1997-03-14", "sexo": "F",
  "telefonoWhatsapp": "+5491134567890", "email": "ana@mail.com",
  "domicilio": "Pilar, Bs. As.", "objetivo": "Mejorar composición corporal",
  "antecedentesSalud": "Hipotiroidismo informado por la paciente",
  "asignaciones": [ { "profesionalId": 3, "area": "NUTRICION" } ]
}
→ 201 Location: /api/v1/pacientes/57
{ "id": 57, "username": "40123456", "notificacion": { "estado": "PENDIENTE" }, "passwordTemporal": null }
```
`passwordTemporal` sólo se devuelve (una vez) si el paciente no tiene teléfono válido o WhatsApp está deshabilitado (CA-01.1.6).

### DTOs
`PacienteCreateRequest`, `PacienteUpdateRequest(version, ...)`, `PacienteResponse`, `PacienteListItem`, `BajaRequest(motivo, notificarCancelaciones)`, `AsignacionRequest`, `AsignacionResponse`.

## Backend
- `pacientes.controller.PacienteController`, `AsignacionController`, `MiPerfilController`.
- `pacientes.service.PacienteService`
  - `crear(req)`: valida unicidad de documento contra `PacienteDao` y `ProfesionalDao`; `UsuarioService.crearUsuario(doc, PACIENTE)` → devuelve contraseña temporal; persiste; publica `PacienteCreado(pacienteId, passwordTemporal)`; `@Auditable(PACIENTES, ALTA)`.
  - `modificar(id, req)`, `darDeBaja(id, req)` (orquesta `TurnoService.cancelarFuturosDePaciente`, `PlanService.finalizarActivo`, `RutinaService.finalizarActiva`, `UsuarioService.darDeBaja`), `reenviarCredenciales(id)`.
- `pacientes.service.AsignacionService` con validaciones CA-01.4.x.
- `common.security.AccesoPacienteGuard` (usado por todos los módulos): `assertPuedeVer(pacienteId)`, `assertPuedeEditar(pacienteId, Area)`.
- `pacientes.dao.PacienteDao` (búsqueda: `WHERE estado='ACTIVO' AND (apellido LIKE ... COLLATE Latin1_General_CI_AI OR ...)`), `PacienteProfesionalDao`.
- Listener `notificaciones.listener.CredencialesListener` (`@TransactionalEventListener(AFTER_COMMIT)` no: se usa `BEFORE_COMMIT` para insertar en el outbox dentro de la misma transacción — D-35).

## Frontend
- `admin/pacientes.html` (listado + buscador + botón Nuevo), `admin/paciente-form.html` (alta/edición), `admin/paciente-ficha.html` (pestañas Datos · Profesionales · Cuenta · Turnos · Planes/Rutinas · Evolución).
- `profesional/pacientes.html` (sólo asignados) y `profesional/paciente-ficha.html`.
- `paciente/perfil.html`.
- Validadores en `js/core/validators.js`: `documento`, `telefonoAR` (normaliza a E.164), `email`, `fechaNacimiento`.
- Modal de confirmación de baja con texto de advertencia y campo motivo.

## Pruebas
| CA | Tipo | Caso |
|---|---|---|
| 01.1.2/3 | Integración | Alta crea paciente, usuario PACIENTE, notificación PENDIENTE y evento bitácora |
| 01.1.4 | Unit + Postman | Cada obligatorio faltante → 400 con `errors[]` |
| 01.1.5 | Integración | Documento existente en paciente y en profesional → 409 |
| 01.1.6 | Unit | Sin teléfono válido → devuelve `passwordTemporal` y crea alerta |
| 01.2.5 | Integración | Versión desactualizada → 409 CONCURRENCIA |
| 01.3.2 | Integración | Baja cancela turnos futuros, finaliza plan/rutina, revoca tokens, usuario no puede loguear |
| 01.4.3/4 | Unit | Sin profesionales / duplicado |
| 01.5.2 | Seguridad | NUTRI no asignado → 403 en `GET /pacientes/{id}` |

## Tareas
- [ ] T-01.1 Migración V3, entidades, DAOs (2 h backend, junto con T-01.2)
- [ ] T-01.2 `PacienteService` + `AsignacionService` + controllers + guard de acceso
- [ ] T-01.3 Pantallas admin (listado, form, ficha, asignación) y perfil paciente (3 h)
- [ ] T-01.4 Tests de integración + carpeta Postman "01-pacientes" (1 h)
