# Spec 01 · Gestión de pacientes

| Campo | Valor |
|---|---|
| Paquete SDP | 1 – Gestión de pacientes |
| RF | RF-01 (Alta) · RF-13 (credenciales) · RF-17 (bitácora) |
| CU | CU-01 Alta · CU-02 Modificación · CU-03 Baja · CU-29 Asignar paciente a profesional · CU-35 (incluido) · CU-37 (incluido) |
| Sprint | 1 · Backlog: Gestión de pacientes BackEnd (2 h), FrontEnd (3 h), Testeos de pacientes (1 h) |
| Depende de | 00, 13 (esqueleto de seguridad, D-27), 14 |
| Decisiones | D-03, D-04, D-05, D-06, D-07, D-30 |

## Contexto
El administrador registra a los pacientes del centro, mantiene sus datos y los vincula con los profesionales que los atenderán. El alta crea las credenciales del paciente y se las envía por WhatsApp. Sin paciente registrado y asignado no hay turnos, planes, rutinas ni evolución.

## Historias de usuario

### US-01.1 Alta de paciente (CU-01)
Como **administrador** quiero registrar un nuevo paciente con sus datos personales, de contacto e iniciales para que pueda usar el sistema.

- **CA-01.1.1** Dado un admin autenticado, cuando accede a *Pacientes › Nuevo paciente*, entonces ve el formulario vacío con ayudas contextuales.
- **CA-01.1.2** Dado el formulario completo y válido, cuando confirma, entonces el paciente queda `ACTIVO`, se crea su usuario con rol `PACIENTE` (username = nro. de documento, contraseña temporal) y se muestra "Paciente registrado correctamente".
- **CA-01.1.3** Dado un alta exitosa, entonces se encola una notificación `CREDENCIALES` al teléfono del paciente (CU-37) y se registra `PACIENTES/ALTA` en bitácora (CU-35).
- **CA-01.1.4** Dado que falta un campo obligatorio (nombre, apellido, tipo y nro. de documento, fecha de nacimiento, sexo, teléfono WhatsApp), cuando confirma, entonces el sistema no guarda e indica cada campo faltante (flujo alterno 1.1).
- **CA-01.1.5** Dado un nro. de documento ya registrado (paciente o profesional activo o dado de baja), cuando confirma, entonces muestra "Ya existe una persona registrada con ese documento" y no guarda (flujo alterno 2.1, código `DUPLICADO`).
- **CA-01.1.6** Dado que el envío de WhatsApp falla o el teléfono es inválido para WhatsApp, entonces el alta igual se completa, el admin ve un aviso con la contraseña temporal para entregarla por otro medio, y se genera una alerta (CU-37 alterno).
- **CA-01.1.7** El formulario permite asignar opcionalmente profesionales en el mismo paso (encadena CU-29).

### US-01.2 Modificación de paciente (CU-02)
Como **administrador** quiero buscar un paciente y modificar sus datos.

- **CA-01.2.1** Dado el listado de pacientes, cuando busca por apellido, nombre o documento (búsqueda parcial, sin distinguir mayúsculas ni acentos), entonces ve resultados paginados.
- **CA-01.2.2** Dada la ficha del paciente, cuando edita y confirma datos válidos, entonces se guardan y se muestra confirmación; la bitácora registra `MODIFICACION` con la lista de campos cambiados (sin valores sensibles).
- **CA-01.2.3** Dados datos inválidos, entonces se bloquea la operación y se muestra el error (alterno 1.1).
- **CA-01.2.4** Si cambia el nro. de documento, también cambia el username; si colisiona con otro, `DUPLICADO`.
- **CA-01.2.5** Si dos admins editan a la vez, el segundo recibe `CONCURRENCIA` y debe recargar.
- **CA-01.2.6** La acción **Reenviar credenciales** (D-05) genera nueva contraseña temporal, fuerza cambio en el próximo ingreso y encola `CREDENCIALES`. Requiere confirmación.

### US-01.3 Baja de paciente (CU-03)
Como **administrador** quiero dar de baja a un paciente.

