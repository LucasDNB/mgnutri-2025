// Spec 04 Editor de plan alimentario: grilla semanal, ítems del catálogo, totales, borrador / confirmar,
// generación automática (CU-14) con indicadores de desvío.
import { api, qs, ApiError } from '../core/api.js';
import { iniciarPagina, encabezado } from '../core/pagina.js';
import { h, toast, modal, confirmar, campo, leerForm, manejarError, vaciar, demorar, param, chip, ayuda } from '../core/ui.js';
import { fmt, hoyISO, DIAS } from '../core/format.js';

const { main } = await iniciarPagina({ roles: ['NUTRICIONISTA', 'ADMIN'], titulo: 'Plan alimentario' });
const TIPOS = [['DESAYUNO', 'Desayuno'], ['COLACION_MANANA', 'Colación mañana'], ['ALMUERZO', 'Almuerzo'], ['MERIENDA', 'Merienda'], ['COLACION_TARDE', 'Colación tarde'], ['CENA', 'Cena']];
const etiquetaTipo = Object.fromEntries(TIPOS);
const idPlan = param('id');
const soloLectura = param('ver') === '1';
let plan = idPlan ? await api.get(`/planes-alimentarios/${idPlan}`) : null;
const pacienteId = plan ? plan.paciente.id : Number(param('pacienteId'));
const paciente = plan ? plan.paciente : await api.get(`/pacientes/${pacienteId}`).then(p => ({ id: p.id, nombre: `${p.nombre} ${p.apellido}`, edad: p.edad, objetivo: p.objetivo }));
const cacheAlimentos = {};

// Estado: celdas[`${dia}-${tipo}`] = {descripcion, items:[{alimentoId, alimento, gramos, cantidadCasera, macros}], desvio}
let celdas = {};
let tiposActivos = new Set(['DESAYUNO', 'ALMUERZO', 'MERIENDA', 'CENA']);
let encabezadoPlan = { nombre: `Plan ${new Date().toLocaleDateString('es-AR', { month: 'long', year: 'numeric' })}`, fechaInicio: hoyISO(), duracionSemanas: 4, observaciones: '', modo: 'MANUAL' };
let objetivos = null;
let parametrosGeneracion = null;
let totalesServidor = null;

function cargarDesde(comidas) {
  celdas = {};
  for (const c of comidas) {
    celdas[`${c.diaSemana}-${c.tipo}`] = { descripcion: c.descripcion || '', items: c.items.map(i => ({ alimentoId: i.alimentoId, alimento: i.alimento, gramos: Number(i.gramos), cantidadCasera: i.cantidadCasera, macros: i.macros })), desvio: c.desvio };
    tiposActivos.add(c.tipo);
    (c.items || []).forEach(i => { cacheAlimentos[i.alimentoId] = cacheAlimentos[i.alimentoId] || null; });
  }
}
if (plan) {
  encabezadoPlan = { nombre: plan.nombre, fechaInicio: plan.fechaInicio, duracionSemanas: plan.duracionSemanas, observaciones: plan.observaciones || '', modo: plan.modo };
  if (plan.kcalObjetivo) objetivos = { kcal: plan.kcalObjetivo, pctHc: plan.pctHc, pctProteinas: plan.pctProteinas, pctGrasas: plan.pctGrasas };
  parametrosGeneracion = plan.parametrosGeneracion;
  tiposActivos = new Set();
  cargarDesde(plan.comidas);
  if (!tiposActivos.size) tiposActivos = new Set(['DESAYUNO', 'ALMUERZO', 'MERIENDA', 'CENA']);
}

