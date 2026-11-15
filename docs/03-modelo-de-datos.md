# 03 · Modelo de datos

Motor: SQL Server / Azure SQL. Convenciones: tablas y columnas en `snake_case` español; PK `id BIGINT IDENTITY`; fechas-hora `DATETIME2(0)` en UTC; fechas `DATE`; dinero `DECIMAL(12,2)`; enums como `VARCHAR(30)` con `CHECK`; auditoría de fila `created_at`, `created_by`, `updated_at`, `updated_by`; concurrencia optimista `version INT` en entidades editables. Columnas marcadas 🔒 se cifran con AES-256-GCM (`NVARCHAR(512)`).

## Diagrama E-R (resumen)

```
usuario 1─* usuario_rol
usuario 1─0..1 paciente          usuario 1─0..1 profesional
paciente *─* profesional  (paciente_profesional, por área)
profesional 1─* disponibilidad   profesional 1─* bloqueo_agenda
paciente 1─* turno *─1 profesional        turno 1─* turno_historial
grupo_alimento 1─* alimento 1─* equivalencia_manual
paciente 1─* plan_alimentario 1─* comida_plan 1─* item_comida *─1 alimento
item_comida 1─* reemplazo_alimento
paciente 1─* registro_comida *─1 comida_plan
paciente 1─* observacion_diaria
grupo_muscular 1─* ejercicio
paciente 1─* rutina 1─* dia_rutina 1─* ejercicio_rutina *─1 ejercicio
ejercicio_rutina 1─* reemplazo_ejercicio
paciente 1─* registro_entrenamiento *─1 ejercicio_rutina
paciente 1─* comentario (plan|rutina)     paciente 1─* molestia
paciente 1─* medicion_antropometrica      paciente 1─* evaluacion_fisica
paciente 1─* cargo    paciente 1─* pago    arancel
notificacion   alerta   bitacora_evento   registro_acceso
token_revocado   token_recupero   backup_registro   configuracion_sistema   terminos_condiciones
```

## 1. Seguridad (spec 13)

**usuario**
| Columna | Tipo | Notas |
|---|---|---|
| id | BIGINT PK | |
| username | VARCHAR(20) UNIQUE | número de documento (D-04) |
| password_hash | VARCHAR(100) | BCrypt |
| estado | VARCHAR(20) | `ACTIVO`, `BLOQUEADO`, `BAJA` |
| debe_cambiar_password | BIT | true tras alta/reenvío |
| intentos_fallidos | INT | |
| bloqueado_hasta | DATETIME2 NULL | |
| terminos_version_aceptada | VARCHAR(10) NULL | |
| terminos_aceptados_at | DATETIME2 NULL | |
| ultimo_acceso_at | DATETIME2 NULL | |
| auditoría + version | | |

**usuario_rol** (`usuario_id` FK, `rol` VARCHAR(20) CHECK IN (`ADMIN`,`NUTRICIONISTA`,`ENTRENADOR`,`PACIENTE`)), PK compuesta.

**token_revocado** (`jti` VARCHAR(36) PK, `expira_at`).
**token_recupero** (`id`, `usuario_id`, `token_hash` CHAR(64) UNIQUE, `expira_at`, `usado_at` NULL, `canal`).

**registro_acceso** — append-only (RF-16)
| id | fecha_hora | usuario_id NULL | username_intentado | roles | ip | user_agent | accion (`LOGIN_OK`,`LOGIN_FALLIDO`,`LOGOUT`,`BLOQUEO`,`RECUPERO_SOLICITADO`,`PASSWORD_RESTABLECIDA`,`PASSWORD_CAMBIADA`) | detalle |

**terminos_condiciones** (`version` PK, `texto` NVARCHAR(MAX), `vigente_desde`).

## 2. Personas (specs 01, 02)

