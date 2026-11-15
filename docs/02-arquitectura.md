# 02 · Arquitectura

## 1. Vista de despliegue

```
 ┌──────────────────────────┐        HTTPS/JSON         ┌─────────────────────────────┐
 │ Navegador (PC/tablet/cel)│ ───────────────────────▶  │ Azure App Service (Linux)   │
 │ HTML + CSS + JS (ESM)    │   Authorization: Bearer   │ Spring Boot 3 · Java 17     │
 │ servido por Azure Static │ ◀───────────────────────  │  /api/v1/**                 │
 │ Web Apps                 │                           │  @Scheduled jobs            │
 └──────────────────────────┘                           └──────┬──────────┬───────────┘
                                                               │ JDBC     │ HTTPS
                                                               ▼          ▼
                                              ┌────────────────────┐  ┌───────────────────────┐
                                              │ Azure SQL Database │  │ WhatsApp Cloud API    │
                                              │ (SQL Server)       │  │ graph.facebook.com    │
                                              └─────────┬──────────┘  └───────────────────────┘
                                                        │ Export BACPAC
                                                        ▼
                                              ┌────────────────────┐
                                              │ Azure Blob Storage │
                                              │ contenedor backups │
                                              └────────────────────┘
```

Entornos: `local` (SQL Server Developer / Docker, adaptador de notificaciones "log", backup local), `dev` y `prod` en Azure.

## 2. Estructura del repositorio

```
mgnutri/
├── .specify/                     # spec-kit (constitución, plantillas)
├── specs/                        # specs por paquete de trabajo (este paquete)
├── docs/                         # documentos transversales
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/java/ar/edu/usal/mgnutri/
│       │   ├── MgNutriApplication.java
│       │   ├── config/           # Security, CORS, Jackson, Scheduling, OpenAPI
│       │   ├── common/           # errores, paginación, AttributeConverters, utilidades de fecha
│       │   ├── seguridad/        # auth, usuarios, tokens, registro de accesos  (P13, RF-16/18)
│       │   ├── bitacora/         # @Auditable, aspecto, consulta                  (P14, RF-17)
│       │   ├── pacientes/        #                                                (P1, RF-01)
│       │   ├── profesionales/    # incluye disponibilidad                         (P2, RF-02)
│       │   ├── turnos/           #                                                (P3, RF-03)
│       │   ├── catalogo/         # alimentos, grupos, ejercicios, grupos musc.   (P10, RF-11)
│       │   ├── nutricion/        # planes, generación, equivalencias, registros  (P4, P8)
│       │   ├── entrenamiento/    # rutinas, alternativas, registros              (P5, P9)
│       │   ├── evolucion/        # antropometría, desempeño                      (P6, RF-06)
│       │   ├── pagos/            # cargos, pagos, aranceles                      (P7, RF-08)
│       │   ├── notificaciones/   # outbox, WhatsApp, jobs, alertas               (P11, RF-07/13)
│       │   ├── reportes/         # generadores PDF/Excel                         (P12, RF-14)
│       │   ├── backup/           #                                                (P15)
│       │   └── configuracion/    # parámetros del sistema, T&C
│       │       (cada módulo: controller/ service/ dao/ domain/ dto/ mapper/)
│       └── main/resources/
│           ├── application.yml, application-{local,dev,prod}.yml
│           ├── db/migration/     # Flyway V1__..., V2__...
│           └── seed/             # datos de catálogo (alimentos, ejercicios)
├── frontend/
│   ├── index.html                # login
│   ├── staticwebapp.config.json
│   ├── css/  (base.css, layout.css, components.css, print.css)
│   ├── js/
│   │   ├── core/   (api.js, auth.js, router-guard.js, ui.js, validators.js, format.js)
│   │   ├── components/ (modal.js, table.js, calendar.js, toast.js, help-tip.js)
│   │   └── pages/  (un módulo por página)
│   ├── admin/ profesional/ paciente/   # páginas HTML por rol
│   └── assets/ (logo, íconos)
├── postman/                      # colección + environments (pruebas de integración)
└── .github/workflows/            # backend.yml, frontend.yml
```

