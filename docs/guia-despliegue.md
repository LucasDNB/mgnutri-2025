# Guía de despliegue y operación (CA-00.3.3, spec 17)

## 1. Recursos de Azure

| Recurso | Nombre sugerido | Configuración |
|---|---|---|
| Resource Group | `rg-mgnutri` | Región permitida por la suscripción (Azure for Students: Chile Central) |
| Azure SQL Server + DB | `sql-mgnutri` / `mgnutri` | Basic o serverless; firewall: servicios de Azure + IP del desarrollador |
| App Service Plan | `asp-mgnutri` | Linux **B1** con *Always On* (necesario para los jobs, D-32) |
| Web App | `app-mgnutri` | Java 17 SE; health check `/api/v1/salud`; sirve la API y el frontend (empaquetado en el jar) |
| Storage Account | `stmgnutri` | Contenedor privado `backups` |
| Application Insights | `appi-mgnutri` | Alerta de disponibilidad sobre `/api/v1/salud` cada 5 min (RNF 3.2) |

## 2. Base de datos

1. Crear la base `mgnutri`.
2. Usuario de **migración** (propietario del esquema) y usuario de **aplicación** con permisos mínimos:

```sql
CREATE USER mgnutri_migracion WITH PASSWORD = '<secreto>';
ALTER ROLE db_owner ADD MEMBER mgnutri_migracion;
CREATE USER mgnutri_app_user WITH PASSWORD = '<secreto>';
-- La migración V2 crea el rol si no existe y le da los permisos (incluye DENY UPDATE/DELETE sobre bitácora y accesos)
CREATE ROLE mgnutri_app;
ALTER ROLE mgnutri_app ADD MEMBER mgnutri_app_user;
```

3. Flyway aplica las migraciones al arrancar con `SPRING_FLYWAY_URL/USER/PASSWORD` (usuario de migración);
   la aplicación usa `DB_URL/DB_USER/DB_PASSWORD` (usuario de aplicación).

## 3. App Settings del backend

Todas las variables de [`.env.example`](../.env.example). Mínimas para producción:

| Variable | Valor |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` (o `dev`) |
| `DB_URL` | `jdbc:sqlserver://sql-mgnutri.database.windows.net:1433;databaseName=mgnutri;encrypt=true` |
| `DB_USER` / `DB_PASSWORD` | usuario de aplicación |
| `SPRING_FLYWAY_URL` / `SPRING_FLYWAY_USER` / `SPRING_FLYWAY_PASSWORD` | usuario de migración |
| `JWT_SECRET` | `openssl rand -base64 48` |
| `DATA_KEY_BASE64` | `openssl rand -base64 32` (**no perderla**: cifra los datos sensibles) |
| `ADMIN_PASSWORD_INICIAL` | contraseña temporal del primer administrador |
| `FRONTEND_URL` | `https://<app>.azurewebsites.net` (CORS; mismo dominio que la API) |
| `WA_ENABLED`, `WA_PHONE_ID`, `WA_TOKEN` | WhatsApp Cloud API |
| `BACKUP_PROVIDER=azure` y `AZ_*` | exportación BACPAC (identidad administrada con rol *SQL DB Contributor*) |

Además, en `configuracion_sistema` (pantalla *Opciones avanzadas › Configuración*): `centro.url_sistema` con la URL del frontend.

## 4. Frontend

- Maven empaqueta `frontend/` dentro del jar (`classpath:/static`) y el App Service lo sirve en el mismo dominio que la
  API, con las cabeceras de seguridad de `SecurityConfig`. Un solo despliegue, sin CORS.
- Alternativa (no usada con Azure for Students, que no permite Static Web Apps en sus regiones): publicar `frontend/`
  en Static Web Apps con el workflow `frontend.yml` y los secretos `AZURE_STATIC_WEB_APPS_API_TOKEN` y `API_BASE_URL`.

## 5. CI/CD (GitHub Actions)

| Workflow | Disparador | Pasos |
|---|---|---|
| `.github/workflows/backend.yml` | push / PR a `main` en `backend/**` o `frontend/**`, o manual | `mvn verify` (unitarias + Testcontainers) → empaquetado → deploy a App Service si existe el secreto `AZURE_WEBAPP_PUBLISH_PROFILE` |
| `.github/workflows/frontend.yml` | push a `main` en `frontend/**` | sintaxis JS (`node --check`) → deploy a Static Web Apps si existe `AZURE_STATIC_WEB_APPS_API_TOKEN` (alternativa) |

Etiquetar cada entrega con `vX.Y.Z` (RNF 3.5).

## 6. WhatsApp Cloud API

1. Crear la app en Meta for Developers, agregar el producto WhatsApp y un número.
2. Generar un token de usuario del sistema permanente (`WA_TOKEN`) y tomar el `phone-number-id` (`WA_PHONE_ID`).
3. Enviar a aprobación las plantillas de [spec 11](../specs/11-notificaciones/spec.md) (categoría *Utility*, idioma `es_AR`).
4. Si Meta rechaza `mg_credenciales` con contraseña, usar `WA_CREDENCIALES_MODO=ENLACE` (envía un enlace de activación).
5. Con `WA_ENABLED=false` los mensajes se registran en el log y quedan `ENVIADA` con id `LOCAL-…`.

## 7. Jobs programados (zona America/Argentina/Buenos_Aires)

| Job | Programación |
|---|---|
| Despacho de notificaciones | cada minuto |
| Recordatorio de turnos de mañana | `turnos.recordatorio_hora` (09:00) |
| Recordatorio de deuda | `pagos.recordatorio_cron` (sábados 10:00) |
| Finalizar planes/rutinas vencidos | 00:30 |
| Limpieza de tokens | 03:00 |
| Backup automático | configurable en *Backups* |

Pueden ejecutarse manualmente desde *Opciones avanzadas › Notificaciones* o con `POST /api/v1/admin/jobs/{job}/ejecutar`.
Si el plan no tiene *Always On*, programar un cron externo que invoque esos endpoints.

## 8. Backup y restauración (spec 15, RTO ≤ 2 h)

- **Automático de Azure SQL**: restauración a un punto en el tiempo desde el portal (*SQL database › Restore*), elegir
  fecha/hora, crear base nueva y actualizar `DB_URL`.
- **BACPAC del sistema**: *Backups › Generar backup* exporta a `backups/mgnutri-AAAAMMDD-HHmmss.bacpac`; la descarga usa
  un enlace SAS de 15 minutos. Para restaurar: *SQL server › Import database* con el BACPAC → base nueva → cambiar `DB_URL`.
- **Local (Docker)**: `BACKUP DATABASE` a `/var/opt/mssql/backup`; restaurar con
  `RESTORE DATABASE mgnutri FROM DISK = '/var/opt/mssql/backup/<archivo>.bak' WITH REPLACE`.

## 9. Rotación de secretos

- `JWT_SECRET`: cambiarlo invalida todas las sesiones (los usuarios vuelven a ingresar).
- `WA_TOKEN`, claves de Storage y contraseñas de BD: rotar en App Settings y reiniciar la Web App.
- `DATA_KEY_BASE64`: **no rotar** sin un proceso de re-cifrado (los datos sensibles quedarían ilegibles).

## 10. Mantenimiento programado

Cargar el texto en `mantenimiento.mensaje` (Configuración): se muestra como banner en todas las pantallas (RNF 3.5).
