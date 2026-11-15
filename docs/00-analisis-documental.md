# 00 · Análisis documental

Fuente de verdad del proyecto **MG Nutrición y Entrenamiento** (SIP 2025 – USAL, Lucas Benitez). Este documento resume qué dice cada entregable, qué jerarquía tienen entre sí y qué inconsistencias se detectaron. Las decisiones que resuelven esas inconsistencias están en [`01-decisiones.md`](01-decisiones.md).

## 1. Documentos analizados

| ID | Documento | Fecha | Páginas | Rol en este paquete de specs |
|---|---|---|---|---|
| PdS | Propósito del Sistema (P01) | 09/04/2025 | 3 | Visión, módulos, roles, exclusiones |
| ERS | Especificación de Requerimientos del Software v1.0.0 (P05) | 06/05/2025 | 54 | **Línea base funcional**: RF-01…RF-15, RNF, CU-01…CU-54, interfaces, aspectos legales |
| SDP | Plan de Desarrollo del Software – IEEE 1058 (P05) | 03/06/2025, rev. 03/08/2025 | 30 | Proceso, stack técnico, 18 paquetes de trabajo (0–17), dependencias, calendario; **reclasifica RNF-1/RNF-2 → RF-16/RF-17, agrega RF-18 y divide CU-53/CU-54** |
| SP | Planificación y Desarrollo – Sprint 1 v1.0.0 (P05) | 20/08/2025 | 4 | Backlog de 42 tareas con horas, 6 sprints, burndown (124 h) |

**Jerarquía aplicada** (de mayor a menor precedencia ante conflicto):
1. SDP rev. 1 — para clasificación de requerimientos (RF-16/17/18, CU-53A/B/C, CU-54A/B/C) y stack técnico.
2. ERS — para el contenido funcional de cada RF y CU.
3. Sprint Planning — para la asignación de tareas a sprints (es posterior al calendario del SDP).
4. PdS — visión; lo que aparece sólo acá y no fue bajado al ERS queda **fuera de alcance** (ver §5).

## 2. Resumen de cada documento

### 2.1 PdS
- Sistema integral para un centro de nutrición y entrenamiento: administración, comunicación profesional↔paciente, experiencia del usuario.
- Tres módulos: **Nutrición** (planes, BD de alimentos, generación automática, alternativas, cumplimiento, recetario, hidratación, listas de compra), **Entrenamiento** (biblioteca multimedia de ejercicios, planes, progreso, comunicación), **Gestión** (pacientes, staff, agenda, pagos, notificaciones).
- Roles: Nutricionista, Entrenador, Administrador, Cliente/Paciente.
- **No hace**: diagnósticos médicos, asesoramiento automatizado que reemplace al profesional, contabilidad avanzada, inventario.

### 2.2 ERS
- **15 RF** (RF-01 a RF-15) con prioridades Alta / Media alta / Media.
- **RNF**: RNF-1 (registro de accesos), RNF-2 (bitácora), Usabilidad (aprendizaje ≤ 30 min, responsive), Confiabilidad (99 % disponibilidad, backup diario, RTO ≤ 2 h), Seguridad (usuario+contraseña, roles, cifrado en BD), Eficiencia (90 % de operaciones < 5 s en servidor, 50 concurrentes, 500 pacientes activos), Mantenibilidad (modular, versionado, datos estables), Soportabilidad (web desacoplada en Azure), Restricciones de diseño (Java, capas controller/service/DAO, MVC, REST, HTML/CSS/JS, SQL Server en Azure SQL, Hibernate, Maven, GitHub), Ayuda en línea (manual PDF por rol, ayudas contextuales, guía técnica de despliegue), Legales (Ley 25.326, aceptación de T&C), Estándares (ISO/IEC 25010, ISO/IEC 27001, HTML5/CSS3/HTTPS).
- **Actores**: Administrador, Profesional nutricionista, Profesional entrenador, **Profesional integral** (ambos roles), Paciente, Sistema.
- **54 CU** especificados con flujo normal/alterno, pre/poscondiciones y requerimientos especiales.
- **Interfaces de usuario** (mockups): login móvil (Usuario / Contraseña / Ingresar / Contacto), home paciente (Nutrición / Entrenamiento / Contacto), home nutricionista (Calendario / Pacientes), home administrador (Calendario / Pacientes / Profesionales / Pagos / Opciones avanzadas), vista de plan alimentario como **grilla semanal** (filas = Desayuno, Colación, Almuerzo, Merienda, Cena; columnas = Lunes…Domingo). Paleta verde oscuro sobre fondo crema.

