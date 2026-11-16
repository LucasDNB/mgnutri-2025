// Selector de huecos libres de un profesional (CU-07 / CU-08), navegable por semana.
import { api, qs } from '../core/api.js';
import { h, vaciar, manejarError } from '../core/ui.js';
import { fmt, hoyISO, sumarDias, fechaLocalDeIso } from '../core/format.js';

/**
 * Crea el selector. alElegir(hueco) se invoca al tocar un horario.
 * Devuelve el elemento DOM.
 */
export function selectorHuecos(profesionalId, alElegir) {
  let desde = hoyISO();
  const cont = h('div');
  const titulo = h('strong');
  const lista = h('div', { class: 'columna' });
  const prev = h('button', { type: 'button', class: 'btn chico secundario', onclick: () => { desde = sumarDias(desde, -7); cargar(); } }, '‹ Semana anterior');
  const sig = h('button', { type: 'button', class: 'btn chico secundario', onclick: () => { desde = sumarDias(desde, 7); cargar(); } }, 'Semana siguiente ›');
  cont.append(h('div', { class: 'fila entre' }, prev, titulo, sig), lista);

  async function cargar() {
    prev.disabled = desde <= hoyISO();
    sig.disabled = desde >= sumarDias(hoyISO(), 28);
    const hasta = sumarDias(desde, 6);
    titulo.textContent = `${fmt.fecha(desde)} al ${fmt.fecha(hasta)}`;
    vaciar(lista, h('div', { class: 'vacio' }, 'Buscando horarios…'));
    try {
      const huecos = await api.get(`/profesionales/${profesionalId}/huecos${qs({ desde, hasta })}`);
      if (!huecos.length) {
        vaciar(lista, h('div', { class: 'vacio' }, 'No hay horarios disponibles esta semana.'));
        return;
      }
      const porDia = {};
      huecos.forEach(x => { (porDia[fechaLocalDeIso(x.inicio)] = porDia[fechaLocalDeIso(x.inicio)] || []).push(x); });
      vaciar(lista, Object.entries(porDia).map(([f, hs]) => h('div', {},
        h('div', { style: 'font-weight:700;margin:.4rem 0;text-transform:capitalize' }, fmt.diaFecha(hs[0].inicio)),
        h('div', { class: 'fila' }, hs.map(x => h('button', { type: 'button', class: 'btn chico secundario', title: x.ubicacion || '', onclick: () => alElegir(x) }, fmt.hora(x.inicio)))))));
    } catch (e) {
      manejarError(e);
    }
  }
  cargar();
  return cont;
}
