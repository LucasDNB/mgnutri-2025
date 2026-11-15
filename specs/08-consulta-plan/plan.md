# Plan 08 · Consulta del plan y actividad nutricional

## Datos
`reemplazo_alimento`, `registro_comida`, `observacion_diaria`, `comentario` (V11); `equivalencia_manual` (V6); `configuracion_sistema` (`centro.*`).

## API
| Método | Ruta | Rol | CU |
|---|---|---|---|
| GET | `/publico/contacto` | anónimo | RF-12 |
| GET | `/me/contactos` → `{central, profesionales:[{nombre, especialidad, whatsappUrl}]}` | PACIENTE | RF-12 |
| GET | `/me/home` → `{proximoTurno, tienePlan, tieneRutina, novedades}` | PACIENTE | US-08.0 |
| GET | `/me/plan-alimentario` | PACIENTE | CU-15 |
| GET | `/me/registro-comidas?desde=&hasta=` | PACIENTE | CU-17 |
| PUT | `/me/registro-comidas` `{comidaPlanId, fecha, realizada, noLeGusto, leCayoMal}` (upsert) | PACIENTE | CU-17 |
| GET | `/me/plan-alimentario/items/{itemId}/alternativas` | PACIENTE | CU-18 |
| PUT | `/me/plan-alimentario/items/{itemId}/reemplazo` `{alimentoId}` | PACIENTE | CU-18 |
| DELETE | `/me/plan-alimentario/items/{itemId}/reemplazo` | PACIENTE | CU-18 |
| POST | `/me/comentarios` `{tipo: PLAN, comidaPlanId?, fecha, texto}` | PACIENTE | CU-41 |
| GET/PUT | `/me/observaciones-diarias[?desde=&hasta=]` / `{fecha, nivelHambre, nivelSaciedad, estadoAnimo, comentario}` | PACIENTE | CU-43 |
| GET | `/pacientes/{pid}/registros-nutricion?desde=&hasta=` | NUTRI asignado | US-08.6 |
| GET | `/novedades?leidas=false` · POST `/novedades/{tipo}/{id}/leer` | NUTRI/ENTR | US-08.6 |

Alternativas:
```json
{ "original": { "alimentoId": 30, "nombre": "Carne vacuna magra", "gramos": 100 },
  "alternativas": [
    { "alimentoId": 44, "nombre": "Huevo", "gramos": 110, "cantidadCasera": "≈ 2 unidades", "origen": "MANUAL" },
    { "alimentoId": 31, "nombre": "Pechuga de pollo", "gramos": 120, "origen": "CALCULADA" } ] }
```

## Backend
- `nutricion.service.EquivalenciaService.alternativas(itemId)`: manuales del alimento → calculadas: `gramosB = gramosA × refA_por_g / refB_por_g` usando `grupo.macro_referencia`; descarta resultados > 3× o < 1/3 de los gramos originales (poco prácticos); orden por |kcalB − kcalA|.
- `nutricion.service.ActividadNutricionalService`: upsert de registros (valida fecha hoy−2..hoy y que la comida pertenezca al plan activo del paciente), observaciones, comentarios (publica `ComentarioRegistrado` → `alerta` al nutricionista).
- `nutricion.service.ReemplazoService`.
- `common.novedades.NovedadesService` (agrega comentarios y molestias no leídos de pacientes asignados; usado también por spec 09).
- `configuracion.service.ContactoService` (arma `https://wa.me/<digits>`).

## Frontend
- `paciente/home.html` (3 botones + próximo turno), `paciente/contacto.html`, `index.html` modal *Contacto*.
- `paciente/nutricion.html` (submenú), `paciente/mi-plan.html` (grilla/pestañas por día, botones Alternativa / Comentar / PDF), `paciente/registro-dia.html` (checkboxes con guardado inmediato y reintento offline simple en memoria), `paciente/observaciones.html` (escalas con botones grandes accesibles).
- `profesional/novedades.html`; pestaña *Registros* en ficha (tabla de cumplimiento + gráfico Chart.js de hambre/saciedad).

## Pruebas
- Unit `EquivalenciaServiceTest`: cálculo por macro de referencia, prioridad de manuales, filtro de proporciones, mismo grupo.
- Integración: registro de comida con fecha hoy−3 → 422; comida de un plan finalizado → 422; reemplazo visible en `GET /me/plan-alimentario` y en el PDF.
- Integración: comentario > 300 → 400; comentario genera alerta al nutricionista asignado.
- E2E manual en celular (Chrome Android / Safari iOS): marcar comidas, ver alternativa.

## Tareas
- [ ] T-08.1 Endpoints `/me` de plan, registros, observaciones, comentarios, reemplazos + equivalencias (3 h)
- [ ] T-08.2 Pantallas paciente (home, contacto, plan, registro, observaciones) y novedades profesional
- [ ] T-08.3 Testeo de planes alimentarios (3 h)
