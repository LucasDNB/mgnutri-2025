# 05 · Matriz de trazabilidad

RF y paquetes corregidos según [`00-analisis-documental.md §4`](00-analisis-documental.md). La columna **Estado / Evidencia** se completa en el spec 16 (revisión de cumplimiento). «E2E guion N» remite a los guiones
automatizados de [`pruebas/e2e`](../pruebas/e2e/RESULTADOS.md), ejecutados en Chrome de escritorio y Chrome Android.

## CU → RF → Paquete → Sprint → Spec

| CU | Nombre | RF | Paquete | Sprint | Spec / Historia | Estado / Evidencia |
|---|---|---|---|---|---|---|
| CU-01 | Alta de paciente | RF-01 | 1 | 1 | 01 · US-01.1 | OK · IntegracionIT, Newman 01-pacientes · E2E guion 2 |
| CU-02 | Modificación de paciente | RF-01 | 1 | 1 | 01 · US-01.2 | OK · Newman CA-01.2.1/01.2.5 |
| CU-03 | Baja de paciente | RF-01 | 1 | 1 | 01 · US-01.3 | OK · prueba manual (baja + turnos cancelados) · E2E guion 8 |
| CU-04 | Alta de profesional | RF-02 | 2 | 1 | 02 · US-02.1 | OK · IntegracionIT, Newman 02 · E2E guion 1 |
| CU-05 | Modificar profesional | RF-02 | 2 | 1 | 02 · US-02.2 | OK · pantalla admin/profesional |
| CU-06 | Baja de profesional | RF-02 | 2 | 1 | 02 · US-02.3 | OK · pantalla admin/profesional · E2E guion 8 |
| CU-07 | Solicitud de turno | RF-03 | 3 | 1 | 03 · US-03.1 | OK · ReglasTurnoTest, prueba API local · E2E guion 2 |
| CU-08 | Modificación de turno | RF-03 | 3 | 1 | 03 · US-03.2 | OK · ReglasTurnoTest (24 h), pantalla paciente/turnos · E2E guion 2 |
| CU-09 | Cancelación de turno | RF-03 | 3 | 1 | 03 · US-03.3 | OK · ReglasTurnoTest (24 h) · E2E guion 2 |
| CU-10 | Visualización de turnos | RF-03 | 3 | 1 | 03 · US-03.5 | OK · comun/calendario (día/semana/mes/listado) · E2E guion 2 |
| CU-11 | Crear plan alimentario | RF-04 | 4 | 2 | 04 · US-04.1 | OK · prueba API local (plan activo + 409) |
| CU-12 | Editar plan alimentario | RF-04 | 4 | 2 | 04 · US-04.3 | OK · editor de plan · E2E guion 3 |
| CU-13 | Eliminar plan alimentario | RF-04 | 4 | 2 | 04 · US-04.4 | OK · ficha › Planes |
| CU-14 | Generar plan automático | RF-04 | 4 | 2 | 04 · US-04.2 | OK · GeneradorPlanServiceTest (±10 % kcal) · E2E guion 3 |
| CU-15 | Consultar plan alimentario | RF-09 | 8 | 3 | 08 · US-08.1 | OK · paciente/mi-plan (móvil verificado) · E2E guion 3 |
| CU-16 | Descargar plan en PDF | RF-04 / RF-09 | 4 | 3 | 04 · US-04.5 · 08 · US-08.1 | OK · PDF plan (prueba API local) · E2E guion 3 |
| CU-17 | Registrar comidas realizadas | RF-09 / RF-15 | 8 | 3 | 08 · US-08.2 | OK · paciente/registro-dia · E2E guion 3 |
| CU-18 | Sugerencia de reemplazo | RF-09 / RF-11 | 8 | 3 | 08 · US-08.3 | OK · EquivalenciaServiceTest · E2E guion 3 |
| CU-19 | Registrar evolución | RF-06 | 6 | 2 | 06 · US-06.1, US-06.2 | OK · prueba API local (rangos, IMC) · E2E guion 5 |
| CU-20 | Visualizar evolución | RF-06 | 6 | 2 | 06 · US-06.3 | OK · comun/evolucion (Chart.js) · E2E guion 5 |
| CU-21 | Crear rutina | RF-05 | 5 | 2 | 05 · US-05.1 | OK · prueba API local · E2E guion 4 |
| CU-22 | Editar rutina | RF-05 | 5 | 2 | 05 · US-05.2 | OK · editor de rutina |
| CU-23 | Eliminar rutina | RF-05 | 5 | 2 | 05 · US-05.3 | OK · ficha › Rutinas |
| CU-24 | Consultar rutina | RF-10 | 9 | 3 | 09 · US-09.1 | OK · paciente/mi-rutina (móvil verificado) · E2E guion 4 |
| CU-25 | Registrar rutina completada | RF-10 / RF-15 | 9 | 3 | 09 · US-09.2 | OK · prueba API local · E2E guion 4 |
| CU-26 | Consultar alternativas de ejercicios | RF-10 / RF-11 | 9 | 3 | 09 · US-09.3 | OK · alternativas por grupo muscular · E2E guion 4 |
| CU-27 | Descargar rutina en PDF | RF-05 / RF-10 | 5 | 3 | 05 · US-05.4 | OK · PDF rutina con enlaces |
| CU-28 | Acceder a video de ejercicio | RF-10 | 9 | 3 | 09 · US-09.4 | OK · botón Ver video · E2E guion 4 |
| CU-29 | Asignar paciente a profesional | RF-01 | 1 | 1 | 01 · US-01.4 | OK · alta con asignaciones / ficha · E2E guion 2 |
| CU-30 | Reporte de turnos | RF-14 | 12 | 4 | 12 · R-1 | OK · Newman 12-reportes (json/xlsx) · E2E guion 7 |
| CU-31 | Reporte de pagos | RF-14 | 12 | 4 | 12 · R-2 | OK · Newman 12-reportes · E2E guion 7 |
| CU-32 | Reporte de evolución | RF-14 / RF-06 | 12 | 4 | 12 · R-3 | OK · reporte evolución PDF · E2E guion 5 |
| CU-33 | Reporte de rutinas | RF-14 | 12 | 4 | 12 · R-4 | OK · Newman 12-reportes · E2E guion 7 |
| CU-34 | Reporte de planes | RF-14 | 12 | 4 | 12 · R-5 | OK · Newman 12-reportes · E2E guion 7 |
| CU-35 | Registrar evento en bitácora | RF-17 | 14 | 1 | 14 · US-14.1 | OK · IntegracionIT (eventos ALTA), ArquitecturaTest · E2E guion 7 |
| CU-36 | Consultar bitácora | RF-17 | 14 | 1 | 14 · US-14.2 | OK · Newman 14-bitacora, IntegracionIT (inmutable) · E2E guion 7 |
| CU-37 | Notificación de alta (WhatsApp) | RF-13 | 11 (+1.3, 2.3) | 4 (puerto en 1) | 11 · US-11.1 | OK · IntegracionIT (outbox CREDENCIALES) · E2E guion 1 |
| CU-38 | Recordatorio de turno (WhatsApp) | RF-07 / RF-13 | 11 (+3.3) | 4 | 11 · US-11.2 | OK · job recordatorio-turnos (ejecución manual) · E2E guion 6 |
| CU-39 | Notificación de deuda (WhatsApp) | RF-07 / RF-13 | 11 (+7.2) | 4 | 11 · US-11.3 | OK · job recordatorio-deuda · E2E guion 6 |
| CU-40 | Comentario sobre rutina | RF-15 | 9 | 3 | 09 · US-09.5 | OK · comentarios de rutina |
| CU-41 | Comentario sobre plan | RF-15 | 8 | 3 | 08 · US-08.4 | OK · comentarios de plan · E2E guion 3 |
| CU-42 | Registrar molestias físicas | RF-15 | 9 | 3 | 09 · US-09.6 | OK · prueba API local (alerta prioritaria) · E2E guion 4 |
| CU-43 | Observaciones hambre/saciedad | RF-15 | 8 | 3 | 08 · US-08.5 | OK · paciente/observaciones · E2E guion 3 |
| CU-44 | Registrar pago | RF-08 | 7 | 2 | 07 · US-07.2 | OK · Newman 07-pagos (comprobante) · E2E guion 6 |
| CU-45 | Registrar deuda pendiente | RF-08 | 7 | 2 | 07 · US-07.4 | OK · Newman 07-pagos (duplicado 409) · E2E guion 6 |
| CU-46 | Historial de pagos | RF-08 | 7 | 2 | 07 · US-07.5 | OK · historial PDF/Excel |
| SDP 7.1.2/7.1.3 | Modificar / eliminar pago | RF-08 | 7 | 2 | 07 · US-07.3 | OK · PUT /pagos/{id}, POST /pagos/{id}/anular (anulación con motivo, sin borrado físico) |
| CU-47 | Autenticación | RF-18 / RF-16 | 13 | 4 (esqueleto 1) | 13 · US-13.1 | OK · IntegracionIT (bloqueo 5 intentos) · E2E guion 1 |
| CU-48 | Recuperar contraseña | RF-18 | 13 | 4 | 13 · US-13.2 | OK · recupero por token (pantalla recupero) |
| CU-49 | Cambiar contraseña | RF-18 | 13 | 4 | 13 · US-13.3 | OK · IntegracionIT, Newman 13 · E2E guion 1 |
| CU-50 | Cerrar sesión | RF-18 / RF-16 | 13 | 4 | 13 · US-13.4 | OK · IntegracionIT, Newman (token revocado 401) |
| CU-51 | Backup manual | RNF Confiabilidad | 15 | 5 | 15 · US-15.1 | OK · backup local generado y descargado · E2E guion 7 |
| CU-52 | Programar backup | RNF Confiabilidad | 15 | 5 | 15 · US-15.2 | OK · ReglasVariasTest (cron), pantalla Backups · E2E guion 7 |
| CU-53A/B/C | Alta / modificación / baja de alimento | RF-11 | 10 | 3 | 10 · US-10.1–10.3 | OK · admin/catalogo (alta, edición, baja lógica y reactivación) |
| CU-54A/B/C | Alta / modificación / baja de ejercicio | RF-11 | 10 | 3 | 10 · US-10.4–10.6 | OK · admin/catalogo (alta, edición, baja lógica y reactivación) |

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
| 3.1 Usabilidad (≤ 30 min, responsive, multi-dispositivo) | Mobile-first, homes por rol, ayudas contextuales. Multi-dispositivo verificado: 8 guiones E2E en Chrome escritorio y Android (16/16). **Pendiente:** prueba con 3 usuarios y Safari iOS | Constitución VIII · 16 · `pruebas/e2e/RESULTADOS.md` |
| 3.2 Disponibilidad 99 % | App Service Always On + health check; Application Insights con prueba estándar cada 5 min (São Paulo y Texas) y alerta por mail (`mgnutri-caido`) | 17 · manual del sistema, «Monitoreo» |
| 3.2 Tolerancia a fallos | Handler global, bitácora de errores en transacción aparte, outbox con reintentos | 14, 11 |
| 3.2 Backup diario / RTO ≤ 2 h | PITR de Azure SQL + CU-51/52. Ensayo del 19/11/2026: PITR ≈ 8 min y BACPAC ≈ 15 min hasta la app funcionando (cumple) | 15 · manual del sistema, «Backup y restauración» |
| 3.3 Autenticación, roles, cifrado, registro de accesos | JWT, BCrypt, AES-GCM, matriz de permisos, RF-16 | 13 |
| 3.4 p90 < 5 s, 50 concurrentes, 500 pacientes | Índices, consultas agregadas. **Cumple** (k6, 18/11/2026): p90 máximo 541 ms (mi plan); login 405 ms, calendario 73 ms, ficha 101 ms, reporte mensual 127 ms. Repetir en `dev` de Azure | 16 · `pruebas/rendimiento/README.md` |
| 3.5 Modularidad, versionado, datos estables, bitácora | Módulos por dominio, tags, Flyway, RF-17 | 02-arquitectura · 14 · 17 |
| 3.6 / 3.7 Stack y despliegue en Azure | Constitución III | 00 · 17 |
| 3.8 Manual, ayuda en línea, guía técnica | Entregables | 17 |
| 3.10 Ley 25.326, T&C | Aceptación obligatoria, minimización de datos en reportes y logs | 13 · Constitución V |
| 3.11 ISO 25010 / 27001 / estándares web | Prácticas de calidad y seguridad descritas | Constitución VII · 13 |
