// CA-08.0.1 Home del paciente: NUTRICIÓN · ENTRENAMIENTO · CONTACTO + próximo turno y accesos.
import { api } from '../core/api.js';
import { iniciarPagina } from '../core/pagina.js';
import { h } from '../core/ui.js';
import { fmt } from '../core/format.js';

const { main } = await iniciarPagina({ roles: ['PACIENTE'], titulo: 'Inicio' });
const home = await api.get('/me/home');
const t = home.proximoTurno;
main.append(
  h('div', { style: 'text-align:center' }, h('div', { class: 'logo grande' }, h('img', { src: '/assets/logo.png', alt: 'MG Nutrición y Entrenamiento' })), h('h1', {}, `¡Hola, ${home.nombre}!`)),
  t ? h('div', { class: 'tarjeta', style: 'max-width:520px;margin:0 auto 1rem' }, h('strong', {}, 'Tu próximo turno: '),
    `${fmt.diaFecha(t.inicio)} a las ${fmt.hora(t.inicio)} con ${t.profesional.nombre}${t.ubicacion ? ' · ' + t.ubicacion : ''}`) : null,
  h('div', { class: 'botones-home' },
    h('a', { class: 'boton-home', href: '/paciente/nutricion.html' }, 'Nutrición'),
    h('a', { class: 'boton-home', href: '/paciente/entrenamiento.html' }, 'Entrenamiento'),
    h('a', { class: 'boton-home secundario', href: '/paciente/contacto.html' }, 'Contacto')),
  h('div', { class: 'fila', style: 'justify-content:center' },
    h('a', { class: 'btn secundario', href: '/paciente/turnos.html' }, 'Mis turnos'),
    h('a', { class: 'btn secundario', href: '/comun/evolucion.html' }, 'Mi evolución'),
    h('a', { class: 'btn secundario', href: '/paciente/cuenta.html' }, 'Mi cuenta'),
    h('a', { class: 'btn secundario', href: '/paciente/perfil.html' }, 'Perfil')));
