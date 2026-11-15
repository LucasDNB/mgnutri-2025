# Spec 07 · Gestión de pagos

| Campo | Valor |
|---|---|
| Paquete SDP | 7 – Gestión de pagos |
| RF | RF-08 · RF-13 (cambio de aranceles) · RF-17 |
| CU | CU-44 Registrar pago · CU-45 Registrar deuda · CU-46 Historial de pagos · SDP: modificar / eliminar pago · CU-39 (spec 11) · CU-35 |
| Sprint | 2 · Backlog: BackEnd (3 h), FrontEnd (2 h), Testeo (1 h) |
| Depende de | 01 |
| Decisiones | D-19, D-20, D-21 |

## Contexto
El administrador lleva la cuenta corriente de cada paciente: registra deudas (cargos) y pagos, ve el estado de cuenta, lista morosos y corrige errores. No es contabilidad (PdS): no hay facturación fiscal, asientos ni caja.

## Historias de usuario

### US-07.1 Estado de cuenta
- **CA-07.1.1** En *Pagos* el admin busca un paciente y ve: saldo actual (deudor/a favor/al día), total de cargos y pagos, movimientos ordenados por fecha (cargo/pago, concepto, monto, saldo acumulado).
- **CA-07.1.2** El paciente ve en su perfil *Mi cuenta*: saldo y últimos movimientos (sólo lectura). **[RF-08 limita la gestión al admin; la visualización propia del paciente es informativa.]**

### US-07.2 Registrar pago (CU-44)
- **CA-07.2.1** *Pagos › {paciente} › Nuevo pago*: monto (> 0), fecha (default hoy, no futura), medio de pago, referencia (opcional), observaciones.
- **CA-07.2.2** Al guardar se asigna número de comprobante interno correlativo, se actualiza el historial y el saldo, y se ofrece **Imprimir comprobante** (PDF) — requerimiento especial CU-44.
- **CA-07.2.3** Datos inválidos o incompletos → error y no guarda (alterno 1.1).
- **CA-07.2.4** Bitácora `PAGOS/ALTA`.
- **CA-07.2.5** Si el pago deja saldo a favor se informa "El paciente queda con saldo a favor de $X".

### US-07.3 Modificar / eliminar pago (SDP paquete 7, RF-08 restricción)
- **CA-07.3.1** El admin puede editar monto, fecha, medio, referencia u observaciones de un pago con motivo obligatorio; bitácora `MODIFICACION` con valores anterior/nuevo de monto y fecha.
- **CA-07.3.2** Eliminar = anular con motivo; el pago anulado se muestra tachado en el historial y no suma al saldo; bitácora `BAJA`.
- **CA-07.3.3** Mismas acciones sobre cargos (corregir o anular deudas registradas por error).

### US-07.4 Registrar deuda (CU-45)
- **CA-07.4.1** Desde el perfil del paciente, *Registrar deuda*: concepto (elegible del catálogo de aranceles o libre), período (mes/año, opcional), monto, fecha y vencimiento (default +10 días).
- **CA-07.4.2** Al guardar se asocia al historial del paciente y se registra en bitácora.
- **CA-07.4.3** Si ya existe una deuda no anulada con el mismo concepto, período y monto → "El paciente ya tiene esta deuda registrada" y bloquea el duplicado (alterno 1.1, `DEUDA_DUPLICADA`).
- **CA-07.4.4** Integración con notificaciones (requerimiento especial): la deuda queda alcanzada por el recordatorio semanal (spec 11). Opción "Notificar ahora" envía `RECORDATORIO_DEUDA` inmediato.
- **CA-07.4.5** *Generar cuotas del mes*: acción masiva que crea, para todos los pacientes activos seleccionados, el cargo de un arancel para el período elegido, omitiendo duplicados (muestra resumen).

### US-07.5 Historial de pagos (CU-46)
- **CA-07.5.1** Desde la ficha, *Historial de pagos* lista todos los pagos con filtros por rango de fechas y estado (vigente/anulado).
- **CA-07.5.2** Sin pagos → "El paciente no registra pagos" (alterno 1.1).
- **CA-07.5.3** Exportación a PDF o Excel (requerimiento especial CU-46).

### US-07.6 Listado de deudores (RF-08)
- **CA-07.6.1** *Pagos › Deudores*: pacientes con saldo > 0, con monto adeudado y **fecha de la deuda** (vencimiento del cargo impago más antiguo, D-19), días de atraso, teléfono; orden por monto o antigüedad; exportable.

### US-07.7 Aranceles (D-20)
- **CA-07.7.1** *Opciones avanzadas › Aranceles*: ABM de conceptos con monto y vigencia.
- **CA-07.7.2** Al modificar el monto de un arancel, el admin puede elegir "Notificar a los pacientes activos"; se encola `CAMBIO_ARANCEL` para cada uno con teléfono (RF-13). Bitácora `PAGOS/MODIFICACION`.

## Reglas de negocio
- RN-07.1 Sólo ADMIN gestiona (RF-08).
- RN-07.2 Montos: 0,01 – 9.999.999,99; dos decimales; moneda ARS.
- RN-07.3 Saldo = Σ cargos no anulados − Σ pagos no anulados.
- RN-07.4 Imputación FIFO sólo para calcular "fecha de deuda" y días de atraso; no se persiste imputación.
- RN-07.5 Un paciente dado de baja conserva su cuenta; aparece en deudores con marca "Baja".
- RN-07.6 Medios: Efectivo, Transferencia, Débito, Crédito, Mercado Pago, Otro.

## Bitácora
`PAGOS` · ALTA / MODIFICACION / BAJA sobre `Pago`, `Cargo`, `Arancel`; `REPORTE`/`DESCARGA` en exportaciones y comprobantes.
