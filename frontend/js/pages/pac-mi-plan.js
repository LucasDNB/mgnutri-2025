// CU-15 Consultar plan · CU-16 PDF · CU-18 alternativas · CU-41 comentarios.
import { api, descargar, ApiError } from '../core/api.js';
import { iniciarPagina, encabezado } from '../core/pagina.js';
import { h, toast, modal, manejarError, vaciar, chip } from '../core/ui.js';
import { fmt, hoyISO, diaSemana, DIAS } from '../core/format.js';

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
let dia = diaSemana(hoyISO());
const ancho = window.matchMedia('(min-width: 900px)');
const cont = h('div');

main.append(encabezado('Mi plan alimentario', [['Nutrición', '/paciente/nutricion.html'], ['Mi plan']],
  h('button', { class: 'btn secundario', onclick: () => descargar('/me/plan-alimentario/pdf', 'mi-plan.pdf').catch(manejarError) }, 'Descargar PDF')),
h('div', { class: 'tarjeta' }, h('h2', {}, plan.nombre),
  h('p', {}, `Vigencia: ${fmt.fecha(plan.fechaInicio)} al ${fmt.fecha(plan.fechaFin)} · ${plan.nutricionista?.nombre || ''}`),
  plan.observaciones ? h('p', {}, h('strong', {}, 'Indicaciones: '), plan.observaciones) : null,
  h('button', { class: 'btn chico secundario', onclick: () => comentar(null) }, 'Comentar sobre el plan')),
cont);

function comidaCard(c) {
  return h('div', { class: 'ejercicio-card' },
    h('div', { class: 'fila entre' }, h('strong', {}, c.tipoEtiqueta), c.totales?.kcal ? h('small', { class: 'suave' }, `≈ ${Math.round(c.totales.kcal)} kcal`) : null),
    c.descripcion ? h('p', { style: 'margin:.3rem 0' }, c.descripcion) : null,
    c.items.map(i => h('div', { class: 'item-alimento' },
      h('div', {}, i.reemplazo ? [h('strong', {}, `${i.reemplazo.alimento} ${fmt.numero(i.reemplazo.gramos)} g`), i.reemplazo.cantidadCasera ? ` (${i.reemplazo.cantidadCasera})` : '',
        h('div', { class: 'reemplazo' }, `reemplaza a ${i.alimento}`)] : [`${i.alimento} ${fmt.numero(i.gramos)} g`, i.cantidadCasera ? h('span', { class: 'suave' }, ` (${i.cantidadCasera})`) : '']),
      h('div', { class: 'fila' },
        i.reemplazo ? h('button', { class: 'btn chico secundario', onclick: () => volver(i) }, 'Volver al original')
          : h('button', { class: 'btn chico secundario', onclick: () => alternativas(i) }, 'Ver alternativa')))),
    !c.items.length ? h('small', { class: 'suave' }, 'Para cambiar algún alimento, consultá con tu nutricionista.') : null,
    h('div', { style: 'margin-top:.4rem' }, h('button', { class: 'btn link', onclick: () => comentar(c) }, 'Comentar esta comida')));
}

function dibujar() {
  if (ancho.matches) {
    const tipos = [...new Map(plan.comidas.map(c => [c.tipo, c.tipoEtiqueta])).entries()];
    vaciar(cont, h('div', { class: 'tarjeta tabla-scroll' }, h('table', { class: 'plan-grilla' },
      h('thead', {}, h('tr', {}, h('th', {}, 'Comida'), DIAS.slice(1).map((d, i) => h('th', { style: i + 1 === diaSemana(hoyISO()) ? 'background:var(--verde-2)' : '' }, d)))),
      h('tbody', {}, tipos.map(([t, et]) => h('tr', {}, h('td', { class: 'tipo' }, et), [1, 2, 3, 4, 5, 6, 7].map(d => {
        const c = plan.comidas.find(x => x.diaSemana === d && x.tipo === t);
        return h('td', {}, c ? comidaCard(c) : '');
      })))))));
  } else {
    vaciar(cont, h('div', { class: 'pestanas', role: 'tablist' }, [1, 2, 3, 4, 5, 6, 7].map(d => h('button', { class: d === dia ? 'activa' : '', onclick: () => { dia = d; dibujar(); } }, DIAS[d].substring(0, 3)))),
      h('h2', {}, DIAS[dia]), plan.comidas.filter(c => c.diaSemana === dia).map(comidaCard));
  }
}
ancho.addEventListener('change', dibujar);

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
dibujar();
