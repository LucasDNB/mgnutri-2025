// Spec 05 Editor de rutina: días, ejercicios del catálogo (búsqueda y filtro por grupo), bloques y esquemas
// («Entrada en calor» › «3 rondas», «WOD» › «21-15-9»), reordenar y duplicar día.
import { api, qs, ApiError } from '../core/api.js';
import { iniciarPagina, encabezado } from '../core/pagina.js';
import { h, toast, modal, confirmar, campo, leerForm, manejarError, vaciar, demorar, param, chip } from '../core/ui.js';
import { hoyISO, DIAS } from '../core/format.js';
import { agrupar } from '../components/bloques.js';

const { main } = await iniciarPagina({ roles: ['ENTRENADOR', 'ADMIN'], titulo: 'Rutina' });
const idRutina = param('id');
const soloLectura = param('ver') === '1';
const rutina = idRutina ? await api.get(`/rutinas/${idRutina}`) : null;
const pacienteId = rutina ? rutina.pacienteId : Number(param('pacienteId'));
const nombrePaciente = rutina ? rutina.paciente : await api.get(`/pacientes/${pacienteId}`).then(p => `${p.nombre} ${p.apellido}`);
const grupos = await api.get('/grupos-musculares');

let dias = rutina ? rutina.dias.map(d => ({ diaSemana: d.diaSemana, nombre: d.nombre || '', ejercicios: d.ejercicios.map(e => ({
  id: e.id, ejercicioId: e.ejercicioId, nombre: e.nombre, grupo: e.grupoMuscular, video: !!e.videoUrl, series: e.series, repeticiones: e.repeticiones,
  descansoSeg: e.descansoSeg, cargaSugerida: e.cargaSugerida || '', notas: e.notas || '', bloque: e.bloque || null, esquema: e.esquema || null })) })) : [];

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
    d.ejercicios.length ? agrupar(d.ejercicios).map(sec => seccion(d, sec)) : h('p', { class: 'suave' }, 'Sin ejercicios.'),
    soloLectura ? null : h('div', { class: 'fila' },
      h('button', { class: 'btn chico', onclick: () => agregarEjercicio(d, { bloque: null, esquema: null, en: d.ejercicios.length }) }, '+ Agregar ejercicio'),
      h('button', { class: 'btn chico secundario', onclick: () => nuevoBloque(d) }, '+ Agregar bloque'))))
    : h('div', { class: 'tarjeta vacio' }, 'Agregá al menos un día de entrenamiento.'));
}

/** Un bloque («Entrada en calor») con sus esquemas («3 rondas»), o los ejercicios sueltos del día. */
function seccion(d, sec) {
  const despuesDe = (items) => items[items.length - 1].i + 1;
  const todos = sec.grupos.flatMap(g => g.items);
  const grupos = sec.grupos.map(g => {
    const tarjetas = g.items.map(x => tarjetaEjercicio(d, x.e, x.i));
    if (!g.esquema) return tarjetas;
    return h('div', { class: 'esquema' },
      h('div', { class: 'fila entre esquema-titulo' },
        soloLectura ? g.esquema : renombrar(g.esquema, 'Ej.: 3 rondas, 21-15-9, AMRAP 12 min', v => g.items.forEach(x => { x.e.esquema = v; })),
        soloLectura ? null : h('button', { class: 'btn chico secundario', onclick: () => agregarEjercicio(d, { bloque: sec.bloque, esquema: g.esquema, en: despuesDe(g.items) }) }, '+ Ejercicio')),
      tarjetas);
  });
  if (!sec.bloque) return grupos;
  return h('section', { class: 'bloque' },
    h('div', { class: 'fila entre' },
      soloLectura ? h('h3', {}, sec.bloque) : renombrar(sec.bloque, 'Ej.: Entrada en calor, WOD', v => todos.forEach(x => { x.e.bloque = v; }), 'titulo-bloque'),
      soloLectura ? null : h('div', { class: 'fila' },
        h('button', { class: 'btn chico secundario', onclick: () => agregarEjercicio(d, { bloque: sec.bloque, esquema: null, en: despuesDe(todos) }) }, '+ Ejercicio'),
        h('button', { class: 'btn chico secundario', onclick: () => nuevoEsquema(d, sec.bloque, despuesDe(todos)) }, '+ Esquema'),
        h('button', { class: 'btn chico peligro secundario', onclick: () => quitarBloque(d, sec.bloque, todos) }, 'Quitar bloque'))),
    grupos);
}

