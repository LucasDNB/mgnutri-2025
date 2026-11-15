# Spec 02 · Gestión de profesionales

| Campo | Valor |
|---|---|
| Paquete SDP | 2 – Gestión de profesionales |
| RF | RF-02 · RF-13 (credenciales) · RF-17 |
| CU | CU-04 Alta · CU-05 Modificar · CU-06 Baja · CU-35, CU-37 (incluidos) |
| Sprint | 1 · Backlog: BackEnd (2 h), FrontEnd (3 h), Testeos (1 h) |
| Depende de | 00, 13 (esqueleto), 14 |
| Decisiones | D-03, D-04, D-06, D-08, D-10 |

## Contexto
El administrador gestiona nutricionistas, entrenadores y profesionales integrales. El tipo de profesional define su rol en el sistema y, por ende, qué puede hacer (ERS 3.9.1). Cada profesional puede tener agenda (disponibilidad horaria) para recibir turnos.

## Historias de usuario

### US-02.1 Alta de profesional (CU-04)
Como **administrador** quiero registrar un profesional y asignarle su rol.
- **CA-02.1.1** *Profesionales › Nuevo profesional* muestra formulario vacío: nombre, apellido, tipo y nro. de documento, **tipo de profesional** (Nutricionista / Entrenador / Integral), matrícula (obligatoria para Nutricionista e Integral), especialidad, teléfono WhatsApp, email, duración de turno (min, default 30).
- **CA-02.1.2** Con datos válidos, se crea `profesional` + `usuario` con roles según tipo (NUTRICIONISTA, ENTRENADOR o ambos), se confirma la operación, se encola `CREDENCIALES` (CU-37) y se registra `PROFESIONALES/ALTA`.
- **CA-02.1.3** Campos vacíos o documento existente → alerta del error, no guarda (alterno 1.1).
- **CA-02.1.4** Falla de WhatsApp → mismo tratamiento que CA-01.1.6.

### US-02.2 Modificar profesional (CU-05)
- **CA-02.2.1** Búsqueda por apellido/nombre/documento/tipo; ficha con datos actuales.
- **CA-02.2.2** Edición válida → guarda y confirma; bitácora `MODIFICACION`.
- **CA-02.2.3** Datos inválidos → no guarda, muestra aviso (alterno 1.1).
- **CA-02.2.4** Cambio de tipo: los roles del usuario se recalculan. Si pasa de Integral a Entrenador y tiene planes alimentarios activos, el sistema advierte que deberán reasignarse (no los borra) y las asignaciones de área NUTRICION se desactivan.
- **CA-02.2.5** Acción *Reenviar credenciales* (igual a CA-01.2.6).

### US-02.3 Baja de profesional (CU-06)
- **CA-02.3.1** Al elegir eliminar, aviso: "El profesional ya no podrá acceder. Sus turnos futuros se cancelarán y sus pacientes quedarán sin profesional asignado en el área." + confirmación.
- **CA-02.3.2** Al confirmar: profesional y usuario `BAJA`, credenciales eliminadas, tokens revocados, turnos futuros `CANCELADO` con notificación `TURNO_CANCELADO` a cada paciente, asignaciones inactivas, disponibilidad desactivada. Los planes/rutinas activos que creó **siguen activos** para el paciente hasta su fin (el paciente no pierde su plan) y el admin ve una alerta "Pacientes con plan/rutina de un profesional dado de baja" para reasignar.
- **CA-02.3.3** Cancelar → sin cambios (alterno 1.1). Bitácora `BAJA`.

### US-02.4 Disponibilidad y bloqueos (D-08, D-10)
Como **administrador o profesional** quiero cargar la agenda semanal para que los pacientes puedan pedir turnos.
- **CA-02.4.1** Se cargan franjas por día de la semana (hora desde/hasta, ubicación). Puede haber varias franjas por día; no pueden superponerse.
- **CA-02.4.2** Se cargan bloqueos (fecha-hora desde/hasta, motivo) para vacaciones, feriados o ausencias.
- **CA-02.4.3** Un bloqueo que pisa turnos `RESERVADO` advierte cuántos turnos afecta y, al confirmar, los cancela notificando a los pacientes.
- **CA-02.4.4** El profesional sólo edita su propia agenda; el admin la de todos. Cambios auditados (`PROFESIONALES/MODIFICACION`, entidad `Disponibilidad`).
- **CA-02.4.5** Un profesional sin franjas activas no aparece como opción al solicitar turno y no ve el botón *Calendario* (D-08).

### US-02.5 Home del profesional
- **CA-02.5.1** Al ingresar, el profesional ve "Bienvenido/a {Lic.} {Apellido}" y los botones *Calendario* (si tiene agenda) y *Pacientes*, más *Novedades* con contador de comentarios/molestias no leídos (spec 08/09) y *Base de alimentos/ejercicios* según rol.

## Reglas de negocio
- RN-02.1 Documento único global (RN-01.1).
- RN-02.2 Matrícula: obligatoria para NUTRICIONISTA e INTEGRAL; alfanumérica ≤ 30.
- RN-02.3 Duración de turno: 15–120 min, múltiplo de 5.
- RN-02.4 Franjas: `hora_desde < hora_hasta`; duración de la franja ≥ duración de turno; sin superposición en el mismo día.
- RN-02.5 Roles por tipo: NUTRICIONISTA → {NUTRICIONISTA}; ENTRENADOR → {ENTRENADOR}; INTEGRAL → {NUTRICIONISTA, ENTRENADOR}.

## Bitácora
`PROFESIONALES` · ALTA / MODIFICACION / BAJA sobre `Profesional`, `Disponibilidad`, `BloqueoAgenda`; `SEGURIDAD/CAMBIO_PASSWORD` en reenvío.
