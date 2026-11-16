// CU-43 Observaciones diarias de hambre y saciedad (escalas 1–5 con botones grandes) + historial.
import { api, qs } from '../core/api.js';
import { iniciarPagina, encabezado } from '../core/pagina.js';
import { h, toast, manejarError, vaciar, tabla } from '../core/ui.js';
import { hoyISO, sumarDias, fmt } from '../core/format.js';
import { grafico, alinear } from '../components/graficos.js';

const { main } = await iniciarPagina({ roles: ['PACIENTE'], titulo: 'Observaciones diarias' });
const valores = { nivelHambre: null, nivelSaciedad: null, estadoAnimo: null };
const ETQ = { nivelHambre: ['Nada', 'Poca', 'Moderada', 'Bastante', 'Mucha'], nivelSaciedad: ['Nada', 'Poca', 'Moderada', 'Bastante', 'Mucha'], estadoAnimo: ['😞', '🙁', '😐', '🙂', '😄'] };
const fecha = h('select', {}, [0, -1, -2].map(n => h('option', { value: sumarDias(hoyISO(), n) }, n === 0 ? 'Hoy' : n === -1 ? 'Ayer' : 'Anteayer')));
const comentario = h('textarea', { maxLength: 300, rows: 3, placeholder: 'Comentario (opcional)' });

function escala(campo, titulo) {
  const cont = h('div', { class: 'escala', role: 'radiogroup', 'aria-label': titulo });
  const dibujar = () => vaciar(cont, [1, 2, 3, 4, 5].map(n => h('button', { type: 'button', role: 'radio', 'aria-checked': valores[campo] === n, class: valores[campo] === n ? 'sel' : '',
    title: ETQ[campo][n - 1], onclick: () => { valores[campo] = n; dibujar(); } }, campo === 'estadoAnimo' ? ETQ[campo][n - 1] : n)));
  dibujar();
  return h('div', { class: 'campo' }, h('label', {}, titulo), cont, campo !== 'estadoAnimo' ? h('small', { class: 'suave' }, '1 = nada · 5 = mucha') : null);
}

const hist = h('div');
const graf = h('div');
main.append(encabezado('Observaciones diarias', [['Nutrición', '/paciente/nutricion.html'], ['Observaciones']]),
  h('div', { class: 'tarjeta columna' }, h('div', { class: 'campo' }, h('label', {}, 'Día'), fecha),
    escala('nivelHambre', '¿Cuánta hambre sentiste? *'), escala('nivelSaciedad', '¿Qué tan saciado/a quedaste? *'), escala('estadoAnimo', 'Estado de ánimo (opcional)'),
    comentario, h('button', { class: 'btn', onclick: guardar }, 'Guardar')),
  h('div', { class: 'tarjeta' }, h('h2', {}, 'Historial'), graf, hist));

async function guardar() {
  if (!valores.nivelHambre || !valores.nivelSaciedad) { toast('Indicá tu nivel de hambre y de saciedad', 'advertencia'); return; }
  try {
    await api.put('/me/observaciones-diarias', { fecha: fecha.value, ...valores, comentario: comentario.value });
    toast('Observación guardada');
    historial();
  } catch (e) { manejarError(e); }
}

async function historial() {
  const lista = await api.get(`/me/observaciones-diarias${qs({ desde: sumarDias(hoyISO(), -30) })}`);
  grafico(graf, { titulo: 'Últimos 30 días', min: 1, max: 5, ...alinear([
    { nombre: 'Hambre', puntos: lista.map(o => ({ fecha: o.fecha, valor: o.nivelHambre })) },
    { nombre: 'Saciedad', puntos: lista.map(o => ({ fecha: o.fecha, valor: o.nivelSaciedad })) }]) });
  vaciar(hist, tabla([{ titulo: 'Fecha', render: o => fmt.fecha(o.fecha) }, { titulo: 'Hambre', campo: 'nivelHambre' }, { titulo: 'Saciedad', campo: 'nivelSaciedad' },
    { titulo: 'Ánimo', render: o => (o.estadoAnimo ? ETQ.estadoAnimo[o.estadoAnimo - 1] : '') }, { titulo: 'Comentario', campo: 'comentario' }], [...lista].reverse(), { vacio: 'Sin observaciones registradas' }));
  const hoy = lista.find(o => o.fecha === fecha.value);
  if (hoy) { valores.nivelHambre = hoy.nivelHambre; valores.nivelSaciedad = hoy.nivelSaciedad; valores.estadoAnimo = hoy.estadoAnimo; comentario.value = hoy.comentario || ''; }
}
historial().catch(manejarError);