const cabeceraEl = h('div');
const grillaEl = h('div');
const totalesEl = h('div');
const accionesEl = h('div', { class: 'acciones' });
main.append(encabezado(plan ? `${soloLectura ? 'Plan' : 'Editar plan'}: ${plan.nombre}` : 'Nuevo plan alimentario',
  [['Pacientes', '/comun/pacientes.html'], [paciente.nombre, `/comun/paciente.html?id=${pacienteId}&tab=planes`], ['Plan']]),
h('div', { class: 'aviso' }, `${paciente.nombre} · ${paciente.edad} años${paciente.objetivo ? ' · Objetivo: ' + paciente.objetivo : ''}${paciente.ultimoPeso ? ' · Último peso: ' + fmt.numero(paciente.ultimoPeso) + ' kg' : ''}`),
plan ? h('p', {}, 'Estado: ', chip(plan.estado, plan.estado)) : null,
cabeceraEl, h('div', { class: 'tarjeta' }, h('div', { class: 'fila entre' }, h('h2', {}, 'Grilla semanal'),
  soloLectura ? null : h('div', { class: 'fila' },
    h('button', { class: 'btn chico secundario', onclick: agregarTipo }, '+ Tipo de comida'),
    h('button', { class: 'btn chico secundario', onclick: replicarDia }, 'Replicar un día a toda la semana'),
    h('button', { class: 'btn chico', onclick: abrirGenerador }, 'Generar automáticamente'))),
h('p', { class: 'suave' }, soloLectura ? '' : 'Tocá una celda para editarla. Al confirmar, cada día debe tener al menos 4 comidas con contenido.'),
grillaEl), totalesEl, accionesEl);

function dibujarCabecera() {
  const f = h('form', { class: 'tarjeta' }, h('div', { class: 'campos c3' },
    campo({ nombre: 'nombre', etiqueta: 'Nombre del plan', valor: encabezadoPlan.nombre, maxLength: 120, disabled: soloLectura }),
    campo({ nombre: 'fechaInicio', etiqueta: 'Fecha de inicio', tipo: 'date', valor: encabezadoPlan.fechaInicio, disabled: soloLectura }),
    campo({ nombre: 'duracionSemanas', etiqueta: 'Duración (semanas)', tipo: 'number', min: 1, max: 52, valor: encabezadoPlan.duracionSemanas, disabled: soloLectura }),
    campo({ nombre: 'observaciones', etiqueta: 'Observaciones generales', tipo: 'textarea', maxLength: 1000, valor: encabezadoPlan.observaciones, ancho: true, disabled: soloLectura })));
  f.addEventListener('input', () => { Object.assign(encabezadoPlan, leerForm(f)); });
  vaciar(cabeceraEl, f);
}

function totalesCelda(c) {
  return c.items.reduce((t, i) => ({ kcal: t.kcal + (i.macros?.kcal || 0), hc: t.hc + (i.macros?.hc || 0), proteinas: t.proteinas + (i.macros?.proteinas || 0), grasas: t.grasas + (i.macros?.grasas || 0) }),
    { kcal: 0, hc: 0, proteinas: 0, grasas: 0 });
}

function dibujarGrilla() {
  const tipos = TIPOS.filter(([t]) => tiposActivos.has(t));
  const tablaEl = h('table', { class: 'plan-grilla' }, h('thead', {}, h('tr', {}, h('th', {}, 'Comida'), DIAS.slice(1).map(d => h('th', {}, d)))),
    h('tbody', {}, tipos.map(([t, et]) => h('tr', {}, h('td', { class: 'tipo' }, et, soloLectura ? null : h('div', {}, h('button', { class: 'btn link', onclick: () => quitarTipo(t), title: 'Quitar tipo de comida' }, 'quitar'))),
      [1, 2, 3, 4, 5, 6, 7].map(d => {
        const c = celdas[`${d}-${t}`];
        const tot = c ? totalesCelda(c) : null;
        return h('td', { class: 'celda-plan', onclick: () => soloLectura ? null : editarCelda(d, t), tabindex: soloLectura ? null : 0, onkeydown: (e) => { if (e.key === 'Enter' && !soloLectura) editarCelda(d, t); } },
          c ? [c.descripcion || c.items.map(i => `${i.alimento} ${i.gramos} g`).join(' + '),
            tot.kcal ? h('span', { class: 'kcal' }, `≈ ${Math.round(tot.kcal)} kcal`) : null,
            c.desvio ? chip(`${c.desvio.kcalPct}%`, c.desvio.nivel) : null] : h('span', { class: 'suave' }, soloLectura ? '' : '+ agregar'));
      })))));
  vaciar(grillaEl, h('div', { class: 'tabla-scroll' }, tablaEl));
  dibujarTotales();
}

