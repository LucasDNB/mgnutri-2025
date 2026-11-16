// CU-17 Registrar comidas realizadas: guardado inmediato, hoy y hasta 2 días antes, barra semanal.
import { api, qs, ApiError } from '../core/api.js';
import { iniciarPagina, encabezado } from '../core/pagina.js';
import { h, toast, manejarError, vaciar } from '../core/ui.js';
import { hoyISO, sumarDias, diaSemana, DIAS, fmt } from '../core/format.js';

const { main } = await iniciarPagina({ roles: ['PACIENTE'], titulo: 'Registro del día' });
let plan;
try { plan = await api.get('/me/plan-alimentario'); } catch (e) {
  if (e instanceof ApiError && e.code === 'SIN_PLAN_ASIGNADO') { main.append(h('div', { class: 'tarjeta vacio' }, 'Todavía no tenés un plan alimentario asignado')); throw e; }
  throw e;
}
let fecha = hoyISO();
const pendientes = [];
const cont = h('div');
const barra = h('div');
main.append(encabezado('Registro del día', [['Nutrición', '/paciente/nutricion.html'], ['Registro']]),
  h('div', { class: 'pestanas' }, [0, -1, -2].map(n => h('button', { class: n === 0 ? 'activa' : '', onclick: (e) => {
    fecha = sumarDias(hoyISO(), n);
    e.currentTarget.parentElement.querySelectorAll('button').forEach(b => b.classList.remove('activa'));
    e.currentTarget.classList.add('activa');
    cargar();
  } }, n === 0 ? 'Hoy' : n === -1 ? 'Ayer' : 'Anteayer'))), barra, cont);

async function cargar() {
  const regs = await api.get(`/me/registro-comidas${qs({ desde: sumarDias(hoyISO(), -6), hasta: hoyISO() })}`);
  const porClave = Object.fromEntries(regs.map(r => [`${r.comidaPlanId}-${r.fecha}`, r]));
  const dia = diaSemana(fecha);
  const comidas = plan.comidas.filter(c => c.diaSemana === dia);
  // barra de cumplimiento semanal (últimos 7 días)
  let programadas = 0;
  let hechas = 0;
  for (let i = 0; i < 7; i++) {
    const f = sumarDias(hoyISO(), -i);
    if (f < plan.fechaInicio) continue;
    const cs = plan.comidas.filter(c => c.diaSemana === diaSemana(f));
    programadas += cs.length;
    hechas += cs.filter(c => porClave[`${c.id}-${f}`]?.realizada).length;
  }
  const pct = programadas ? Math.round(hechas * 100 / programadas) : 0;
  vaciar(barra, h('div', { class: 'tarjeta' }, h('div', { class: 'fila entre' }, h('strong', {}, 'Cumplimiento de la semana'), `${pct} %`), h('div', { class: 'barra' }, h('span', { style: `width:${pct}%` }))));
  vaciar(cont, h('h2', {}, `${DIAS[dia]} ${fmt.fecha(fecha)}`), comidas.length ? comidas.map(c => {
    const r = porClave[`${c.id}-${fecha}`] || { realizada: false, noLeGusto: false, leCayoMal: false };
    const estado = { ...r };
    const check = (campo, texto) => h('label', { class: 'check' }, h('input', { type: 'checkbox', checked: !!estado[campo], onchange: async (e) => {
      estado[campo] = e.target.checked;
      await guardar(c.id, estado);
    } }), texto);
    return h('div', { class: `ejercicio-card ${estado.realizada ? 'hecho' : ''}` }, h('strong', {}, c.tipoEtiqueta),
      h('p', { style: 'margin:.3rem 0' }, c.descripcion || c.items.map(i => i.reemplazo ? `${i.reemplazo.alimento} ${i.reemplazo.gramos} g` : `${i.alimento} ${i.gramos} g`).join(' + ')),
      check('realizada', 'La hice como estaba propuesta'), h('div', { class: 'fila' }, check('noLeGusto', 'No me gustó'), check('leCayoMal', 'Me cayó mal')));
  }) : h('div', { class: 'vacio' }, 'Tu plan no tiene comidas para este día.'));
}

async function guardar(comidaPlanId, estado) {
  const cuerpo = { comidaPlanId, fecha, realizada: !!estado.realizada, noLeGusto: !!estado.noLeGusto, leCayoMal: !!estado.leCayoMal };
  try {
    await api.put('/me/registro-comidas', cuerpo);
    toast('Guardado ✔', 'ok', 1500);
    cargar();
  } catch (e) {
    if (e.code === 'SIN_CONEXION') { pendientes.push(cuerpo); toast('Sin conexión: se reintentará', 'advertencia'); setTimeout(reintentar, 5000); } else manejarError(e);
  }
}

async function reintentar() {
  while (pendientes.length) {
    try { await api.put('/me/registro-comidas', pendientes[0]); pendientes.shift(); } catch { setTimeout(reintentar, 10000); return; }
  }
  toast('Registros sincronizados');
  cargar();
}
cargar().catch(manejarError);
