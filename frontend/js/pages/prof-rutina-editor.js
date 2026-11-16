// Spec 05 Editor de rutina: días, ejercicios del catálogo (búsqueda y filtro por grupo), reordenar y duplicar día.
import { api, qs, ApiError } from '../core/api.js';
import { iniciarPagina, encabezado } from '../core/pagina.js';
import { h, toast, modal, confirmar, campo, leerForm, manejarError, vaciar, demorar, param, chip } from '../core/ui.js';
import { hoyISO, DIAS } from '../core/format.js';

const { main } = await iniciarPagina({ roles: ['ENTRENADOR', 'ADMIN'], titulo: 'Rutina' });
const idRutina = param('id');
const soloLectura = param('ver') === '1';
const rutina = idRutina ? await api.get(`/rutinas/${idRutina}`) : null;
const pacienteId = rutina ? rutina.pacienteId : Number(param('pacienteId'));
const nombrePaciente = rutina ? rutina.paciente : await api.get(`/pacientes/${pacienteId}`).then(p => `${p.nombre} ${p.apellido}`);
const grupos = await api.get('/grupos-musculares');

let dias = rutina ? rutina.dias.map(d => ({ diaSemana: d.diaSemana, nombre: d.nombre || '', ejercicios: d.ejercicios.map(e => ({
  id: e.id, ejercicioId: e.ejercicioId, nombre: e.nombre, grupo: e.grupoMuscular, video: !!e.videoUrl, series: e.series, repeticiones: e.repeticiones,
  descansoSeg: e.descansoSeg, cargaSugerida: e.cargaSugerida || '', notas: e.notas || '' })) })) : [];

const cab = h('form', { class: 'tarjeta' }, h('div', { class: 'campos c3' },
  campo({ nombre: 'nombre', etiqueta: 'Nombre', requerido: true, maxLength: 120, valor: rutina?.nombre || 'Rutina', disabled: soloLectura }),
  campo({ nombre: 'objetivo', etiqueta: 'Objetivo', maxLength: 300, valor: rutina?.objetivo, disabled: soloLectura }),
  campo({ nombre: 'fechaInicio', etiqueta: 'Fecha de inicio', tipo: 'date', valor: rutina?.fechaInicio || hoyISO(), disabled: soloLectura }),
  campo({ nombre: 'duracionSemanas', etiqueta: 'Duración (semanas)', tipo: 'number', min: 1, max: 52, valor: rutina?.duracionSemanas || 6, disabled: soloLectura }),
  campo({ nombre: 'observaciones', etiqueta: 'Observaciones', tipo: 'textarea', maxLength: 1000, valor: rutina?.observaciones, ancho: true, disabled: soloLectura })));
const diasEl = h('div');
main.append(encabezado(rutina ? `${soloLectura ? 'Rutina' : 'Editar rutina'}: ${rutina.nombre}` : 'Nueva rutina',
  [['Pacientes', '/comun/pacientes.html'], [nombrePaciente, `/comun/paciente.html?id=${pacienteId}&tab=rutinas`], ['Rutina']]),
rutina ? h('p', {}, 'Estado: ', chip(rutina.estado, rutina.estado)) : null, cab, diasEl,
h('div', { class: 'acciones' }, h('a', { class: 'btn secundario', href: `/comun/paciente.html?id=${pacienteId}&tab=rutinas` }, soloLectura ? 'Volver' : 'Cancelar'),
  soloLectura ? null : h('button', { class: 'btn secundario', onclick: agregarDia }, '+ Agregar día'),
  soloLectura ? null : h('button', { class: 'btn', onclick: guardar }, rutina ? 'Guardar cambios' : 'Confirmar rutina')));