function dibujarTotales() {
  const obj = objetivos ? { kcal: objetivos.kcal, hc: objetivos.kcal * objetivos.pctHc / 400, proteinas: objetivos.kcal * objetivos.pctProteinas / 400, grasas: objetivos.kcal * objetivos.pctGrasas / 900 } : null;
  const filas = [1, 2, 3, 4, 5, 6, 7].map(d => {
    const t = Object.entries(celdas).filter(([k]) => k.startsWith(`${d}-`) && tiposActivos.has(k.split('-')[1])).map(([, c]) => totalesCelda(c))
      .reduce((a, b) => ({ kcal: a.kcal + b.kcal, hc: a.hc + b.hc, proteinas: a.proteinas + b.proteinas, grasas: a.grasas + b.grasas }), { kcal: 0, hc: 0, proteinas: 0, grasas: 0 });
    let nivel = null;
    if (obj && t.kcal) {
      const dk = Math.abs(t.kcal - obj.kcal) / obj.kcal * 100;
      const dm = Math.max(...['hc', 'proteinas', 'grasas'].map(m => Math.abs(t[m] - obj[m]) / obj[m] * 100));
      nivel = [dk <= 10 && dm <= 15 ? 'VERDE' : dk <= 20 && dm <= 30 ? 'AMARILLO' : 'ROJO', Math.round(dk)];
    }
    return h('tr', {}, h('td', {}, DIAS[d]), h('td', { class: 'num' }, Math.round(t.kcal)), h('td', { class: 'num' }, fmt.numero(t.hc)),
      h('td', { class: 'num' }, fmt.numero(t.proteinas)), h('td', { class: 'num' }, fmt.numero(t.grasas)), h('td', {}, nivel ? chip(`desvío ${nivel[1]}% kcal`, nivel[0]) : ''));
  });
  vaciar(totalesEl, h('div', { class: 'tarjeta' }, h('h2', {}, 'Totales por día (ítems estructurados)'),
    obj ? h('p', { class: 'suave' }, `Objetivo: ${obj.kcal} kcal · HC ${Math.round(obj.hc)} g · Proteínas ${Math.round(obj.proteinas)} g · Grasas ${Math.round(obj.grasas)} g`) : null,
    h('div', { class: 'tabla-scroll' }, h('table', {}, h('thead', {}, h('tr', {}, ['Día', 'kcal', 'HC (g)', 'Proteínas (g)', 'Grasas (g)', ''].map(x => h('th', {}, x)))), h('tbody', {}, filas)))));
}

async function buscarAlimentos(texto, tipo) {
  const r = await api.get(`/alimentos${qs({ q: texto, activo: true, aptoPara: texto ? '' : tipo, size: 12 })}`);
  r.content.forEach(a => { cacheAlimentos[a.id] = a; });
  return r.content;
}

function macrosDe(a, g) {
  const f = g / 100;
  return { kcal: Number(a.kcal) * f, hc: Number(a.hc) * f, proteinas: Number(a.proteinas) * f, grasas: Number(a.grasas) * f };
}

