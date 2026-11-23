// CU-07 Solicitar turno: 1) profesional (habituales primero) 2) horario 3) confirmación. Regla de 7 días.
import { api, qs, ApiError } from '../core/api.js';
import { iniciarPagina, encabezado } from '../core/pagina.js';
import { h, toast, manejarError, vaciar, chip, modal } from '../core/ui.js';
import { fmt } from '../core/format.js';
import { selectorHuecos } from '../components/huecos.js';

const { main } = await iniciarPagina({ roles: ['PACIENTE'], titulo: 'Nuevo turno' });
// Los turnos son sólo de nutrición (v1.1)
const area = 'NUTRICION';
const paso = h('div');
main.append(encabezado('Nuevo turno de nutrición', [['Turnos', '/paciente/turnos.html'], ['Nuevo']]), paso);

async function paso1() {
  const profs = await api.get(`/profesionales/disponibles${qs({ area })}`);
  vaciar(paso, h('div', { class: 'tarjeta' }, h('h2', {}, '1. Elegí el profesional'),
    profs.length ? profs.map(p => h('button', { class: 'btn secundario bloque', style: 'justify-content:space-between;margin-bottom:.5rem', onclick: () => paso2(p) },
      h('span', {}, p.nombre, p.especialidad ? h('small', { class: 'suave' }, ` · ${p.especialidad}`) : null), p.asignado ? chip('Tu profesional', 'OK') : null))
      : h('div', { class: 'vacio' }, 'No hay nutricionistas con agenda disponible.')));
}

function paso2(p) {
  vaciar(paso, h('div', { class: 'tarjeta' }, h('h2', {}, `2. Elegí el horario con ${p.nombre}`),
    h('p', { class: 'suave' }, 'Se muestran los próximos 30 días, en hora de Argentina.'),
    selectorHuecos(p.id, (x) => paso3(p, x)), h('button', { class: 'btn secundario', style: 'margin-top:1rem', onclick: paso1 }, '‹ Cambiar profesional')));
}

async function paso3(p, hueco) {
  const ok = await modal({ titulo: '3. Confirmá tu turno', cuerpo: h('div', {},
    h('p', { style: 'text-transform:capitalize' }, h('strong', {}, `${fmt.diaFecha(hueco.inicio)} a las ${fmt.hora(hueco.inicio)}`)),
    h('p', {}, `Con ${p.nombre}`), hueco.ubicacion ? h('p', {}, `Lugar: ${hueco.ubicacion}`) : null),
  botones: [{ texto: 'Volver', clase: 'secundario', valor: false }, { texto: 'Confirmar turno', valor: true }] });
  if (!ok) return;
  try {
    const t = await api.post('/me/turnos', { profesionalId: p.id, inicio: hueco.inicio, area });
    vaciar(paso, h('div', { class: 'tarjeta' }, h('div', { class: 'aviso' }, `Turno confirmado para el ${fmt.diaFecha(t.inicio)} a las ${fmt.hora(t.inicio)} con ${t.profesional.nombre}. Te enviamos la confirmación por WhatsApp.`),
      h('a', { class: 'btn', href: '/paciente/turnos.html' }, 'Ver mis turnos')));
  } catch (e) {
    if (e instanceof ApiError && e.code === 'TURNO_PROXIMO_EXISTENTE') {
      vaciar(paso, h('div', { class: 'tarjeta' }, h('div', { class: 'aviso advertencia' }, e.message), h('a', { class: 'btn', href: '/paciente/turnos.html' }, 'Modificar o cancelar turnos')));
    } else if (e instanceof ApiError && e.code === 'HORARIO_NO_DISPONIBLE') {
      toast(e.message, 'error');
      paso2(p);
    } else manejarError(e);
  }
}
paso1().catch(manejarError);
