// US-01.5.3 Perfil del paciente: sólo email y domicilio son editables.
import { api } from '../core/api.js';
import { iniciarPagina, encabezado } from '../core/pagina.js';
import { h, campo, leerForm, toast, manejarError, validarForm } from '../core/ui.js';
import { fmt } from '../core/format.js';
import { validar } from '../core/validators.js';

const { main } = await iniciarPagina({ roles: ['PACIENTE'], titulo: 'Mi perfil' });
const p = await api.get('/me/perfil');
const form = h('form', { class: 'tarjeta', novalidate: true }, h('div', { class: 'campos c2' },
  campo({ nombre: 'email', etiqueta: 'Email', tipo: 'email', valor: p.email }),
  campo({ nombre: 'domicilio', etiqueta: 'Domicilio', valor: p.domicilio, maxLength: 200 })),
h('div', { class: 'acciones' }, h('button', { class: 'btn', type: 'submit' }, 'Guardar')));
form.addEventListener('submit', async (e) => {
  e.preventDefault();
  if (!validarForm(form, { email: validar.email })) return;
  try { await api.patch('/me/perfil', leerForm(form)); toast('Datos actualizados'); } catch (er) { manejarError(er, form); }
});
const fila = (k, v) => h('tr', {}, h('th', { style: 'width:40%' }, k), h('td', {}, v || '—'));
main.append(encabezado('Mi perfil', [['Inicio', '/paciente/home.html'], ['Perfil']]),
  h('div', { class: 'tarjeta' }, h('table', {}, h('tbody', {}, fila('Nombre', `${p.nombre} ${p.apellido}`), fila('Documento', `${p.tipoDocumento} ${p.nroDocumento}`),
    fila('Fecha de nacimiento', fmt.fecha(p.fechaNacimiento)), fila('WhatsApp', p.telefonoWhatsapp), fila('Objetivo', p.objetivo))),
  h('p', { class: 'suave' }, 'Para modificar estos datos comunicate con el centro.')),
  form,
  h('div', { class: 'fila' }, h('a', { class: 'btn secundario', href: '/cambiar-password.html' }, 'Cambiar contraseña')));
