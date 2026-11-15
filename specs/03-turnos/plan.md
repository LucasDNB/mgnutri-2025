# Plan 03 · Gestión de turnos

## Datos
`turno`, `turno_historial`, `disponibilidad`, `bloqueo_agenda` (V4). Índice único filtrado `(profesional_id, inicio) WHERE estado='RESERVADO'` como red de seguridad ante carrera (CA-03.1.5): la violación se traduce a `HORARIO_NO_DISPONIBLE`.

## Algoritmo de huecos (`AgendaService.huecosLibres(profId, desde, hasta)`)
```
para cada día d en [desde, hasta]:
  para cada franja f de profId con f.diaSemana == d.dayOfWeek:
     t = d@f.horaDesde
     mientras t + dur <= d@f.horaHasta:
        si t >= ahora + 2h
           y no existe turno RESERVADO de profId que solape [t, t+dur)
           y no existe bloqueo que solape:
             agregar Hueco(t, t+dur, f.ubicacion)
        t += dur
```
Turnos y bloqueos del rango se cargan con 2 consultas y se resuelven en memoria (rango máx. 31 días).

## API
| Método | Ruta | Rol | CU |
|---|---|---|---|
| GET | `/profesionales/{id}/huecos?desde=&hasta=` | PACIENTE, NUTRI/ENTR, ADMIN | CU-07/08 |
| GET | `/me/turnos?estado=&desde=&hasta=` | PACIENTE | CU-10 |
| POST | `/me/turnos` `{profesionalId, inicio}` | PACIENTE | CU-07 |
| POST | `/me/turnos/{id}/reprogramar` `{inicio, version}` | PACIENTE | CU-08 |
| POST | `/me/turnos/{id}/cancelar` `{version}` | PACIENTE | CU-09 |
| GET | `/turnos?profesionalId=&pacienteId=&area=&estado=&desde=&hasta=&sort=` | NUTRI/ENTR (propios), ADMIN | CU-10 |
| GET | `/turnos/{id}` | involucrados, ADMIN | CU-10 |
| POST | `/turnos` `{pacienteId, profesionalId, inicio, forzar?}` | NUTRI/ENTR, ADMIN | RF-03 |
| POST | `/turnos/{id}/reprogramar` `{inicio, motivo, version}` | NUTRI/ENTR, ADMIN | RF-03 |
| POST | `/turnos/{id}/cancelar` `{motivo, version}` | NUTRI/ENTR, ADMIN | RF-03 |
| POST | `/turnos/{id}/estado` `{estado: ATENDIDO|AUSENTE, version}` | NUTRI/ENTR, ADMIN | RF-03 / RF-14 |

Respuesta de turno:
```json
{ "id": 812, "inicio": "2025-08-22T10:00:00-03:00", "fin": "2025-08-22T10:30:00-03:00",
  "estado": "RESERVADO", "area": "NUTRICION", "ubicacion": "Consultorio 1",
  "paciente": { "id": 57, "nombre": "Ana Gómez" },
  "profesional": { "id": 3, "nombre": "Lic. López" },
  "puedeModificar": true, "version": 0 }
```
`puedeModificar` lo calcula el servidor según rol y R-T2 (la UI deshabilita según este campo).

## Backend
- `turnos.service.TurnoService`: `solicitar`, `reprogramar`, `cancelar`, `crearPorStaff`, `cambiarEstado`, `cancelarFuturosDePaciente`, `cancelarFuturosDeProfesional`, `turnosParaRecordatorio(fecha)` (spec 11).
- `turnos.service.ReglasTurno` (componente puro con `Clock`): `validarVentana7Dias`, `validar24h`, `validarDentroDeAgenda`, `validarSinSolapamiento` → testeable sin BD.
- Eventos: `TurnoReservado`, `TurnoReprogramado`, `TurnoCancelado` → listener de notificaciones (outbox).
- `@Auditable(TURNOS, …)` en cada operación.

## Frontend
- `paciente/turnos.html` (Mis turnos + botones Nuevo / Modificar-Cancelar), `paciente/turno-nuevo.html` (paso 1 profesional, paso 2 hueco, paso 3 confirmación), `paciente/turno-modificar.html`.
- `profesional/calendario.html` y `admin/calendario.html` usando `js/components/calendar.js` (vistas día/semana/mes renderizadas con CSS grid; sin librerías).
- Modal de detalle con acciones según `puedeModificar` y rol.

## Pruebas
- Unit `ReglasTurnoTest` con `Clock` fijo: 7 días (borde 6d23h / 7d), 24 h (23h59 / 24h00), solapamientos.
- Unit `AgendaServiceTest`: huecos con franjas múltiples, bloqueos, turnos tomados, ventana de 2 h.
- Integración: carrera de dos reservas al mismo hueco → una 201 y otra 409 `HORARIO_NO_DISPONIBLE`.
- Integración: reprogramar resetea recordatorio y escribe historial; cancelar libera hueco.
- Seguridad: paciente no puede cancelar turno ajeno (404 para no filtrar existencia).
- Postman "03-turnos".

## Tareas
- [ ] T-03.1 Entidades, `ReglasTurno`, `AgendaService.huecosLibres`, `TurnoService`, controllers (3 h)
- [ ] T-03.2 Componente calendario + pantallas paciente/profesional/admin (3 h)
- [ ] T-03.3 Tests (1 h)