### 2.3 SDP (IEEE 1058)
- Modelo incremental iterativo, híbrido Scrum + Proceso Unificado.
- Stack: Java SE 17, SQL Server (Azure), Azure App Service (backend) + Azure Static Web Apps (frontend), HTML/CSS/JS desacoplado, Maven, Hibernate con patrón DAO, MVC + REST, Singleton para acceso a BD, validación cliente y servidor, bitácora automática, Postman, GitHub, WhatsApp Cloud API.
- 18 paquetes de trabajo (0 Entorno … 17 Despliegue) con CU asociados y matriz de dependencias.
- Prioridades: 1) cumplimiento funcional, 2) calidad (usabilidad, seguridad), 3) calendario, 4) documentación, 5) optimización de recursos.
- Cambios a la ERS (v1.0.1, 01/06/2025): RNF-1 → **RF-16**, RNF-2 → **RF-17**, nuevo **RF-18** (acceso y autenticación); CU-53 → **CU-53A/B/C**, CU-54 → **CU-54A/B/C**.
- Entregables: PdS, ERS, SDP, Top-10 riesgos, Prototipo (oct-2025), Manual de usuario y del sistema, Informe final.
- Soporte: backup manual y automático (CU-51/52), manual por rol, FAQ, escalado en Azure.

### 2.4 Sprint Planning
| Sprint | Fin | Paquetes | Horas |
|---|---|---|---|
| 1 | 20/08/2025 | 0, 1, 2, 3, 14 | 32 |
| 2 | 04/09/2025 | 4, 5, 6, 7 | 24 |
| 3 | 18/09/2025 | 8, 9, 10 | 20 |
| 4 | 02/10/2025 | 11, 12, 13 | 20 |
| 5 | 16/10/2025 | 15 (y 16 según tabla) | 8 |
| 6 | 30/10/2025 | 17 (y 16 según backlog) | 20 |

Backlog relevante: "Estructura del proyecto e inicialización con **Spring** y Maven", "Bitácora/log del sistema" en Sprint 1, "Capa de servicios" en S1 y S2, WhatsApp (credenciales, recordatorio de turno, recordatorio de deuda) en S4, "Registro y seguridad de accesos" en S4.

## 3. Catálogo consolidado de requerimientos

| RF | Nombre | Prioridad | Paquete SDP | CU |
|---|---|---|---|---|
| RF-01 | Gestionar pacientes | Alta | 1 | CU-01, 02, 03, 29 |
| RF-02 | Gestionar profesionales | Alta | 2 | CU-04, 05, 06 |
| RF-03 | Gestionar turnos | Alta | 3 | CU-07, 08, 09, 10 |
| RF-04 | Gestión de planes alimentarios | Alta | 4 | CU-11, 12, 13, 14, 16 |
| RF-05 | Gestión de rutinas de entrenamiento | Alta | 5 | CU-21, 22, 23, 27 |
| RF-06 | Evolución del paciente | Media alta | 6 | CU-19, 20 |
| RF-07 | Recordatorios automáticos | Media | 11 | CU-38, 39 |
| RF-08 | Gestión de pagos | Media | 7 | CU-44, 45, 46 |
| RF-09 | Consultar plan alimentario | Media alta | 8 | CU-15, 17, 18 |
| RF-10 | Consultar rutina de entrenamiento | Media alta | 9 | CU-24, 25, 26, 28 |
| RF-11 | BD de alimentos y ejercicios | Media alta | 10 | CU-53A/B/C, CU-54A/B/C |
| RF-12 | Contacto directo con el profesional | Media alta | 8/9 (UI paciente) | — (sin CU; ver D-24) |
| RF-13 | Notificaciones automáticas por WhatsApp | Media alta | 11 | CU-37, 38, 39 |
| RF-14 | Generación y consulta de reportes | Media alta | 12 | CU-30…34 |
| RF-15 | Registro de actividad del paciente | Media | 8, 9 | CU-17, 25, 40, 41, 42, 43 |
| RF-16 | Registro de accesos (ex RNF-1) | Alta | 13 | CU-47, 50 (registro) |
| RF-17 | Bitácora de eventos (ex RNF-2) | Alta | 14 | CU-35, 36 |
| RF-18 | Gestión de acceso y autenticación | Alta (asumida) | 13 | CU-47, 48, 49, 50 |
| RNF | Backup | — | 15 | CU-51, 52 |

