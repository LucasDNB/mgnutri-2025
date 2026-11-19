// US-11.6 Panel de notificaciones: filtros, detalle, reintento y ejecución manual de recordatorios.
import { api, qs } from '../core/api.js';
import { iniciarPagina, encabezado } from '../core/pagina.js';
import { h, tabla, paginador, campo, leerForm, toast, manejarError, vaciar, chip, param, confirmar } from '../core/ui.js';
import { fmt } from '../core/format.js';

const { main } = await iniciarPagina({ roles: ['ADMIN'], titulo: 'Notificaciones' });
const TIPOS = ['', 'CREDENCIALES', 'TURNO_CONFIRMADO', 'TURNO_REPROGRAMADO', 'TURNO_CANCELADO', 'RECORDATORIO_TURNO', 'RECORDATORIO_DEUDA', 'CAMBIO_ARANCEL', 'RECUPERO_PASSWORD'];
let pagina = 0;
const form = h('form', {}, h('div', { class: 'campos c3' },
  campo({ nombre: 'tipo', etiqueta: 'Tipo', tipo: 'select', opciones: TIPOS.map(t => ({ valor: t, texto: t ? t.replaceAll('_', ' ').toLowerCase() : 'Todos' })) }),
  campo({ nombre: 'estado', etiqueta: 'Estado', tipo: 'select', valor: param('estado') || '', opciones: ['', 'PENDIENTE', 'ENVIADA', 'FALLIDA', 'OMITIDA'].map(e => ({ valor: e, texto: e || 'Todos' })) }),
  campo({ nombre: 'desde', etiqueta: 'Desde', tipo: 'date' }), campo({ nombre: 'hasta', etiqueta: 'Hasta', tipo: 'date' })),
h('div', { class: 'acciones' }, h('button', { class: 'btn', type: 'submit' }, 'Aplicar filtros')));
form.addEventListener('submit', (e) => { e.preventDefault(); pagina = 0; cargar(); });
const job = (nombre, texto) => h('button', { class: 'btn secundario', onclick: async () => {
  if (!(await confirmar(`¿Ejecutar ahora: ${texto}?`, { texto: 'Ejecutar' }))) return;
  try { const r = await api.post(`/admin/jobs/${nombre}/ejecutar`); toast(`${texto}: ${r.encoladas} mensaje(s)`); cargar(); } catch (e) { manejarError(e); }
} }, texto);
const res = h('div');
main.append(encabezado('Notificaciones', [['Opciones avanzadas', '/admin/opciones.html'], ['Notificaciones']],
  job('recordatorio-turnos', 'Recordatorio de turnos de mañana'), job('recordatorio-deuda', 'Recordatorio de deudas'), job('despacho', 'Enviar pendientes')),
h('div', { class: 'tarjeta' }, form), h('div', { class: 'tarjeta' }, res));

// Cada carga lleva un número: si llega la respuesta de un pedido viejo (p. ej. la carga inicial después de
// aplicar un filtro), se descarta para no pisar el resultado más reciente.
let pedidos = 0;
async function cargar() {
  const n = ++pedidos;
  try {
    const r = await api.get(`/notificaciones${qs({ ...leerForm(form), page: pagina, size: 25 })}`);
    if (n !== pedidos) return;
    vaciar(res, tabla([
      { titulo: 'Fecha', render: n => fmt.fechaHora(n.createdAt) }, { titulo: 'Tipo', render: n => n.tipo.replaceAll('_', ' ').toLowerCase() },
      { titulo: 'Destinatario', render: n => [n.destinatario, h('div', { class: 'suave' }, n.telefono || 'sin teléfono')] },
      { titulo: 'Estado', render: n => chip(n.estado, n.estado) }, { titulo: 'Intentos', campo: 'intentos', clase: 'num' },
      { titulo: 'Error', render: n => h('small', {}, n.error || '') },
      { titulo: '', render: n => ['FALLIDA', 'OMITIDA'].includes(n.estado) ? h('button', { class: 'btn chico secundario', onclick: async () => {
        try { await api.post(`/notificaciones/${n.id}/reintentar`); toast('Notificación reencolada'); cargar(); } catch (e) { manejarError(e); }
      } }, 'Reintentar') : '' },
    ], r.content, { vacio: 'Sin notificaciones' }), paginador(r, (p) => { pagina = p; cargar(); }));
  } catch (e) { manejarError(e); }
}
cargar();
