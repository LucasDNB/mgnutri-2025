// US-07.6 Listado de deudores con fecha de deuda (FIFO) y exportación.
// D-40: filtro por días de atraso y envío del recordatorio de deuda a los pacientes filtrados.
import { api, descargar, qs } from '../core/api.js';
import { iniciarPagina, encabezado } from '../core/pagina.js';
import { h, tabla, manejarError, vaciar, chip, confirmar, toast, demorar } from '../core/ui.js';
import { fmt } from '../core/format.js';

const { main } = await iniciarPagina({ roles: ['ADMIN'], titulo: 'Deudores' });
const orden = h('select', { 'aria-label': 'Orden', style: 'width:auto' }, h('option', { value: 'monto' }, 'Ordenar por monto'), h('option', { value: 'antiguedad' }, 'Ordenar por antigüedad'));
const dias = h('input', { type: 'number', min: 0, step: 1, value: 0, style: 'width:6rem', 'aria-label': 'Días de atraso (mínimo)' });
const enviar = h('button', { class: 'btn', type: 'button', onclick: () => enviarRecordatorios() }, 'Enviar recordatorio de deuda');
const res = h('div');
const diasMinimos = () => Math.max(0, parseInt(dias.value, 10) || 0);
const filtros = () => ({ orden: orden.value, diasMinimos: diasMinimos() });
main.append(encabezado('Deudores', [['Pagos', '/admin/pagos.html'], ['Deudores']],
  h('button', { class: 'btn secundario', onclick: () => descargar(`/deudores${qs({ ...filtros(), formato: 'pdf' })}`) }, 'PDF'),
  h('button', { class: 'btn secundario', onclick: () => descargar(`/deudores${qs({ ...filtros(), formato: 'xlsx' })}`) }, 'Excel')),
h('div', { class: 'tarjeta' },
  h('div', { class: 'fila entre' },
    h('div', { class: 'fila' }, orden, h('label', { class: 'fila' }, 'Con al menos', dias, 'días de atraso')),
    enviar),
  h('div', { style: 'margin-top:.8rem' }, res)));

let lista = [];
// Cada carga lleva un número: si llega la respuesta de un pedido viejo (p. ej. la carga inicial después de
// aplicar un filtro), se descarta para no pisar el resultado más reciente.
let pedidos = 0;
async function cargar() {
  const n = ++pedidos;
  try {
    const r = await api.get(`/deudores${qs(filtros())}`);
    if (n !== pedidos) return;
    lista = r;
    const total = lista.reduce((s, d) => s + Number(d.saldo), 0);
    const destinatarios = lista.filter(d => !d.baja && d.telefono).length;
    enviar.disabled = destinatarios === 0;
    enviar.textContent = `Enviar recordatorio de deuda (${destinatarios})`;
    vaciar(res, h('p', {}, `${lista.length} paciente(s) con saldo deudor · Total: `, h('strong', {}, fmt.moneda(total))), tabla([
      { titulo: 'Paciente', render: d => [h('a', { href: `/admin/pagos.html?pacienteId=${d.pacienteId}` }, d.paciente), d.baja ? chip('Baja', 'FINALIZADO') : null] },
      { titulo: 'Documento', campo: 'documento' }, { titulo: 'Teléfono', campo: 'telefono' },
      { titulo: 'Saldo', render: d => fmt.moneda(d.saldo), clase: 'num' },
      { titulo: 'Fecha de deuda', render: d => fmt.fecha(d.fechaDeuda) },
      { titulo: 'Días de atraso', render: d => chip(d.diasAtraso, d.diasAtraso > 30 ? 'ROJO' : d.diasAtraso > 0 ? 'AMARILLO' : 'OK'), clase: 'num' },
    ], lista, { vacio: diasMinimos() ? 'No hay pacientes con ese atraso' : 'No hay pacientes con deuda 🎉' }));
  } catch (e) { manejarError(e); }
}

async function enviarRecordatorios() {
  const d = diasMinimos();
  const destinatarios = lista.filter(x => !x.baja && x.telefono).length;
  const ok = await confirmar(`Se enviará el recordatorio de deuda por WhatsApp a ${destinatarios} paciente(s) con ${d ? `al menos ${d} días de atraso` : 'saldo deudor'}. `
    + 'Los que ya recibieron uno en los últimos 6 días se omiten. ¿Continuar?');
  if (!ok) return;
  enviar.disabled = true;
  try {
    const r = await api.post(`/deudores/recordatorios${qs({ diasMinimos: d })}`);
    toast(`Recordatorios enviados: ${r.encolados}` + (r.omitidos ? ` · omitidos: ${r.omitidos} (ya recibieron uno en los últimos 6 días, sin teléfono o de baja)` : ''), 'ok', 6000);
  } catch (e) { manejarError(e); } finally { enviar.disabled = false; }
}

orden.addEventListener('change', cargar);
dias.addEventListener('input', demorar(cargar));
cargar();
