# Plan 12 · Reportes

## API
| Método | Ruta | Rol |
|---|---|---|
| GET | `/reportes/turnos?desde=&hasta=&profesionalId=&pacienteId=&area=&estado=&formato=json|pdf|xlsx` | ADMIN, NUTRI/ENTR |
| GET | `/reportes/pagos?desde=&hasta=&pacienteId=&estado=&medio=&formato=` | ADMIN |
| GET | `/reportes/evolucion/{pacienteId}?desde=&hasta=&formato=` | asignados, ADMIN |
| GET | `/me/reportes/evolucion?desde=&hasta=&formato=pdf` | PACIENTE |
| GET | `/reportes/rutinas?desde=&hasta=&pacienteId=&entrenadorId=&estado=&agrupar=SEMANA|MES&formato=` | ADMIN, ENTR |
| GET | `/reportes/planes?desde=&hasta=&pacienteId=&nutricionistaId=&estado=&modo=&formato=` | ADMIN, NUTRI |

`formato=json` devuelve `{ filtros, resumen, filas[], graficos: { nombre: {labels[], series[]} } }` para que la UI dibuje con Chart.js; `pdf`/`xlsx` devuelven el archivo.

## Diseño
- `reportes.service.<Tipo>ReporteService` → arma `ReporteDTO` (consultas con proyecciones JPQL / SQL nativo agregando en BD, no en memoria).
- `reportes.export.ReporteExporterFactory` (patrón Factory, D-02) → `PdfReporteExporter` (OpenPDF + JFreeChart → PNG embebido) | `ExcelReporteExporter` (Apache POI, `SXSSFWorkbook` para volúmenes grandes).
- Plantilla común `PdfLayout` (encabezado/pie).
- Índices de soporte: `turno(profesional_id, inicio)`, `pago(fecha)`, `registro_entrenamiento(paciente_id, fecha)`, `registro_comida(paciente_id, fecha)`.
- `@Auditable(REPORTES, REPORTE)` con filtros serializados en `detalle`.

## Frontend
- `comun/reportes.html` (tarjetas según rol) y `comun/reporte.html?tipo=` (filtros dinámicos, tabla, gráficos, botones de descarga). Descarga con `fetch` → `Blob` → `URL.createObjectURL` para enviar el token.

## Pruebas
- Integración por reporte con dataset fijo: totales y tasas esperadas; restricción por profesional; sin datos → `resumen.vacio=true` y la UI muestra el mensaje.
- PDF/Excel: tipo de contenido, apertura con PDFBox/POI, cantidad de filas.
- Rendimiento: dataset sintético (500 pacientes × 1 año) generado por script; p90 < 5 s.

## Tareas
- [ ] T-12.1 Infraestructura de exportación (factory, layout PDF, Excel) + R-1 Turnos + R-2 Pagos
- [ ] T-12.2 R-3 Evolución, R-4 Rutinas, R-5 Planes + pantallas (total paquete: 4 h según backlog; riesgo de subestimación, ver roadmap)