function dibujar() {
  vaciar(diasEl, dias.length ? dias.sort((a, b) => a.diaSemana - b.diaSemana).map((d, di) => h('div', { class: 'tarjeta' },
    h('div', { class: 'fila entre' },
      h('div', { class: 'fila' }, h('h2', { style: 'margin:0' }, DIAS[d.diaSemana]),
        soloLectura ? (d.nombre ? `– ${d.nombre}` : '') : h('input', { type: 'text', maxLength: 80, value: d.nombre, placeholder: 'Ej.: Tren superior', onchange: (e) => { d.nombre = e.target.value; } })),
      soloLectura ? null : h('div', { class: 'fila' },
        h('button', { class: 'btn chico secundario', onclick: () => duplicarDia(d) }, 'Duplicar día'),
        h('button', { class: 'btn chico peligro secundario', onclick: async () => { if (await confirmar(`¿Quitar el ${DIAS[d.diaSemana]}?`, { peligro: true, texto: 'Quitar' })) { dias.splice(di, 1); dibujar(); } } }, 'Quitar día'))),
    d.ejercicios.length ? d.ejercicios.map((e, i) => h('div', { class: 'ejercicio-card' },
      h('div', { class: 'fila entre' }, h('strong', {}, `${i + 1}. ${e.nombre}`), h('span', { class: 'suave' }, e.grupo, e.video ? ' · 🎬 video' : ''),
        soloLectura ? null : h('div', { class: 'fila' },
          h('button', { class: 'btn chico secundario', disabled: i === 0, 'aria-label': 'Subir', onclick: () => { [d.ejercicios[i - 1], d.ejercicios[i]] = [d.ejercicios[i], d.ejercicios[i - 1]]; dibujar(); } }, '↑'),
          h('button', { class: 'btn chico secundario', disabled: i === d.ejercicios.length - 1, 'aria-label': 'Bajar', onclick: () => { [d.ejercicios[i + 1], d.ejercicios[i]] = [d.ejercicios[i], d.ejercicios[i + 1]]; dibujar(); } }, '↓'),
          h('button', { class: 'btn chico peligro secundario', onclick: () => { d.ejercicios.splice(i, 1); dibujar(); } }, '✕'))),
      h('div', { class: 'campos c3', style: 'margin-top:.5rem' },
        num('Series', e, 'series', 1, 20), txt('Repeticiones', e, 'repeticiones', 20, '10, 8-12, 30 s'), num('Descanso (s)', e, 'descansoSeg', 0, 900),
        txt('Carga sugerida', e, 'cargaSugerida', 30, 'RPE 8, 20 kg'), txt('Notas', e, 'notas', 300, '')),
      i > 0 && d.ejercicios[i - 1].ejercicioId === e.ejercicioId ? h('div', { class: 'aviso advertencia' }, 'El mismo ejercicio aparece dos veces seguidas.') : null))
      : h('p', { class: 'suave' }, 'Sin ejercicios.'),
    soloLectura ? null : h('button', { class: 'btn chico', onclick: () => agregarEjercicio(d) }, '+ Agregar ejercicio')))
    : h('div', { class: 'tarjeta vacio' }, 'Agregá al menos un día de entrenamiento.'));
}

function num(etiqueta, e, k, min, max) {
  return h('div', { class: 'campo' }, h('label', {}, etiqueta), h('input', { type: 'number', min, max, value: e[k] ?? '', disabled: soloLectura, onchange: (ev) => { e[k] = ev.target.value === '' ? null : Number(ev.target.value); } }));
}
function txt(etiqueta, e, k, max, ph) {
  return h('div', { class: 'campo' }, h('label', {}, etiqueta), h('input', { type: 'text', maxLength: max, value: e[k] ?? '', placeholder: ph, disabled: soloLectura, onchange: (ev) => { e[k] = ev.target.value; } }));
}

async function agregarDia() {
  const libres = [1, 2, 3, 4, 5, 6, 7].filter(n => !dias.some(d => d.diaSemana === n));
  if (!libres.length) { toast('Ya están los 7 días', 'advertencia'); return; }
  const sel = h('select', {}, libres.map(n => h('option', { value: n }, DIAS[n])));
  const ok = await modal({ titulo: 'Agregar día', cuerpo: sel, botones: [{ texto: 'Cancelar', clase: 'secundario', valor: false }, { texto: 'Agregar', valor: true }] });
  if (ok) { dias.push({ diaSemana: Number(sel.value), nombre: '', ejercicios: [] }); dibujar(); }
}

