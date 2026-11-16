// D-25 Términos y condiciones (Ley 25.326): aceptación obligatoria.
import { api } from '../core/api.js';
import { actualizarUsuario, token, destinoInicial, limpiarSesion } from '../core/auth.js';
import { h, toast, manejarError } from '../core/ui.js';

const app = document.getElementById('app');
if (!token()) location.href = '/index.html';

const t = await api.get('/terminos/vigentes');
const check = h('input', { type: 'checkbox', id: 'acepto' });
const boton = h('button', { class: 'btn', disabled: true, onclick: aceptar }, 'Continuar');
check.addEventListener('change', () => { boton.disabled = !check.checked; });

async function aceptar() {
  try {
    const u = await api.post('/terminos/aceptar', { version: t.version });
    actualizarUsuario(u);
    toast('¡Gracias! Ya podés usar el sistema.');
    location.href = destinoInicial(u);
  } catch (err) {
    manejarError(err);
  }
}

app.append(h('main', {}, h('div', { class: 'tarjeta', style: 'max-width:760px;margin:1rem auto' },
  h('h1', {}, 'Términos y condiciones'),
  h('p', { class: 'suave' }, `Versión ${t.version}`),
  h('div', { style: 'white-space:pre-wrap;max-height:55vh;overflow:auto;border:1px solid var(--borde);border-radius:10px;padding:1rem;background:#fffdf7' }, t.texto),
  h('label', { class: 'check', for: 'acepto', style: 'margin-top:1rem' }, check, 'Leí y acepto los términos y condiciones y la política de privacidad'),
  h('div', { class: 'acciones' },
    h('button', { class: 'btn secundario', onclick: () => { limpiarSesion(); location.href = '/index.html'; } }, 'No acepto'),
    boton))));
