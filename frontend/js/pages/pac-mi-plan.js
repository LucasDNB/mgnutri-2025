// CU-15 Consultar plan · CU-16 PDF · CU-18 alternativas · CU-41 comentarios · CU-17 marcas de cada comida y
// CU-43 observaciones del día, sin pasar por el registro (D-38).
import { api, descargar, ApiError } from '../core/api.js';
import { iniciarPagina, encabezado } from '../core/pagina.js';
import { h, toast, modal, manejarError, vaciar } from '../core/ui.js';
import { fmt, hoyISO, sumarDias, diaSemana, DIAS } from '../core/format.js';
import { marcasComida, completada } from '../components/marcas-comida.js';
import { observacionDelDia } from '../components/observacion-dia.js';

const { main } = await iniciarPagina({ roles: ['PACIENTE'], titulo: 'Mi plan alimentario' });
let plan;
try {
  plan = await api.get('/me/plan-alimentario');
} catch (e) {
  if (e instanceof ApiError && e.code === 'SIN_PLAN_ASIGNADO') {
    main.append(h('div', { class: 'tarjeta vacio' }, 'Todavía no tenés un plan alimentario asignado'), h('a', { class: 'btn', href: '/paciente/nutricion.html' }, 'Volver'));
    throw e;
  }
  throw e;
}
const HOY = hoyISO();
let dia = diaSemana(HOY);
// Registros y observaciones de los días que todavía se pueden marcar (hoy y hasta 2 días antes)
let registros = {};
let observaciones = {};
const cont = h('div');

/** Fecha en que se marca el día de la semana d: la última vez que cayó ese día (hoy incluido). */
function fechaDe(d) {
  const atras = (diaSemana(HOY) - d + 7) % 7;
  const fecha = sumarDias(HOY, -atras);
  return { fecha, editable: atras <= 2 && fecha >= plan.fechaInicio && fecha <= plan.fechaFin,
    cuando: atras === 0 ? 'hoy' : atras === 1 ? 'ayer' : atras === 2 ? 'anteayer' : null };
}

async function cargarMarcas() {
  const [regs, obs] = await Promise.all([
    api.get(`/me/registro-comidas?desde=${sumarDias(HOY, -2)}&hasta=${HOY}`),
    api.get(`/me/observaciones-diarias?desde=${sumarDias(HOY, -2)}&hasta=${HOY}`)]);
  registros = Object.fromEntries(regs.map(r => [`${r.comidaPlanId}-${r.fecha}`, r]));
  observaciones = Object.fromEntries(obs.map(o => [o.fecha, o]));
}

main.append(encabezado('Mi plan alimentario', [['Nutrición', '/paciente/nutricion.html'], ['Mi plan']],
  h('button', { class: 'btn secundario', onclick: () => descargar('/me/plan-alimentario/pdf', 'mi-plan.pdf').catch(manejarError) }, 'Descargar PDF')),
h('div', { class: 'tarjeta' }, h('h2', {}, plan.nombre),
  h('p', {}, `Vigencia: ${fmt.fecha(plan.fechaInicio)} al ${fmt.fecha(plan.fechaFin)} · ${plan.nutricionista?.nombre || ''}`),
  plan.observaciones ? h('p', {}, h('strong', {}, 'Indicaciones: '), plan.observaciones) : null,
  h('button', { class: 'btn chico secundario', onclick: () => comentar(null) }, 'Comentar sobre el plan')),
cont);

function comidaCard(c, f) {
  const reg = registros[`${c.id}-${f.fecha}`];
  return h('div', { class: `ejercicio-card ${completada(reg) ? 'hecho' : ''}` },
    h('div', { class: 'fila entre' }, h('strong', {}, c.tipoEtiqueta), c.totales?.kcal ? h('small', { class: 'suave' }, `≈ ${Math.round(c.totales.kcal)} kcal`) : null),
    c.descripcion ? h('p', { style: 'margin:.3rem 0' }, c.descripcion) : null,
    c.items.map(i => h('div', { class: 'item-alimento' },
      h('div', {}, i.reemplazo ? [h('strong', {}, `${i.reemplazo.alimento} ${fmt.numero(i.reemplazo.gramos)} g`), i.reemplazo.cantidadCasera ? ` (${i.reemplazo.cantidadCasera})` : '',
        h('div', { class: 'reemplazo' }, `reemplaza a ${i.alimento}`)] : [`${i.alimento} ${fmt.numero(i.gramos)} g`, i.cantidadCasera ? h('span', { class: 'suave' }, ` (${i.cantidadCasera})`) : '']),
      h('div', { class: 'fila' },
        i.reemplazo ? h('button', { class: 'btn chico secundario', onclick: () => volver(i) }, 'Volver al original')
          : h('button', { class: 'btn chico secundario', onclick: () => alternativas(i) }, 'Ver alternativa')))),
    !c.items.length ? h('small', { class: 'suave' }, 'Para cambiar algún alimento, consultá con tu nutricionista.') : null,
    marcasComida(c, { fecha: f.fecha, registro: reg, editable: f.editable, aviso: 'Podés marcar las comidas de hoy y de hasta 2 días antes.',
      alGuardar: (r) => { registros[`${c.id}-${f.fecha}`] = r; dibujar(); } }),
    h('div', { style: 'margin-top:.4rem' }, h('button', { class: 'btn link', onclick: () => comentar(c) }, 'Comentar esta comida')));
}

