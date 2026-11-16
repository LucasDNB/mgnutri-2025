// CU-42 Registrar molestias físicas: intensidad 1–10 con escala visual, zona, contexto y ejercicio opcional.
import { api } from '../core/api.js';
import { iniciarPagina, encabezado } from '../core/pagina.js';
import { h, toast, manejarError, vaciar, tabla, chip, param } from '../core/ui.js';
import { fmt } from '../core/format.js';

const { main } = await iniciarPagina({ roles: ['PACIENTE'], titulo: 'Registrar molestia' });
const ejercicioRutinaId = param('ejercicioRutinaId');
let intensidad = null;
const ZONAS = ['Cuello', 'Hombro izq.', 'Hombro der.', 'Espalda alta', 'Zona lumbar', 'Codo', 'Muñeca', 'Cadera', 'Rodilla izq.', 'Rodilla der.', 'Tobillo', 'Otra'];
const desc = h('textarea', { maxLength: 300, rows: 3, placeholder: 'Contanos qué sentís y cuándo' });
const zona = h('select', {}, h('option', { value: '' }, 'Elegí la zona…'), ZONAS.map(z => h('option', { value: z }, z)));
const ctx = h('select', {}, [['ENTRENAMIENTO', 'Durante el entrenamiento'], ['ALIMENTACION', 'Relacionada con la alimentación'], ['GENERAL', 'General']].map(([v, t]) => h('option', { value: v }, t)));
const escala = h('div', { class: 'escala', role: 'radiogroup', 'aria-label': 'Intensidad' });
const dibujarEscala = () => vaciar(escala, Array.from({ length: 10 }, (_, i) => i + 1).map(n => h('button', { type: 'button', class: intensidad === n ? 'sel' : '', role: 'radio', 'aria-checked': intensidad === n,
  style: n >= 7 ? 'border-color:var(--rojo)' : '', onclick: () => { intensidad = n; dibujarEscala(); } }, n)));
dibujarEscala();
const hist = h('div');
main.append(encabezado('Registrar molestia', [['Entrenamiento', '/paciente/entrenamiento.html'], ['Molestia']]),
  h('div', { class: 'tarjeta columna' },
    ejercicioRutinaId ? h('div', { class: 'aviso' }, 'Molestia relacionada con un ejercicio de tu rutina.') : null,
    h('div', { class: 'campo' }, h('label', { class: 'obligatorio' }, 'Descripción'), desc),
    h('div', { class: 'campo' }, h('label', { class: 'obligatorio' }, 'Intensidad (1 = leve · 10 = muy intensa)'), escala),
    h('div', { class: 'campo' }, h('label', {}, 'Zona del cuerpo'), zona),
    h('div', { class: 'campo' }, h('label', {}, 'Contexto'), ctx),
    h('button', { class: 'btn', onclick: guardar }, 'Enviar a mi profesional'),
    h('small', { class: 'suave' }, 'Si el dolor es intenso o persistente, consultá a un médico. El sistema no realiza diagnósticos.')),
  h('div', { class: 'tarjeta' }, h('h2', {}, 'Mis molestias registradas'), hist));

async function guardar() {
  if (!desc.value.trim() || !intensidad) { toast('Completá la descripción y la intensidad', 'advertencia'); return; }
  try {
    await api.post('/me/molestias', { descripcion: desc.value.trim(), intensidad, ubicacionCorporal: zona.value || null, contexto: ctx.value, ejercicioRutinaId: ejercicioRutinaId ? Number(ejercicioRutinaId) : null });
    toast('Tu profesional fue avisado');
    desc.value = '';
    intensidad = null;
    dibujarEscala();
    historial();
  } catch (e) { manejarError(e); }
}

async function historial() {
  const lista = await api.get('/me/molestias');
  vaciar(hist, tabla([{ titulo: 'Fecha', render: m => fmt.fecha(m.fecha) }, { titulo: 'Descripción', campo: 'descripcion' },
    { titulo: 'Intensidad', render: m => chip(`${m.intensidad}/10`, m.prioritaria ? 'prioritaria' : '') }, { titulo: 'Zona', campo: 'ubicacionCorporal' },
    { titulo: 'Revisada', render: m => (m.revisadaAt ? '✔' : '') }], lista, { vacio: 'No registraste molestias' }));
}
historial().catch(manejarError);