La trazabilidad completa RF ↔ CU ↔ paquete ↔ sprint ↔ spec está en [`05-trazabilidad.md`](05-trazabilidad.md).

## 4. Inconsistencias detectadas

### 4.1 Referencias cruzadas erróneas en la ERS
| # | Dónde | Dice | Debería | Tratamiento |
|---|---|---|---|---|
| I-01 | CU-01, 02, 03, 05, 44, 45 (Puntos de extensión) | "CU-34: Registrar evento en bitácora" | CU-35 | Se usa CU-35 |
| I-02 | CU-07, CU-08 | "CU-36: Enviar notificación de turno" | CU-38 (CU-36 es Consultar bitácora) | Se usa CU-38 |
| I-03 | CU-18 | RF-10 | RF-09 (es alimentario) | RF-09 |
| I-04 | CU-24, 25, 27 | RF-11 | RF-10 (CU-27 también RF-05) | RF-10 / RF-05 |
| I-05 | CU-26 | RF-12 | RF-10 | RF-10 |
| I-06 | CU-35, 36 | RF-08 | RF-17 | RF-17 |
| I-07 | CU-44, 45, 46 | RF-07 | RF-08 | RF-08 |
| I-08 | CU-53 | RF-10 | RF-11 | RF-11 |
| I-09 | CU-40, CU-41 | RF-11 / RF-09 | RF-15 | RF-15 |
| I-10 | CU-31 | Campos "Requerimiento" y "Precondición" invertidos | Req: RF-14; Pre: admin autenticado | Corregido |
| I-11 | CU-49 | Nombre/Descripción/Requerimiento desplazados | Nombre: Cambiar contraseña; Req: RF-18 | Corregido |
| I-12 | CU-47…52 | Requerimiento vacío | RF-18 (47–50), RNF-Backup (51–52) | Asignado |

