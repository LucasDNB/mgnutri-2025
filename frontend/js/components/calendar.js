// Componente calendario (D-13): vistas día / semana / mes renderizadas con CSS grid, sin librerías.
import { h } from '../core/ui.js';
import { fmt, hoyISO, sumarDias, diaSemana, fechaLocalDeIso, DIAS } from '../core/format.js';

const MESES = ['enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio', 'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre'];

/** Devuelve el rango [desde, hasta] (YYYY-MM-DD) que cubre la vista para la fecha de referencia. */
export function rangoVista(vista, ref) {
  if (vista === 'dia') return [ref, ref];
  if (vista === 'semana') {
    const lunes = sumarDias(ref, 1 - diaSemana(ref));
    return [lunes, sumarDias(lunes, 6)];
  }
  const primero = `${ref.substring(0, 8)}01`;
  const inicio = sumarDias(primero, 1 - diaSemana(primero));
  return [inicio, sumarDias(inicio, 41)];
}

export function moverReferencia(vista, ref, delta) {
  if (vista === 'dia') return sumarDias(ref, delta);
  if (vista === 'semana') return sumarDias(ref, 7 * delta);
  const [a, m] = ref.split('-').map(Number);
  const d = new Date(Date.UTC(a, m - 1 + delta, 1));
  return d.toISOString().substring(0, 10);
}

export function tituloVista(vista, ref) {
  if (vista === 'dia') return `${DIAS[diaSemana(ref)]} ${fmt.fecha(ref)}`;
  if (vista === 'semana') {
    const [d, hh] = rangoVista('semana', ref);
    return `Semana del ${fmt.fecha(d)} al ${fmt.fecha(hh)}`;
  }
  const [a, m] = ref.split('-').map(Number);
  return `${MESES[m - 1]} ${a}`;
}

/**
 * Renderiza eventos ({inicio, titulo, estado, ...}) en la vista elegida.
 * onClick(evento) al tocar un evento; onDia(fecha) al tocar un día (vista mes).
 */
export function renderCalendario({ vista, ref, eventos, onClick, onDia }) {
  const porDia = {};
  for (const e of eventos) {
    const f = fechaLocalDeIso(e.inicio);
    (porDia[f] = porDia[f] || []).push(e);
  }
  const boton = (e) => h('button', { class: `cal-evento ${e.estado || ''}`, onclick: () => onClick?.(e), title: e.detalle || '' },
    `${fmt.hora(e.inicio)} ${e.titulo}`);
  if (vista === 'mes') {
    const [inicio] = rangoVista('mes', ref);
    const mes = ref.substring(0, 7);
    const hoy = hoyISO();
    const celdas = [];
    for (let i = 0; i < 42; i++) {
      const f = sumarDias(inicio, i);
      const evs = porDia[f] || [];
      celdas.push(h('div', { class: `dia ${f.substring(0, 7) !== mes ? 'fuera' : ''} ${f === hoy ? 'hoy' : ''}`, onclick: (ev) => { if (ev.target === ev.currentTarget || ev.target.classList.contains('n')) onDia?.(f); } },
        h('div', { class: 'n' }, Number(f.substring(8))),
        evs.slice(0, 3).map(boton), evs.length > 3 ? h('div', { class: 'suave' }, `+${evs.length - 3} más`) : null));
    }
    return h('div', { class: 'cal-mes' }, ['L', 'M', 'M', 'J', 'V', 'S', 'D'].map(d => h('div', { class: 'cab' }, d)), celdas);
  }
  const [desde, hasta] = rangoVista(vista, ref);
  const columnas = [];
  for (let f = desde; f <= hasta; f = sumarDias(f, 1)) {
    const evs = (porDia[f] || []).sort((a, b) => a.inicio.localeCompare(b.inicio));
    columnas.push(h('div', { class: 'col' }, h('h4', {}, `${DIAS[diaSemana(f)]} ${fmt.fecha(f).substring(0, 5)}`),
      evs.length ? evs.map(boton) : h('div', { class: 'suave' }, 'Sin turnos')));
  }
  return h('div', { class: 'cal-semana', style: vista === 'dia' ? 'grid-template-columns:1fr' : '' }, columnas);
}
