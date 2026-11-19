// CU-49 Cambiar contraseña (también obligatorio en el primer ingreso, D-04).
import { api } from '../core/api.js';
import { guardarSesion, usuarioActual, token, destinoInicial, limpiarSesion } from '../core/auth.js';
import { h, toast, campo, leerForm, manejarError, param } from '../core/ui.js';
import { validar, fortaleza } from '../core/validators.js';

const app = document.getElementById('app');
if (!token()) location.href = '/index.html';
const u = usuarioActual();
const primer = param('primer') === '1' || u?.debeCambiarPassword;

const medidor = h('div', { class: 'barra', title: 'Fortaleza' }, h('span', { style: 'width:0' }));
const form = h('form', { class: 'tarjeta', novalidate: true },
  primer ? h('div', { class: 'aviso advertencia' }, 'Es tu primer ingreso: por seguridad tenés que reemplazar la contraseña temporal.') : null,
  h('div', { class: 'campos' },
    campo({ nombre: 'actual', etiqueta: primer ? 'Contraseña temporal' : 'Contraseña actual', tipo: 'password', requerido: true, autocomplete: 'current-password' }),
    campo({ nombre: 'nueva', etiqueta: 'Nueva contraseña', tipo: 'password', requerido: true, autocomplete: 'new-password',
      ayuda: 'Mínimo 8 caracteres, con al menos una letra y un número. Debe ser distinta de la actual y no contener tu documento.' }),
    medidor,
    campo({ nombre: 'confirmacion', etiqueta: 'Repetí la nueva contraseña', tipo: 'password', requerido: true, autocomplete: 'new-password' })),
  h('div', { class: 'acciones' },
    primer ? h('button', { type: 'button', class: 'btn secundario', onclick: () => { limpiarSesion(); location.href = '/index.html'; } }, 'Salir')
      : h('a', { href: u?.home || '/index.html', class: 'btn secundario' }, 'Volver'),
    h('button', { class: 'btn', type: 'submit' }, 'Cambiar contraseña')));

form.nueva.addEventListener('input', () => {
  const f = fortaleza(form.nueva.value);
  medidor.firstChild.style.width = `${f * 25}%`;
  medidor.firstChild.style.background = f <= 1 ? 'var(--rojo)' : f === 2 ? 'var(--ambar)' : 'var(--verde-2)';
});

form.addEventListener('submit', async (e) => {
  e.preventDefault();
  const d = leerForm(form);
  const err = validar.password(d.nueva, u?.username);
  if (!d.actual) { toast('Ingresá tu contraseña actual', 'advertencia'); return; }
  if (err) { toast(err, 'advertencia'); return; }
  if (d.nueva !== d.confirmacion) { toast('Las contraseñas no coinciden', 'advertencia'); return; }
  try {
    const r = await api.post('/auth/cambiar-password', { actual: d.actual, nueva: d.nueva });
    guardarSesion(r);
    toast('Contraseña actualizada', 'ok');
    setTimeout(() => { location.href = destinoInicial(r.usuario); }, 600);
  } catch (er) {
    manejarError(er, form);
  }
});

app.append(h('div', { class: 'pantalla-login' }, h('div', { class: 'caja-login', style: 'max-width:440px' },
  h('div', { class: 'logo grande' }, h('img', { src: '/assets/logo.png', alt: 'MG Nutrición y Entrenamiento' })), h('h1', {}, 'Cambiar contraseña'), form)));
