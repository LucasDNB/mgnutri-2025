# Plan 10 · Catálogo de alimentos y ejercicios

## Datos
`grupo_alimento`, `alimento`, `equivalencia_manual`, `grupo_muscular`, `ejercicio`, `ejercicio_grupo_secundario` (V6 estructura, V12 semilla).

## API
| Método | Ruta | Rol | CU |
|---|---|---|---|
| GET | `/grupos-alimento` | profesionales, ADMIN | — |
| GET | `/alimentos?q=&grupoId=&activo=&aptoPara=&page=` | profesionales, ADMIN | autocompletar / listado |
| GET | `/alimentos/{id}` (incluye equivalencias manuales) | idem | |
| POST | `/alimentos` | NUTRI, ADMIN | CU-53A |
| PUT | `/alimentos/{id}` | NUTRI, ADMIN | CU-53B |
| DELETE | `/alimentos/{id}` · POST `/alimentos/{id}/reactivar` | NUTRI, ADMIN | CU-53C |
| GET | `/grupos-musculares` | profesionales, ADMIN | — |
| GET | `/ejercicios?q=&grupoMuscularId=&dificultad=&activo=&page=` | idem | |
| POST / PUT / DELETE | `/ejercicios[/{id}]` · POST `/ejercicios/{id}/reactivar` | ENTR, ADMIN | CU-54A/B/C |

`POST /alimentos` con kcal incoherentes y sin `confirmarAdvertencias: true` → `422 { code: "KCAL_INCOHERENTES", kcalCalculadas }`.

## Backend
- `catalogo.service.AlimentoService` (normaliza nombre para unicidad: `Normalizer.NFD` sin diacríticos + lower), `EjercicioService`.
- Cache en memoria (`@Cacheable("catalogo")`, invalidación en altas/bajas/modificaciones) para generador y alternativas.
- `@Auditable(CATALOGO, …)`.

## Frontend
- `profesional/alimentos.html` + modal formulario con sección "Equivalencias" (tabla editable).
- `profesional/ejercicios.html` + modal formulario con previsualización de enlace.
- Mismas páginas accesibles al admin desde *Opciones avanzadas*.

## Pruebas
- Unit validación nutricional (bordes y coherencia de kcal).
- Integración: baja lógica excluye de `/alimentos?activo=true` y del generador; alimento dado de baja sigue en plan existente.
- Integración: permisos por rol (ENTR no crea alimentos → 403).
- Semilla: test que verifica ≥ 4 ejercicios por grupo muscular y ≥ 5 alimentos por grupo de intercambio.

## Tareas
- [ ] T-10.0 (S2) Migración V6 + entidades + semilla mínima (20 alimentos, 20 ejercicios)
- [ ] T-10.1 ABM ejercicios back + front + semilla completa (4 h)
- [ ] T-10.2 ABM alimentos + equivalencias back + front + semilla completa (4 h)
