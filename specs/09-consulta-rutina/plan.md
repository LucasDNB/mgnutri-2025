# Plan 09 · Consulta de rutina y actividad física

## Datos
`reemplazo_ejercicio`, `registro_entrenamiento`, `comentario`, `molestia` (V11).

## API
| Método | Ruta | Rol | CU |
|---|---|---|---|
| GET | `/me/rutina` (incluye `ultimaCarga` por ejercicio y reemplazos) | PACIENTE | CU-24, CU-28 |
| GET | `/me/rutina/calendario?mes=2025-09` → días programados y cumplimiento | PACIENTE | CU-24 (vista mes) |
| GET | `/me/registro-entrenamiento?desde=&hasta=` | PACIENTE | CU-25 |
| PUT | `/me/registro-entrenamiento` `{ejercicioRutinaId, fecha, completado, cargaKg, seriesRealizadas, repeticionesRealizadas}` | PACIENTE | CU-25 |
| GET | `/me/rutina/ejercicios/{ejercicioRutinaId}/alternativas` | PACIENTE | CU-26 |
| PUT / DELETE | `/me/rutina/ejercicios/{ejercicioRutinaId}/reemplazo` `{ejercicioId}` | PACIENTE | CU-26 |
| POST | `/me/comentarios` `{tipo: RUTINA, ejercicioRutinaId?, fecha, texto}` | PACIENTE | CU-40 |
| GET / POST | `/me/molestias` | PACIENTE | CU-42 |
| GET | `/pacientes/{pid}/registros-entrenamiento?desde=&hasta=` | ENTR asignado | US-09.7 |
| GET | `/pacientes/{pid}/molestias` · POST `/molestias/{id}/revisar` | asignados | US-09.7 |

## Backend
- `entrenamiento.service.AlternativaEjercicioService` (consulta por `grupo_muscular_principal_id`, `activo=1`, `id <> actual`, orden `ABS(dificultad - actual)`, luego nombre; tope 10).
- `entrenamiento.service.ActividadFisicaService` (upsert registro; última carga por ejercicio con `MAX(fecha)` por `ejercicio_id` — se busca por ejercicio de catálogo, no por `ejercicio_rutina`, para que la carga se recuerde entre rutinas).
- `entrenamiento.service.MolestiaService` → evento `MolestiaRegistrada` → `alerta` a profesionales según contexto (prioridad si intensidad ≥ 7).
- Reutiliza `NovedadesService` (spec 08).

## Frontend
- `paciente/entrenamiento.html` (submenú), `paciente/mi-rutina.html` con selector Día/Semana/Mes; tarjeta por ejercicio con ✔, inputs numéricos grandes (`inputmode="decimal"`), botones *Video*, *Variante*, *Comentar*, *Molestia*.
- `paciente/molestia.html` (escala 1–10 con botones, selector de zona).
- Pestaña *Registros de entrenamiento* en ficha del profesional (tabla + mini gráfico de cargas).

## Pruebas
- Unit alternativas (mismo grupo, excluye actual e inactivos, orden por dificultad).
- Integración: registrar ejercicio de rutina eliminada → 422; última carga persiste entre rutinas del mismo ejercicio; molestia con contexto GENERAL crea alertas a ambos profesionales.
- Postman "09-rutina-paciente".

## Tareas
- [ ] T-09.1 Endpoints `/me/rutina`, registros, alternativas, comentarios, molestias (3 h)
- [ ] T-09.2 Pantallas paciente y pestaña del entrenador
- [ ] T-09.3 Testeo de rutinas (3 h)