**paciente**
| Columna | Tipo | Notas |
|---|---|---|
| id | BIGINT PK | |
| usuario_id | FK UNIQUE | |
| nombre, apellido | NVARCHAR(80) | obligatorios |
| tipo_documento | VARCHAR(10) | DNI, PAS, LC… |
| nro_documento | VARCHAR(20) UNIQUE | obligatorio |
| fecha_nacimiento | DATE | obligatorio |
| sexo | VARCHAR(10) | `F`,`M`,`X` |
| telefono_whatsapp 🔒 | | obligatorio (contacto, CU-01) formato E.164 |
| email 🔒 | | opcional |
| domicilio 🔒 | | opcional |
| objetivo | NVARCHAR(300) | p. ej. "descenso de peso" |
| antecedentes_salud 🔒 | | opcional (no es diagnóstico) |
| observaciones 🔒 | | |
| estado | VARCHAR(10) | `ACTIVO`, `BAJA` |
| fecha_baja, motivo_baja | | |
| auditoría + version | | |

**profesional**
| id | usuario_id FK UNIQUE | nombre | apellido | tipo_documento | nro_documento UNIQUE | matricula NVARCHAR(30) NULL | tipo (`NUTRICIONISTA`,`ENTRENADOR`,`INTEGRAL`) | especialidad NVARCHAR(100) | telefono_whatsapp 🔒 | email 🔒 | duracion_turno_min INT DEFAULT 30 | estado | fecha_baja | auditoría + version |

**administrador**: no requiere tabla propia; es un `usuario` con rol `ADMIN` y nombre visible en `configuracion_sistema` o tabla `administrador(id, usuario_id, nombre, apellido, telefono🔒)`. Se crea uno inicial por migración (password temporal desde variable de entorno).

**paciente_profesional** (CU-29)
| id | paciente_id FK | profesional_id FK | area (`NUTRICION`,`ENTRENAMIENTO`) | fecha_asignacion | activo BIT |
UNIQUE (`paciente_id`,`profesional_id`,`area`) WHERE activo = 1.

## 3. Agenda (spec 03)

**disponibilidad** (`id`, `profesional_id`, `dia_semana` TINYINT 1-7, `hora_desde` TIME, `hora_hasta` TIME, `ubicacion` NVARCHAR(120), `activo`). CHECK `hora_desde < hora_hasta`.
**bloqueo_agenda** (`id`, `profesional_id`, `desde` DATETIME2, `hasta` DATETIME2, `motivo`).

**turno**
| Columna | Tipo | Notas |
|---|---|---|
| id | BIGINT | |
| paciente_id, profesional_id | FK | |
| area | VARCHAR(15) | `NUTRICION`/`ENTRENAMIENTO` |
| inicio, fin | DATETIME2 | UTC |
| ubicacion | NVARCHAR(120) | copiada de la disponibilidad |
| estado | VARCHAR(12) | `RESERVADO`,`CANCELADO`,`ATENDIDO`,`AUSENTE` |
| motivo_cancelacion | NVARCHAR(200) | |
| cancelado_por_usuario_id | FK NULL | |
| recordatorio_enviado_at | DATETIME2 NULL | idempotencia D-11 |
| creado_por_usuario_id | FK | |
| auditoría + version | | |
Índice único filtrado: (`profesional_id`, `inicio`) WHERE estado = 'RESERVADO'. Índice (`paciente_id`, `inicio`).

**turno_historial** (`id`, `turno_id`, `inicio_anterior`, `fin_anterior`, `cambiado_por`, `cambiado_at`).

## 4. Catálogo (spec 10)

**grupo_alimento** (`id`, `nombre` UNIQUE, `macro_referencia` (`HC`,`PROTEINAS`,`GRASAS`,`KCAL`), `descripcion`). Semilla: Lácteos (PROTEINAS), Carnes y huevos (PROTEINAS), Cereales, legumbres y derivados (HC), Frutas (HC), Verduras (HC), Aceites y grasas (GRASAS), Frutos secos y semillas (GRASAS), Azúcares y dulces (HC), Bebidas e infusiones (KCAL).

**alimento**
| id | nombre NVARCHAR(120) UNIQUE | grupo_alimento_id FK | kcal_100g DECIMAL(7,2) | hc_100g | proteinas_100g | grasas_100g | fibra_100g | sodio_mg_100g NULL | unidad_casera NVARCHAR(60) NULL (p. ej. "1 unidad mediana") | gramos_unidad_casera DECIMAL NULL | apto_desayuno, apto_almuerzo… (BIT flags por tipo de comida, para generación) | activo | auditoría |
CHECK: valores ≥ 0; `hc+proteinas+grasas+fibra ≤ 100`; kcal coherente ±15 % con 4·HC + 4·P + 9·G (validación en servicio).

