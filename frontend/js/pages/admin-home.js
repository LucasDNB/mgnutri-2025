// Home del administrador (mockup ERS 3.9.1) con alertas y contador de notificaciones fallidas (CA-11.6.2).
import { api } from '../core/api.js';
import { iniciarPagina } from '../core/pagina.js';
import { h, chip, tabla, toast, manejarError, confirmar } from '../core/ui.js';
import { fmt } from '../core/format.js';

const { usuario, main } = await iniciarPagina({ roles: ['ADMIN'], titulo: 'Inicio' });
const resumen = await api.get('/notificaciones/resumen').catch(() => ({ fallidas: 0, omitidas: 0 }));
const problemas = resumen.fallidas + resumen.omitidas;

const boton = (texto, href, badge, secundario) => h('a', { class: `boton-home ${secundario ? 'secundario' : ''}`, href },
  texto, badge ? h('span', { class: 'badge' }, badge) : null);

const alertasEl = h('div');
const todasEl = h('button', { class: 'btn chico', style: 'display:none', onclick: marcarTodas }, 'Marcar todas como leídas');

async function marcarTodas() {
  if (!(await confirmar('¿Marcar todas las alertas como leídas?', { texto: 'Marcar todas' }))) return;
  try {
    const { marcadas } = await api.post('/alertas/leer-todas');
    toast(marcadas === 1 ? '1 alerta marcada como leída' : `${marcadas} alertas marcadas como leídas`);
    cargarAlertas();
  } catch (e) { manejarError(e); }
}
main.append(
  h('h1', {}, `Hola, ${usuario.nombre}`),
  h('div', { class: 'botones-home ancho' },
    boton('Calendario', '/comun/calendario.html'),
    boton('Pacientes', '/comun/pacientes.html'),
    boton('Profesionales', '/admin/profesionales.html'),
    boton('Pagos', '/admin/pagos.html'),
    boton('Reportes', '/comun/reportes.html', null, true),
    boton('Opciones avanzadas', '/admin/opciones.html', problemas || null, true)),
  h('div', { class: 'tarjeta' }, h('div', { class: 'fila entre' }, h('h2', {}, 'Alertas'),
    h('div', { class: 'fila' },
      problemas ? h('a', { class: 'btn chico secundario', href: '/admin/notificaciones.html?estado=FALLIDA' }, `Notificaciones con problemas: ${problemas}`) : null,
      todasEl)),
  alertasEl));

async function cargarAlertas() {
  const lista = await api.get('/alertas');
  todasEl.style.display = lista.length ? '' : 'none';
  alertasEl.replaceChildren(tabla([
    { titulo: 'Fecha', render: a => fmt.fechaHora(a.createdAt) },
    { titulo: 'Tipo', render: a => chip(a.tipo.replaceAll('_', ' ').toLowerCase(), a.prioritaria ? 'prioritaria' : '') },
    { titulo: 'Mensaje', campo: 'mensaje' },
    { titulo: '', render: a => h('button', { class: 'btn chico secundario', onclick: async () => {
      try { await api.post(`/alertas/${a.id}/leer`); toast('Alerta marcada como leída'); cargarAlertas(); } catch (e) { manejarError(e); }
    } }, 'Marcar leída') },
  ], lista, { vacio: 'No hay alertas pendientes 🎉' }));
}
cargarAlertas();
