# Plan 11 · Notificaciones automáticas

## Datos
`notificacion`, `alerta` (V5). Índices: (`estado`, `proximo_intento_at`), (`usuario_id`, `tipo`, `created_at`).

## Arquitectura (outbox, D-35)
```
Servicio de negocio ──(evento de dominio, misma TX)──▶ NotificacionListener
      └─ inserta fila PENDIENTE en `notificacion` (o OMITIDA si no hay teléfono)
DespachoNotificacionesJob (cada 1 min)
      └─ SELECT TOP 50 ... WHERE estado='PENDIENTE' AND proximo_intento_at <= now  (UPDLOCK, READPAST)
          └─ MensajeriaPort.enviar(plantilla, telefono, parametros)
                ├─ WhatsAppCloudAdapter (prod/dev)
                └─ LogAdapter (local / habilitado=false)
          ├─ OK  → ENVIADA, wa_message_id, borra parámetros sensibles
          └─ ERR → intentos++, backoff; si intentos=3 → FALLIDA + alerta admin
RecordatorioTurnosJob (09:00) → TurnoService.turnosParaRecordatorio(mañana) → encola y marca recordatorio_enviado_at
RecordatorioDeudaJob (sáb 10:00) → v_saldo_paciente saldo>0, sin RECORDATORIO_DEUDA en 6 días → encola
```

## WhatsApp Cloud API (adaptador)
```http
POST https://graph.facebook.com/{api-version}/{phone-number-id}/messages
Authorization: Bearer {WA_TOKEN}
{
  "messaging_product": "whatsapp",
  "to": "5491134567890",
  "type": "template",
  "template": {
    "name": "mg_recordatorio_turno",
    "language": { "code": "es_AR" },
    "components": [ { "type": "body", "parameters": [
      { "type": "text", "text": "Ana" }, { "type": "text", "text": "viernes 22/08" },
      { "type": "text", "text": "10:00" }, { "type": "text", "text": "Lic. López" },
      { "type": "text", "text": "Consultorio 1" } ] } ]
  }
}
```
- Cliente: `RestClient` de Spring con timeout 10 s.
- Errores 4xx de plantilla/número → sin reintento (FALLIDA directa); 5xx/timeout → reintento.
- Números: se envía sin `+`. Verificar en pruebas el formato que acepta la API para móviles argentinos (con/sin el `9`) y guardar el `wa_id` devuelto en `contacts[0].wa_id` para normalizar.
- Se valida en el arranque que existan las variables si `habilitado=true`.

## API (ADMIN)
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/notificaciones?tipo=&estado=&desde=&hasta=&page=` | US-11.6 |
| GET | `/notificaciones/{id}` | detalle (sin parámetros sensibles) |
| POST | `/notificaciones/{id}/reintentar` | relee teléfono actual del usuario y vuelve a PENDIENTE |
| GET | `/alertas?leidas=false` · POST `/alertas/{id}/leer` | alertas del admin |
| POST | `/admin/jobs/{recordatorio-turnos|recordatorio-deuda}/ejecutar` | ejecución manual (demo/pruebas) |

## Backend
- `notificaciones.domain.Notificacion`, `TipoNotificacion` (con nombre de plantilla), `EstadoNotificacion`.
- `notificaciones.service.NotificacionService.encolar(tipo, usuarioId, parametros, referencia)`.
- `notificaciones.listener.*Listener` (`@TransactionalEventListener(phase = BEFORE_COMMIT)`).
- `notificaciones.port.MensajeriaPort` + `WhatsAppCloudAdapter` + `LogMensajeriaAdapter` (`@ConditionalOnProperty`).
- `notificaciones.job.*` con `@Scheduled(cron = "${...}", zone = "America/Argentina/Buenos_Aires")`.
- `alertas.AlertaService` (compartido con molestias, backup).
- Formateo de fecha en mensajes: `EEEE dd/MM` en `es-AR`.

## Frontend
- `admin/notificaciones.html` (tabla + filtros + reintentar), badge de fallidas en `admin/home.html`, panel de alertas.

## Pruebas
- Unit jobs con `Clock` fijo: recordatorio sólo turnos de mañana RESERVADO; idempotencia (segunda corrida no encola); deuda respeta ventana de 6 días.
- Unit adaptador con `MockRestServiceServer`: cuerpo JSON exacto, manejo 400/500/timeout.
- Integración: alta de paciente sin teléfono válido → OMITIDA + alerta; 3 fallos → FALLIDA + alerta; reintento manual.
- Integración: parámetros sensibles borrados tras envío.
- Prueba real controlada (dev): cada plantilla a un número de prueba verificado; evidencia (captura) para el informe final.

## Tareas
- [ ] T-11.0 (S1) Outbox, `NotificacionService`, `LogMensajeriaAdapter`, listeners de credenciales y turnos
- [ ] T-11.1 Adaptador WhatsApp + plantilla credenciales + panel admin (4 h)
- [ ] T-11.2 Recordatorio de turnos (job + plantilla + tests) (4 h)
- [ ] T-11.3 Recordatorio de deuda + cambio de arancel (4 h)