async function duplicarDia(d) {
  const libres = [1, 2, 3, 4, 5, 6, 7].filter(n => !dias.some(x => x.diaSemana === n));
  if (!libres.length) { toast('No hay días libres', 'advertencia'); return; }
  const sel = h('select', {}, libres.map(n => h('option', { value: n }, DIAS[n])));
  const ok = await modal({ titulo: `Duplicar ${DIAS[d.diaSemana]} en…`, cuerpo: sel, botones: [{ texto: 'Cancelar', clase: 'secundario', valor: false }, { texto: 'Duplicar', valor: true }] });
  if (ok) { dias.push({ diaSemana: Number(sel.value), nombre: d.nombre, ejercicios: d.ejercicios.map(e => ({ ...e, id: null })) }); dibujar(); }
}

async function agregarEjercicio(d) {
  const q = h('input', { type: 'search', placeholder: 'Buscar ejercicio…' });
  const g = h('select', {}, h('option', { value: '' }, 'Todos los grupos'), grupos.map(x => h('option', { value: x.id }, x.nombre)));
  const res = h('div', { class: 'columna' });
  const cargar = async () => {
    const r = await api.get(`/ejercicios${qs({ q: q.value, grupoMuscularId: g.value, activo: true, size: 15 })}`);
    vaciar(res, r.content.map(e => h('button', { type: 'button', class: 'btn secundario', style: 'justify-content:space-between', onclick: () => {
      d.ejercicios.push({ id: null, ejercicioId: e.id, nombre: e.nombre, grupo: e.grupoMuscular, video: !!e.videoUrl, series: 3, repeticiones: '10', descansoSeg: 60, cargaSugerida: '', notas: '' });
      dibujar();
      toast(`${e.nombre} agregado`);
    } }, h('span', {}, e.nombre), h('small', {}, `${e.grupoMuscular} · ${e.dificultad.toLowerCase()}${e.videoUrl ? ' · 🎬' : ''}`))));
  };
  q.addEventListener('input', demorar(cargar));
  g.addEventListener('change', cargar);
  cargar();
  await modal({ titulo: `Agregar ejercicio al ${DIAS[d.diaSemana]}`, cuerpo: h('div', {}, h('div', { class: 'fila' }, q, g), res), ancho: true, botones: [{ texto: 'Listo', valor: null }] });
}

async function guardar() {
  const enc = leerForm(cab);
  if (!enc.nombre) { toast('Ingresá el nombre de la rutina', 'advertencia'); return; }
  if (!dias.length || dias.some(d => !d.ejercicios.length)) { toast('Cada día debe tener al menos un ejercicio', 'advertencia'); return; }
  if (dias.some(d => d.ejercicios.some(e => !e.series || e.series < 1 || e.series > 20 || !String(e.repeticiones || '').trim()))) { toast('Revisá series (1 a 20) y repeticiones', 'advertencia'); return; }
  const cuerpo = (finalizarActual) => ({ ...enc, duracionSemanas: Number(enc.duracionSemanas), finalizarActual, version: rutina?.version,
    dias: dias.map(d => ({ diaSemana: d.diaSemana, nombre: d.nombre || null, ejercicios: d.ejercicios.map(e => ({ id: e.id, ejercicioId: e.ejercicioId, series: e.series,
      repeticiones: String(e.repeticiones), descansoSeg: e.descansoSeg, cargaSugerida: e.cargaSugerida || null, notas: e.notas || null })) })) });
  const enviar = (f) => (rutina ? api.put(`/rutinas/${rutina.id}`, cuerpo(f)) : api.post(`/pacientes/${pacienteId}/rutinas`, cuerpo(f)));
  try {
    let r;
    try {
      r = await enviar(false);
    } catch (e) {
      if (e instanceof ApiError && e.code === 'RUTINA_ACTIVA_EXISTENTE') {
        if (!(await confirmar(e.message, { texto: 'Finalizar la actual y confirmar' }))) return;
        r = await enviar(true);
      } else throw e;
    }
    (r.advertencias || []).forEach(a => toast(a, 'advertencia', 8000));
    toast('Rutina guardada: el paciente ya puede verla');
    location.href = `/comun/paciente.html?id=${pacienteId}&tab=rutinas`;
  } catch (e) { manejarError(e); }
}

dibujar();
