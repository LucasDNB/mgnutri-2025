// CU-48 Recuperar contraseña: solicitud y confirmación con token de un solo uso.
import { api } from '../core/api.js';
import { h, toast, campo, leerForm, manejarError, param, vaciar } from '../core/ui.js';
import { validar, fortaleza } from '../core/validators.js';

const app = document.getElementById('app');
const tokenUrl = param('token');
const caja = h('div', { class: 'caja-login' }, h('div', { class: 'logo grande' }, 'MG'));
app.append(h('div', { class: 'pantalla-login' }, caja));

if (!tokenUrl) {
  const form = h('form', { class: 'tarjeta', novalidate: true },
    h('p', {}, 'Ingresá tu número de documento o tu teléfono. Te enviaremos un enlace por WhatsApp para crear una nueva contraseña.'),
    campo({ nombre: 'documentoOTelefono', etiqueta: 'Documento o teléfono', requerido: true }),
    h('div', { class: 'acciones' }, h('a', { href: '/index.html', class: 'btn secundario' }, 'Volver'), h('button', { class: 'btn', type: 'submit' }, 'Enviar enlace')));
  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const d = leerForm(form);
    if (!d.documentoOTelefono) { toast('Ingresá tu documento o teléfono', 'advertencia'); return; }
    try {
      const r = await api.post('/auth/recupero', d);
      vaciar(form, h('div', { class: 'aviso' }, r.mensaje), h('a', { href: '/index.html', class: 'btn' }, 'Volver al inicio'));
    } catch (err) {
      manejarError(err, form);
    }
  });
  caja.append(h('h1', {}, 'Recuperar contraseña'), form);
} else {
  caja.append(h('h1', {}, 'Nueva contraseña'));
  const valido = await api.get(`/auth/recupero/${encodeURIComponent(tokenUrl)}`).catch(() => ({ valido: false }));
  if (!valido.valido) {
    caja.append(h('div', { class: 'tarjeta' }, h('div', { class: 'aviso error' }, 'El enlace ya no es válido. Pedí uno nuevo.'),
      h('a', { class: 'btn', href: '/recupero.html' }, 'Pedir un enlace nuevo')));
  } else {
    const medidor = h('div', { class: 'barra' }, h('span', { style: 'width:0' }));
    const form = h('form', { class: 'tarjeta', novalidate: true },
      h('p', {}, `Usuario: ${valido.username}`),
      h('div', { class: 'campos' },
        campo({ nombre: 'nuevaPassword', etiqueta: 'Nueva contraseña', tipo: 'password', requerido: true, autocomplete: 'new-password',
          ayuda: 'Mínimo 8 caracteres, con al menos una letra y un número. No uses tu documento.' }),
        medidor,
        campo({ nombre: 'confirmacion', etiqueta: 'Repetí la contraseña', tipo: 'password', requerido: true, autocomplete: 'new-password' })),
      h('div', { class: 'acciones' }, h('button', { class: 'btn', type: 'submit' }, 'Guardar contraseña')));
    form.nuevaPassword.addEventListener('input', () => {
      medidor.firstChild.style.width = `${fortaleza(form.nuevaPassword.value) * 25}%`;
    });
    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      const d = leerForm(form);
      const err = validar.password(d.nuevaPassword, valido.username);
      if (err) { toast(err, 'advertencia'); return; }
      if (d.nuevaPassword !== d.confirmacion) { toast('Las contraseñas no coinciden', 'advertencia'); return; }
      try {
        await api.post('/auth/recupero/confirmar', { token: tokenUrl, nuevaPassword: d.nuevaPassword });
        vaciar(form, h('div', { class: 'aviso' }, 'Tu contraseña se actualizó. Ya podés ingresar.'), h('a', { href: '/index.html', class: 'btn' }, 'Ingresar'));
      } catch (er) {
        manejarError(er, form);
      }
    });
    caja.append(form);
  }
}
