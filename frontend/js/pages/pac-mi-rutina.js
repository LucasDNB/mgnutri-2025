// CU-24 Consultar rutina (día/semana/mes) · CU-25 registrar · CU-26 variantes · CU-28 video · CU-40 comentar · CU-27 PDF.
import { api, qs, descargar, ApiError } from '../core/api.js';
import { iniciarPagina, encabezado } from '../core/pagina.js';
import { h, toast, modal, manejarError, vaciar, chip } from '../core/ui.js';
import { fmt, hoyISO, sumarDias, diaSemana, DIAS } from '../core/format.js';

const { main } = await iniciarPagina({ roles: ['PACIENTE'], titulo: 'Mi rutina' });
let rutina;
try { rutina = await api.get('/me/rutina'); } catch (e) {
  if (e instanceof ApiError && e.code === 'SIN_RUTINA_ASIGNADA') { main.append(h('div', { class: 'tarjeta vacio' }, 'Todavía no tenés una rutina asignada'), h('a', { class: 'btn', href: '/paciente/entrenamiento.html' }, 'Volver')); throw e; }
  throw e;
}
let vista = 'dia';
let fecha = hoyISO();
let mes = hoyISO().substring(0, 7);
const cont = h('div');
main.append(encabezado('Mi rutina', [['Entrenamiento', '/paciente/entrenamiento.html'], ['Mi rutina']],
  h('button', { class: 'btn secundario', onclick: () => descargar('/me/rutina/pdf', 'mi-rutina.pdf').catch(manejarError) }, 'Descargar PDF')),
h('div', { class: 'tarjeta' }, h('h2', {}, rutina.nombre), h('p', {}, `${rutina.objetivo ? rutina.objetivo + ' · ' : ''}Vigencia: ${fmt.fecha(rutina.fechaInicio)} al ${fmt.fecha(rutina.fechaFin)} · ${rutina.entrenador}`),
  rutina.observaciones ? h('p', {}, h('strong', {}, 'Indicaciones: '), rutina.observaciones) : null,
  h('button', { class: 'btn chico secundario', onclick: () => comentar(null) }, 'Comentar sobre la rutina')),
h('div', { class: 'pestanas' }, [['dia', 'Día'], ['semana', 'Semana'], ['mes', 'Mes']].map(([v, t]) => h('button', { class: v === vista ? 'activa' : '', dataset: { v }, onclick: (e) => {
  vista = v; e.currentTarget.parentElement.querySelectorAll('button').forEach(b => b.classList.toggle('activa', b.dataset.v === v)); dibujar();
} }, t))), cont);

