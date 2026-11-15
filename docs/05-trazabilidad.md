# 05 · Matriz de trazabilidad

RF y paquetes corregidos según [`00-analisis-documental.md §4`](00-analisis-documental.md). La columna **Estado / Evidencia** se completa en el spec 16 (revisión de cumplimiento).

## CU → RF → Paquete → Sprint → Spec

| CU | Nombre | RF | Paquete | Sprint | Spec / Historia | Estado / Evidencia |
|---|---|---|---|---|---|---|
| CU-01 | Alta de paciente | RF-01 | 1 | 1 | 01 · US-01.1 | |
| CU-02 | Modificación de paciente | RF-01 | 1 | 1 | 01 · US-01.2 | |
| CU-03 | Baja de paciente | RF-01 | 1 | 1 | 01 · US-01.3 | |
| CU-04 | Alta de profesional | RF-02 | 2 | 1 | 02 · US-02.1 | |
| CU-05 | Modificar profesional | RF-02 | 2 | 1 | 02 · US-02.2 | |
| CU-06 | Baja de profesional | RF-02 | 2 | 1 | 02 · US-02.3 | |
| CU-07 | Solicitud de turno | RF-03 | 3 | 1 | 03 · US-03.1 | |
| CU-08 | Modificación de turno | RF-03 | 3 | 1 | 03 · US-03.2 | |
| CU-09 | Cancelación de turno | RF-03 | 3 | 1 | 03 · US-03.3 | |
| CU-10 | Visualización de turnos | RF-03 | 3 | 1 | 03 · US-03.5 | |
| CU-11 | Crear plan alimentario | RF-04 | 4 | 2 | 04 · US-04.1 | |
| CU-12 | Editar plan alimentario | RF-04 | 4 | 2 | 04 · US-04.3 | |
| CU-13 | Eliminar plan alimentario | RF-04 | 4 | 2 | 04 · US-04.4 | |
| CU-14 | Generar plan automático | RF-04 | 4 | 2 | 04 · US-04.2 | |
| CU-15 | Consultar plan alimentario | RF-09 | 8 | 3 | 08 · US-08.1 | |
| CU-16 | Descargar plan en PDF | RF-04 / RF-09 | 4 | 3 | 04 · US-04.5 · 08 · US-08.1 | |
| CU-17 | Registrar comidas realizadas | RF-09 / RF-15 | 8 | 3 | 08 · US-08.2 | |
| CU-18 | Sugerencia de reemplazo | RF-09 / RF-11 | 8 | 3 | 08 · US-08.3 | |
| CU-19 | Registrar evolución | RF-06 | 6 | 2 | 06 · US-06.1, US-06.2 | |
| CU-20 | Visualizar evolución | RF-06 | 6 | 2 | 06 · US-06.3 | |
| CU-21 | Crear rutina | RF-05 | 5 | 2 | 05 · US-05.1 | |
| CU-22 | Editar rutina | RF-05 | 5 | 2 | 05 · US-05.2 | |
| CU-23 | Eliminar rutina | RF-05 | 5 | 2 | 05 · US-05.3 | |
| CU-24 | Consultar rutina | RF-10 | 9 | 3 | 09 · US-09.1 | |
| CU-25 | Registrar rutina completada | RF-10 / RF-15 | 9 | 3 | 09 · US-09.2 | |
| CU-26 | Consultar alternativas de ejercicios | RF-10 / RF-11 | 9 | 3 | 09 · US-09.3 | |
| CU-27 | Descargar rutina en PDF | RF-05 / RF-10 | 5 | 3 | 05 · US-05.4 | |
| CU-28 | Acceder a video de ejercicio | RF-10 | 9 | 3 | 09 · US-09.4 | |
| CU-29 | Asignar paciente a profesional | RF-01 | 1 | 1 | 01 · US-01.4 | |
| CU-30 | Reporte de turnos | RF-14 | 12 | 4 | 12 · R-1 | |
| CU-31 | Reporte de pagos | RF-14 | 12 | 4 | 12 · R-2 | |
| CU-32 | Reporte de evolución | RF-14 / RF-06 | 12 | 4 | 12 · R-3 | |
| CU-33 | Reporte de rutinas | RF-14 | 12 | 4 | 12 · R-4 | |
| CU-34 | Reporte de planes | RF-14 | 12 | 4 | 12 · R-5 | |
| CU-35 | Registrar evento en bitácora | RF-17 | 14 | 1 | 14 · US-14.1 | |
| CU-36 | Consultar bitácora | RF-17 | 14 | 1 | 14 · US-14.2 | |
| CU-37 | Notificación de alta (WhatsApp) | RF-13 | 11 (+1.3, 2.3) | 4 (puerto en 1) | 11 · US-11.1 | |
| CU-38 | Recordatorio de turno (WhatsApp) | RF-07 / RF-13 | 11 (+3.3) | 4 | 11 · US-11.2 | |
| CU-39 | Notificación de deuda (WhatsApp) | RF-07 / RF-13 | 11 (+7.2) | 4 | 11 · US-11.3 | |
| CU-40 | Comentario sobre rutina | RF-15 | 9 | 3 | 09 · US-09.5 | |
| CU-41 | Comentario sobre plan | RF-15 | 8 | 3 | 08 · US-08.4 | |
| CU-42 | Registrar molestias físicas | RF-15 | 9 | 3 | 09 · US-09.6 | |
| CU-43 | Observaciones hambre/saciedad | RF-15 | 8 | 3 | 08 · US-08.5 | |
| CU-44 | Registrar pago | RF-08 | 7 | 2 | 07 · US-07.2 | |
| CU-45 | Registrar deuda pendiente | RF-08 | 7 | 2 | 07 · US-07.4 | |
| CU-46 | Historial de pagos | RF-08 | 7 | 2 | 07 · US-07.5 | |
| SDP 7.1.2/7.1.3 | Modificar / eliminar pago | RF-08 | 7 | 2 | 07 · US-07.3 | |
| CU-47 | Autenticación | RF-18 / RF-16 | 13 | 4 (esqueleto 1) | 13 · US-13.1 | |
| CU-48 | Recuperar contraseña | RF-18 | 13 | 4 | 13 · US-13.2 | |
| CU-49 | Cambiar contraseña | RF-18 | 13 | 4 | 13 · US-13.3 | |
| CU-50 | Cerrar sesión | RF-18 / RF-16 | 13 | 4 | 13 · US-13.4 | |
| CU-51 | Backup manual | RNF Confiabilidad | 15 | 5 | 15 · US-15.1 | |
| CU-52 | Programar backup | RNF Confiabilidad | 15 | 5 | 15 · US-15.2 | |
| CU-53A/B/C | Alta / modificación / baja de alimento | RF-11 | 10 | 3 | 10 · US-10.1–10.3 | |
| CU-54A/B/C | Alta / modificación / baja de ejercicio | RF-11 | 10 | 3 | 10 · US-10.4–10.6 | |

