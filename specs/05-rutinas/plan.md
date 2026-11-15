# Plan 05 · Gestión de rutinas

## Datos
`rutina`, `dia_rutina`, `ejercicio_rutina` (+ `activo BIT`) (V8); usa `ejercicio`, `grupo_muscular` (V6).

## API
| Método | Ruta | Rol | CU |
|---|---|---|---|
| GET | `/pacientes/{pid}/rutinas?estado=` | ENTR asignado, ADMIN lectura | US-05.5 |
| GET | `/rutinas/{id}` | ENTR asignado, ADMIN | CU-22 |
| POST | `/pacientes/{pid}/rutinas` `{..., finalizarActual}` | ENTR | CU-21 |
| PUT | `/rutinas/{id}` `{..., version}` | ENTR | CU-22 |
| DELETE | `/rutinas/{id}` | ENTR | CU-23 |
| POST | `/rutinas/{id}/duplicar` | ENTR | US-05.5 |
| GET | `/rutinas/{id}/pdf` | ENTR asignado, ADMIN | CU-27 |
| GET | `/me/rutina/pdf` | PACIENTE | CU-27 |

```json
{
  "nombre": "Hipertrofia fase 1", "objetivo": "Ganancia de masa muscular",
  "fechaInicio": "2025-08-25", "duracionSemanas": 6, "observaciones": "Entrada en calor 10 min",
  "dias": [
    { "diaSemana": 1, "nombre": "Tren superior", "ejercicios": [
      { "ejercicioId": 40, "orden": 1, "series": 4, "repeticiones": "8-10", "descansoSeg": 90, "cargaSugerida": "RPE 8", "notas": "" }
    ] }
  ],
  "version": 0
}
```

## Backend
- `entrenamiento.controller.RutinaController`, `MiRutinaController` (con spec 09).
- `entrenamiento.service.RutinaService`: `crear`, `actualizar` (merge por id de `ejercicio_rutina`; los quitados → `activo=false`), `eliminar`, `duplicar`, `finalizarVencidas()`, `finalizarActiva(pacienteId)`.
- `entrenamiento.service.ValidadorRutina`.
- `reportes.pdf.RutinaPdf` (OpenPDF; `Anchor` con URL del video).

## Frontend
- `profesional/rutina-editor.html` + `js/pages/rutina-editor.js`: lista de días (acordeón), buscador de ejercicios con filtro por grupo muscular (`/ejercicios?q=&grupoMuscularId=`), filas editables con reordenamiento por botones (accesible, sin drag obligatorio).
- Pestaña *Rutinas* en ficha.

## Pruebas
- Unit `ValidadorRutinaTest` (sin días, día vacío, series fuera de rango).
- Integración: activo único + finalización; edición conserva registros (ejercicio inactivo); eliminación oculta al paciente.
- PDF contiene link de video (verificar anotación de link con PDFBox).
- Seguridad: NUTRI-only → 403.

## Tareas
- [ ] T-05.1 Entidades, servicio, validador, controllers (3 h)
- [ ] T-05.2 Editor de rutina (2 h)
- [ ] T-05.3 PDF de rutina (S3: "Consulta de rutina")
- [ ] T-05.4 Tests (1 h)