async function dibujar() {
  if (vista === 'semana') {
    vaciar(cont, rutina.dias.map(d => h('div', { class: 'tarjeta' }, h('h2', {}, `${DIAS[d.diaSemana]}${d.nombre ? ' – ' + d.nombre : ''}`), d.ejercicios.map(e => tarjeta(e, null, null)))));
    return;
  }
  if (vista === 'mes') {
    const dias = await api.get(`/me/rutina/calendario${qs({ mes })}`);
    const [a, m] = mes.split('-').map(Number);
    const primero = dias[0].fecha;
    const offset = diaSemana(primero) - 1;
    vaciar(cont, h('div', { class: 'cal-barra' },
      h('button', { class: 'btn chico secundario', onclick: () => { const d = new Date(Date.UTC(a, m - 2, 1)); mes = d.toISOString().substring(0, 7); dibujar(); } }, '‹'),
      h('span', { class: 'cal-titulo' }, new Date(Date.UTC(a, m - 1, 15)).toLocaleDateString('es-AR', { month: 'long', year: 'numeric' })),
      h('button', { class: 'btn chico secundario', onclick: () => { const d = new Date(Date.UTC(a, m, 1)); mes = d.toISOString().substring(0, 7); dibujar(); } }, '›')),
    h('div', { class: 'cal-mes' }, ['L', 'M', 'M', 'J', 'V', 'S', 'D'].map(x => h('div', { class: 'cab' }, x)),
      Array.from({ length: offset }, () => h('div', { class: 'dia fuera' })),
      dias.map(d => h('div', { class: `dia ${d.fecha === hoyISO() ? 'hoy' : ''}`, onclick: () => { if (d.programado) { fecha = d.fecha; vista = 'dia'; dibujar(); } } },
        h('div', { class: 'n' }, Number(d.fecha.substring(8))),
        d.programado ? chip(d.cumplido ? '✔ cumplido' : `${d.completados}/${d.ejercicios}`, d.cumplido ? 'OK' : d.fecha < hoyISO() ? 'AUSENTE' : '') : null))));
    return;
  }
  // vista día: hoy o el próximo día programado
  let f = fecha;
  for (let i = 0; i < 7 && !rutina.dias.some(d => d.diaSemana === diaSemana(f)); i++) f = sumarDias(f, 1);
  const dia = rutina.dias.find(d => d.diaSemana === diaSemana(f));
  const editable = f <= hoyISO() && f >= sumarDias(hoyISO(), -2);
  const regs = await api.get(`/me/registro-entrenamiento${qs({ desde: f, hasta: f })}`).catch(() => []);
  const porEr = Object.fromEntries(regs.map(r => [r.ejercicioRutinaId, r]));
  const completos = dia ? dia.ejercicios.every(e => porEr[e.id]?.completado) : false;
  vaciar(cont, h('div', { class: 'fila', style: 'margin-bottom:.6rem' },
    [0, -1, -2].map(n => h('button', { class: `btn chico ${fecha === sumarDias(hoyISO(), n) ? '' : 'secundario'}`, onclick: () => { fecha = sumarDias(hoyISO(), n); dibujar(); } }, n === 0 ? 'Hoy' : n === -1 ? 'Ayer' : 'Anteayer'))),
  dia ? h('div', {}, h('h2', {}, `${DIAS[dia.diaSemana]} ${fmt.fecha(f)}${dia.nombre ? ' – ' + dia.nombre : ''}`),
    f !== fecha ? h('div', { class: 'aviso' }, 'Hoy no tenés entrenamiento: te mostramos tu próxima sesión.') : null,
    completos ? h('div', { class: 'aviso' }, '¡Sesión completa! 💪') : null,
    dia.ejercicios.map(e => tarjeta(e, editable ? f : null, porEr[e.id]))) : h('div', { class: 'vacio' }, 'No hay sesiones programadas.'));
}

