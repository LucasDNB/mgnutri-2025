# Spec 11 · Notificaciones automáticas por WhatsApp

| Campo | Valor |
|---|---|
| Paquete SDP | 11 – Notificaciones automáticas (+ 1.3, 2.3, 3.3, 7.2) |
| RF | RF-07 · RF-13 |
| CU | CU-37 Credenciales post-alta · CU-38 Recordatorio de turno · CU-39 Recordatorio de deuda (+ confirmación/reprogramación/cancelación de turno, cambio de arancel, recupero de contraseña) |
| Sprint | 4 · Backlog: WhatsApp – envío de credenciales (4 h), recordatorio de turnos (4 h), recordatorio de deuda (4 h). El outbox y el adaptador "log" existen desde Sprint 1 (D-27). |
| Depende de | 00 (cuenta Meta), 01, 02, 03, 07 |
| Decisiones | D-11, D-12, D-26, D-35 |

## Contexto
El sistema avisa por WhatsApp los eventos que importan al paciente/profesional: sus credenciales, sus turnos y sus deudas. Los envíos se hacen en forma confiable (outbox con reintentos) y, si fallan, el administrador se entera.

## Historias de usuario

### US-11.1 Credenciales post-alta (CU-37)
- **CA-11.1.1** Confirmada el alta de paciente o profesional (o un *Reenviar credenciales*), el sistema envía un WhatsApp con nombre, usuario, contraseña temporal y enlace al sistema, indicando que deberá cambiarla en el primer ingreso.
- **CA-11.1.2** Si el servicio falla, se notifica al administrador (alerta en *Notificaciones fallidas* con opción *Reintentar*) — alterno 1.1.
- **CA-11.1.3** El parámetro de contraseña nunca se guarda en claro: `notificacion.parametros` va cifrado y se borra (se reemplaza por `"***"`) cuando el mensaje queda `ENVIADA` o `FALLIDA`.

### US-11.2 Avisos de turno (RF-13) y recordatorio (CU-38, RF-07)
- **CA-11.2.1** Al crear, reprogramar o cancelar un turno se envía de inmediato el mensaje correspondiente (spec 03 §Notificaciones).
- **CA-11.2.2** Todos los días a las 09:00 (configurable) el proceso automático busca los turnos `RESERVADO` del día siguiente y envía a cada paciente: nombre, profesional, fecha, hora y ubicación (RF-07 restricciones).
- **CA-11.2.3** Cada turno se recuerda una sola vez (idempotente aunque el job se ejecute dos veces o se reinicie la app).
- **CA-11.2.4** Turnos creados después de la corrida del día y antes del turno no reciben recordatorio (ya recibieron la confirmación inmediata).
- **CA-11.2.5** Si la integración falla, se registra el fallo y se informa al administrador (alterno 1.1).

### US-11.3 Recordatorio de deuda (CU-39, RF-07)
- **CA-11.3.1** Todos los sábados a las 10:00 el proceso identifica los pacientes activos con saldo deudor > 0 y les envía un mensaje personalizado con nombre, monto adeudado y, si `pagos.link` está configurado, el enlace de pago.
- **CA-11.3.2** También se envía en forma inmediata cuando el admin registra una deuda con *Notificar ahora* (spec 07).
- **CA-11.3.3** Un paciente no recibe más de un recordatorio de deuda en 6 días aunque haya envíos manuales.
- **CA-11.3.4** Falla → se registra y alerta al admin (alterno 1.1).

### US-11.4 Cambio de arancel (RF-13)
- **CA-11.4.1** Cuando el admin confirma notificar un cambio de arancel, cada paciente activo con teléfono recibe: concepto, monto nuevo y fecha de vigencia.

### US-11.5 Condiciones comunes (RF-13)
- **CA-11.5.1** Sólo se envía si el destinatario tiene número de WhatsApp registrado y válido; si no, la notificación queda `OMITIDA` (visible para el admin) — RF-07/RF-13.
- **CA-11.5.2** Los mensajes incluyen datos personalizados (nombre, fecha/hora, profesional, monto…).
- **CA-11.5.3** Reintentos automáticos: 3 intentos (1, 5 y 15 min). Luego `FALLIDA` + alerta.
- **CA-11.5.4** Con `whatsapp.habilitado=false` (entorno local) los mensajes se "envían" al log y quedan `ENVIADA` con `wa_message_id = "LOCAL-…"`.
- **CA-11.5.5** Cada envío exitoso o fallido registra `NOTIFICACIONES/NOTIFICACION` en bitácora (sin contenido sensible).

### US-11.6 Panel de notificaciones (admin)
- **CA-11.6.1** *Opciones avanzadas › Notificaciones*: listado filtrable por tipo, estado y fecha; detalle con error; acción *Reintentar* para `FALLIDA`/`OMITIDA` (por ejemplo, después de corregir el teléfono).
- **CA-11.6.2** Contador de fallidas en el home del admin.

## Plantillas de WhatsApp (a aprobar en Meta, idioma es_AR, categoría *Utility*)
| Nombre | Cuerpo propuesto | Variables |
|---|---|---|
| `mg_credenciales` | Hola {{1}}, te damos la bienvenida a MG Nutrición y Entrenamiento. Tu usuario es {{2}} y tu contraseña temporal es {{3}}. Ingresá en {{4}} y cambiala en tu primer acceso. | nombre, usuario, contraseña, url |
| `mg_turno_confirmado` | Hola {{1}}, tu turno con {{2}} quedó confirmado para el {{3}} a las {{4}} en {{5}}. | nombre, profesional, fecha, hora, ubicación |
| `mg_turno_reprogramado` | Hola {{1}}, tu turno con {{2}} del {{3}} fue reprogramado para el {{4}} a las {{5}} en {{6}}. | |
| `mg_turno_cancelado` | Hola {{1}}, tu turno con {{2}} del {{3}} a las {{4}} fue cancelado. Motivo: {{5}}. Podés pedir uno nuevo desde el sistema. | |
| `mg_recordatorio_turno` | Hola {{1}}, te recordamos tu turno de mañana {{2}} a las {{3}} con {{4}} en {{5}}. | |
| `mg_recordatorio_deuda` | Hola {{1}}, registrás un saldo pendiente de {{2}} en MG Nutrición y Entrenamiento. {{3}} | nombre, monto, texto de enlace o "Acercate a la central para regularizarlo." |
| `mg_cambio_arancel` | Hola {{1}}, te informamos que desde el {{2}} el valor de {{3}} será {{4}}. | |
| `mg_recupero_password` | Hola {{1}}, recibimos un pedido para restablecer tu contraseña. Usá este enlace (válido 24 h): {{2}}. Si no fuiste vos, ignorá este mensaje. | |

**Riesgo**: Meta puede rechazar plantillas que transmitan contraseñas. Alternativa preparada: `mg_credenciales` envía un **enlace de activación** de un solo uso (mismo mecanismo que el recupero, spec 13) en lugar de la contraseña. Se decide según la aprobación de la plantilla; el código soporta ambas variantes por configuración (`whatsapp.credenciales-modo: PASSWORD | ENLACE`).