**equivalencia_manual** (`id`, `alimento_id`, `alimento_equivalente_id`, `gramos_origen`, `gramos_equivalente`, `nota`) — prioridad sobre el cálculo automático (D-15).

**grupo_muscular** (`id`, `nombre` UNIQUE). Semilla: Pecho, Espalda, Hombros, Bíceps, Tríceps, Antebrazos, Core/Abdominales, Glúteos, Cuádriceps, Isquiotibiales, Aductores, Pantorrillas, Cuerpo completo, Cardiovascular.

**ejercicio**
| id | nombre NVARCHAR(120) UNIQUE | descripcion NVARCHAR(500) | grupo_muscular_principal_id FK | dificultad (`BAJA`,`MEDIA`,`ALTA`) | equipamiento NVARCHAR(100) | video_url NVARCHAR(300) NULL (https) | activo | auditoría |
**ejercicio_grupo_secundario** (`ejercicio_id`, `grupo_muscular_id`).

## 5. Nutrición (specs 04, 08)

**plan_alimentario**
| id | paciente_id | nutricionista_id (profesional) | nombre | fecha_inicio DATE | duracion_semanas INT | fecha_fin DATE (calculada) | modo (`MANUAL`,`AUTOMATICO`) | kcal_objetivo INT NULL | pct_hc, pct_proteinas, pct_grasas DECIMAL(5,2) NULL | estado (`BORRADOR`,`ACTIVO`,`FINALIZADO`,`ELIMINADO`) | observaciones NVARCHAR(1000) | auditoría + version |
Índice único filtrado (`paciente_id`) WHERE estado = 'ACTIVO'.

**comida_plan** (`id`, `plan_id`, `dia_semana` TINYINT 1-7, `tipo_comida` (`DESAYUNO`,`COLACION_MANANA`,`ALMUERZO`,`MERIENDA`,`COLACION_TARDE`,`CENA`), `orden`, `descripcion` NVARCHAR(500) — texto libre como en el mockup, `kcal_estimadas`). UNIQUE (`plan_id`,`dia_semana`,`tipo_comida`).
**item_comida** (`id`, `comida_plan_id`, `alimento_id`, `gramos` DECIMAL(7,1), `cantidad_casera` NVARCHAR(60), `orden`).

**reemplazo_alimento** (CU-18) (`id`, `item_comida_id`, `alimento_reemplazo_id`, `gramos`, `paciente_id`, `fecha_desde`, `activo`). Un reemplazo activo por ítem.

**registro_comida** (CU-17, RF-15) (`id`, `paciente_id`, `comida_plan_id`, `fecha` DATE, `realizada` BIT, `no_le_gusto` BIT, `le_cayo_mal` BIT, `created_at`). UNIQUE (`paciente_id`,`comida_plan_id`,`fecha`).

**observacion_diaria** (CU-43) (`id`, `paciente_id`, `fecha`, `nivel_hambre` TINYINT 1-5, `nivel_saciedad` TINYINT 1-5, `estado_animo` TINYINT 1-5 NULL, `comentario` NVARCHAR(300)). UNIQUE (`paciente_id`,`fecha`).

## 6. Entrenamiento (specs 05, 09)

**rutina** (`id`, `paciente_id`, `entrenador_id`, `nombre`, `objetivo`, `fecha_inicio`, `duracion_semanas`, `fecha_fin`, `estado` (`ACTIVA`,`FINALIZADA`,`ELIMINADA`), `observaciones`, auditoría + version). Índice único filtrado (`paciente_id`) WHERE estado='ACTIVA'.
**dia_rutina** (`id`, `rutina_id`, `dia_semana` 1-7, `nombre` NVARCHAR(80), `orden`).
**ejercicio_rutina** (`id`, `dia_rutina_id`, `ejercicio_id`, `orden`, `series` TINYINT 1-20, `repeticiones` VARCHAR(20) (p. ej. "10", "8-12", "30 s"), `descanso_seg` INT NULL, `carga_sugerida` NVARCHAR(30) NULL, `notas` NVARCHAR(300)).
**reemplazo_ejercicio** (CU-26) (`id`, `ejercicio_rutina_id`, `ejercicio_reemplazo_id`, `paciente_id`, `fecha`, `activo`).
**registro_entrenamiento** (CU-25, RF-15) (`id`, `paciente_id`, `ejercicio_rutina_id`, `fecha`, `completado` BIT, `series_realizadas`, `repeticiones_realizadas`, `carga_kg` DECIMAL(6,2) NULL, `created_at`). UNIQUE (`paciente_id`,`ejercicio_rutina_id`,`fecha`).

