# Spec 14 · Bitácora del sistema

| Campo | Valor |
|---|---|
| Paquete SDP | 14 – Bitácora del sistema (+ subpaquetes x.2/x.3 "Bitácora de …" de los paquetes 1–7 y 13) |
| RF | RF-17 Registro y consulta de eventos en bitácora (ex RNF-2) · RNF 3.2 (tolerancia a fallos), 3.5 |
| CU | CU-35 Registrar evento en bitácora · CU-36 Consultar bitácora |
| Sprint | 1 · Backlog: Bitácora/log del sistema (4 h) |
| Depende de | 00 |
| Decisiones | D-28 |

## Contexto
Toda acción relevante deja una huella inalterable: quién hizo qué, cuándo, sobre qué entidad y con qué resultado. El administrador la consulta con filtros. Es la base de la trazabilidad exigida por el SDP (§3.4) y la ERS.

## Historias de usuario

### US-14.1 Registrar evento (CU-35)
- **CA-14.1.1** Ante una acción crítica (alta, baja, modificación, generación de reportes, descargas de documentos, cambios de contraseña y de configuración, envíos de notificaciones, backups) el sistema crea un registro con: fecha y hora, usuario (o "SISTEMA" para jobs), roles, módulo, tipo de evento, entidad y su id, descripción legible, resultado, IP.
- **CA-14.1.2** Si la acción principal falla con error inesperado, el sistema registra igualmente un evento `ERROR` con el código de error (alterno 1.1), y la aplicación sigue operando (RNF tolerancia a fallos).
- **CA-14.1.3** El registro de bitácora se hace en una transacción independiente: un rollback de la operación principal no borra el evento de error, y un fallo al escribir la bitácora se loguea pero no tumba la operación del usuario **[Validar: alternativa más estricta es abortar la operación si no se puede auditar]**.
- **CA-14.1.4** En modificaciones, `detalle` incluye los nombres de campos cambiados y, para campos no sensibles, valores anterior → nuevo. Nunca contraseñas, tokens ni datos de salud.
- **CA-14.1.5** La bitácora está protegida contra edición manual: no hay endpoints de modificación/eliminación y la base de datos rechaza UPDATE/DELETE (requerimiento especial).

### US-14.2 Consultar bitácora (CU-36)
- **CA-14.2.1** *Opciones avanzadas › Bitácora* muestra filtros: rango de fecha/hora, usuario, módulo, tipo de evento, entidad/id, resultado.
- **CA-14.2.2** Al aplicar filtros se muestra el detalle de eventos paginado, ordenado del más reciente al más antiguo, con posibilidad de abrir el `detalle`.
- **CA-14.2.3** Sin eventos en el período → "Sin resultados" (alterno 1.1).
- **CA-14.2.4** El contenido no puede modificarse desde la interfaz (sólo lectura).
- **CA-14.2.5** Exportación a Excel del resultado filtrado (registra a su vez un evento `REPORTE`).
- **CA-14.2.6** Sólo ADMIN con permiso `VER_AUDITORIA`.

### US-14.3 Retención
- **CA-14.3.1** Retención mínima 6 meses; no existe proceso que borre eventos más nuevos que 180 días. El archivado de eventos más viejos queda fuera de alcance (se conservan).

## Catálogo de eventos mínimos
| Módulo | Eventos |
|---|---|
| PACIENTES | ALTA, MODIFICACION, BAJA, asignación/desasignación, CONSULTA_SENSIBLE |
| PROFESIONALES | ALTA, MODIFICACION, BAJA, disponibilidad, bloqueos |
| TURNOS | ALTA, MODIFICACION (reprogramación, estado), BAJA (cancelación) |
| NUTRICION | ALTA/MODIFICACION/BAJA de plan, DESCARGA PDF, reemplazo de alimento |
| ENTRENAMIENTO | ALTA/MODIFICACION/BAJA de rutina, DESCARGA PDF, reemplazo de ejercicio |
| EVOLUCION | ALTA/MODIFICACION/BAJA de mediciones y evaluaciones |
| PAGOS | ALTA/MODIFICACION/BAJA de pagos, cargos y aranceles; DESCARGA comprobante |
| CATALOGO | ALTA/MODIFICACION/BAJA de alimentos y ejercicios |
| NOTIFICACIONES | NOTIFICACION (enviada/fallida/omitida), reintento |
| REPORTES | REPORTE, DESCARGA |
| SEGURIDAD | CAMBIO_PASSWORD, reenvío de credenciales, nueva versión de T&C |
| BACKUP | BACKUP (manual/automático, OK/ERROR), cambio de programación |
| CONFIGURACION | CONFIGURACION (cambio de parámetros) |
