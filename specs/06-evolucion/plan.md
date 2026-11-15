# Plan 06 · Evolución del paciente

## Datos
`medicion_antropometrica`, `evaluacion_fisica` (V9); lectura de `registro_entrenamiento` (V11) para cargas.

## API
| Método | Ruta | Rol | CU |
|---|---|---|---|
| GET | `/pacientes/{pid}/antropometria?desde=&hasta=` | asignados, ADMIN | CU-20 |
| POST | `/pacientes/{pid}/antropometria` | NUTRI asignado | CU-19 |
| PUT/DELETE | `/antropometria/{id}` | autor < 48 h | CU-19 |
| GET | `/pacientes/{pid}/evaluaciones-fisicas?desde=&hasta=&prueba=` | asignados, ADMIN | CU-20 |
| POST | `/pacientes/{pid}/evaluaciones-fisicas` `{fecha, pruebas:[{prueba, ejercicioId?, valor, unidad, observaciones}]}` | ENTR asignado | CU-19 |
| PUT/DELETE | `/evaluaciones-fisicas/{id}` | autor < 48 h | CU-19 |
| GET | `/pacientes/{pid}/evolucion?desde=&hasta=` | asignados, ADMIN | CU-20 |
| GET | `/me/evolucion?desde=&hasta=` | PACIENTE | CU-20 |

`/evolucion` devuelve series listas para graficar:
```json
{
  "peso": [ { "fecha": "2025-06-01", "valor": 82.4 }, ... ],
  "imc": [ ... ],
  "composicion": { "grasa": [ ... ], "musculo": [ ... ] },
  "medidas": { "cintura": [ ... ], "cadera": [ ... ], "brazo": [ ... ], "muslo": [ ... ], "pecho": [ ... ], "pantorrilla": [ ... ] },
  "desempeno": [ { "prueba": "Sentadilla 1RM", "unidad": "kg", "serie": [ ... ] } ],
  "cargas": [ { "ejercicio": "Press banca", "serie": [ { "fecha": "...", "valor": 50 } ] } ],
  "tabla": [ { "fecha": "...", "peso": 82.4, "deltaAnterior": -0.8, "deltaInicial": -3.1, ... } ]
}
```

## Backend
- `evolucion.service.AntropometriaService` (rangos con Bean Validation `@DecimalMin/@DecimalMax` + validaciones cruzadas; ventana de 48 h con `Clock`).
- `evolucion.service.EvaluacionFisicaService`.
- `evolucion.service.EvolucionQueryService` (arma series; cargas = máximo `carga_kg` por ejercicio y fecha desde `registro_entrenamiento`).
- `evolucion.domain.Imc.calcular(peso, talla)` con 1 decimal.

## Frontend
- `profesional/antropometria-form.html`, `profesional/evaluacion-form.html`.
- `comun/evolucion.html` (usada por profesional con `?pacienteId=` y por paciente) con Chart.js: line charts; selector de período; tabla con deltas; botón PDF.

## Pruebas
- Unit rangos (bordes 19.9/20/300/300.1), IMC, suma grasa+músculo.
- Integración: edición fuera de 48 h → 422; NUTRI no puede cargar evaluación física (403); serie de cargas calculada desde registros.
- Postman "06-evolucion".

## Tareas
- [ ] T-06.1 Antropometría (carga + vista de composición corporal) (2 h)
- [ ] T-06.2 Evaluación de desempeño (carga + vista) (2 h)
- [ ] T-06.3 Serie de cargas (cuando exista spec 09) y tests