## 7. Interacción paciente (specs 08, 09)

**comentario** (CU-40, CU-41) (`id`, `paciente_id`, `tipo` (`PLAN`,`RUTINA`), `plan_id` NULL, `comida_plan_id` NULL, `rutina_id` NULL, `ejercicio_rutina_id` NULL, `fecha`, `texto` NVARCHAR(300), `leido_at` NULL, `leido_por` NULL).
**molestia** (CU-42) (`id`, `paciente_id`, `fecha`, `descripcion` NVARCHAR(300), `intensidad` TINYINT 1-10, `ubicacion_corporal` NVARCHAR(60), `ejercicio_rutina_id` NULL, `contexto` (`ENTRENAMIENTO`,`ALIMENTACION`,`GENERAL`), `revisada_at` NULL, `revisada_por` NULL).

## 8. Evolución (spec 06)

**medicion_antropometrica**
| id | paciente_id | profesional_id | fecha | peso_kg DECIMAL(5,2) | talla_cm DECIMAL(5,1) | imc (calc.) | pct_grasa DECIMAL(4,1) NULL | pct_musculo DECIMAL(4,1) NULL | circ_cintura_cm, circ_cadera_cm, circ_brazo_cm, circ_muslo_cm, circ_pecho_cm, circ_pantorrilla_cm NULL | observaciones 🔒 | auditoría |
**evaluacion_fisica** (`id`, `paciente_id`, `profesional_id`, `fecha`, `prueba` NVARCHAR(80), `ejercicio_id` NULL, `valor` DECIMAL(8,2), `unidad` VARCHAR(15), `observaciones`).

## 9. Pagos (spec 07)

**arancel** (`id`, `concepto` NVARCHAR(100), `monto`, `vigente_desde` DATE, `activo`, auditoría).
**cargo** (deuda) (`id`, `paciente_id`, `concepto`, `periodo` CHAR(7) NULL (`AAAA-MM`), `monto`, `fecha` DATE, `vencimiento` DATE, `arancel_id` NULL, `anulado` BIT, `motivo_anulacion`, auditoría + version).
UNIQUE (`paciente_id`,`concepto`,`periodo`,`monto`) WHERE anulado = 0 (D-19).
**pago** (`id`, `paciente_id`, `fecha` DATE, `monto` > 0, `medio` (`EFECTIVO`,`TRANSFERENCIA`,`DEBITO`,`CREDITO`,`MERCADO_PAGO`,`OTRO`), `referencia` NVARCHAR(60), `observaciones`, `nro_comprobante` (secuencial), `anulado`, `motivo_anulacion`, auditoría + version).
Vista `v_saldo_paciente` (`paciente_id`, `total_cargos`, `total_pagos`, `saldo`, `fecha_deuda_mas_antigua`).

## 10. Notificaciones (spec 11)

**notificacion** (outbox)
| id | tipo (`CREDENCIALES`,`TURNO_CONFIRMADO`,`TURNO_REPROGRAMADO`,`TURNO_CANCELADO`,`RECORDATORIO_TURNO`,`RECORDATORIO_DEUDA`,`CAMBIO_ARANCEL`,`RECUPERO_PASSWORD`) | usuario_id | telefono 🔒 | plantilla VARCHAR(60) | parametros NVARCHAR(MAX) 🔒 (JSON) | referencia_tipo, referencia_id | estado (`PENDIENTE`,`ENVIADA`,`FALLIDA`,`OMITIDA`) | intentos | proximo_intento_at | error NVARCHAR(500) | wa_message_id | created_at | enviada_at |
**alerta** (`id`, `destinatario_usuario_id` NULL (NULL = todos los admin), `tipo` (`NOTIFICACION_FALLIDA`,`MOLESTIA`,`COMENTARIO`,`BACKUP_FALLIDO`,`ERROR_SISTEMA`), `mensaje`, `referencia_tipo`, `referencia_id`, `created_at`, `leida_at`).

