// Gráficos con Chart.js (única librería del frontend, D-31).
import { h } from '../core/ui.js';

const COLORES = ['#2E5E3A', '#7FA36B', '#C98B2B', '#1A5DAB', '#B3261E', '#6B4E9E', '#3A8C8C'];
const activos = new WeakMap();

/** Dibuja un gráfico dentro de un contenedor. series: [{nombre, datos}] alineadas a labels. */
export function grafico(contenedor, { tipo = 'line', titulo, labels, series, min, max }) {
  contenedor.replaceChildren();
  if (!labels?.length || !series?.some(s => s.datos?.some(v => v !== null && v !== undefined))) {
    contenedor.append(h('div', { class: 'vacio' }, 'Sin datos para graficar'));
    return;
  }
  if (!window.Chart) {
    contenedor.append(h('div', { class: 'vacio' }, 'No se pudo cargar la librería de gráficos.'));
    return;
  }
  const canvas = h('canvas', { 'aria-label': titulo || 'Gráfico', role: 'img' });
  const caja = h('div', { class: 'grafico' }, canvas);
  contenedor.append(caja);
  activos.get(contenedor)?.destroy?.();
  const chart = new window.Chart(canvas, {
    type: tipo,
    data: {
      labels,
      datasets: series.map((s, i) => ({
        label: s.nombre, data: s.datos, borderColor: COLORES[i % COLORES.length], backgroundColor: tipo === 'bar' ? COLORES[i % COLORES.length] : 'transparent',
        tension: 0.25, spanGaps: true, pointRadius: 3,
      })),
    },
    options: {
      responsive: true, maintainAspectRatio: false,
      plugins: { title: { display: !!titulo, text: titulo, color: '#2E5E3A', font: { weight: 'bold' } }, legend: { position: 'bottom' } },
      scales: { y: { min, max } },
    },
  });
  activos.set(contenedor, chart);
}

/** Convierte series de puntos {fecha, valor} a labels comunes + datos alineados. */
export function alinear(seriesPuntos) {
  const fechas = [...new Set(seriesPuntos.flatMap(s => s.puntos.map(p => p.fecha)))].sort();
  return {
    labels: fechas.map(f => `${f.substring(8, 10)}/${f.substring(5, 7)}/${f.substring(2, 4)}`),
    series: seriesPuntos.map(s => {
      const m = Object.fromEntries(s.puntos.map(p => [p.fecha, p.valor]));
      return { nombre: s.nombre, datos: fechas.map(f => m[f] ?? null) };
    }),
  };
}
