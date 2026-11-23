// Bloques de ejercicios de la rutina (v1.1): los ejercicios seguidos con el mismo bloque («Entrada en calor», «WOD»)
// y esquema («3 rondas», «21-15-9») se muestran agrupados.
import { h } from '../core/ui.js';

/** Agrupa ejercicios consecutivos: [{ bloque, grupos: [{ esquema, items: [{ e, i }] }] }] (i = índice original). */
export function agrupar(ejercicios) {
  const secciones = [];
  ejercicios.forEach((e, i) => {
    const bloque = e.bloque || null;
    const esquema = bloque ? (e.esquema || null) : null;
    let s = secciones[secciones.length - 1];
    if (!s || s.bloque !== bloque) {
      s = { bloque, grupos: [] };
      secciones.push(s);
    }
    let g = s.grupos[s.grupos.length - 1];
    if (!g || g.esquema !== esquema) {
      g = { esquema, items: [] };
      s.grupos.push(g);
    }
    g.items.push({ e, i });
  });
  return secciones;
}

/** Texto de la prescripción: «3 × 10», «30», «5 min» o «3 series». */
export function prescripcion(e) {
  const reps = (e.repeticiones || '').trim();
  if (e.series && reps) return `${e.series} × ${reps}`;
  if (reps) return reps;
  return e.series ? `${e.series} series` : '';
}

/** Dibuja los ejercicios agrupados en bloques; render(e, i) arma la tarjeta de cada ejercicio. */
export function dibujarBloques(ejercicios, render) {
  return agrupar(ejercicios).map(s => {
    const grupos = s.grupos.map(g => (g.esquema
      ? h('div', { class: 'esquema' }, h('div', { class: 'esquema-titulo' }, g.esquema), g.items.map(x => render(x.e, x.i)))
      : g.items.map(x => render(x.e, x.i))));
    return s.bloque ? h('section', { class: 'bloque' }, h('h3', {}, s.bloque), grupos) : grupos;
  });
}
