// RF-16 Registro de accesos: consulta con filtros y exportación a Excel.
import { api, qs, descargar } from '../core/api.js';
import { iniciarPagina, encabezado } from '../core/pagina.js';
import { h, tabla, paginador, campo, leerForm, manejarError, vaciar, chip } from '../core/ui.js';
import { fmt, hoyISO, sumarDias } from '../core/format.js';

const { main } = await iniciarPagina({ roles: ['ADMIN'], titulo: 'Registro de accesos' });
const ACCIONES = ['', 'LOGIN_OK', 'LOGIN_FALLIDO', 'LOGOUT', 'BLOQUEO', 'RECUPERO_SOLICITADO', 'PASSWORD_RESTABLECIDA', 'PASSWORD_CAMBIADA'];
let pagina = 0;
const form = h('form', {}, h('div', { class: 'campos c3' },
  campo({ nombre: 'desde', etiqueta: 'Desde', tipo: 'date', valor: sumarDias(hoyISO(), -7) }),
  campo({ nombre: 'hasta', etiqueta: 'Hasta', tipo: 'date', valor: hoyISO() }),
  campo({ nombre: 'usuario', etiqueta: 'Usuario' }),
  campo({ nombre: 'accion', etiqueta: 'Acción', tipo: 'select', opciones: ACCIONES.map(a => ({ valor: a, texto: a ? a.replaceAll('_', ' ').toLowerCase() : 'Todas' })) }),
  campo({ nombre: 'ip', etiqueta: 'IP' })),
h('div', { class: 'acciones' }, h('button', { type: 'button', class: 'btn secundario', onclick: () => descargar(`/accesos/exportar${qs(leerForm(form))}`, 'accesos.xlsx') }, 'Exportar a Excel'),
  h('button', { class: 'btn', type: 'submit' }, 'Aplicar filtros')));
const res = h('div');
form.addEventListener('submit', (e) => { e.preventDefault(); pagina = 0; cargar(); });
main.append(encabezado('Registro de accesos', [['Opciones avanzadas', '/admin/opciones.html'], ['Accesos']]),
  h('div', { class: 'tarjeta' }, form), h('div', { class: 'tarjeta' }, res));

// Cada carga lleva un número: si llega la respuesta de un pedido viejo (p. ej. la carga inicial después de
// aplicar un filtro), se descarta para no pisar el resultado más reciente.
let pedidos = 0;
async function cargar() {
  const n = ++pedidos;
  try {
    const r = await api.get(`/accesos${qs({ ...leerForm(form), page: pagina, size: 25 })}`);
    if (n !== pedidos) return;
    vaciar(res, tabla([
      { titulo: 'Fecha y hora', render: a => fmt.fechaHora(a.fechaHora) }, { titulo: 'Usuario', campo: 'usuario' }, { titulo: 'Roles', campo: 'roles' },
      { titulo: 'Acción', render: a => chip(a.accion.replaceAll('_', ' ').toLowerCase(), a.accion.includes('FALLIDO') || a.accion === 'BLOQUEO' ? 'ROJO' : 'OK') },
      { titulo: 'IP', campo: 'ip' }, { titulo: 'Detalle', campo: 'detalle' },
      { titulo: 'Navegador', render: a => h('small', {}, (a.userAgent || '').substring(0, 60)) },
    ], r.content), paginador(r, (p) => { pagina = p; cargar(); }));
  } catch (e) { manejarError(e); }
}
cargar();
