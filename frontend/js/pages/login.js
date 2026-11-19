// CU-47 Autenticación · botón CONTACTO (RF-12) · enlace de recupero (CU-48)
import { api } from '../core/api.js';
import { guardarSesion, usuarioActual, token, destinoInicial } from '../core/auth.js';
import { h, toast, modal, campo, leerForm, manejarError } from '../core/ui.js';

const app = document.getElementById('app');
if (token() && usuarioActual()) {
  location.href = destinoInicial(usuarioActual());
}

const mensaje = sessionStorage.getItem('mg.mensaje');
sessionStorage.removeItem('mg.mensaje');

const form = h('form', { class: 'tarjeta', novalidate: true },
  h('div', { class: 'campos' },
    campo({ nombre: 'username', etiqueta: 'Usuario', requerido: true, autocomplete: 'username', ayuda: 'Tu usuario es tu número de documento, sin puntos.' }),
    campo({ nombre: 'password', etiqueta: 'Contraseña', tipo: 'password', requerido: true, autocomplete: 'current-password' })),
  h('div', { class: 'columna', style: 'margin-top:1rem' },
    h('button', { class: 'btn bloque', type: 'submit' }, 'INGRESAR'),
    h('button', { class: 'btn bloque secundario', type: 'button', onclick: verContacto }, 'CONTACTO'),
    h('a', { href: '/recupero.html', style: 'text-align:center' }, '¿Olvidaste tu contraseña?')));

form.addEventListener('submit', async (e) => {
  e.preventDefault();
  const datos = leerForm(form);
  if (!datos.username || !datos.password) {
    toast('Ingresá tu usuario y contraseña', 'advertencia');
    return;
  }
  const boton = form.querySelector('button[type=submit]');
  boton.disabled = true;
  try {
    const r = await api.post('/auth/login', datos);
    guardarSesion(r);
    location.href = destinoInicial(r.usuario);
  } catch (err) {
    manejarError(err);
    boton.disabled = false;
  }
});

async function verContacto() {
  try {
    const c = await api.get('/publico/contacto');
    await modal({
      titulo: 'Contacto',
      cuerpo: h('div', {},
        h('p', {}, h('strong', {}, c.nombre)),
        c.direccion ? h('p', {}, '📍 ', c.direccion) : null,
        c.horario ? h('p', {}, '🕘 ', c.horario) : null,
        c.telefono ? h('p', {}, '📞 ', c.telefono) : null,
        c.whatsappUrl ? h('a', { class: 'btn', href: c.whatsappUrl, target: '_blank', rel: 'noopener' }, 'Escribir por WhatsApp') : null),
    });
  } catch (err) {
    manejarError(err);
  }
}

app.append(h('div', { class: 'pantalla-login' }, h('div', { class: 'caja-login' },
  h('div', { class: 'logo grande' }, h('img', { src: '/assets/logo.png', alt: 'MG Nutrición y Entrenamiento' })),
  h('h1', {}, 'MG Nutrición y Entrenamiento'),
  mensaje ? h('div', { class: 'aviso advertencia' }, mensaje) : null,
  form)));
