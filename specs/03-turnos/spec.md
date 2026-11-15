# Spec 03 · Gestión de turnos

| Campo | Valor |
|---|---|
| Paquete SDP | 3 – Gestión de turnos |
| RF | RF-03 · RF-13 (confirmación/modificación/cancelación) · RF-17 |
| CU | CU-07 Solicitar · CU-08 Modificar · CU-09 Cancelar · CU-10 Visualizar · CU-38 (confirmación inmediata; recordatorio en spec 11) · CU-35 |
| Sprint | 1 · Backlog: BackEnd (3 h), FrontEnd (3 h), Testeos (1 h) |
| Depende de | 01, 02 |
| Decisiones | D-08, D-09, D-10, D-13, reglas R-T1…R-T4 |

## Contexto
Los pacientes reservan turnos con los profesionales sobre huecos de su agenda. Profesionales y administrador ven y gestionan los turnos en un calendario. Toda creación, reprogramación o cancelación notifica al paciente por WhatsApp.

## Historias de usuario

### US-03.1 Solicitar turno — paciente (CU-07)
- **CA-03.1.1** Desde *Nutrición › Turnos › Nuevo turno* (o *Entrenamiento › Turnos › Nuevo turno*), el sistema lista los profesionales con agenda del área, **primero los asignados al paciente** (los "habituales").
- **CA-03.1.2** Al elegir profesional, ve los huecos libres de los próximos 30 días (navegable por semana), en hora Argentina, con la ubicación.
- **CA-03.1.3** Al elegir un hueco y confirmar, el turno queda `RESERVADO`, aparece en el calendario del profesional, se muestra "Turno confirmado para el {día} {fecha} a las {hora} con {profesional}" y se encola `TURNO_CONFIRMADO`.
- **CA-03.1.4** Si el paciente ya tiene un turno `RESERVADO` con ese profesional en los próximos 7 días (R-T1), se muestra "Ya tenés un turno el {fecha} a las {hora}" y un botón que lleva a *Modificar/Cancelar turnos* (alterno 1.1–1.3); no se crea turno.
- **CA-03.1.5** Si el hueco fue tomado por otro usuario entre la consulta y la confirmación, se informa `HORARIO_NO_DISPONIBLE` y se refrescan los huecos.

### US-03.2 Modificar (reprogramar) turno — paciente (CU-08)
- **CA-03.2.1** *Turnos › Modificar/Cancelar turnos* lista los turnos futuros `RESERVADO` del paciente.
- **CA-03.2.2** Los turnos que empiezan en menos de 24 h aparecen deshabilitados con la leyenda "No se puede modificar con menos de 24 h de anticipación. Comunicate con el centro." (R-T2, alterno 1.1).
- **CA-03.2.3** Al elegir *Modificar*, se ofrecen sólo huecos del **mismo profesional** (R-T3). Al confirmar, el turno cambia de fecha/hora, se guarda el horario anterior en el historial, se confirma en pantalla y se encola `TURNO_REPROGRAMADO`.
- **CA-03.2.4** Si el turno ya tenía recordatorio enviado, se resetea `recordatorio_enviado_at` para que se recuerde el nuevo horario.

### US-03.3 Cancelar turno — paciente (CU-09)
- **CA-03.3.1** Igual restricción de 24 h (alterno 1.1).
- **CA-03.3.2** Al confirmar, turno `CANCELADO` (cancelado por el paciente), el hueco queda libre, confirmación en pantalla, `TURNO_CANCELADO` encolado y evento en bitácora.

### US-03.4 Gestión por profesional y administrador (RF-03)
- **CA-03.4.1** El profesional puede crear turnos para sus pacientes asignados y el admin para cualquier paciente, sin la restricción de 7 días ni la de 24 h; puede ofrecer cualquier hueco de la agenda o forzar un horario fuera de agenda (sólo admin, con advertencia).
- **CA-03.4.2** Profesional y admin pueden reprogramar y cancelar en cualquier momento (con motivo obligatorio); el paciente recibe la notificación correspondiente.
- **CA-03.4.3** Profesional y admin marcan un turno pasado como `ATENDIDO` o `AUSENTE`. Un turno no puede marcarse antes de su hora de inicio.
- **CA-03.4.4** Un profesional sólo ve y gestiona sus propios turnos; el admin todos.

### US-03.5 Calendario (CU-10)
- **CA-03.5.1** *Calendario* muestra por defecto el día actual con los turnos asignados.
- **CA-03.5.2** Filtro de vista **día / semana / mes** (D-13); navegación anterior/siguiente/hoy.
- **CA-03.5.3** El listado alternativo permite ordenar por fecha, paciente o profesional.
- **CA-03.5.4** Admin: filtro por profesional (uno o todos) y por área. Colores por estado (reservado, atendido, ausente, cancelado; cancelados ocultos por defecto).
- **CA-03.5.5** Clic en un turno → detalle (paciente con link a ficha, teléfono, ubicación, estado) y acciones permitidas.
- **CA-03.5.6** Paciente: *Mis turnos* lista próximos y pasados (últimos 6 meses).

## Reglas de negocio
- R-T1 Un turno `RESERVADO` por paciente y profesional en una ventana de 7 días (sólo aplica a solicitudes del paciente).
- R-T2 Paciente no modifica/cancela con < 24 h.
- R-T3 Reprogramación por el paciente sólo con el mismo profesional.
- R-T4 Sin superposición por profesional; sin turnos en el pasado; el turno debe caer completo dentro de una franja y fuera de bloqueos (salvo forzado por admin).
- R-T5 Un paciente no puede tener dos turnos superpuestos (con distintos profesionales).
- R-T6 Paciente o profesional dados de baja no pueden recibir turnos.
- R-T7 Huecos: se generan cada `duracion_turno_min` desde `hora_desde` de cada franja; se ofrecen sólo los que empiezan al menos 2 h después de "ahora".

## Notificaciones (RF-13)
| Evento | Tipo | Plantilla | Parámetros |
|---|---|---|---|
| Creación | TURNO_CONFIRMADO | `mg_turno_confirmado` | nombre, profesional, fecha, hora, ubicación |
| Reprogramación | TURNO_REPROGRAMADO | `mg_turno_reprogramado` | nombre, profesional, fecha y hora anterior, nueva fecha y hora, ubicación |
| Cancelación | TURNO_CANCELADO | `mg_turno_cancelado` | nombre, profesional, fecha, hora, motivo |

## Bitácora
`TURNOS` · ALTA / MODIFICACION (reprogramación, cambio de estado) / BAJA (cancelación) sobre `Turno`.

## Casos borde
- Cambio de horario de verano: Argentina no lo usa actualmente, pero todo cálculo usa `ZoneId` (no offsets fijos).
- Turno que cruza medianoche: no permitido (franjas dentro del mismo día).
- Profesional modifica su agenda: los turnos ya reservados fuera de las nuevas franjas se mantienen y se listan como "fuera de agenda" para que decida.
