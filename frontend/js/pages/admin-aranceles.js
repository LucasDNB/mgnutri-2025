// US-07.7 Aranceles (D-20) con notificación opcional del cambio de monto.
import { api } from '../core/api.js';
import { iniciarPagina, encabezado } from '../core/pagina.js';
import { h, tabla, modal, campo, leerForm, toast, manejarError, vaciar, chip } from '../core/ui.js';
import { fmt, hoyISO } from '../core/format.js';

const { main } = await iniciarPagina({ roles: ['ADMIN'], titulo: 'Aranceles' });
const res = h('div');
main.append(encabezado('Aranceles', [['Opciones avanzadas', '/admin/opciones.html'], ['Aranceles']], h('button', { class: 'btn', onclick: () => editar() }, '+ Nuevo arancel')),
  h('div', { class: 'tarjeta' }, res));

async function cargar() {
  const lista = await api.get('/aranceles');
  vaciar(res, tabla([
    { titulo: 'Concepto', campo: 'concepto' }, { titulo: 'Monto', render: a => fmt.moneda(a.monto), clase: 'num' },
    { titulo: 'Vigente desde', render: a => fmt.fecha(a.vigenteDesde) }, { titulo: 'Estado', render: a => chip(a.activo ? 'Activo' : 'Inactivo', a.activo ? 'OK' : 'FINALIZADO') },
    { titulo: '', render: a => h('button', { class: 'btn chico secundario', onclick: () => editar(a) }, 'Modificar') },
  ], lista, { vacio: 'Sin aranceles cargados' }));
}

async function editar(a = null) {
  const form = h('form', {}, h('div', { class: 'campos c2' },
    campo({ nombre: 'concepto', etiqueta: 'Concepto', requerido: true, maxLength: 100, valor: a?.concepto }),
    campo({ nombre: 'monto', etiqueta: 'Monto', tipo: 'number', step: 0.01, min: 0.01, requerido: true, valor: a?.monto }),
    campo({ nombre: 'vigenteDesde', etiqueta: 'Vigente desde', tipo: 'date', requerido: true, valor: a?.vigenteDesde || hoyISO() }),
    campo({ nombre: 'activo', etiqueta: 'Activo', tipo: 'checkbox', valor: a ? a.activo : true }),
    a ? campo({ nombre: 'notificarPacientes', etiqueta: 'Si cambia el monto, notificar a los pacientes activos por WhatsApp', tipo: 'checkbox', ancho: true }) : null));
  await modal({ titulo: a ? 'Modificar arancel' : 'Nuevo arancel', cuerpo: form, botones: [{ texto: 'Cancelar', clase: 'secundario' }, { texto: 'Guardar', accion: async () => {
    const d = leerForm(form);
    const r = a ? await api.put(`/aranceles/${a.id}`, { ...d, version: a.version }) : await api.post('/aranceles', d);
    toast(r.notificados != null ? `Arancel guardado. Pacientes notificados: ${r.notificados}` : 'Arancel guardado');
    cargar();
  } }] });
}
cargar().catch(manejarError);
