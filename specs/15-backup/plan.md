# Plan 15 · Backup

## Datos
`backup_registro` (V14) + claves `backup.*` en `configuracion_sistema`.

## Puerto y adaptadores
```java
public interface BackupProvider {
  BackupResultado ejecutar(String nombreArchivo);     // bloqueante, corre en hilo de @Async
  URI urlDescarga(String archivo, Duration vigencia);
  void eliminar(String archivo);
}
```
- `AzureBacpacBackupProvider` (perfil dev/prod): usa `azure-resourcemanager-sql` (`database.exportTo(storageUri).withSqlAdministratorLoginAndPassword(...)`) con identidad administrada o service principal; destino `https://<cuenta>.blob.core.windows.net/backups/mgnutri-AAAAMMDD-HHmm.bacpac`; `urlDescarga` genera SAS de lectura con `azure-storage-blob`.
- `LocalSqlBackupProvider` (perfil local): `BACKUP DATABASE mgnutri TO DISK = '/var/opt/mssql/backup/...bak' WITH COPY_ONLY, COMPRESSION` vía JDBC y descarga por streaming desde el volumen.
- Selección por `mgnutri.backup.proveedor`.

## Programación dinámica (CU-52)
- `BackupScheduler` implementa `SchedulingConfigurer`; registra un `TriggerTask` cuyo `CronTrigger` se lee de `configuracion_sistema` en cada disparo, así un cambio aplica sin reinicio.
- Conversión UI → cron: diaria `0 m h * * *`; semanal `0 m h * * DOW`; mensual `0 m h D * *`.
- Limpieza por retención al finalizar cada backup automático.

## API (ADMIN)
| Método | Ruta | CU |
|---|---|---|
| GET | `/backups?page=` | CU-51 |
| POST | `/backups` → 202 `{id, estado: EN_CURSO}` | CU-51 |
| GET | `/backups/{id}` (estado) | CU-51 |
| GET | `/backups/{id}/descarga` → 302 a URL SAS (o stream en local) | CU-51 |
| GET / PUT | `/backups/programacion` `{habilitado, frecuencia: DIARIA|SEMANAL|MENSUAL, diaSemana?, diaMes?, hora: "03:00", retencionDias}` | CU-52 |

## Frontend
- `admin/backups.html`: botón Generar, listado con estado y descarga, formulario de programación con vista previa en texto ("Todos los domingos a las 03:00, se conservan 30 días").

## Pruebas
- Unit conversión a cron y validaciones (día de mes 1–28 para evitar meses cortos).
- Integración con `LocalSqlBackupProvider` contra SQL Server de Testcontainers: genera archivo > 0 bytes; segundo backup concurrente → 409.
- Prueba manual en Azure: export BACPAC, descarga, importación en una base nueva; medición de tiempo (evidencia para RNF 3.2).

## Tareas
- [ ] T-15.1 Puerto, adaptador local y Azure, registro y API (4 h)
- [ ] T-15.2 Programación dinámica + retención + pantalla (2 h)
- [ ] T-15.3 Ensayo de restauración y documentación en Manual del sistema (2 h)