- **CA-01.3.1** Cuando elige *Dar de baja*, el sistema advierte: "El paciente perderá el acceso al sistema. Sus turnos futuros se cancelarán. Esta acción no se puede deshacer." y pide confirmación y motivo (opcional).
- **CA-01.3.2** Al confirmar: paciente `BAJA`, usuario `BAJA` con contraseña invalidada y tokens revocados, turnos futuros `CANCELADO` (con notificación al paciente sólo si así se elige), plan y rutina activos `FINALIZADO`, asignaciones inactivas. Mensaje en pantalla y evento `BAJA` en bitácora.
- **CA-01.3.3** Si cancela, no hay cambios (alterno 1.1).
- **CA-01.3.4** El paciente dado de baja no aparece en listados ni búsquedas operativas, pero sí en reportes históricos y bitácora (D-06).
- **CA-01.3.5** No existe reactivación desde la UI.

### US-01.4 Asignar paciente a profesional (CU-29)
Como **administrador** quiero vincular un paciente con uno o más profesionales por área.

- **CA-01.4.1** Desde la ficha, *Asignar profesional* muestra los profesionales activos agrupados por tipo; los integrales aparecen en ambas áreas.
- **CA-01.4.2** Al confirmar uno o varios, se crean las asignaciones (`NUTRICION` para nutricionista/integral, `ENTRENAMIENTO` para entrenador/integral) y se muestra confirmación; bitácora `MODIFICACION` sobre `paciente_profesional`.
- **CA-01.4.3** Si no hay profesionales activos: "No hay profesionales disponibles" y no permite continuar (alterno 1.1).
- **CA-01.4.4** Si la asignación ya existe: "El paciente ya está asignado a ese profesional" (alterno 2.1).
- **CA-01.4.5** Se puede **desasignar**; el profesional pierde acceso de edición pero el historial de planes/rutinas creado por él se conserva.
- **CA-01.4.6** Un paciente puede tener más de un profesional por área (p. ej. dos entrenadores), pero sólo uno de ellos con plan/rutina activos a la vez (regla de spec 04/05).

### US-01.5 Listado y ficha
- **CA-01.5.1** Listado con columnas: Apellido y nombre, Documento, Teléfono, Profesionales asignados, Saldo (sólo admin), Estado; orden por apellido.
- **CA-01.5.2** Un profesional ve en *Pacientes* sólo sus pacientes asignados activos (lectura de datos personales, sin editar).
- **CA-01.5.3** El paciente ve y puede editar en *Perfil* sólo email y domicilio; el resto lo solicita al centro.

## Reglas de negocio
- RN-01.1 Nro. de documento único entre todas las personas del sistema (pacientes y profesionales, activos o de baja).
- RN-01.2 Fecha de nacimiento: pasado, edad entre 5 y 110 años.
- RN-01.3 Teléfono en formato E.164 Argentina (`+549` + área + número, 13 dígitos); la UI acepta formatos locales y normaliza.
- RN-01.4 Email opcional, formato válido.
- RN-01.5 Textos: nombre/apellido 2–80 caracteres letras, espacios, apóstrofes y guiones; objetivo ≤ 300; antecedentes/observaciones ≤ 2000.
- RN-01.6 Antecedentes de salud son informativos (no diagnóstico, PdS).

## Bitácora (CU-35)
| Acción | modulo | tipo_evento | entidad |
|---|---|---|---|
| Alta | PACIENTES | ALTA | Paciente |
| Modificación | PACIENTES | MODIFICACION | Paciente |
| Reenvío credenciales | SEGURIDAD | CAMBIO_PASSWORD | Usuario |
| Baja | PACIENTES | BAJA | Paciente |
| Asignar / desasignar | PACIENTES | MODIFICACION | PacienteProfesional |
| Ver ficha completa (datos de salud) | PACIENTES | CONSULTA_SENSIBLE | Paciente |

## Fuera de alcance
Autoregistro de pacientes; importación masiva; reactivación de bajas.
