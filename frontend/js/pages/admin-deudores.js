// US-07.6 Listado de deudores con fecha de deuda (FIFO) y exportación.
import { api, descargar } from '../core/api.js';
import { iniciarPagina, encabezado } from '../core/pagina.js';
import { h, tabla, manejarError, vaciar, chip } from '../core/ui.js';
import { fmt } from '../core/format.js';

const { main } = await iniciarPagina({ roles: ['ADMIN'], titulo: 'Deudores' });
const orden = h('select', {}, h('option', { value: 'monto' }, 'Ordenar por monto'), h('option', { value: 'antiguedad' }, 'Ordenar por antigüedad'));
const res = h('div');
main.append(encabezado('Deudores', [['Pagos', '/admin/pagos.html'], ['Deudores']],
  h('button', { class: 'btn secundario', onclick: () => descargar(`/deudores?orden=${orden.value}&formato=pdf`) }, 'PDF'),
  h('button', { class: 'btn secundario', onclick: () => descargar(`/deudores?orden=${orden.value}&formato=xlsx`) }, 'Excel')),
h('div', { class: 'tarjeta' }, orden, h('div', { style: 'margin-top:.8rem' }, res)));

// Cada carga lleva un número: si llega la respuesta de un pedido viejo (p. ej. la carga inicial después de
// aplicar un filtro), se descarta para no pisar el resultado más reciente.
let pedidos = 0;
async function cargar() {
  const n = ++pedidos;
  try {
    const lista = await api.get(`/deudores?orden=${orden.value}`);
    if (n !== pedidos) return;
    const total = lista.reduce((s, d) => s + Number(d.saldo), 0);
    vaciar(res, h('p', {}, `${lista.length} paciente(s) con saldo deudor · Total: `, h('strong', {}, fmt.moneda(total))), tabla([
      { titulo: 'Paciente', render: d => [h('a', { href: `/admin/pagos.html?pacienteId=${d.pacienteId}` }, d.paciente), d.baja ? chip('Baja', 'FINALIZADO') : null] },
      { titulo: 'Documento', campo: 'documento' }, { titulo: 'Teléfono', campo: 'telefono' },
      { titulo: 'Saldo', render: d => fmt.moneda(d.saldo), clase: 'num' },
      { titulo: 'Fecha de deuda', render: d => fmt.fecha(d.fechaDeuda) },
      { titulo: 'Días de atraso', render: d => chip(d.diasAtraso, d.diasAtraso > 30 ? 'ROJO' : d.diasAtraso > 0 ? 'AMARILLO' : 'OK'), clase: 'num' },
    ], lista, { vacio: 'No hay pacientes con deuda 🎉' }));
  } catch (e) { manejarError(e); }
}
orden.addEventListener('change', cargar);
cargar();
