// US-02.4 Disponibilidad semanal y bloqueos (profesional: la propia; admin: ?id=).
import { api } from '../core/api.js';
import { iniciarPagina, encabezado, usaAgenda } from '../core/pagina.js';
import { h, tabla, toast, confirmar, manejarError, vaciar, campo, modal } from '../core/ui.js';
import { fmt, DIAS, aIsoArgentina, hoyISO } from '../core/format.js';
import { ApiError } from '../core/api.js';

const { usuario, main } = await iniciarPagina({ roles: ['ADMIN', 'NUTRICIONISTA', 'ENTRENADOR'], titulo: 'Agenda' });
const id = Number(new URLSearchParams(location.search).get('id')) || usuario.profesionalId;
const prof = await api.get(`/profesionales/${id}`);
if (!usaAgenda(usuario) || prof.tipo === 'ENTRENADOR') {
  // Los turnos son sólo de nutrición: el entrenador no tiene agenda (v1.1)
  main.append(encabezado(`Agenda de ${prof.nombreVisible}`), h('div', { class: 'aviso' }, 'Los entrenadores no tienen agenda: los turnos son sólo de nutrición.'));
  throw new Error('sin agenda');
}
let franjas = await api.get(`/profesionales/${id}/disponibilidad`);

const editor = h('div');
const bloqueosEl = h('div');
main.append(encabezado(`Agenda de ${prof.nombreVisible}`, usuario.roles.includes('ADMIN') ? [['Profesionales', '/admin/profesionales.html'], [prof.apellido, `/admin/profesional.html?id=${id}`], ['Agenda']] : []),
  h('p', { class: 'suave' }, `Duración de cada turno: ${prof.duracionTurnoMin} minutos.`),
  h('div', { class: 'tarjeta' }, h('h2', {}, 'Franjas semanales'), h('p', { class: 'suave' }, 'Podés cargar varias franjas por día; no pueden superponerse.'), editor),
  h('div', { class: 'tarjeta' }, h('div', { class: 'fila entre' }, h('h2', {}, 'Bloqueos (vacaciones, feriados, ausencias)'),
    h('button', { class: 'btn chico', onclick: nuevoBloqueo }, '+ Nuevo bloqueo')), bloqueosEl));

function dibujarEditor() {
  const filas = franjas.map((f, i) => h('tr', {},
    h('td', {}, h('select', { onchange: (e) => { f.diaSemana = Number(e.target.value); } }, DIAS.slice(1).map((d, j) => h('option', { value: j + 1, selected: f.diaSemana === j + 1 }, d)))),
    h('td', {}, h('input', { type: 'time', value: String(f.horaDesde).substring(0, 5), onchange: (e) => { f.horaDesde = e.target.value; } })),
    h('td', {}, h('input', { type: 'time', value: String(f.horaHasta).substring(0, 5), onchange: (e) => { f.horaHasta = e.target.value; } })),
    h('td', {}, h('input', { type: 'text', maxLength: 120, value: f.ubicacion || '', placeholder: 'Consultorio / sede', onchange: (e) => { f.ubicacion = e.target.value; } })),
    h('td', {}, h('button', { class: 'btn chico peligro secundario', onclick: () => { franjas.splice(i, 1); dibujarEditor(); } }, 'Quitar'))));
  vaciar(editor,
    h('div', { class: 'tabla-scroll' }, h('table', {}, h('thead', {}, h('tr', {}, ['Día', 'Desde', 'Hasta', 'Ubicación', ''].map(t => h('th', {}, t)))),
      h('tbody', {}, filas.length ? filas : h('tr', {}, h('td', { colspan: 5, class: 'vacio' }, 'Sin franjas cargadas'))))),
    h('div', { class: 'acciones' },
      h('button', { class: 'btn secundario', onclick: () => { franjas.push({ diaSemana: 1, horaDesde: '09:00', horaHasta: '13:00', ubicacion: '' }); dibujarEditor(); } }, '+ Agregar franja'),
      h('button', { class: 'btn', onclick: guardar }, 'Guardar agenda')));
}

async function guardar() {
  try {
    franjas = await api.put(`/profesionales/${id}/disponibilidad`, {
      franjas: franjas.map(f => ({ diaSemana: f.diaSemana, horaDesde: String(f.horaDesde).substring(0, 5), horaHasta: String(f.horaHasta).substring(0, 5), ubicacion: f.ubicacion || null })),
    });
    toast('Agenda guardada');
    dibujarEditor();
  } catch (e) { manejarError(e); }
}

async function cargarBloqueos() {
  const lista = await api.get(`/profesionales/${id}/bloqueos`);
  vaciar(bloqueosEl, tabla([
    { titulo: 'Desde', render: b => fmt.fechaHora(b.desde) }, { titulo: 'Hasta', render: b => fmt.fechaHora(b.hasta) },
    { titulo: 'Motivo', campo: 'motivo' },
    { titulo: '', render: b => h('button', { class: 'btn chico peligro secundario', onclick: async () => {
      if (!(await confirmar('¿Eliminar el bloqueo?', { texto: 'Eliminar', peligro: true }))) return;
      try { await api.del(`/profesionales/${id}/bloqueos/${b.id}`); cargarBloqueos(); } catch (e) { manejarError(e); }
    } }, 'Eliminar') },
  ], lista, { vacio: 'Sin bloqueos' }));
}

async function nuevoBloqueo() {
  const form = h('form', {}, h('div', { class: 'campos c2' },
    campo({ nombre: 'fd', etiqueta: 'Desde (fecha)', tipo: 'date', requerido: true, valor: hoyISO() }),
    campo({ nombre: 'hd', etiqueta: 'Desde (hora)', tipo: 'time', requerido: true, valor: '00:00' }),
    campo({ nombre: 'fh', etiqueta: 'Hasta (fecha)', tipo: 'date', requerido: true, valor: hoyISO() }),
    campo({ nombre: 'hh', etiqueta: 'Hasta (hora)', tipo: 'time', requerido: true, valor: '23:59' }),
    campo({ nombre: 'motivo', etiqueta: 'Motivo', maxLength: 200, ancho: true })));
  const enviar = async (confirmarCancelacion) => {
    const d = Object.fromEntries(new FormData(form));
    return api.post(`/profesionales/${id}/bloqueos`, { desde: aIsoArgentina(d.fd, d.hd), hasta: aIsoArgentina(d.fh, d.hh), motivo: d.motivo, confirmarCancelacion });
  };
  await modal({
    titulo: 'Nuevo bloqueo de agenda', cuerpo: form,
    botones: [{ texto: 'Cancelar', clase: 'secundario' }, { texto: 'Guardar', accion: async () => {
      try {
        await enviar(false);
      } catch (e) {
        if (e instanceof ApiError && e.code === 'TURNOS_AFECTADOS') {
          if (!(await confirmar(e.message, { texto: 'Cancelar turnos y bloquear', peligro: true }))) return false;
          const r = await enviar(true);
          toast(`Bloqueo creado. Turnos cancelados: ${r.turnosCancelados}`);
          cargarBloqueos();
          return true;
        }
        throw e;
      }
      toast('Bloqueo creado');
      cargarBloqueos();
    } }],
  });
}

dibujarEditor();
cargarBloqueos();
