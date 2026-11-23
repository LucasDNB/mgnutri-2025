// CA-02.5.1 Home del profesional: Calendario (nutricionista e integral), Pacientes y Novedades.
import { api, qs } from '../core/api.js';
import { iniciarPagina, actualizarContadorNovedades, usaAgenda } from '../core/pagina.js';
import { h, tabla, chip } from '../core/ui.js';
import { fmt, hoyISO } from '../core/format.js';

const { usuario, main } = await iniciarPagina({ roles: ['NUTRICIONISTA', 'ENTRENADOR'], titulo: 'Inicio' });
const novedades = await actualizarContadorNovedades();
const b = (t, href, badge, sec) => h('a', { class: `boton-home ${sec ? 'secundario' : ''}`, href }, t, badge ? h('span', { class: 'badge' }, badge) : null);
const conAgenda = usaAgenda(usuario);
const hoy = conAgenda && usuario.tieneAgenda ? await api.get(`/turnos${qs({ desde: hoyISO(), hasta: hoyISO() })}`).catch(() => []) : [];

main.append(h('h1', {}, `Bienvenido/a ${usuario.nombre}`),
  h('div', { class: 'botones-home ancho' },
    conAgenda ? b('Calendario', '/comun/calendario.html') : null,
    b('Pacientes', '/comun/pacientes.html'),
    // Catálogos y reportes quedan en el menú: el inicio sólo muestra Calendario (si usa agenda), Pacientes y Novedades
    b('Novedades', '/profesional/novedades.html', novedades || null)),
  !conAgenda ? null : usuario.tieneAgenda ? h('div', { class: 'tarjeta' }, h('h2', {}, 'Turnos de hoy'), tabla([
    { titulo: 'Hora', render: t => fmt.hora(t.inicio) },
    { titulo: 'Paciente', render: t => h('a', { href: `/comun/paciente.html?id=${t.paciente.id}` }, t.paciente.nombre) },
    { titulo: 'Ubicación', campo: 'ubicacion' }, { titulo: 'Estado', render: t => chip(t.estado, t.estado) },
  ], hoy, { vacio: 'No tenés turnos para hoy' })) : h('div', { class: 'aviso' }, 'Todavía no tenés agenda cargada. ', h('a', { href: '/profesional/agenda.html' }, 'Cargá tus horarios'), ' para recibir turnos.'));
