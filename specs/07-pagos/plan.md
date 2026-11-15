# Plan 07 · Gestión de pagos

## Datos
`arancel`, `cargo`, `pago`, vista `v_saldo_paciente` (V10). `pago.nro_comprobante` desde `SEQUENCE seq_comprobante`.

## API (todas rol ADMIN salvo `/me/cuenta`)
| Método | Ruta | CU |
|---|---|---|
| GET | `/pacientes/{pid}/cuenta?desde=&hasta=` → `{saldo, totalCargos, totalPagos, fechaDeuda, movimientos[]}` | US-07.1 |
| GET | `/me/cuenta` (PACIENTE) | US-07.1 |
| POST | `/pacientes/{pid}/pagos` | CU-44 |
| GET | `/pacientes/{pid}/pagos?desde=&hasta=&estado=&formato=json|pdf|xlsx` | CU-46 |
| PUT | `/pagos/{id}` `{..., motivo, version}` | SDP 7.1.2 |
| POST | `/pagos/{id}/anular` `{motivo}` | SDP 7.1.3 |
| GET | `/pagos/{id}/comprobante.pdf` | CU-44 |
| POST | `/pacientes/{pid}/cargos` `{concepto, arancelId?, periodo?, monto, fecha, vencimiento, notificarAhora}` | CU-45 |
| PUT | `/cargos/{id}` · POST `/cargos/{id}/anular` | US-07.3 |
| POST | `/cargos/generacion-masiva` `{arancelId, periodo, pacienteIds|todos}` → `{creados, omitidosDuplicados}` | US-07.4 |
| GET | `/deudores?orden=monto|antiguedad&formato=json|pdf|xlsx` | US-07.6 |
| GET/POST/PUT | `/aranceles[/{id}]` (PUT acepta `notificarPacientes`) | US-07.7 |

## Backend
- `pagos.service.CuentaCorrienteService` (saldo, movimientos con saldo acumulado, fecha de deuda FIFO).
- `pagos.service.PagoService`, `CargoService` (duplicado → consulta + índice único filtrado), `ArancelService` (publica `ArancelModificado`).
- `reportes.pdf.ComprobantePagoPdf`; exportaciones vía `ReporteFactory` (spec 12).
- `BigDecimal` con `RoundingMode.HALF_EVEN`; nunca `double`.

## Frontend
- `admin/pagos.html` (buscador de paciente + accesos: Deudores, Aranceles), `admin/cuenta-paciente.html` (resumen + movimientos + botones Nuevo pago / Registrar deuda), modales de pago y deuda, `admin/deudores.html`, `admin/aranceles.html`.
- `paciente/mi-cuenta.html`.
- Formato moneda `Intl.NumberFormat('es-AR', {style:'currency', currency:'ARS'})`.

## Pruebas
- Unit `CuentaCorrienteServiceTest`: saldo con anulados; fecha de deuda FIFO (cargos 01/07 $10k y 01/08 $10k, pago $12k → fecha deuda = venc. del de agosto).
- Integración: duplicado de deuda → 409; anulación excluye del saldo; edición deja rastro en bitácora; generación masiva idempotente.
- Seguridad: NUTRI → 403 en todo `/pagos`.

## Tareas
- [ ] T-07.1 Entidades, vista de saldo, servicios, controllers (3 h)
- [ ] T-07.2 Pantallas de pagos, deudores, aranceles, mi cuenta (2 h)
- [ ] T-07.3 Tests (1 h)
