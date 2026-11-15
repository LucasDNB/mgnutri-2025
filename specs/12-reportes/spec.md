# Spec 12 · Reportes del sistema

| Campo | Valor |
|---|---|
| Paquete SDP | 12 – Reportes del sistema |
| RF | RF-14 · RF-06 (PDF de evolución) · RF-17 |
| CU | CU-30 Turnos · CU-31 Pagos · CU-32 Evolución · CU-33 Rutinas · CU-34 Planes |
| Sprint | 4 · Backlog: Reportes de sistema (4 h) |
| Depende de | 03, 04, 05, 06, 07, 08, 09 |
| Decisiones | D-21, D-22 |

## Contexto
Administrador y profesionales obtienen informes con datos en tiempo real, en pantalla y exportables. Los profesionales sólo ven datos de sus pacientes asignados; los reportes de pagos son exclusivos del admin.

## Comportamiento común (todos los reportes)
- **CA-12.0.1** *Reportes* muestra las tarjetas de reportes permitidos para el rol.
- **CA-12.0.2** Al elegir uno se muestran sus filtros (fecha desde/hasta obligatorias, default último mes; paciente; profesional; tipo/estado según reporte).
- **CA-12.0.3** *Generar* muestra el resultado en pantalla (tabla + gráficos cuando aplica) y botones **Descargar PDF** y **Descargar Excel**.
- **CA-12.0.4** Sin datos en el rango → "No hay resultados para los filtros elegidos" (alterno 1.1 de CU-30…34).
- **CA-12.0.5** Los datos se consultan en el momento (sin caché) — RF-14 "tiempo real".
- **CA-12.0.6** PDF con encabezado (logo, nombre del centro, título, filtros aplicados, fecha/hora de emisión, usuario emisor), pie con número de página; optimizado para impresión A4.
- **CA-12.0.7** Excel con una hoja de datos (encabezados, filtros congelados, formatos de fecha y moneda) y, si aplica, una hoja "Resumen".
- **CA-12.0.8** Cada generación o descarga registra `REPORTES/REPORTE` (o `DESCARGA`) en bitácora con tipo y filtros.
- **CA-12.0.9** Rango máximo 2 años; respuesta < 5 s para 500 pacientes con 1 año de historial (ERS 3.4).

## Reportes

### R-1 Turnos (CU-30) — ADMIN, profesionales (propios)
- Filtros: rango, profesional, paciente, área, estado.
- Contenido: listado (fecha, hora, paciente, profesional, área, estado, motivo de cancelación); resumen por estado (reservados, atendidos, **ausentes**, **cancelados** — distinguiendo cancelados por paciente/profesional/admin); tasa de ausentismo y de cancelación por profesional; gráfico de barras por semana.

### R-2 Pagos y deudas (CU-31) — sólo ADMIN
- Filtros: rango, paciente, estado (vigentes/anulados), medio de pago.
- Contenido: pagos del período (fecha, comprobante, paciente, medio, monto), total por medio de pago, total recaudado; sección **deudas pendientes** al cierre del rango (paciente, saldo, fecha de deuda, días de atraso); gráfico de recaudación por mes.

### R-3 Evolución del paciente (CU-32) — profesionales asignados (y el paciente desde *Mi evolución*)
- Se genera desde la ficha del paciente; filtro: rango de fechas.
- Contenido: datos del paciente; tabla de mediciones con deltas; **gráficos comparativos** de peso, composición corporal (% grasa / % músculo) y medidas; desempeño físico por prueba; primera vs. última medición del período (requerimiento especial: comparaciones gráficas, apto impresión).

### R-4 Rutinas (CU-33) — ADMIN, entrenadores
- Filtros: rango, paciente, entrenador, estado de rutina.
- Contenido: rutinas asignadas (paciente, entrenador, vigencia, estado), **% de cumplimiento** (ejercicios completados / programados en el período), cantidad de reemplazos, comentarios y molestias del período; **comparativo por período** (requerimiento especial): cumplimiento por mes o por semana en columnas.

### R-5 Planes alimentarios (CU-34) — ADMIN, nutricionistas
- Filtros: rango (de asignación), paciente, nutricionista, estado, modo (manual/automático).
- Contenido: planes con fecha de asignación, vigencia y estado; % de comidas registradas como realizadas; cantidad de reemplazos y comentarios; promedio de hambre/saciedad del período.

## Reglas
- RN-12.1 Autorización: un profesional que pide un reporte con un `pacienteId` no asignado → 403; sin `pacienteId`, los resultados se restringen a sus pacientes.
- RN-12.2 Los pacientes dados de baja aparecen en reportes históricos marcados "(baja)".
- RN-12.3 El reporte no incluye antecedentes de salud ni datos cifrados no necesarios.
