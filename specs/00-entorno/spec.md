# Spec 00 · Instalación y configuración del entorno

| Campo | Valor |
|---|---|
| Paquete SDP | 0 – Instalación y configuración del entorno de trabajo |
| RF / CU | — (habilitante de todos) · RNF 3.5, 3.6, 3.7 |
| Sprint | 1 · Backlog: Instalación de herramientas (3 h), Configuración de entorno (2 h), Configuración paquete Azure (1 h), Configuración API WhatsApp (1 h), Diagrama de clases / E-R (1 h), Estructura del proyecto Spring + Maven (0 h) |
| Depende de | — |
| Bloquea a | Todos los specs |

## Objetivo
Dejar un repositorio que compila, corre localmente contra SQL Server, despliega un "hola mundo" a Azure y puede enviar un mensaje de prueba por WhatsApp Cloud API, con la estructura de capas definida en la constitución.

## Historias
**US-00.1** Como desarrollador quiero un esqueleto Spring Boot + Maven con la estructura de paquetes por módulo y capas, para empezar a implementar sin decidir estructura cada vez.
- CA-00.1.1 `mvn verify` compila y corre tests en verde en un repo recién clonado.
- CA-00.1.2 `GET /api/v1/salud` responde `200 {"estado":"OK","version":"x.y.z","bd":"OK"}`.
- CA-00.1.3 Existen los paquetes de módulo listados en `docs/02-arquitectura.md §2`, cada uno con `controller/service/dao/domain/dto`.

**US-00.2** Como desarrollador quiero conectar a SQL Server local y a Azure SQL con el mismo código, cambiando sólo el perfil.
- CA-00.2.1 Perfil `local` arranca contra SQL Server en Docker (`mcr.microsoft.com/mssql/server:2022-latest`) con `docker compose up`.
- CA-00.2.2 Flyway aplica V1 al arrancar; `ddl-auto=validate` no reporta diferencias.
- CA-00.2.3 Ningún secreto está versionado; `.env.example` lista todas las variables.

**US-00.3** Como desarrollador quiero los recursos de Azure creados y documentados.
- CA-00.3.1 Existen: Resource Group `rg-mgnutri`, Azure SQL Server + DB `mgnutri` (tier Basic / serverless), App Service Plan Linux (B1, Always On) + Web App Java 17, Static Web App, Storage Account con contenedor `backups`.
- CA-00.3.2 El firewall de Azure SQL permite sólo los servicios de Azure y la IP del desarrollador.
- CA-00.3.3 `docs/guia-despliegue.md` describe cada recurso y variable (insumo del "instructivo de despliegue", ERS 3.8).

**US-00.4** Como desarrollador quiero validar la integración con WhatsApp Cloud API.
- CA-00.4.1 App de Meta creada, número de prueba, token de sistema permanente generado.
- CA-00.4.2 Plantillas enviadas a aprobación (ver spec 11 §Plantillas).
- CA-00.4.3 Un test manual (`WhatsAppSmokeTest`, deshabilitado por defecto) envía `hello_world` a un número verificado y obtiene `messages[0].id`.

**US-00.5** Como desarrollador quiero el diagrama de clases y el E-R.
- CA-00.5.1 `docs/diagramas/er.drawio` y `docs/diagramas/clases.drawio` coherentes con `docs/03-modelo-de-datos.md`.

**US-00.6** Como desarrollador quiero CI.
- CA-00.6.1 GitHub Actions: `backend.yml` (build + tests + Newman contra SQL Server de servicio) y `frontend.yml` (lint + deploy a Static Web Apps) en push a `main`.

## Plan técnico

### Herramientas (0.1)
IntelliJ IDEA, VS Code, Git + GitHub (repo privado `mgnutri`), SQL Server 2022 Developer o Docker + SSMS/Azure Data Studio, Postman, Draw.io, JDK 17 (Temurin), Maven 3.9, Node LTS (sólo para `npx` de linters y SWA CLI).

### Dependencias Maven (pom.xml)
`spring-boot-starter-web`, `-data-jpa`, `-validation`, `-security`, `-actuator`; `com.microsoft.sqlserver:mssql-jdbc`; `org.flywaydb:flyway-core` + `flyway-sqlserver`; `io.jsonwebtoken:jjwt-api/impl/jackson`; `org.springframework.boot:spring-boot-starter-aop`; `com.github.librepdf:openpdf`; `org.jfree:jfreechart`; `org.apache.poi:poi-ooxml`; `org.springdoc:springdoc-openapi-starter-webmvc-ui`; `org.mapstruct:mapstruct` (opcional); `com.azure:azure-storage-blob` y `com.azure.resourcemanager:azure-resourcemanager-sql` (spec 15). Test: `spring-boot-starter-test`, `spring-security-test`, `org.testcontainers:mssqlserver`, `junit-jupiter`.

### Archivos base
- `docker-compose.yml` (SQL Server con volumen).
- `application.yml` + perfiles (ver arquitectura §8).
- `common/error/GlobalExceptionHandler`, `BusinessException`, `ErrorCode`.
- `config/SecurityConfig` (provisorio: permite `/salud`), `CorsConfig`, `JacksonConfig` (fechas ISO, zona), `ClockConfig`.
- `frontend/` con `index.html`, `css/base.css` (tokens de color), `js/core/api.js` y `staticwebapp.config.json` (fallback y headers de seguridad: CSP, X-Frame-Options DENY).

### Tareas
- [ ] T-00.1 Instalar herramientas (0.1.1–0.1.6) · 3 h
- [ ] T-00.2 Crear proyecto Spring Boot/Maven, estructura MVC por módulos, docker-compose, perfiles, `/salud` (0.2.1–0.2.4) · 2 h
- [ ] T-00.3 Crear recursos Azure y App Settings; pipeline de deploy "hola mundo" (0.3) · 1 h
- [ ] T-00.4 App Meta, token, plantillas, smoke test WhatsApp (0.4.1–0.4.3) · 1 h
- [ ] T-00.5 Diagramas de clases y E-R · 1 h
- [ ] T-00.6 Workflows de GitHub Actions · (incluido en T-00.2)