function dibujar() {
  const f = fechaDe(dia);
  const comidas = plan.comidas.filter(c => c.diaSemana === dia);
  const obs = observaciones[f.fecha];
  // Los 7 días en dos filas (4 + 3), sin barra de desplazamiento
  vaciar(cont, h('div', { class: 'pestanas dias-plan', role: 'tablist' }, [1, 2, 3, 4, 5, 6, 7].map(d => h('button', {
    role: 'tab', class: d === dia ? 'activa' : '', 'aria-selected': String(d === dia), onclick: () => { dia = d; dibujar(); } }, h('span', { class: 'largo' }, DIAS[d]), h('span', { class: 'corto' }, DIAS[d].substring(0, 3))))),
  h('div', { class: 'fila entre' }, h('h2', { style: 'margin:0' }, DIAS[dia]),
    h('span', { class: 'suave' }, f.editable ? `Marcás las comidas de ${f.cuando} (${fmt.fecha(f.fecha)})` : '')),
  comidas.length ? h('div', { class: 'grilla g2', style: 'margin-top:.6rem' }, comidas.map(c => comidaCard(c, f)))
    : h('div', { class: 'tarjeta vacio' }, 'Tu plan no tiene comidas para este día.'),
  h('div', { class: 'tarjeta fila entre' },
    h('div', {}, h('strong', {}, 'Observaciones del día'), h('div', { class: 'suave' }, !f.editable ? 'Se cargan el mismo día o hasta 2 días después.'
      : obs ? `Hambre ${obs.nivelHambre}/5 · Saciedad ${obs.nivelSaciedad}/5${obs.comentario ? ' · ' + obs.comentario : ''}` : 'Contanos cómo te sentiste con las comidas del día.')),
    h('button', { class: `btn ${obs ? 'secundario' : ''}`, disabled: !f.editable, onclick: async () => {
      const o = await observacionDelDia(f.fecha, obs);
      if (o) { observaciones[f.fecha] = o; dibujar(); }
    } }, obs ? '✔ Observaciones del día' : 'Observaciones del día')));
}

async function alternativas(i) {
  try {
    const r = await api.get(`/me/plan-alimentario/items/${i.id}/alternativas`);
    if (!r.alternativas.length) { toast(r.mensaje || 'Consultá con tu nutricionista', 'advertencia'); return; }
    const elegido = await modal({
      titulo: `Alternativas para ${fmt.numero(i.gramos)} g de ${i.alimento}`,
      cuerpo: h('p', { class: 'suave' }, 'Equivalentes del mismo grupo (las indicadas por tu nutricionista aparecen primero). Elegí una para reemplazarlo en tu plan.'),
      botones: [...r.alternativas.map(a => ({ clase: 'secundario bloque', valor: a.alimentoId,
        texto: `${a.nombre} ${fmt.numero(a.gramos)} g${a.cantidadCasera ? ' (' + a.cantidadCasera + ')' : ''}${a.origen === 'MANUAL' ? ' · indicada' : ''}` })),
      { texto: 'Cancelar', clase: 'peligro secundario', valor: null }],
    });
    if (!elegido) return;
    plan = await api.put(`/me/plan-alimentario/items/${i.id}/reemplazo`, { alimentoId: elegido });
    toast('Reemplazo aplicado');
    dibujar();
  } catch (e) { manejarError(e); }
}

async function volver(i) {
  try { plan = await api.del(`/me/plan-alimentario/items/${i.id}/reemplazo`); toast('Volviste al alimento original'); dibujar(); } catch (e) { manejarError(e); }
}

async function comentar(c) {
  const t = h('textarea', { maxLength: 300, rows: 4, placeholder: 'Contale a tu nutricionista cómo te va…' });
  const cont2 = h('div', { class: 'contador-caracteres' }, '0/300');
  t.addEventListener('input', () => { cont2.textContent = `${t.value.length}/300`; });
  await modal({ titulo: c ? `Comentario sobre ${c.tipoEtiqueta} del ${DIAS[c.diaSemana]}` : 'Comentario sobre el plan', cuerpo: h('div', {}, t, cont2),
    botones: [{ texto: 'Cancelar', clase: 'secundario' }, { texto: 'Enviar', accion: async () => {
      if (!t.value.trim()) { toast('Escribí tu comentario', 'advertencia'); return false; }
      await api.post('/me/comentarios', { tipo: 'PLAN', comidaPlanId: c?.id || null, fecha: hoyISO(), texto: t.value.trim() });
      toast('Comentario enviado a tu nutricionista');
    } }] });
}
await cargarMarcas().catch(manejarError);
dibujar();
