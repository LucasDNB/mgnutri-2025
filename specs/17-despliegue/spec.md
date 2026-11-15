# Spec 17 · Despliegue y documentación final

| Campo | Valor |
|---|---|
| Paquete SDP | 17 – Despliegue (17.1.1 entorno Azure, 17.1.2 backend, 17.1.3 frontend, 17.1.4 base de datos, 17.1.5 pruebas finales) |
| RNF | 3.6 Soportabilidad, 3.7 Restricciones de diseño, 3.8 Documentación, 3.5 Actualización programada |
| Entregables SDP | Prototipo funcional (DEMO), Manual de usuario, Manual del sistema, Informe final |
| Sprint | 6 · Backlog: Despliegue de sistema completo (8 h) |
| Depende de | 16 |

## Objetivo
Sistema completo operando en Azure, accesible por HTTPS desde cualquier dispositivo, con datos de demo, integraciones activas, y la documentación de usuario y técnica entregada.

## Criterios de aceptación
- **CA-17.1** Backend en Azure App Service (Linux, Java 17, plan con Always On) respondiendo `/api/v1/salud` con `bd: OK`.
- **CA-17.2** Frontend en Azure Static Web Apps con dominio `*.azurestaticapps.net` (o propio), HTTPS, cabeceras de seguridad y `apiBaseUrl` apuntando al backend.
- **CA-17.3** Azure SQL Database con esquema migrado por Flyway al arrancar; usuario de aplicación con permisos mínimos (`db_datareader`, `db_datawriter`, `EXECUTE`, DENY en bitácora/accesos) distinto del usuario de migración.
- **CA-17.4** Secretos (`DB_URL`, credenciales, `JWT_SECRET`, `DATA_KEY_BASE64`, `WA_TOKEN`, claves de Storage) sólo en App Settings / Key Vault.
- **CA-17.5** CI/CD: push a `main` → tests → deploy backend (GitHub Action `azure/webapps-deploy`) y frontend (`Azure/static-web-apps-deploy`). Etiqueta `vX.Y.Z` por entrega (gestión de versiones, RNF 3.5).
- **CA-17.6** WhatsApp habilitado en producción con plantillas aprobadas y número verificado; jobs activos.
- **CA-17.7** Backup automático programado y una restauración ensayada (spec 15).
- **CA-17.8** Pruebas finales en entorno real (17.1.5): guiones E2E 1–8 de spec 16 ejecutados sobre producción con datos demo; evidencia en el informe.
- **CA-17.9** Monitoreo básico: Application Insights (o logs de App Service) con alerta de disponibilidad (ping cada 5 min) para medir el 99 % mensual (RNF 3.2).
- **CA-17.10** Ventanas de actualización: los despliegues de producción se hacen en horario de bajo uso; el sistema muestra un banner configurable de "mantenimiento programado" (`configuracion_sistema.mantenimiento.mensaje`) — RNF 3.5 "comunicación previa".

## Documentación (ERS 3.8, SDP §4.2)
| Documento | Contenido mínimo | Formato |
|---|---|---|
| Manual de usuario | Un capítulo por rol (paciente, profesional nutricionista/entrenador/integral, administrador) con capturas y pasos de cada función; FAQ de errores comunes | PDF |
| Manual del sistema / Guía técnica | Arquitectura, modelo de datos, variables de entorno, despliegue paso a paso en Azure (backend, frontend, BD, Storage, WhatsApp), procedimiento de backup y restauración, operación de jobs, rotación de secretos | PDF + `docs/guia-despliegue.md` |
| Ayuda en línea | Textos de ayuda contextual (`help-tip`) revisados en todas las pantallas | en la app |
| Informe final | Objetivos vs. resultados, matriz de cumplimiento (spec 16), burndown real vs. planificado, riesgos materializados, lecciones aprendidas | PDF |

## Tareas
- [ ] T-17.1 Configuración final de recursos Azure y App Settings
- [ ] T-17.2 Pipelines de deploy backend y frontend
- [ ] T-17.3 Migración de BD productiva + carga de catálogo y datos demo
- [ ] T-17.4 Activación de WhatsApp y jobs en producción
- [ ] T-17.5 Pruebas finales en entorno real (8 h en total para T-17.1–17.5)
- [ ] T-17.6 Manual de usuario, manual del sistema e informe final (etapa "Despliegue y documentación", oct–nov)