## 3. Capas del backend

| Capa | Responsabilidad | Reglas |
|---|---|---|
| `controller` | HTTP, validación sintáctica (`@Valid`), mapeo DTO, códigos de estado | Sin lógica de negocio. Anotaciones `@PreAuthorize` por rol. |
| `service` | Reglas de negocio, transacciones (`@Transactional`), autorización por pertenencia, auditoría (`@Auditable`), publicación de eventos | Único lugar donde se decide. Lanza `BusinessException(code)` |
| `dao` | Acceso a datos | Interfaces `XxxDao extends JpaRepository`; consultas complejas con `@Query`/Criteria. Sin reglas. |
| `domain` | Entidades JPA, enums, value objects | Validaciones invariantes simples |
| `dto` / `mapper` | Records de entrada/salida, mapeo manual o MapStruct | Las entidades no se serializan |

Componentes transversales:
- `GlobalExceptionHandler` → Problem Details (ver [`04-convenciones-api.md`](04-convenciones-api.md)).
- `AuditAspect` → intercepta `@Auditable(modulo, tipo, entidad)` y persiste en `bitacora_evento` (en transacción `REQUIRES_NEW` para registrar también errores).
- `AccesoPacienteGuard` → `assertPuedeVer(pacienteId)` / `assertPuedeEditar(pacienteId, area)` usado por todos los servicios con datos de pacientes.
- `CryptoConverter` → `AttributeConverter<String,String>` AES-256-GCM.
- `Clock` inyectable (tests de reglas de 24 h / 7 días y jobs).
- Eventos de dominio (`ApplicationEventPublisher`): `PacienteCreado`, `ProfesionalCreado`, `TurnoReservado`, `TurnoReprogramado`, `TurnoCancelado`, `CargoRegistrado`, `ArancelModificado`, `MolestiaRegistrada`… consumidos por notificaciones y alertas.

## 4. Seguridad

- Spring Security, stateless, filtro JWT.
- `POST /api/v1/auth/login` → `{ token, expiraEn, usuario: {id, nombre, roles, debeCambiarPassword, debeAceptarTerminos} }`.
- Claims: `sub` (usuarioId), `roles`, `pacienteId`/`profesionalId`, `jti`, `exp`.
- Logout: inserta `jti` en `token_revocado` (limpieza diaria de expirados).
- Usuario con `debeCambiarPassword` o `debeAceptarTerminos` sólo puede llamar a `/auth/**`, `/terminos/**`, `/me`.
- Roles y matriz de permisos: ver §6.

## 5. Frontend

- Multipágina; cada página HTML carga `js/pages/<pagina>.js` como módulo.
- `core/api.js`: `fetch` con base URL por entorno, agrega `Authorization`, maneja 401 (redirige a login) y Problem Details (muestra `detail` en toast).
- `core/auth.js`: guarda token en `sessionStorage`, expone `usuarioActual()`, `tieneRol()`.
- `core/router-guard.js`: al cargar una página verifica token y rol permitido; si no, redirige al home correspondiente.
- Diseño: fondo crema `#FBF6EA`, verde primario `#2E5E3A`, verde secundario `#7FA36B`, texto `#1F2A1F`, botones con borde redondeado y tipografía sans en negrita para acciones (según mockups). Tokens CSS en `:root`.
- Componentes: `calendar.js` (vistas día/semana/mes), `table.js` (orden/paginación/filtros), `modal.js`, `toast.js`, `help-tip.js` (ícono "?" con texto de ayuda — ERS 3.8).
- Impresión: `print.css` para vistas imprimibles.

