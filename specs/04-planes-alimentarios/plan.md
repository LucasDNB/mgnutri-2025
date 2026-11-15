# Plan 04 · Gestión de planes alimentarios

## Datos
`plan_alimentario`, `comida_plan` (+ columna `activo BIT`), `item_comida` (V7). Usa `alimento`, `grupo_alimento` (V6). Índice único filtrado de plan activo por paciente.

## API
| Método | Ruta | Rol | CU |
|---|---|---|---|
| GET | `/pacientes/{pid}/planes-alimentarios?estado=` | NUTRI asignado, ADMIN (lectura) | US-04.6 |
| GET | `/planes-alimentarios/{id}` | NUTRI asignado, ADMIN | CU-12 |
| POST | `/pacientes/{pid}/planes-alimentarios` `{..., estado: BORRADOR|ACTIVO, finalizarActual: bool}` | NUTRI | CU-11 |
| PUT | `/planes-alimentarios/{id}` `{..., version}` | NUTRI | CU-12 |
| POST | `/planes-alimentarios/{id}/activar` `{finalizarActual}` | NUTRI | CU-11 (borrador → activo) |
| DELETE | `/planes-alimentarios/{id}` | NUTRI | CU-13 |
| POST | `/planes-alimentarios/generar` `{pacienteId, kcal, pctHc, pctProteinas, pctGrasas, comidas:[{tipo, pct}], excluirAlimentoIds, variedad: DIARIA|SEMANAL, semilla?}` | NUTRI | CU-14 (no persiste) |
| POST | `/planes-alimentarios/{id}/duplicar` | NUTRI | US-04.6 |
| GET | `/planes-alimentarios/{id}/pdf` | NUTRI asignado, ADMIN | CU-16 |
| GET | `/me/plan-alimentario/pdf` | PACIENTE | CU-16 |

Si existe plan activo y `finalizarActual=false` → `409 PLAN_ACTIVO_EXISTENTE` con `{planActualId, fechaInicio}` para que la UI pida confirmación.

Cuerpo de plan:
```json
{
  "nombre": "Plan agosto 2025", "fechaInicio": "2025-08-25", "duracionSemanas": 4,
  "observaciones": "Hidratarse 2 L/día",
  "modo": "MANUAL", "kcalObjetivo": null, "pctHc": null, "pctProteinas": null, "pctGrasas": null,
  "comidas": [
    { "diaSemana": 1, "tipo": "DESAYUNO", "descripcion": "Infusión c/ edulcorante + 3 tostadas integrales con queso untable + 1 fruta",
      "items": [ { "alimentoId": 12, "gramos": 60, "cantidadCasera": "3 tostadas" } ] }
  ],
  "version": 0
}
```
Respuesta agrega `totalesPorDia: [{diaSemana, kcal, hc, proteinas, grasas}]` y `desvios` (si es automático).

## Generador (`nutricion.service.GeneradorPlanService`)
```
entrada: params, catálogo activo (cacheado por request), paciente
para cada día d en 1..7 (o sólo d=1 y luego replicar si variedad=SEMANAL):
  para cada comida m en params.comidas:
    kcal_m = K * m.pct/100
    obj = {HC: kcal_m*pctHc/400, P: kcal_m*pctP/400, G: kcal_m*pctG/900}
    plantilla = PLANTILLAS[m.tipo]           # lista de slots (grupo, rol, porción fija o variable)
    items = []
    # 1) slots de porción fija
    verdura → 150 g ; fruta → gramos_unidad_casera ?: 150 g ; lácteo → 200 g
    # 2) proteína: g = clamp((obj.P - P(items)) / prot_por_g, 50, 250)
    # 3) hidratos:  g = clamp((obj.HC - HC(items)) / hc_por_g, 20, 300)
    # 4) grasas:    g = clamp((obj.G - G(items)) / grasa_por_g, 0, 30)
    elección de alimento por slot: candidatos = grupo ∩ aptoPara(m.tipo) − excluidos,
       evitando el usado en la misma comida los 2 días previos; Random(semilla)
    redondear a 5 g; si hay medida casera, expresar "≈ n unidades"
    descripcion = join(items, " + ")
    desvio_m = |total - objetivo| / objetivo por kcal y macros
devolver BorradorPlan(comidas, totalesPorDia, desvios, semillaUsada)
```
`PLANTILLAS` es configuración en código (enum `PlantillaComida`), fácil de ajustar.

## Backend
- `nutricion.controller.PlanAlimentarioController`, `MiPlanController` (compartido con spec 08).
- `nutricion.service.PlanAlimentarioService`: `crear`, `actualizar` (merge de comidas por clave `(diaSemana, tipo)`; quita → `activo=false`), `activar`, `eliminar`, `duplicar`, `finalizarVencidos()` (`@Scheduled`), `finalizarActivo(pacienteId)` (para bajas).
- `nutricion.service.CalculadoraNutricional` (puro): macros de un ítem, totales por comida/día.
- `nutricion.service.ValidadorPlan`: mínimo 4 comidas por día, fechas, gramos.
- `reportes.pdf.PlanAlimentarioPdf` (OpenPDF, apaisado, tabla 8 columnas como el mockup).
- `AccesoPacienteGuard.assertPuedeEditar(pid, NUTRICION)`.

## Frontend
- `profesional/plan-editor.html` + `js/pages/plan-editor.js`: grilla editable (tabla responsive: en celular se ve por día con pestañas), panel lateral de búsqueda de alimentos (autocompletar a `/alimentos?q=`), totales por día, botones Guardar borrador / Confirmar plan.
- `profesional/plan-generador.html`: formulario de parámetros con validación de sumas en vivo; botón Generar → abre el editor con el borrador e indicadores de desvío (colores).
- Pestaña *Planes* en ficha del paciente.

## Pruebas
- Unit `ValidadorPlanTest`: día con 3 comidas → error; 4 tipos distintos → ok.
- Unit `GeneradorPlanServiceTest` con catálogo fijo y semilla: totales dentro de ±10 % kcal en caso nominal (2000 kcal, 50/20/30, 5 comidas); exclusiones respetadas; variedad SEMANAL replica.
- Integración: crear activo con otro activo sin confirmar → 409; con confirmar → anterior FINALIZADO; índice filtrado impide 2 activos.
- Integración: PDF devuelve `application/pdf`, > 0 bytes, contiene nombre del paciente (PDFBox en test).
- Seguridad: ENTR → 403; NUTRI no asignado → 403.
- Rendimiento: generación + respuesta < 2 s con 300 alimentos.

## Tareas
- [ ] T-04.1 Entidades, servicios CRUD, validador, controllers (3 h)
- [ ] T-04.2 Generador automático + calculadora (incluido en "Capa de servicios" S2, 2 h)
- [ ] T-04.3 Editor de plan + generador (FrontEnd, 2 h)
- [ ] T-04.4 PDF del plan (S3: "Consultas y descargas de planes")
- [ ] T-04.5 Tests (1 h)
