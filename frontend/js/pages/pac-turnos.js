// CU-10 Mis turnos (próximos y pasados) · CU-08 reprogramar · CU-09 cancelar (regla de 24 h).
import { api } from '../core/api.js';
import { iniciarPagina, encabezado } from '../core/pagina.js';
import { h, toast, modal, confirmar, manejarError, vaciar, chip, param } from '../core/ui.js';
import { fmt, etiquetaEstado } from '../core/format.js';
import { selectorHuecos } from '../components/huecos.js';

const { main } = await iniciarPagina({ roles: ['PACIENTE'], titulo: 'Mis turnos' });
const area = param('area');
const cont = h('div');
main.append(encabezado('Mis turnos', [['Inicio', '/paciente/home.html'], ['Turnos']], h('a', { class: 'btn', href: `/paciente/turno-nuevo.html${area ? '?area=' + area : ''}` }, '+ Nuevo turno')), cont);

async function cargar() {
  const lista = (await api.get('/me/turnos')).filter(t => !area || t.area === area);
  const proximos = lista.filter(t => !t.pasado && t.estado === 'RESERVADO');
  const pasados = lista.filter(t => t.pasado || t.estado !== 'RESERVADO').reverse();
  const tarjeta = (t) => h('div', { class: 'ejercicio-card' },
    h('div', { class: 'fila entre' }, h('strong', { style: 'text-transform:capitalize' }, `${fmt.diaFecha(t.inicio)} · ${fmt.hora(t.inicio)}`), chip(etiquetaEstado(t.estado), t.estado)),
    h('div', {}, `${t.profesional.nombre} · ${t.area === 'NUTRICION' ? 'Nutrición' : 'Entrenamiento'}${t.ubicacion ? ' · ' + t.ubicacion : ''}`),
    t.estado === 'RESERVADO' && !t.pasado ? (t.puedeModificar ? h('div', { class: 'fila', style: 'margin-top:.5rem' },
      h('button', { class: 'btn chico secundario', onclick: () => reprogramar(t) }, 'Modificar'),
      h('button', { class: 'btn chico peligro secundario', onclick: () => cancelar(t) }, 'Cancelar'))
      : h('div', { class: 'aviso advertencia', style: 'margin-top:.5rem' }, 'No se puede modificar con menos de 24 h de anticipación. Comunicate con el centro.')) : null,
    t.motivoCancelacion ? h('small', { class: 'suave' }, `Motivo: ${t.motivoCancelacion}`) : null);
  vaciar(cont, h('div', { class: 'tarjeta' }, h('h2', {}, 'Próximos'), proximos.length ? proximos.map(tarjeta) : h('div', { class: 'vacio' }, 'No tenés turnos reservados')),
    h('div', { class: 'tarjeta' }, h('h2', {}, 'Anteriores (últimos 6 meses)'), pasados.length ? pasados.map(tarjeta) : h('div', { class: 'vacio' }, 'Sin turnos anteriores')));
}

async function cancelar(t) {
  if (!(await confirmar(`¿Cancelar tu turno del ${fmt.diaFecha(t.inicio)} a las ${fmt.hora(t.inicio)}?`, { texto: 'Cancelar turno', peligro: true }))) return;
  try { await api.post(`/me/turnos/${t.id}/cancelar`, { version: t.version }); toast('Turno cancelado'); cargar(); } catch (e) { manejarError(e); }
}

async function reprogramar(t) {
  let elegido = null;
  const info = h('div', { class: 'aviso' }, `Turno actual: ${fmt.diaFecha(t.inicio)} ${fmt.hora(t.inicio)}. Elegí un nuevo horario con ${t.profesional.nombre}.`);
  await modal({ titulo: 'Modificar turno', ancho: true, cuerpo: h('div', {}, info, selectorHuecos(t.profesional.id, (x) => { elegido = x; info.textContent = `Nuevo horario: ${fmt.diaFecha(x.inicio)} a las ${fmt.hora(x.inicio)}`; })),
    botones: [{ texto: 'Volver', clase: 'secundario' }, { texto: 'Confirmar cambio', accion: async () => {
      if (!elegido) { toast('Elegí un horario', 'advertencia'); return false; }
      const r = await api.post(`/me/turnos/${t.id}/reprogramar`, { inicio: elegido.inicio, version: t.version });
      toast(`Turno modificado para el ${fmt.diaFecha(r.inicio)} a las ${fmt.hora(r.inicio)}`);
      cargar();
    } }] });
}
cargar().catch(manejarError);