/** Campo para renombrar un bloque o un esquema (no puede quedar vacío). */
function renombrar(valor, ph, aplicar, clase = '') {
  return h('input', { type: 'text', maxLength: 60, value: valor, placeholder: ph, class: clase, 'aria-label': ph,
    onchange: (ev) => {
      const v = ev.target.value.trim();
      if (!v) { ev.target.value = valor; toast('El nombre no puede quedar vacío', 'advertencia'); return; }
      aplicar(v);
      dibujar();
    } });
}

function tarjetaEjercicio(d, e, i) {
  // ↑/↓ mueven el ejercicio dentro de su mismo bloque y esquema
  const vecino = (j) => d.ejercicios[j] && (d.ejercicios[j].bloque || null) === (e.bloque || null) && (d.ejercicios[j].esquema || null) === (e.esquema || null);
  const mover = (j) => { [d.ejercicios[j], d.ejercicios[i]] = [d.ejercicios[i], d.ejercicios[j]]; dibujar(); };
  return h('div', { class: 'ejercicio-card' },
    h('div', { class: 'fila entre' }, h('strong', {}, e.nombre), h('span', { class: 'suave' }, e.grupo, e.video ? ' · 🎬 video' : ''),
      soloLectura ? null : h('div', { class: 'fila' },
        h('button', { class: 'btn chico secundario', disabled: !vecino(i - 1), 'aria-label': 'Subir', onclick: () => mover(i - 1) }, '↑'),
        h('button', { class: 'btn chico secundario', disabled: !vecino(i + 1), 'aria-label': 'Bajar', onclick: () => mover(i + 1) }, '↓'),
        h('button', { class: 'btn chico peligro secundario', onclick: () => { d.ejercicios.splice(i, 1); dibujar(); } }, '✕'))),
    h('div', { class: 'campos c3', style: 'margin-top:.5rem' },
      num(e.bloque ? 'Series (opcional)' : 'Series', e, 'series', 1, 20),
      txt(e.bloque ? 'Reps o tiempo (opcional)' : 'Repeticiones', e, 'repeticiones', 20, e.bloque ? '30, 5 min' : '10, 8-12, 30 s'),
      num('Descanso (s)', e, 'descansoSeg', 0, 900),
      txt('Carga sugerida', e, 'cargaSugerida', 30, 'RPE 8, 20 kg'), txt('Notas', e, 'notas', 300, '')),
    vecino(i - 1) && d.ejercicios[i - 1].ejercicioId === e.ejercicioId ? h('div', { class: 'aviso advertencia' }, 'El mismo ejercicio aparece dos veces seguidas.') : null);
}

async function pedirNombre(titulo, ph, ayuda) {
  const input = h('input', { type: 'text', maxLength: 60, placeholder: ph });
  const ok = await modal({ titulo, cuerpo: h('div', { class: 'columna' }, h('p', { class: 'suave' }, ayuda), input),
    botones: [{ texto: 'Cancelar', clase: 'secundario', valor: false }, { texto: 'Continuar', valor: true }] });
  return ok ? input.value.trim() : null;
}

async function nuevoBloque(d) {
  const nombre = await pedirNombre(`Nuevo bloque del ${DIAS[d.diaSemana]}`, 'Ej.: Entrada en calor, WOD',
    'Agrupá ejercicios bajo un título. Después podés sumar esquemas como «3 rondas» o «21-15-9».');
  if (nombre === null) return;
  if (!nombre) { toast('Ingresá el nombre del bloque', 'advertencia'); return; }
  if (d.ejercicios.some(e => e.bloque === nombre)) { toast('Ya hay un bloque con ese nombre en el día', 'advertencia'); return; }
  await agregarEjercicio(d, { bloque: nombre, esquema: null, en: d.ejercicios.length });
}

async function nuevoEsquema(d, bloque, en) {
  const esquema = await pedirNombre(`Nuevo esquema en «${bloque}»`, 'Ej.: 3 rondas, 21-15-9, AMRAP 12 min',
    'Los ejercicios que agregues se hacen con este esquema.');
  if (esquema === null) return;
  if (!esquema) { toast('Ingresá el esquema', 'advertencia'); return; }
  await agregarEjercicio(d, { bloque, esquema, en });
}