async function editarCelda(dia, tipo) {
  const clave = `${dia}-${tipo}`;
  const c = JSON.parse(JSON.stringify(celdas[clave] || { descripcion: '', items: [] }));
  const desc = h('textarea', { maxLength: 500, rows: 3, placeholder: 'Ej.: Infusión con edulcorante + 3 tostadas integrales con queso untable + 1 fruta' });
  desc.value = c.descripcion;
  const itemsEl = h('div');
  const dibujarItems = () => vaciar(itemsEl, c.items.length ? c.items.map((i, idx) => h('div', { class: 'item-alimento' },
    h('span', {}, i.alimento), h('span', { class: 'fila' },
      h('input', { type: 'number', min: 1, max: 2000, step: 5, value: i.gramos, style: 'width:90px', onchange: (e) => {
        const anterior = i.gramos;
        i.gramos = Number(e.target.value);
        const a = cacheAlimentos[i.alimentoId];
        if (a) i.macros = macrosDe(a, i.gramos);
        else if (i.macros && anterior) i.macros = Object.fromEntries(Object.entries(i.macros).map(([k, v]) => [k, v * i.gramos / anterior]));
        dibujarItems();
      } }), 'g',
      h('input', { type: 'text', maxLength: 60, value: i.cantidadCasera || '', placeholder: 'medida casera', style: 'width:150px', onchange: (e) => { i.cantidadCasera = e.target.value; } }),
      h('small', {}, `${Math.round(i.macros?.kcal || 0)} kcal`),
      h('button', { type: 'button', class: 'btn chico peligro secundario', onclick: () => { c.items.splice(idx, 1); dibujarItems(); } }, '✕')))) : h('p', { class: 'suave' }, 'Sin ítems estructurados (opcional).'));
  dibujarItems();
  const busq = h('input', { type: 'search', placeholder: 'Buscar alimento del catálogo…' });
  const opciones = h('div', { class: 'fila' });
  const mostrar = async () => {
    const lista = await buscarAlimentos(busq.value, tipo);
    vaciar(opciones, lista.map(a => h('button', { type: 'button', class: 'btn chico secundario', onclick: () => {
      const g = Number(a.gramosUnidadCasera) || 100;
      c.items.push({ alimentoId: a.id, alimento: a.nombre, gramos: g, cantidadCasera: a.unidadCasera ? `1 × ${a.unidadCasera}` : null, macros: macrosDe(a, g) });
      dibujarItems();
    } }, `${a.nombre} (${Math.round(a.kcal)} kcal/100 g)`)));
  };
  busq.addEventListener('input', demorar(mostrar));
  mostrar();
  const r = await modal({
    titulo: `${DIAS[dia]} · ${etiquetaTipo[tipo]}`, ancho: true,
    cuerpo: h('div', {}, h('div', { class: 'campo' }, h('label', {}, 'Descripción', ayuda('Texto libre como lo verá el paciente. Si cargás ítems estructurados, se calculan kcal y macros.')), desc),
      h('h3', { style: 'margin-top:1rem' }, 'Ítems estructurados'), itemsEl, busq, opciones),
    botones: [{ texto: 'Vaciar celda', clase: 'peligro secundario', valor: 'vaciar' }, { texto: 'Copiar a…', clase: 'secundario', valor: 'copiar' },
      { texto: 'Cancelar', clase: 'secundario', valor: null }, { texto: 'Aceptar', valor: 'ok' }],
  });
  c.descripcion = desc.value.trim();
  if (r === 'vaciar') delete celdas[clave];
  if (r === 'ok' || r === 'copiar') {
    c.desvio = null;
    if (c.descripcion || c.items.length) celdas[clave] = c; else delete celdas[clave];
  }
  if (r === 'copiar' && celdas[clave]) {
    const dias = await elegirDias(`Copiar ${etiquetaTipo[tipo]} del ${DIAS[dia]} a:`);
    dias.forEach(d => { celdas[`${d}-${tipo}`] = JSON.parse(JSON.stringify(celdas[clave])); });
  }
  dibujarGrilla();
}