## 11. Bitácora (spec 14)

**bitacora_evento** — append-only (RF-17)
| id BIGINT | fecha_hora | usuario_id NULL (NULL = SISTEMA) | username | roles | modulo (`PACIENTES`,`PROFESIONALES`,`TURNOS`,`NUTRICION`,`ENTRENAMIENTO`,`EVOLUCION`,`PAGOS`,`CATALOGO`,`NOTIFICACIONES`,`REPORTES`,`SEGURIDAD`,`BACKUP`,`CONFIGURACION`) | tipo_evento (`ALTA`,`BAJA`,`MODIFICACION`,`CONSULTA_SENSIBLE`,`REPORTE`,`DESCARGA`,`CAMBIO_PASSWORD`,`CONFIGURACION`,`NOTIFICACION`,`BACKUP`,`ERROR`) | entidad VARCHAR(40) | entidad_id | descripcion NVARCHAR(500) | resultado (`OK`,`ERROR`) | ip | detalle NVARCHAR(MAX) NULL (JSON con campos cambiados, sin datos sensibles) |
Índices: (`fecha_hora`), (`usuario_id`,`fecha_hora`), (`modulo`,`tipo_evento`,`fecha_hora`).
Protección: `DENY UPDATE, DELETE ON bitacora_evento TO mgnutri_app;` + trigger `INSTEAD OF UPDATE, DELETE` que hace `THROW`. Ídem `registro_acceso`.

## 12. Operación (spec 15, configuración)

**backup_registro** (`id`, `tipo` (`MANUAL`,`AUTOMATICO`), `iniciado_at`, `finalizado_at`, `estado` (`EN_CURSO`,`OK`,`ERROR`), `archivo`, `tamano_bytes`, `ubicacion`, `solicitado_por` NULL, `error`).
**configuracion_sistema** (`clave` VARCHAR(80) PK, `valor` NVARCHAR(500), `descripcion`, `updated_at`, `updated_by`). Claves: `centro.nombre`, `centro.telefono_whatsapp`, `centro.direccion`, `centro.horario`, `turnos.recordatorio_hora`, `pagos.recordatorio_cron`, `pagos.link`, `backup.programacion_cron`, `backup.habilitado`, `backup.retencion_dias`.

## 13. Plan de migraciones Flyway

| Versión | Contenido | Sprint |
|---|---|---|
| V1 | usuario, usuario_rol, token_revocado, configuracion_sistema, terminos_condiciones, admin inicial | 1 |
| V2 | bitacora_evento, registro_acceso, triggers y DENY | 1 |
| V3 | paciente, profesional, administrador, paciente_profesional | 1 |
| V4 | disponibilidad, bloqueo_agenda, turno, turno_historial | 1 |
| V5 | notificacion, alerta | 1 |
| V6 | grupo_alimento, alimento, equivalencia_manual, grupo_muscular, ejercicio, ejercicio_grupo_secundario (estructura) | 2 |
| V7 | plan_alimentario, comida_plan, item_comida | 2 |
| V8 | rutina, dia_rutina, ejercicio_rutina | 2 |
| V9 | medicion_antropometrica, evaluacion_fisica | 2 |
| V10 | arancel, cargo, pago, v_saldo_paciente | 2 |
| V11 | reemplazo_alimento, registro_comida, observacion_diaria, reemplazo_ejercicio, registro_entrenamiento, comentario, molestia | 3 |
| V12 | Semilla de catálogo (≥ 150 alimentos, ≥ 80 ejercicios) | 3 |
| V13 | token_recupero | 4 |
| V14 | backup_registro | 5 |

Nota: el catálogo (V6) se adelanta a Sprint 2 en estructura porque planes y rutinas lo referencian; la carga de datos y el ABM completo quedan en Sprint 3 como dice el backlog.