async function quitarBloque(d, bloque, items) {
  const r = await modal({ titulo: `Quitar el bloque «${bloque}»`, cuerpo: h('p', {}, '¿Qué hacemos con sus ejercicios?'),
    botones: [{ texto: 'Cancelar', clase: 'secundario', valor: null }, { texto: 'Dejarlos sueltos', clase: 'secundario', valor: 'sueltos' },
      { texto: 'Quitarlos', clase: 'peligro', valor: 'quitar' }] });
  if (r === 'sueltos') {
    // Fuera de un bloque, series y repeticiones vuelven a ser obligatorias
    items.forEach(x => { x.e.bloque = null; x.e.esquema = null; x.e.series = x.e.series || 3; x.e.repeticiones = x.e.repeticiones || '10'; });
  } else if (r === 'quitar') {
    const quitar = new Set(items.map(x => x.e));
    d.ejercicios = d.ejercicios.filter(e => !quitar.has(e));
  } else return;
  dibujar();
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

async function agregarEjercicio(d, destino) {
  let en = destino.en;
  const q = h('input', { type: 'search', placeholder: 'Buscar ejercicio…' });
  const g = h('select', {}, h('option', { value: '' }, 'Todos los grupos'), grupos.map(x => h('option', { value: x.id }, x.nombre)));
  const res = h('div', { class: 'columna' });
  const cargar = async () => {
    const r = await api.get(`/ejercicios${qs({ q: q.value, grupoMuscularId: g.value, activo: true, size: 15 })}`);
    vaciar(res, r.content.map(e => h('button', { type: 'button', class: 'btn secundario', style: 'justify-content:space-between', onclick: () => {
      const enBloque = !!destino.bloque;
      d.ejercicios.splice(en++, 0, { id: null, ejercicioId: e.id, nombre: e.nombre, grupo: e.grupoMuscular, video: !!e.videoUrl,
        series: enBloque ? null : 3, repeticiones: enBloque ? '' : '10', descansoSeg: enBloque ? null : 60, cargaSugerida: '', notas: '',
        bloque: destino.bloque, esquema: destino.esquema });
      dibujar();
      toast(`${e.nombre} agregado`);
    } }, h('span', {}, e.nombre), h('small', {}, `${e.grupoMuscular} · ${e.dificultad.toLowerCase()}${e.videoUrl ? ' · 🎬' : ''}`))));
  };
  q.addEventListener('input', demorar(cargar));
  g.addEventListener('change', cargar);
  cargar();
  await modal({ titulo: destino.bloque ? `Agregar a «${destino.esquema || destino.bloque}»` : `Agregar ejercicio al ${DIAS[d.diaSemana]}`, cuerpo: h('div', {}, h('div', { class: 'fila' }, q, g), res), ancho: true, botones: [{ texto: 'Listo', valor: null }] });
}

async function guardar() {
  const enc = leerForm(cab);
  if (!enc.nombre) { toast('Ingresá el nombre de la rutina', 'advertencia'); return; }
  if (!dias.length || dias.some(d => !d.ejercicios.length)) { toast('Cada día debe tener al menos un ejercicio', 'advertencia'); return; }
  // Fuera de un bloque, series y repeticiones son obligatorias; dentro, opcionales
  if (dias.some(d => d.ejercicios.some(e => (e.series != null && (e.series < 1 || e.series > 20)) || (!e.bloque && (!e.series || !String(e.repeticiones || '').trim()))))) {
    toast('Revisá series (1 a 20) y repeticiones de los ejercicios sueltos', 'advertencia'); return;
  }
  const cuerpo = (finalizarActual) => ({ ...enc, duracionSemanas: Number(enc.duracionSemanas), finalizarActual, version: rutina?.version,
    dias: dias.map(d => ({ diaSemana: d.diaSemana, nombre: d.nombre || null, ejercicios: d.ejercicios.map(e => ({ id: e.id, ejercicioId: e.ejercicioId, series: e.series,
      repeticiones: String(e.repeticiones ?? '').trim() || null, descansoSeg: e.descansoSeg, cargaSugerida: e.cargaSugerida || null, notas: e.notas || null,
      bloque: e.bloque || null, esquema: e.bloque ? e.esquema || null : null })) })) });
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