async function elegirDias(titulo) {
  const cont = h('div', { class: 'columna' }, [1, 2, 3, 4, 5, 6, 7].map(d => h('label', { class: 'check' }, h('input', { type: 'checkbox', value: d }), DIAS[d])));
  const ok = await modal({ titulo, cuerpo: cont, botones: [{ texto: 'Cancelar', clase: 'secundario', valor: false }, { texto: 'Aplicar', valor: true }] });
  return ok ? [...cont.querySelectorAll('input:checked')].map(i => Number(i.value)) : [];
}

async function replicarDia() {
  const sel = h('select', {}, [1, 2, 3, 4, 5, 6, 7].map(d => h('option', { value: d }, DIAS[d])));
  const ok = await modal({ titulo: 'Replicar un día a toda la semana', cuerpo: h('div', {}, h('p', {}, 'Elegí el día de origen. Se reemplazarán las comidas de los demás días.'), sel),
    botones: [{ texto: 'Cancelar', clase: 'secundario', valor: false }, { texto: 'Replicar', valor: true }] });
  if (!ok) return;
  const origen = Number(sel.value);
  for (let d = 1; d <= 7; d++) {
    if (d === origen) continue;
    for (const [t] of TIPOS) {
      const c = celdas[`${origen}-${t}`];
      if (c) celdas[`${d}-${t}`] = JSON.parse(JSON.stringify(c)); else delete celdas[`${d}-${t}`];
    }
  }
  dibujarGrilla();
}

async function agregarTipo() {
  const disponibles = TIPOS.filter(([t]) => !tiposActivos.has(t));
  if (!disponibles.length) { toast('Ya están todos los tipos de comida', 'advertencia'); return; }
  const sel = h('select', {}, disponibles.map(([t, e]) => h('option', { value: t }, e)));
  const ok = await modal({ titulo: 'Agregar tipo de comida', cuerpo: sel, botones: [{ texto: 'Cancelar', clase: 'secundario', valor: false }, { texto: 'Agregar', valor: true }] });
  if (ok) { tiposActivos.add(sel.value); dibujarGrilla(); }
}

async function quitarTipo(t) {
  if (!(await confirmar(`¿Quitar ${etiquetaTipo[t]} de todos los días?`, { texto: 'Quitar', peligro: true }))) return;
  tiposActivos.delete(t);
  for (let d = 1; d <= 7; d++) delete celdas[`${d}-${t}`];
  dibujarGrilla();
}

