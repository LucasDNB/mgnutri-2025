// CU-17 Registro del día: % de cumplimiento semanal y las marcas de cada comida (hoy y hasta 2 días antes).
// Las mismas marcas se cargan desde «Mi plan alimentario» (D-38).
import { api, qs, ApiError } from '../core/api.js';
import { iniciarPagina, encabezado } from '../core/pagina.js';
import { h, manejarError, vaciar } from '../core/ui.js';
import { hoyISO, sumarDias, diaSemana, DIAS, fmt } from '../core/format.js';
import { marcasComida, completada } from '../components/marcas-comida.js';

const { main } = await iniciarPagina({ roles: ['PACIENTE'], titulo: 'Registro del día' });
let plan;
try { plan = await api.get('/me/plan-alimentario'); } catch (e) {
  if (e instanceof ApiError && e.code === 'SIN_PLAN_ASIGNADO') { main.append(h('div', { class: 'tarjeta vacio' }, 'Todavía no tenés un plan alimentario asignado')); throw e; }
  throw e;
}
let fecha = hoyISO();
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
    const r = porClave[`${c.id}-${fecha}`];
    return h('div', { class: `ejercicio-card ${completada(r) ? 'hecho' : ''}` }, h('strong', {}, c.tipoEtiqueta),
      h('p', { style: 'margin:.3rem 0' }, c.descripcion || c.items.map(i => i.reemplazo ? `${i.reemplazo.alimento} ${i.reemplazo.gramos} g` : `${i.alimento} ${i.gramos} g`).join(' + ')),
      marcasComida(c, { fecha, registro: r, editable: fecha >= plan.fechaInicio, aviso: 'El plan todavía no había empezado ese día.', alGuardar: () => cargar().catch(manejarError) }));
  }) : h('div', { class: 'vacio' }, 'Tu plan no tiene comidas para este día.'));
}
cargar().catch(manejarError);
