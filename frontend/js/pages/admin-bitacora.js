// CU-36 Consultar bitácora (sólo lectura) con filtros y exportación a Excel.
import { api, qs, descargar } from '../core/api.js';
import { iniciarPagina, encabezado } from '../core/pagina.js';
import { h, tabla, paginador, campo, leerForm, modal, manejarError, vaciar, chip } from '../core/ui.js';
import { fmt, hoyISO, sumarDias } from '../core/format.js';

const { main } = await iniciarPagina({ roles: ['ADMIN'], titulo: 'Bitácora' });
const MODULOS = ['', 'PACIENTES', 'PROFESIONALES', 'TURNOS', 'NUTRICION', 'ENTRENAMIENTO', 'EVOLUCION', 'PAGOS', 'CATALOGO', 'NOTIFICACIONES', 'REPORTES', 'SEGURIDAD', 'BACKUP', 'CONFIGURACION'];
const TIPOS = ['', 'ALTA', 'BAJA', 'MODIFICACION', 'CONSULTA_SENSIBLE', 'REPORTE', 'DESCARGA', 'CAMBIO_PASSWORD', 'CONFIGURACION', 'NOTIFICACION', 'BACKUP', 'ERROR'];
let pagina = 0;
const form = h('form', {}, h('div', { class: 'campos c3' },
  campo({ nombre: 'desde', etiqueta: 'Desde', tipo: 'date', valor: sumarDias(hoyISO(), -7) }),
  campo({ nombre: 'hasta', etiqueta: 'Hasta', tipo: 'date', valor: hoyISO() }),
  campo({ nombre: 'usuario', etiqueta: 'Usuario' }),
  campo({ nombre: 'modulo', etiqueta: 'Módulo', tipo: 'select', opciones: MODULOS.map(m => ({ valor: m, texto: m || 'Todos' })) }),
  campo({ nombre: 'tipo', etiqueta: 'Tipo de evento', tipo: 'select', opciones: TIPOS.map(m => ({ valor: m, texto: m || 'Todos' })) }),
  campo({ nombre: 'resultado', etiqueta: 'Resultado', tipo: 'select', opciones: [{ valor: '', texto: 'Todos' }, { valor: 'OK', texto: 'OK' }, { valor: 'ERROR', texto: 'Error' }] }),
  campo({ nombre: 'entidad', etiqueta: 'Entidad' }), campo({ nombre: 'entidadId', etiqueta: 'Id de entidad' })),
h('div', { class: 'acciones' }, h('button', { type: 'button', class: 'btn secundario', onclick: () => descargar(`/bitacora/exportar${qs(leerForm(form))}`, 'bitacora.xlsx') }, 'Exportar a Excel'),
  h('button', { class: 'btn', type: 'submit' }, 'Aplicar filtros')));
const res = h('div');
form.addEventListener('submit', (e) => { e.preventDefault(); pagina = 0; cargar(); });
main.append(encabezado('Bitácora', [['Opciones avanzadas', '/admin/opciones.html'], ['Bitácora']]),
  h('details', { class: 'tarjeta', open: window.innerWidth > 700 }, h('summary', {}, 'Filtros'), form), h('div', { class: 'tarjeta' }, res));

// Cada carga lleva un número: si llega la respuesta de un pedido viejo (p. ej. la carga inicial después de
// aplicar un filtro), se descarta para no pisar el resultado más reciente.
let pedidos = 0;
async function cargar() {
  const n = ++pedidos;
  try {
    const r = await api.get(`/bitacora${qs({ ...leerForm(form), page: pagina, size: 25 })}`);
    if (n !== pedidos) return;
    vaciar(res, tabla([
      { titulo: 'Fecha y hora', render: e => fmt.fechaHora(e.fechaHora) }, { titulo: 'Usuario', campo: 'username' },
      { titulo: 'Módulo', campo: 'modulo' }, { titulo: 'Evento', campo: 'tipoEvento' }, { titulo: 'Entidad', render: e => `${e.entidad || ''} ${e.entidadId || ''}` },
      { titulo: 'Descripción', campo: 'descripcion' }, { titulo: 'Resultado', render: e => chip(e.resultado, e.resultado) },
      { titulo: '', render: e => e.detalle ? h('button', { class: 'btn chico secundario', onclick: () => modal({ titulo: `Detalle del evento #${e.id}`,
        cuerpo: h('pre', { style: 'white-space:pre-wrap;background:#f6f6f0;padding:.8rem;border-radius:8px' }, JSON.stringify(JSON.parse(e.detalle), null, 2)) }) }, 'Detalle') : '' },
    ], r.content, { vacio: 'Sin resultados' }), paginador(r, (p) => { pagina = p; cargar(); }));
  } catch (e) { manejarError(e); }
}
cargar();