### Mapa de pantallas
| Rol | Home (botones) | Páginas |
|---|---|---|
| Paciente | Nutrición · Entrenamiento · Contacto | Mi plan (grilla semanal) · Registro diario · Observaciones diarias · Mis turnos · Nuevo turno · Mi rutina · Registro de entrenamiento · Molestias · Mi evolución · Perfil |
| Nutricionista | Calendario · Pacientes | Ficha paciente (pestañas: datos, plan, evolución, registros, comentarios) · Editor de plan · Generador automático · Antropometría · Reportes · Base de alimentos · Novedades |
| Entrenador | (Calendario) · Pacientes | Ficha paciente (rutina, evolución, registros) · Editor de rutina · Evaluación física · Reportes · Base de ejercicios · Novedades |
| Integral | Calendario · Pacientes | Unión de nutricionista + entrenador |
| Admin | Calendario · Pacientes · Profesionales · Pagos · Opciones avanzadas | ABM pacientes · Asignación · ABM profesionales · Disponibilidad · Turnos (todos) · Cuenta corriente · Aranceles · Reportes · Bitácora · Accesos · Backups · Configuración · Notificaciones fallidas |

## 6. Matriz de permisos (resumen)

| Recurso / acción | ADMIN | NUTRI | ENTR | PACIENTE |
|---|---|---|---|---|
| ABM pacientes, asignación | ✔ | — | — | — |
| ABM profesionales | ✔ | — | — | — |
| Disponibilidad propia | ✔ (de todos) | ✔ propia | ✔ propia | — |
| Turnos | ✔ todos | ✔ propios | ✔ propios | ✔ propios (R-T1/R-T2) |
| Planes alimentarios (ABM, generar) | — (lectura) | ✔ asignados | — | ver/descargar propio |
| Rutinas (ABM) | — (lectura) | — | ✔ asignados | ver/descargar propia |
| Antropometría | lectura | ✔ asignados | lectura asignados | ver propia |
| Evaluación física | lectura | lectura asignados | ✔ asignados | ver propia |
| Registros diarios, comentarios, molestias | lectura | ver asignados | ver asignados | ✔ propios |
| Base de alimentos | ✔ | ✔ | lectura | — |
| Base de ejercicios | ✔ | lectura | ✔ | — |
| Pagos, cargos, aranceles | ✔ | — | — | ver saldo propio |
| Reportes | ✔ todos | ✔ de sus pacientes (no pagos) | ✔ de sus pacientes (no pagos) | — |
| Bitácora, accesos | ✔ | — | — | — |
| Backups, configuración | ✔ | — | — | — |

Integral = unión de NUTRI y ENTR.

## 7. Jobs programados

| Job | Cron (ART) | Spec |
|---|---|---|
| Despachar outbox de notificaciones | cada 1 min | 11 |
| Recordatorio de turnos del día siguiente | `0 0 9 * * *` | 11 |
| Recordatorio de deuda | `0 0 10 * * SAT` | 11 |
| Finalizar planes/rutinas vencidos | `0 30 0 * * *` | 04/05 |
| Limpiar tokens revocados/recupero expirados | `0 0 3 * * *` | 13 |
| Backup programado | configurable (CU-52) | 15 |

## 8. Configuración (`application.yml`)

```yaml
mgnutri:
  zona-horaria: America/Argentina/Buenos_Aires
  cors.allowed-origins: ${FRONTEND_URL}
  jwt: { secret: ${JWT_SECRET}, expiracion-horas: 8 }
  crypto.key: ${DATA_KEY_BASE64}          # AES-256
  seguridad: { max-intentos: 5, bloqueo-minutos: 15, recupero-horas: 24 }
  whatsapp:
    habilitado: ${WA_ENABLED:false}
    phone-number-id: ${WA_PHONE_ID}
    token: ${WA_TOKEN}
    api-version: v21.0
    idioma-plantillas: es_AR
  backup:
    proveedor: ${BACKUP_PROVIDER:local}   # local | azure
    azure: { subscription-id: ..., resource-group: ..., server: ..., database: ..., storage-uri: ..., storage-key: ... }
spring:
  datasource.url: ${DB_URL}
  jpa.hibernate.ddl-auto: validate
  flyway.enabled: true
```
