// CA-09.0.1 Menú Entrenamiento del paciente.
import { api } from '../core/api.js';
import { iniciarPagina, encabezado } from '../core/pagina.js';
import { h } from '../core/ui.js';

const { main } = await iniciarPagina({ roles: ['PACIENTE'], titulo: 'Entrenamiento' });
const home = await api.get('/me/home');
main.append(encabezado('Entrenamiento', [['Inicio', '/paciente/home.html'], ['Entrenamiento']]),
  home.tieneRutina ? null : h('div', { class: 'aviso advertencia' }, 'Todavía no tenés una rutina asignada'),
  h('div', { class: 'botones-home' },
    home.tieneRutina ? h('a', { class: 'boton-home', href: '/paciente/mi-rutina.html' }, 'Mi rutina')
      : h('div', { class: 'boton-home', style: 'opacity:.5;cursor:not-allowed', 'aria-disabled': 'true' }, 'Mi rutina'),
    h('a', { class: 'boton-home secundario', href: '/paciente/molestia.html' }, 'Registrar molestia'),
    home.entrenadorConAgenda ? h('a', { class: 'boton-home secundario', href: '/paciente/turnos.html?area=ENTRENAMIENTO' }, 'Turnos de entrenamiento') : null));