function tarjeta(e, fechaRegistro, reg) {
  const nombre = e.reemplazo ? e.reemplazo.nombre : e.nombre;
  const video = e.reemplazo ? e.reemplazo.videoUrl : e.videoUrl;
  const estado = { completado: !!reg?.completado, cargaKg: reg?.cargaKg ?? '', seriesRealizadas: reg?.seriesRealizadas ?? '', repeticionesRealizadas: reg?.repeticionesRealizadas ?? '' };
  const guardar = async () => {
    try {
      const r = await api.put('/me/registro-entrenamiento', { ejercicioRutinaId: e.id, fecha: fechaRegistro, completado: estado.completado,
        cargaKg: estado.cargaKg === '' ? null : Number(estado.cargaKg), seriesRealizadas: estado.seriesRealizadas === '' ? null : Number(estado.seriesRealizadas),
        repeticionesRealizadas: estado.repeticionesRealizadas === '' ? null : Number(estado.repeticionesRealizadas) });
      toast(r.sesionCompleta ? '¡Sesión completa! 💪' : 'Guardado ✔', 'ok', 1500);
      if (r.sesionCompleta) dibujar();
    } catch (er) { manejarError(er); }
  };
  const numInput = (campo, ph, step = 1) => h('input', { type: 'number', inputmode: 'decimal', step, min: 0, placeholder: ph, value: estado[campo], style: 'width:90px',
    onchange: (ev) => { estado[campo] = ev.target.value; guardar(); } });
  return h('div', { class: `ejercicio-card ${estado.completado ? 'hecho' : ''}` },
    h('div', { class: 'fila entre' }, h('strong', {}, nombre), h('span', { class: 'suave' }, e.grupoMuscular)),
    e.reemplazo ? h('div', { class: 'reemplazo' }, `variante de ${e.nombre}`) : null,
    h('div', {}, `${e.series} × ${e.repeticiones}${e.descansoSeg ? ` · descanso ${e.descansoSeg} s` : ''}${e.cargaSugerida ? ` · carga sugerida ${e.cargaSugerida}` : ''}`),
    e.notas ? h('small', { class: 'suave' }, e.notas) : null,
    e.ultimaCarga ? h('div', {}, h('small', {}, `Última carga registrada: ${fmt.numero(e.ultimaCarga)} kg`)) : null,
    fechaRegistro ? h('div', { class: 'fila', style: 'margin-top:.5rem' },
      h('label', { class: 'check' }, h('input', { type: 'checkbox', checked: estado.completado, onchange: (ev) => { estado.completado = ev.target.checked; ev.target.closest('.ejercicio-card').classList.toggle('hecho', estado.completado); guardar(); } }), 'Completado'),
      numInput('cargaKg', 'kg', 0.5), numInput('seriesRealizadas', 'series'), numInput('repeticionesRealizadas', 'reps')) : null,
    h('div', { class: 'fila', style: 'margin-top:.5rem' },
      video ? h('a', { class: 'btn chico secundario', href: video, target: '_blank', rel: 'noopener' }, '🎬 Ver video') : null,
      e.reemplazo ? h('button', { class: 'btn chico secundario', onclick: () => volver(e) }, 'Volver al original') : h('button', { class: 'btn chico secundario', onclick: () => variante(e) }, 'Pedir variante'),
      h('button', { class: 'btn chico secundario', onclick: () => comentar(e) }, 'Comentar'),
      h('a', { class: 'btn chico peligro secundario', href: `/paciente/molestia.html?ejercicioRutinaId=${e.id}` }, 'Molestia')));
}

async function variante(e) {
  try {
    const r = await api.get(`/me/rutina/ejercicios/${e.id}/alternativas`);
    if (!r.alternativas.length) { toast(r.mensaje, 'advertencia'); return; }
    const elegido = await modal({ titulo: `Variantes de ${r.original}`, cuerpo: h('p', { class: 'suave' }, 'Ejercicios que trabajan el mismo grupo muscular. Se mantienen series y repeticiones.'),
      botones: [...r.alternativas.map(a => ({ clase: 'secundario bloque', valor: a.ejercicioId, texto: `${a.nombre} · ${a.dificultad.toLowerCase()}${a.equipamiento ? ' · ' + a.equipamiento : ''}` })),
        { texto: 'Cancelar', clase: 'peligro secundario', valor: null }] });
    if (!elegido) return;
    rutina = await api.put(`/me/rutina/ejercicios/${e.id}/reemplazo`, { ejercicioId: elegido });
    toast('Variante aplicada');
    dibujar();
  } catch (er) { manejarError(er); }
}

async function volver(e) {
  try { rutina = await api.del(`/me/rutina/ejercicios/${e.id}/reemplazo`); toast('Volviste al ejercicio original'); dibujar(); } catch (er) { manejarError(er); }
}

async function comentar(e) {
  const t = h('textarea', { maxLength: 300, rows: 4 });
  await modal({ titulo: e ? `Comentario sobre ${e.nombre}` : 'Comentario sobre la rutina', cuerpo: t, botones: [{ texto: 'Cancelar', clase: 'secundario' }, { texto: 'Enviar', accion: async () => {
    if (!t.value.trim()) { toast('Escribí tu comentario', 'advertencia'); return false; }
    await api.post('/me/comentarios', { tipo: 'RUTINA', ejercicioRutinaId: e?.id || null, fecha: hoyISO(), texto: t.value.trim() });
    toast('Comentario enviado a tu entrenador');
  } }] });
}
dibujar().catch(manejarError);
