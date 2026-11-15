# Plan 02 · Gestión de profesionales

## Datos
`profesional`, `disponibilidad`, `bloqueo_agenda`, `usuario`, `usuario_rol` (V3, V4).

## API
| Método | Ruta | Rol | CU |
|---|---|---|---|
| GET | `/profesionales?q=&tipo=&estado=&page=` | ADMIN | CU-05/06 |
| GET | `/profesionales/{id}` | ADMIN, el propio | CU-05 |
| POST | `/profesionales` | ADMIN | CU-04 |
| PUT | `/profesionales/{id}` | ADMIN | CU-05 |
| DELETE | `/profesionales/{id}` | ADMIN | CU-06 |
| POST | `/profesionales/{id}/reenviar-credenciales` | ADMIN | CU-37 |
| GET | `/profesionales/{id}/disponibilidad` | ADMIN, propio | US-02.4 |
| PUT | `/profesionales/{id}/disponibilidad` body `{franjas:[{diaSemana, horaDesde, horaHasta, ubicacion}]}` (reemplazo completo) | ADMIN, propio | US-02.4 |
| GET/POST/DELETE | `/profesionales/{id}/bloqueos[/{bloqueoId}]` | ADMIN, propio | US-02.4 |
| GET | `/profesionales/disponibles?area=NUTRICION` | PACIENTE, ADMIN | insumo de CU-07 |
| GET | `/me/profesional` | NUTRI/ENTR | home |

`POST /profesionales/{id}/bloqueos` con turnos afectados devuelve `409 { code: "TURNOS_AFECTADOS", turnos: n }` salvo que se envíe `confirmarCancelacion: true`.

## Backend
- `profesionales.service.ProfesionalService` (`crear`, `modificar`, `darDeBaja`, `reenviarCredenciales`, `rolesPorTipo(tipo)`).
- `profesionales.service.AgendaService` (`reemplazarFranjas`, `crearBloqueo`, `franjasDe(profesionalId)`); validación de superposición en memoria antes de persistir.
- Reutiliza `UsuarioService`, `TurnoService.cancelarFuturosDeProfesional(id, motivo)`.
- Evento `ProfesionalCreado` → outbox `CREDENCIALES`.

## Frontend
- `admin/profesionales.html`, `admin/profesional-form.html`, `admin/profesional-ficha.html` (pestañas Datos · Agenda · Pacientes asignados).
- `profesional/home.html` (bienvenida + botones), `profesional/agenda.html` (editor de franjas tipo grilla semanal + lista de bloqueos).
- Componente `js/components/franjas-editor.js`.

## Pruebas
- Unit: `rolesPorTipo`, validación de franjas superpuestas, matrícula obligatoria condicional.
- Integración: alta integral → usuario con 2 roles; baja → turnos futuros cancelados + notificaciones encoladas; bloqueo con turnos → 409 y luego cancelación confirmada.
- Seguridad: ENTR no puede editar agenda de otro profesional (403).

## Tareas
- [ ] T-02.1 Entidades, DAOs, servicios, controllers (2 h)
- [ ] T-02.2 Pantallas ABM + agenda + home profesional (3 h)
- [ ] T-02.3 Tests + Postman "02-profesionales" (1 h)
