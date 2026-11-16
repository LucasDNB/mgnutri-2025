// RF-12 Contacto: central y profesionales asignados, con botón de WhatsApp (D-24).
import { api } from '../core/api.js';
import { iniciarPagina, encabezado } from '../core/pagina.js';
import { h } from '../core/ui.js';

const { main } = await iniciarPagina({ roles: ['PACIENTE'], titulo: 'Contacto' });
const c = await api.get('/me/contactos');
const wa = (url) => (url ? h('a', { class: 'btn', href: url, target: '_blank', rel: 'noopener' }, 'Escribir por WhatsApp') : null);
main.append(encabezado('Contacto', [['Inicio', '/paciente/home.html'], ['Contacto']]),
  h('div', { class: 'tarjeta' }, h('h2', {}, c.central.nombre), c.central.direccion ? h('p', {}, '📍 ', c.central.direccion) : null,
    c.central.horario ? h('p', {}, '🕘 ', c.central.horario) : null, wa(c.central.whatsappUrl)),
  c.profesionales.map(p => h('div', { class: 'tarjeta' }, h('h2', {}, p.nombre), h('p', {}, [p.especialidad, p.area].filter(Boolean).join(' · ')), wa(p.whatsappUrl))));