// ------------------------------------------------------------------ generador (CU-14)
async function abrirGenerador() {
  const DEFAULTS = { 4: { DESAYUNO: 20, ALMUERZO: 35, MERIENDA: 15, CENA: 30 }, 5: { DESAYUNO: 20, COLACION_MANANA: 10, ALMUERZO: 30, MERIENDA: 15, CENA: 25 },
    6: { DESAYUNO: 20, COLACION_MANANA: 10, ALMUERZO: 30, MERIENDA: 10, COLACION_TARDE: 5, CENA: 25 } };
  const sel = new Set(tiposActivos.size >= 4 ? tiposActivos : ['DESAYUNO', 'ALMUERZO', 'MERIENDA', 'CENA']);
  const pctInputs = {};
  const comidasEl = h('div');
  const suma = h('strong');
  const dibujarComidas = () => {
    const def = DEFAULTS[sel.size] || {};
    vaciar(comidasEl, TIPOS.map(([t, e]) => {
      const chk = h('input', { type: 'checkbox', checked: sel.has(t), onchange: () => { if (chk.checked) sel.add(t); else sel.delete(t); dibujarComidas(); } });
      pctInputs[t] = h('input', { type: 'number', min: 1, max: 100, value: def[t] ?? Math.round(100 / sel.size), disabled: !sel.has(t), style: 'width:80px', oninput: actualizarSuma });
      return h('div', { class: 'fila' }, h('label', { class: 'check', style: 'min-width:170px' }, chk, e), pctInputs[t], '%');
    }));
    actualizarSuma();
  };
  function actualizarSuma() {
    const s = [...sel].reduce((a, t) => a + Number(pctInputs[t]?.value || 0), 0);
    suma.textContent = `${s} %`;
    suma.style.color = s === 100 ? 'var(--verde)' : 'var(--rojo)';
  }
  const excluidos = new Map();
  const excEl = h('div', { class: 'fila' });
  const dibujarExc = () => vaciar(excEl, [...excluidos].map(([id, n]) => h('button', { type: 'button', class: 'btn chico peligro secundario', onclick: () => { excluidos.delete(id); dibujarExc(); } }, `${n} ✕`)));
  const busq = h('input', { type: 'search', placeholder: 'Buscar alimento a excluir…' });
  const opc = h('div', { class: 'fila' });
  busq.addEventListener('input', demorar(async () => {
    const l = await buscarAlimentos(busq.value);
    vaciar(opc, l.map(a => h('button', { type: 'button', class: 'btn chico secundario', onclick: () => { excluidos.set(a.id, a.nombre); dibujarExc(); } }, a.nombre)));
  }));
  const macros = h('div', { class: 'campos c3' },
    campo({ nombre: 'kcal', etiqueta: 'kcal diarias', tipo: 'number', min: 800, max: 5000, valor: objetivos?.kcal || 2000 }),
    campo({ nombre: 'pctHc', etiqueta: '% HC', tipo: 'number', min: 5, max: 80, valor: objetivos?.pctHc || 50 }),
    campo({ nombre: 'pctProteinas', etiqueta: '% proteínas', tipo: 'number', min: 5, max: 80, valor: objetivos?.pctProteinas || 20 }),
    campo({ nombre: 'pctGrasas', etiqueta: '% grasas', tipo: 'number', min: 5, max: 80, valor: objetivos?.pctGrasas || 30 }),
    campo({ nombre: 'variedad', etiqueta: 'Variedad', tipo: 'select', opciones: [{ valor: 'DIARIA', texto: 'Menú distinto cada día' }, { valor: 'SEMANAL', texto: 'Mismo menú toda la semana' }] }));
  const sumaMacros = h('strong');
  macros.addEventListener('input', () => {
    const d = leerForm(form);
    const s = (d.pctHc || 0) + (d.pctProteinas || 0) + (d.pctGrasas || 0);
    sumaMacros.textContent = `${s} %`;
    sumaMacros.style.color = s === 100 ? 'var(--verde)' : 'var(--rojo)';
  });
  const form = h('form', {}, h('h3', {}, 'Calorías y macronutrientes'), macros, h('p', {}, 'Suma de macros: ', sumaMacros),
    h('h3', {}, 'Comidas y distribución calórica (mínimo 4)'), comidasEl, h('p', {}, 'Suma: ', suma),
    h('h3', {}, 'Alimentos a excluir'), excEl, busq, opc,
    h('div', { class: 'aviso advertencia' }, 'El sistema propone un borrador: revisalo y ajustalo antes de confirmarlo. No reemplaza tu criterio profesional.'));
  dibujarComidas();
  macros.dispatchEvent(new Event('input'));
  let semilla = Math.floor(Math.random() * 1e9);
  await modal({ titulo: 'Generar plan automáticamente', cuerpo: form, ancho: true, botones: [{ texto: 'Cancelar', clase: 'secundario' }, { texto: 'Generar borrador', accion: async () => {
    const d = leerForm(form);
    if (sel.size < 4) { toast('Elegí al menos 4 comidas', 'advertencia'); return false; }
    const comidas = [...sel].map(t => ({ tipo: t, pct: Number(pctInputs[t].value) }));
    if (comidas.reduce((a, c) => a + c.pct, 0) !== 100) { toast('La distribución de comidas debe sumar 100 %', 'advertencia'); return false; }
    if (d.pctHc + d.pctProteinas + d.pctGrasas !== 100) { toast('Los porcentajes de macros deben sumar 100 %', 'advertencia'); return false; }
    const req = { pacienteId, kcal: d.kcal, pctHc: d.pctHc, pctProteinas: d.pctProteinas, pctGrasas: d.pctGrasas, comidas, variedad: d.variedad, excluirAlimentoIds: [...excluidos.keys()], semilla };
    const b = await api.post('/planes-alimentarios/generar', req);
    tiposActivos = new Set(comidas.map(c => c.tipo));
    cargarDesde(b.comidas);
    objetivos = { kcal: d.kcal, pctHc: d.pctHc, pctProteinas: d.pctProteinas, pctGrasas: d.pctGrasas };
    parametrosGeneracion = JSON.stringify(b.parametros);
    encabezadoPlan.modo = 'AUTOMATICO';
    totalesServidor = b.totalesPorDia;
    b.advertencias.forEach(a => toast(a, 'advertencia', 7000));
    toast('Borrador generado. Podés regenerar, editar celdas y luego confirmar.');
    dibujarGrilla();
    dibujarAcciones(true);
  } }] });
}