### 4.2 Contradicciones de contenido
| # | Tema | Conflicto | Decisión |
|---|---|---|---|
| I-13 | RF-05 | Dice que los entrenadores crean "planes de alimentación" | Error de redacción: son **rutinas de entrenamiento** (D-01) |
| I-14 | Recordatorio de turno | RF-07: proceso diario sobre turnos "del día siguiente" / "próximas 48 h"; RF-13: 48 h antes; CU-38: 24 h antes | Job diario que notifica los turnos del día siguiente (D-11) |
| I-15 | Recordatorio de deuda | RF-07: "cada fin de semana"; RF-13: "semanalmente" | Job semanal, sábados (D-12) |
| I-16 | CU-10 | Filtro "día/semana/año" vs. requerimiento especial "día, semana o mes" | Día / semana / mes (D-13) |
| I-17 | Agenda del entrenador | ERS 3.9.1: entrenador sólo asigna rutinas (sin calendario); PdS/SDP: "agendas de nutrición y entrenamiento" | Todo profesional puede tener agenda; el calendario aparece si tiene disponibilidad cargada (D-08) |
| I-18 | RF-03 "conceder" turnos | Sugiere aprobación; CU-07 confirma automáticamente | Confirmación automática; el profesional puede cancelar o reprogramar (D-09) |
| I-19 | CU-02 incluye CU-37 | Modificar paciente "incluye" enviar notificación de alta | Sólo se reenvían credenciales si el admin lo pide explícitamente (D-05) |
| I-20 | CU-19/20 | ERS: CU-20 = visualizar evolución; SDP paquete 6: CU-19 = evolución física, CU-20 = evolución nutricional; SP: "desempeño físico" + "composición corporal" | Dos tipos de registro (antropometría y evaluación de desempeño) y una vista de evolución (D-16) |
| I-21 | CU-44/45/46 vs SDP paquete 7 | ERS: registrar pago / registrar deuda / historial; SDP: registrar / modificar / eliminar pago | Se cubren los 5 comportamientos (D-19) |
| I-22 | Reportes | RF-14: PDF; CU-30/31/34/46: PDF **o Excel** | PDF y Excel (D-21) |
| I-23 | Sprint 1 | SDP: 14/08, 28 h, paquetes 0-3; SP: 20/08, 32 h, paquetes 0-3 + 14 | Prevalece SP |
| I-24 | Paquete 16 (Pruebas) | Tabla de sprints lo pone en S5; backlog lo pone en S6 | S6 (coincide con horas) |
| I-25 | Notificaciones y seguridad | Paquetes 1–3 incluyen CU-37/38 (S1) pero el backlog ubica WhatsApp y Seguridad en S4 | Puerto de notificaciones y seguridad mínima desde S1; adaptador WhatsApp y endurecimiento en S4 (D-27) |
| I-26 | Paquete 2 | "2.1.3 CU-06: Baja de paciente" | Baja de **profesional** |
| I-27 | Horas totales | SDP §3.5 y §5.6: 124 h; SDP §5.3: 120 h | 124 h |
| I-28 | CU-14 | Precondición "se inicia mediante CU-11" pero en CU-11 aparece como extensión | Se modela como extensión opcional de CU-11 |

### 4.3 Vacíos de especificación (necesarios para implementar)
| # | Falta | Resolución |
|---|---|---|
| V-01 | Contenido de RF-18 (sólo nombrado en el SDP) | Definido en spec 13 |
| V-02 | Modelo de agenda: disponibilidad horaria, duración de turno | Disponibilidad semanal + duración por profesional (D-10) |
| V-03 | Estados del turno (necesarios para reportes de cancelaciones y ausencias) | RESERVADO, CANCELADO, ATENDIDO, AUSENTE (D-09) |
| V-04 | Algoritmo de generación automática de plan | Heurística por distribución calórica y grupos de alimentos, siempre como borrador editable (D-14) |
| V-05 | Cálculo de equivalencias alimentarias | Por grupo de intercambio y macronutriente de referencia (D-15) |
| V-06 | Criterio de alternativas de ejercicio | Mismo grupo muscular principal (D-18) |
| V-07 | Modelo de cuenta corriente (deuda, saldo, "fecha del saldo") | Cargos y pagos, saldo FIFO (D-19) |
| V-08 | Formato de usuario para login | Número de documento (D-04) |
| V-09 | Canal de recupero de contraseña sin infraestructura de email | WhatsApp; email opcional (D-26) |
| V-10 | Mecanismo de backup en Azure SQL | Exportación BACPAC a Blob Storage + backups automáticos nativos (D-29) |
| V-11 | Rangos válidos de antropometría | Tabla de rangos en spec 06 |
| V-12 | Términos y condiciones (Ley 25.326) sin CU | Aceptación obligatoria en el primer ingreso (D-25) |
| V-13 | "Cambios de aranceles" (RF-13) sin entidad | Catálogo de aranceles simple con notificación (D-20) |

## 5. Fuera de alcance

Explícitamente excluido por el PdS: diagnóstico médico, asesoramiento automatizado que reemplace al profesional, contabilidad avanzada, inventario.

Mencionado sólo en el PdS y **no bajado a la ERS** (queda como backlog futuro, no se especifica):
- Listas de compra generadas automáticamente.
- Recetario digital.
- Seguimiento de hidratación.
- Mensajería interna profesional↔paciente (se cubre con RF-12: link directo a WhatsApp y comentarios).
- Pasarela de pago (el "enlace de pago" de RF-07/RF-13 es un campo opcional configurable, no una integración).
