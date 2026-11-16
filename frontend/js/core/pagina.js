// Estructura común de las páginas autenticadas: guard por rol (router-guard), cabecera, menú y banner.
import { api } from './api.js';
import { usuarioActual, actualizarUsuario, limpiarSesion, destinoInicial, token } from './auth.js';
import { h, vaciar, confirmar } from './ui.js';

const MENUS = {
  ADMIN: [
    ['Inicio', '/admin/home.html'], ['Calendario', '/comun/calendario.html'], ['Pacientes', '/comun/pacientes.html'],
    ['Profesionales', '/admin/profesionales.html'], ['Pagos', '/admin/pagos.html'], ['Reportes', '/comun/reportes.html'],
    ['Opciones avanzadas', '/admin/opciones.html'],
  ],
  PROFESIONAL: [
    ['Inicio', '/profesional/home.html'], ['Calendario', '/comun/calendario.html', 'agenda'], ['Pacientes', '/comun/pacientes.html'],
    ['Novedades', '/profesional/novedades.html', 'novedades'], ['Mi agenda', '/profesional/agenda.html'],
    ['Alimentos', '/comun/alimentos.html', 'nutri'], ['Ejercicios', '/comun/ejercicios.html', 'entr'], ['Reportes', '/comun/reportes.html'],
  ],
  PACIENTE: [
    ['Inicio', '/paciente/home.html'], ['Nutrición', '/paciente/nutricion.html'], ['Entrenamiento', '/paciente/entrenamiento.html'],
    ['Turnos', '/paciente/turnos.html'], ['Mi evolución', '/comun/evolucion.html'], ['Mi cuenta', '/paciente/cuenta.html'],
    ['Contacto', '/paciente/contacto.html'], ['Perfil', '/paciente/perfil.html'],
  ],
};

function grupo(u) {
  if (u.roles.includes('ADMIN')) return 'ADMIN';
  if (u.roles.includes('PACIENTE')) return 'PACIENTE';
  return 'PROFESIONAL';
}

/**
 * Inicializa una página protegida.
 * @param {{roles?: string[], titulo: string}} opciones roles permitidos (ADMIN, NUTRICIONISTA, ENTRENADOR, PACIENTE)
 * @returns {Promise<{usuario: object, main: HTMLElement}>}
 */
export async function iniciarPagina({ roles, titulo }) {
  if (!token()) {
    location.href = '/index.html';
    throw new Error('sin sesión');
  }
  let u = usuarioActual();
  try {
    u = await api.get('/me');
    actualizarUsuario(u);
  } catch {
    // api.js ya redirige ante 401/428
  }
  if (!u) {
    location.href = '/index.html';
    throw new Error('sin sesión');
  }
  if (u.debeCambiarPassword || u.debeAceptarTerminos) {
    location.href = destinoInicial(u);
    throw new Error('primer ingreso');
  }
  if (roles && !roles.some(r => u.roles.includes(r))) {
    location.href = u.home;
    throw new Error('rol no permitido');
  }
  document.title = `${titulo} · MG Nutrición y Entrenamiento`;
  const g = grupo(u);
  const nav = h('nav', { class: 'nav', id: 'menu', 'aria-label': 'Menú principal' });
  const ruta = location.pathname;
  for (const [texto, href, cond] of MENUS[g]) {
    if (cond === 'agenda' && !u.tieneAgenda) continue;
    if (cond === 'nutri' && !u.roles.includes('NUTRICIONISTA')) continue;
    if (cond === 'entr' && !u.roles.includes('ENTRENADOR')) continue;
    const a = h('a', { href, class: ruta === href ? 'activo' : '' }, texto);
    if (cond === 'novedades') a.id = 'nav-novedades';
    nav.appendChild(a);
  }
  nav.appendChild(h('a', { href: '/cambiar-password.html' }, 'Cambiar contraseña'));
  nav.appendChild(h('a', { href: '#', onclick: (e) => { e.preventDefault(); salir(); } }, 'Cerrar sesión'));
  const cabecera = h('header', { class: 'cabecera' },
    h('button', { class: 'btn-menu', 'aria-label': 'Abrir menú', 'aria-controls': 'menu', onclick: () => nav.classList.toggle('abierto') }, '☰'),
    h('a', { class: 'marca', href: u.home }, h('span', { class: 'logo' }, 'MG'), h('span', {}, 'MG Nutrición')),
    h('span', { class: 'espacio' }),
    h('span', { class: 'usuario' }, u.nombre),
    h('button', { class: 'btn chico secundario', onclick: salir, style: 'border-color:#fff' }, 'Salir'));
  const main = h('main', { id: 'contenido' });
  // append tolerante a null/false (las páginas arman contenido condicional)
  const appendOriginal = main.append.bind(main);
  main.append = (...hijos) => appendOriginal(...hijos.flat(Infinity).filter(x => x !== null && x !== undefined && x !== false));
  vaciar(document.body, cabecera, nav, main);
  mostrarBanner();
  if (g === 'PROFESIONAL') actualizarContadorNovedades();
  return { usuario: u, main };
}

async function mostrarBanner() {
  try {
    const c = await api.get('/publico/contacto');
    if (c.mantenimiento) document.body.insertBefore(h('div', { class: 'banner', role: 'note' }, c.mantenimiento), document.querySelector('main'));
  } catch { /* opcional */ }
}

export async function actualizarContadorNovedades() {
  try {
    const { noLeidas } = await api.get('/novedades/contador');
    const a = document.getElementById('nav-novedades');
    if (a) {
      a.querySelector('.contador')?.remove();
      if (noLeidas > 0) a.appendChild(h('span', { class: 'contador' }, noLeidas));
    }
    return noLeidas;
  } catch {
    return 0;
  }
}

/** CU-50: revoca el token en el servidor y lo borra del navegador. */
export async function salir() {
  if (!(await confirmar('¿Querés cerrar la sesión?', { texto: 'Cerrar sesión' }))) return;
  try { await api.post('/auth/logout'); } catch { /* igual se limpia */ }
  limpiarSesion();
  location.href = '/index.html';
}

/** Encabezado de sección con migas de pan opcionales. */
export function encabezado(titulo, migas = [], ...acciones) {
  return h('div', {},
    migas.length ? h('div', { class: 'migas' }, migas.map(([t, href], i) => [i ? ' › ' : '', href ? h('a', { href }, t) : t])) : null,
    h('div', { class: 'fila entre' }, h('h1', {}, titulo), h('div', { class: 'fila' }, acciones)));
}