// ------------------------------------------------------------------ guardar
function cuerpoPlan(estado, finalizarActual = false) {
  const comidas = Object.entries(celdas).filter(([k]) => tiposActivos.has(k.split('-')[1])).map(([k, c]) => {
    const [dia, tipo] = k.split('-');
    return { diaSemana: Number(dia), tipo, descripcion: c.descripcion || null, items: c.items.map(i => ({ alimentoId: i.alimentoId, gramos: i.gramos, cantidadCasera: i.cantidadCasera || null })) };
  });
  return { ...encabezadoPlan, duracionSemanas: Number(encabezadoPlan.duracionSemanas), kcalObjetivo: objetivos?.kcal ?? null, pctHc: objetivos?.pctHc ?? null,
    pctProteinas: objetivos?.pctProteinas ?? null, pctGrasas: objetivos?.pctGrasas ?? null, parametrosGeneracion, comidas, estado, finalizarActual, version: plan?.version };
}

async function guardar(estado) {
  const enviar = (finalizar) => plan ? api.put(`/planes-alimentarios/${plan.id}`, cuerpoPlan(estado, finalizar)) : api.post(`/pacientes/${pacienteId}/planes-alimentarios`, cuerpoPlan(estado, finalizar));
  try {
    let r;
    try {
      r = await enviar(false);
    } catch (e) {
      if (e instanceof ApiError && e.code === 'PLAN_ACTIVO_EXISTENTE') {
        if (!(await confirmar(e.message, { texto: 'Finalizar el actual y confirmar' }))) return;
        r = await enviar(true);
      } else throw e;
    }
    (r.advertencias || []).forEach(a => toast(a, 'advertencia', 8000));
    toast(estado === 'ACTIVO' ? 'Plan confirmado: el paciente ya puede verlo' : 'Borrador guardado');
    location.href = `/profesional/plan-editor.html?id=${r.id}`;
  } catch (e) { manejarError(e); }
}

function dibujarAcciones(generado = false) {
  vaciar(accionesEl, soloLectura ? h('a', { class: 'btn secundario', href: `/comun/paciente.html?id=${pacienteId}&tab=planes` }, 'Volver') : [
    h('a', { class: 'btn secundario', href: `/comun/paciente.html?id=${pacienteId}&tab=planes` }, 'Cancelar'),
    generado ? h('button', { class: 'btn secundario', onclick: abrirGenerador }, 'Regenerar') : null,
    !plan || plan.estado === 'BORRADOR' ? h('button', { class: 'btn secundario', onclick: () => guardar('BORRADOR') }, 'Guardar borrador') : null,
    h('button', { class: 'btn', onclick: () => guardar('ACTIVO') }, plan?.estado === 'ACTIVO' ? 'Guardar cambios' : 'Confirmar plan'),
  ]);
}

dibujarCabecera();
dibujarGrilla();
dibujarAcciones();
if (param('modo') === 'generar' && !plan) abrirGenerador();
void totalesServidor;
