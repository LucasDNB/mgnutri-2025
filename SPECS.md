# MG Nutrición y Entrenamiento · Paquete de specs

Especificaciones para desarrollar de forma integral el sistema de gestión del centro **MG Nutrición y Entrenamiento** (Seminario de Integración Profesional 2025 – USAL, Lucas Benitez), derivadas de:

- P01 · Propósito del Sistema (PdS)
- P05 · Especificación de Requerimientos del Software (ERS v1.0.0)
- P05 · Plan de Desarrollo del Software IEEE 1058 (SDP rev. 1)
- P05 · Planificación y Desarrollo – Sprint Planning v1.0.0

Formato compatible con **GitHub spec-kit** (Spec-Driven Development): constitución en `.specify/memory/`, un directorio por feature en `specs/` con `spec.md` (qué y por qué) y `plan.md` (cómo).

## Contenido

```
.specify/memory/constitution.md   Principios no negociables (stack, capas, seguridad, auditoría, calidad)
docs/
  00-analisis-documental.md       Resumen de los 4 documentos, catálogo de RF, 28 inconsistencias, 13 vacíos, fuera de alcance
  01-decisiones.md                35 decisiones (D-xx) que resuelven inconsistencias y vacíos; [Validar] = confirmar con el tutor
  02-arquitectura.md              Despliegue Azure, estructura de repo, capas, seguridad, frontend, matriz de permisos, jobs, configuración
  03-modelo-de-datos.md           Todas las tablas, columnas, restricciones y plan de migraciones Flyway por sprint
  04-convenciones-api.md          REST, errores RFC 7807, paginación, códigos de negocio, Postman
  05-trazabilidad.md              CU → RF → paquete → sprint → spec/historia; cobertura de RF y RNF
  06-roadmap.md                   Orden de implementación, backlog ↔ tareas, hitos por sprint, riesgos
specs/
  00-entorno/                     Paquete 0  · Sprint 1
  01-pacientes/                   Paquete 1  · RF-01 · CU-01/02/03/29 · Sprint 1
  02-profesionales/               Paquete 2  · RF-02 · CU-04/05/06 + agenda · Sprint 1
  03-turnos/                      Paquete 3  · RF-03 · CU-07/08/09/10 · Sprint 1
  04-planes-alimentarios/         Paquete 4  · RF-04 · CU-11/12/13/14/16 · Sprint 2
  05-rutinas/                     Paquete 5  · RF-05 · CU-21/22/23/27 · Sprint 2
  06-evolucion/                   Paquete 6  · RF-06 · CU-19/20 · Sprint 2
  07-pagos/                       Paquete 7  · RF-08 · CU-44/45/46 · Sprint 2
  08-consulta-plan/               Paquete 8  · RF-09/12/15 · CU-15/17/18/41/43 · Sprint 3
  09-consulta-rutina/             Paquete 9  · RF-10/15 · CU-24/25/26/28/40/42 · Sprint 3
  10-catalogo/                    Paquete 10 · RF-11 · CU-53A-C/54A-C · Sprint 3 (estructura en S2)
  11-notificaciones/              Paquete 11 · RF-07/13 · CU-37/38/39 · Sprint 4 (outbox en S1)
  12-reportes/                    Paquete 12 · RF-14 · CU-30…34 · Sprint 4
  13-seguridad-accesos/           Paquete 13 · RF-16/18 · CU-47…50 · Sprint 4 (esqueleto en S1)
  14-bitacora/                    Paquete 14 · RF-17 · CU-35/36 · Sprint 1
  15-backup/                      Paquete 15 · CU-51/52 · Sprint 5
  16-pruebas/                     Paquete 16 · estrategia de pruebas y revisión de cumplimiento · Sprint 6
  17-despliegue/                  Paquete 17 · despliegue y documentación final · Sprint 6
```

Cada `spec.md` trae: metadatos (paquete, RF, CU, sprint, dependencias, decisiones), historias de usuario con criterios de aceptación numerados (CA-NN.x.y) que cubren flujo normal, flujos alternos y requerimientos especiales de cada CU, reglas de negocio y eventos de bitácora. Cada `plan.md` trae: tablas, endpoints, componentes backend y frontend, pruebas por CA y tareas vinculadas al backlog.

## Cómo usarlo con spec-kit + Claude Code

1. Inicializar el repo: `specify init mgnutri --ai claude` y copiar este paquete encima (`.specify/memory/constitution.md`, `docs/`, `specs/`).
2. Antes de empezar, revisar las decisiones marcadas **[Validar]** en `docs/01-decisiones.md`.
3. Para cada spec, en el orden de `docs/06-roadmap.md`:
   - `/speckit.clarify` (opcional) sobre `specs/NN-*/spec.md`.
   - `/speckit.plan` usando el `plan.md` existente como base (ya respeta la constitución).
   - `/speckit.tasks` para desglosar en `tasks.md`.
   - `/speckit.implement`.
4. Al cerrar cada sprint, completar la columna *Estado / Evidencia* de `docs/05-trazabilidad.md`.

## Configuración del sistema (transversal)
Parámetros editables por el admin en *Opciones avanzadas › Configuración* (`configuracion_sistema`): datos de la central (RF-12, spec 08), hora del recordatorio de turnos y cron de deuda (spec 11), enlace de pago (spec 07/11), programación de backups (spec 15), mensaje de mantenimiento (spec 17), versión de T&C (spec 13). Todo cambio se audita (`CONFIGURACION`).