## RF → cobertura

| RF | Specs | CU |
|---|---|---|
| RF-01 | 01 | 01, 02, 03, 29 |
| RF-02 | 02 | 04, 05, 06 |
| RF-03 | 03 | 07, 08, 09, 10 |
| RF-04 | 04 | 11, 12, 13, 14, 16 |
| RF-05 | 05 | 21, 22, 23, 27 |
| RF-06 | 06, 12 | 19, 20, 32 |
| RF-07 | 11 | 38, 39 |
| RF-08 | 07 | 44, 45, 46 (+ modificar/eliminar) |
| RF-09 | 08 | 15, 16, 17, 18 |
| RF-10 | 09 | 24, 25, 26, 27, 28 |
| RF-11 | 10 | 53A/B/C, 54A/B/C (+ consumo en 18, 26) |
| RF-12 | 08 (US-08.0) | — (sin CU en la ERS) |
| RF-13 | 11, 03, 07 | 37, 38, 39 |
| RF-14 | 12 | 30–34 |
| RF-15 | 08, 09, 06 | 17, 25, 40, 41, 42, 43 |
| RF-16 | 13 | 47, 50 (+ registro de todas las acciones de acceso) |
| RF-17 | 14 (+ todos) | 35, 36 |
| RF-18 | 13 | 47, 48, 49, 50 |

## RNF → cobertura

| RNF (ERS §) | Cómo se cumple | Spec / doc |
|---|---|---|
| 3.1 Usabilidad (≤ 30 min, responsive, multi-dispositivo) | Mobile-first, homes por rol, ayudas contextuales, prueba de usabilidad | Constitución VIII · 16 |
| 3.2 Disponibilidad 99 % | App Service Always On + monitoreo de disponibilidad | 17 |
| 3.2 Tolerancia a fallos | Handler global, bitácora de errores en transacción aparte, outbox con reintentos | 14, 11 |
| 3.2 Backup diario / RTO ≤ 2 h | PITR de Azure SQL + CU-51/52 + ensayo de restauración | 15 |
| 3.3 Autenticación, roles, cifrado, registro de accesos | JWT, BCrypt, AES-GCM, matriz de permisos, RF-16 | 13 |
| 3.4 p90 < 5 s, 50 concurrentes, 500 pacientes | Índices, consultas agregadas, prueba k6 | 16 |
| 3.5 Modularidad, versionado, datos estables, bitácora | Módulos por dominio, tags, Flyway, RF-17 | 02-arquitectura · 14 · 17 |
| 3.6 / 3.7 Stack y despliegue en Azure | Constitución III | 00 · 17 |
| 3.8 Manual, ayuda en línea, guía técnica | Entregables | 17 |
| 3.10 Ley 25.326, T&C | Aceptación obligatoria, minimización de datos en reportes y logs | 13 · Constitución V |
| 3.11 ISO 25010 / 27001 / estándares web | Prácticas de calidad y seguridad descritas | Constitución VII · 13 |
