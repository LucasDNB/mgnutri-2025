# 06 · Roadmap de implementación

Respeta la asignación de paquetes y horas del Sprint Planning v1.0.0 (124 h) con los ajustes D-27 (seguridad mínima y puerto de notificaciones en Sprint 1) y el adelanto de la estructura del catálogo a Sprint 2.

## Orden de implementación con spec-kit

```
Sprint 1 (32 h) ─ 00 entorno → 14 bitácora → 13 (esqueleto) → 01 pacientes → 02 profesionales → 03 turnos → 11 (outbox + adaptador log)
Sprint 2 (24 h) ─ 10 (estructura + semilla mínima) → 04 planes → 05 rutinas → 06 evolución → 07 pagos
Sprint 3 (20 h) ─ 10 catálogo completo → 08 consulta plan → 09 consulta rutina (+ PDFs de 04/05)
Sprint 4 (20 h) ─ 11 WhatsApp + jobs → 12 reportes → 13 seguridad completa
Sprint 5 ( 8 h) ─ 15 backup
Sprint 6 (20 h) ─ 16 pruebas e2e → 17 despliegue + documentación
```

## Backlog del Sprint Planning ↔ tareas de los specs

| Tarea del backlog | h | Sprint | Tareas spec |
|---|---|---|---|
| Instalación de herramientas | 3 | 1 | T-00.1 |
| Configuración de entorno | 2 | 1 | T-00.2, T-00.6 |
| Configuración de paquete Azure | 1 | 1 | T-00.3 |
| Configuración API WhatsApp | 1 | 1 | T-00.4 |
| Diagrama de clases / BD E-R | 1 | 1 | T-00.5 |
| Estructura del proyecto Spring + Maven | 0 | 1 | T-00.2 |
| Bitácora/log del sistema | 4 | 1 | T-14.1–14.3 |
| Gestión de pacientes – BackEnd | 2 | 1 | T-01.1, T-01.2 |
| Gestión de profesionales – BackEnd | 2 | 1 | T-02.1 |
| Gestión de pacientes – FrontEnd | 3 | 1 | T-01.3 |
| Gestión de turnos – BackEnd | 3 | 1 | T-03.1 |
| Gestión de turnos – FrontEnd | 3 | 1 | T-03.2 |
| Gestión de profesionales – FrontEnd | 3 | 1 | T-02.2 |
| Capa de servicios | 1 | 1 | T-13.0, T-11.0 (transversales) |
| Testeos de profesionales / pacientes / turnos | 1+1+1 | 1 | T-02.3, T-01.4, T-03.3 |
| Gestión de planes alimentarios – BackEnd | 3 | 2 | T-04.1 |
| Gestión de rutinas – BackEnd | 3 | 2 | T-05.1 |
| Gestión de pagos – BackEnd | 3 | 2 | T-07.1 |
| Planes – FrontEnd | 2 | 2 | T-04.3 |
| Rutinas – FrontEnd | 2 | 2 | T-05.2 |
| Pagos – FrontEnd | 2 | 2 | T-07.2 |
| Evolución de desempeño físico | 2 | 2 | T-06.2 |
| Evolución nutricional (composición corporal) | 2 | 2 | T-06.1 |
| Capa de servicios | 2 | 2 | T-04.2, T-10.0 |
| Testeo de planes / rutinas / pagos | 1+1+1 | 2 | T-04.5, T-05.4, T-07.3 |
| Base de datos de ejercicios | 4 | 3 | T-10.1 |
| Base de datos de alimentos | 4 | 3 | T-10.2 |
| Consulta de rutina de entrenamiento | 3 | 3 | T-09.1, T-09.2, T-05.3 |
| Consultas y descargas de planes alimentarios | 3 | 3 | T-08.1, T-08.2, T-04.4 |
| Testeo de rutinas | 3 | 3 | T-09.3, T-06.3 |
| Testeo de planes alimentarios | 3 | 3 | T-08.3 |
| WhatsApp – envío de credenciales | 4 | 4 | T-11.1 |
| WhatsApp – recordatorio de turnos | 4 | 4 | T-11.2 |
| WhatsApp – recordatorio de deuda | 4 | 4 | T-11.3 |
| Reportes de sistema | 4 | 4 | T-12.1, T-12.2 |
| Registro y seguridad de accesos | 4 | 4 | T-13.1–13.3 |
| Gestión de back-ups | 8 | 5 | T-15.1–15.3 |
| Testeo de sistema end-to-end | 12 | 6 | T-16.1–16.5 |
| Despliegue de sistema completo | 8 | 6 | T-17.1–17.5 |
| **Total** | **124** | | |

## Hitos y definición de "terminado" por sprint
| Sprint | Demo al tutor |
|---|---|
| 1 | Admin inicia sesión, da de alta profesional y paciente (mensaje en log), asigna, carga agenda; paciente pide/reprograma/cancela turno; bitácora visible |
| 2 | Nutricionista crea y genera plan; entrenador crea rutina; carga de antropometría con gráficos; cuenta corriente con pagos y deudas |
| 3 | Paciente usa Nutrición y Entrenamiento desde el celular (registros, reemplazos, alternativas, videos, comentarios, molestias); PDFs; catálogos completos |
| 4 | Mensajes reales por WhatsApp; recordatorios; 5 reportes PDF/Excel; recupero y cambio de contraseña; registro de accesos |
| 5 | Backup manual y programado con descarga |
| 6 | Sistema en Azure con datos demo, matriz de cumplimiento completa, manuales |

Un spec está terminado cuando: todos sus CA tienen test o caso Postman en verde, la bitácora registra sus eventos, la pantalla funciona en celular y escritorio, y la columna de trazabilidad está completa.

## Riesgos de planificación (para la lista de riesgos del SDP)
| Riesgo | Señal | Mitigación |
|---|---|---|
| Subestimación: 124 h para 54 CU + 2 integraciones externas es muy ajustado (≈ 2,3 h por CU incluyendo front, back y tests) | Burndown real por encima del planificado en S1 | Priorizar por prioridad ERS (Alta → Media alta → Media); recortar primero: generación masiva de cuotas, aranceles con notificación, exportación Excel de bitácora, vista "mes" del calendario de rutina |
| Rechazo de plantillas de WhatsApp | Plantilla `mg_credenciales` rechazada | Modo `ENLACE` (activación de un solo uso) ya previsto |
| Plan F1 de App Service sin Always On → jobs no corren | Recordatorios no salen | Plan B1 (créditos Azure for Students) o disparar jobs con un timer externo (GitHub Actions cron llamando a `/admin/jobs/...`) |
| Exportación BACPAC requiere permisos de gestión en la suscripción | Error de autorización en export | Usar service principal con rol "SQL DB Contributor"; fallback: documentar PITR nativo como cumplimiento del backup diario |
| Datos de catálogo nutricional sin fuente confiable | Valores inconsistentes | Usar una tabla pública citada y la validación de coherencia de kcal |
