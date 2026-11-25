// CA-08.0.5 Menú Nutrición del paciente.
import { api } from '../core/api.js';
import { iniciarPagina, encabezado } from '../core/pagina.js';
import { h } from '../core/ui.js';

const { main } = await iniciarPagina({ roles: ['PACIENTE'], titulo: 'Nutrición' });
const home = await api.get('/me/home');
const bloqueado = (texto) => h('div', { class: 'boton-home', style: 'opacity:.5;cursor:not-allowed', 'aria-disabled': 'true', title: 'Todavía no tenés un plan alimentario asignado' }, texto);
main.append(encabezado('Nutrición', [['Inicio', '/paciente/home.html'], ['Nutrición']]),
  home.tienePlan ? null : h('div', { class: 'aviso advertencia' }, 'Todavía no tenés un plan alimentario asignado.'),
  h('div', { class: 'botones-home' },
    home.tienePlan ? h('a', { class: 'boton-home', href: '/paciente/mi-plan.html' }, 'Mi plan alimentario') : bloqueado('Mi plan alimentario'),
    home.tienePlan ? h('a', { class: 'boton-home', href: '/paciente/registro-dia.html' }, 'Registro del día') : bloqueado('Registro del día'),
    h('a', { class: 'boton-home secundario', href: '/paciente/turnos.html?area=NUTRICION' }, 'Turnos de nutrición')));
