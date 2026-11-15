# Plan 14 · Bitácora

## Datos
`bitacora_evento` (V2) con protección:
```sql
CREATE TRIGGER trg_bitacora_inmutable ON bitacora_evento
INSTEAD OF UPDATE, DELETE AS
BEGIN THROW 51000, 'La bitácora es de solo lectura', 1; END;
DENY UPDATE, DELETE ON bitacora_evento TO mgnutri_app;
```
(Las migraciones corren con un usuario distinto, propietario del esquema.)

## Mecanismo
```java
@Auditable(modulo = Modulo.PACIENTES, tipo = TipoEvento.ALTA, entidad = "Paciente",
           descripcion = "Alta de paciente #{#result.id} {#result.apellido}")
public PacienteResponse crear(PacienteCreateRequest req) { ... }
```
- `AuditAspect` (`@Around`): ejecuta el método; en éxito registra `OK` con `entidadId` resuelto por SpEL; en excepción registra `ERROR` con `code` de `BusinessException` o tipo de excepción, y relanza.
- `BitacoraService.registrar(EventoBitacora)` con `@Transactional(propagation = REQUIRES_NEW)` y try/catch que loguea a `ERROR` si falla (CA-14.1.3).
- `CambiosDetector` (compara DTO anterior/nuevo por reflexión sobre campos anotados `@AuditarCambio`; campos `@Sensible` se informan sólo por nombre).
- Contexto: usuario/roles desde `SecurityContext`; IP desde `RequestContextHolder` (o "SISTEMA" en jobs).

## API
| Método | Ruta | Rol |
|---|---|---|
| GET | `/bitacora?desde=&hasta=&usuarioId=&modulo=&tipo=&entidad=&entidadId=&resultado=&page=&size=` | ADMIN (`VER_AUDITORIA`) |
| GET | `/bitacora/{id}` | idem |
| GET | `/bitacora/exportar?…filtros…` (xlsx) | idem |

No existen POST/PUT/DELETE.

## Frontend
- `admin/bitacora.html`: filtros en panel colapsable (en celular), tabla paginada, modal de detalle con JSON formateado.

## Pruebas
- Unit `AuditAspectTest`: éxito → OK con id; excepción → ERROR y se relanza; fallo de bitácora no interrumpe.
- Integración: alta de paciente con rollback forzado → existe evento ERROR; `UPDATE bitacora_evento` con usuario de app → excepción SQL.
- Integración: filtros combinados y paginación; NUTRI → 403.
- Revisión: cada servicio de escritura tiene `@Auditable` (test de arquitectura con ArchUnit: métodos públicos `crear|modificar|eliminar|darDeBaja|anular*` en `..service..` deben estar anotados).

## Tareas
- [ ] T-14.1 Migración V2 + entidad + `BitacoraService` + `AuditAspect` (S1)
- [ ] T-14.2 Consulta y pantalla de bitácora (S1)
- [ ] T-14.3 Tests y regla ArchUnit (total 4 h)
