# MG Nutrición y Entrenamiento

Sistema de gestión del centro **MG Nutrición y Entrenamiento** — Seminario de Integración Profesional 2025 (USAL).
Desarrollado a partir del paquete de especificaciones en [`specs/`](specs/), [`docs/`](docs/) y la constitución en
[`.specify/memory/constitution.md`](.specify/memory/constitution.md) (ver [`SPECS.md`](SPECS.md)).

## Qué incluye

| Área | Funcionalidad | Spec |
|---|---|---|
| Seguridad | Login JWT, roles (admin, nutricionista, entrenador, integral, paciente), bloqueo por intentos, recupero por WhatsApp, cambio de contraseña, primer ingreso, T&C (Ley 25.326), registro de accesos | 13 |
| Bitácora | Registro inalterable de altas, bajas, modificaciones, reportes, descargas y configuración (`@Auditable` + triggers en BD) | 14 |
| Pacientes / profesionales | ABM, credenciales por WhatsApp, asignación por área, agenda semanal y bloqueos | 01, 02 |
| Turnos | Huecos libres, solicitud/reprogramación/cancelación con reglas de 7 días y 24 h, calendario día/semana/mes | 03 |
| Nutrición | Planes semanales manuales y **generación automática** con desvíos, equivalencias, reemplazos, registro de comidas, hambre/saciedad, PDF | 04, 08 |
| Entrenamiento | Rutinas desde catálogo, registro de cargas, variantes por grupo muscular, videos, molestias, PDF | 05, 09 |
| Evolución | Antropometría, evaluaciones físicas y gráficos (peso, composición, medidas, desempeño, cargas) | 06 |
| Pagos | Cuenta corriente, cargos, pagos con comprobante, anulaciones, deudores (FIFO), aranceles, cuotas masivas | 07 |
| Catálogo | 163 alimentos y 88 ejercicios iniciales con ABM | 10 |
| Notificaciones | Outbox con reintentos, WhatsApp Cloud API o adaptador "log", recordatorios de turnos y deudas | 11 |
| Reportes | Turnos, pagos, evolución, rutinas y planes en pantalla, PDF y Excel | 12 |
| Backup | Manual y programado (BACKUP DATABASE local o BACPAC en Azure Blob) | 15 |

## Stack

Java 17 · Spring Boot 3.3 · Hibernate/JPA (DAOs Spring Data) · SQL Server / Azure SQL · Flyway ·
Frontend HTML + CSS + JavaScript (módulos ES, sin framework) + Chart.js · OpenPDF + JFreeChart · Apache POI ·
WhatsApp Cloud API · Azure App Service, Static Web Apps, SQL Database y Blob Storage.

```
backend/   Spring Boot (controller → service → dao por módulo), migraciones Flyway, tests
frontend/  Páginas por rol (admin/, profesional/, paciente/, comun/) y módulos JS en js/
postman/   Colección de pruebas de API (Newman)
docs/      Documentos transversales + guía de despliegue
specs/     Specs por paquete de trabajo
```

## Ejecutar en local

Requisitos: JDK 17, Docker.

```bash
docker compose up -d
```

```bash
cd backend && ./mvnw spring-boot:run
```

Abrir http://localhost:8090 · usuario `admin`, contraseña temporal `Admin2025!` (perfil local; se exige cambiarla).
El backend sirve también el frontend en local. Swagger UI: http://localhost:8090/swagger-ui.html

Con datos de demostración (5 profesionales y 30 pacientes con 6 meses de historia):

```bash
cd backend && SPRING_PROFILES_ACTIVE=local,demo ./mvnw spring-boot:run
```

Cuentas demo (contraseña `Demo2025!`): nutricionistas `20111111`, `20222222`; entrenadores `20333333`, `20444444`;
integral `20555555`; pacientes `40000000`, `40001111`, `40002222`, …

## Pruebas

```bash
cd backend && ./mvnw verify
```

Incluye unitarias (reglas de turnos, generador de planes, equivalencias, FIFO de deudas, contraseñas, cron de backup),
reglas de arquitectura (ArchUnit) e integración contra SQL Server real con Testcontainers (se omiten si no hay Docker).
La colección [`postman/mgnutri.postman_collection.json`](postman/mgnutri.postman_collection.json) se ejecuta con Newman.

## Despliegue

Ver [`docs/guia-despliegue.md`](docs/guia-despliegue.md). Las variables están listadas en [`.env.example`](.env.example).

## Decisiones de implementación

Además de las decisiones D-01…D-35 de [`docs/01-decisiones.md`](docs/01-decisiones.md):

- El administrador inicial se crea al arrancar (`ADMIN_PASSWORD_INICIAL`) en lugar de en la migración V1, para no guardar un hash en el repositorio.
- R-T1 (un turno por paciente y profesional cada 7 días) se evalúa en la ventana de ±7 días alrededor del turno pedido.
- Las notificaciones se encolan con una llamada directa al outbox dentro de la misma transacción del evento (equivalente al listener `BEFORE_COMMIT` de D-35).
- En local la API corre en el puerto 8090.
