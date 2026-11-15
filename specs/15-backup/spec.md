# Spec 15 · Backup del sistema

| Campo | Valor |
|---|---|
| Paquete SDP | 15 – Backup del sistema |
| RF | RNF 3.2 Confiabilidad (backup diario, recuperación ≤ 2 h) · SDP §4.3 Soporte técnico |
| CU | CU-51 Realizar backup manual · CU-52 Programar backup automático |
| Sprint | 5 · Backlog: Gestión de back-ups del sistema (8 h) |
| Depende de | 00 (Storage Account), 14 |
| Decisiones | D-29 |

## Contexto
Además de los backups automáticos que Azure SQL realiza por sí mismo (restauración a un punto en el tiempo), el administrador puede generar una copia descargable antes de una actualización o mantenimiento, y programar copias periódicas desde el sistema.

## Historias de usuario

### US-15.1 Backup manual (CU-51)
- **CA-15.1.1** *Opciones avanzadas › Backups › Generar backup* pide confirmación ("La operación puede tardar varios minutos. El sistema sigue disponible.").
- **CA-15.1.2** Al confirmar, se inicia el backup en segundo plano; la pantalla muestra el estado (`EN_CURSO` → `OK`/`ERROR`) refrescando cada 10 s.
- **CA-15.1.3** Al terminar OK se muestra confirmación y el archivo aparece en el listado con fecha, tipo, tamaño y botón **Descargar** (requerimiento especial CU-51). La descarga usa un enlace temporal (15 min).
- **CA-15.1.4** Error en la generación → mensaje de error, registro `ERROR` y alerta (alterno 1.1).
- **CA-15.1.5** No se puede iniciar un backup si hay otro `EN_CURSO` (`BACKUP_EN_CURSO`).
- **CA-15.1.6** Bitácora `BACKUP/BACKUP` con resultado.

### US-15.2 Programar backup automático (CU-52)
- **CA-15.2.1** *Programar backup*: habilitado sí/no, frecuencia (diaria / semanal con día / mensual con día del mes), horario, retención (cantidad de días que se conservan los archivos, 7–365).
- **CA-15.2.2** Valores inválidos → error y solicita corrección (alterno 1.1).
- **CA-15.2.3** Al guardar, la programación queda activa sin reiniciar la aplicación y se confirma; bitácora `CONFIGURACION`.
- **CA-15.2.4** Cada backup automático queda registrado (requerimiento especial) con tipo `AUTOMATICO`.
- **CA-15.2.5** Los archivos más viejos que la retención se borran automáticamente del almacenamiento (el registro en `backup_registro` permanece, marcado como expirado).

### US-15.3 Restauración (procedimiento, no CU)
- **CA-15.3.1** El *Manual del sistema* documenta paso a paso: (a) restauración a un punto en el tiempo desde el portal de Azure; (b) importación de un BACPAC a una nueva base y cambio de cadena de conexión. Se ensaya una vez y se mide el tiempo (objetivo ≤ 2 h, RNF 3.2).

## Reglas
- RN-15.1 Sólo ADMIN.
- RN-15.2 Los archivos de backup contienen datos personales: almacenamiento privado (sin acceso público), cifrado en reposo (Azure Storage lo provee), enlaces SAS de corta duración, descarga auditada.
