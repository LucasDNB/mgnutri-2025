// Parámetros del sistema (configuracion_sistema) y versiones de términos y condiciones (US-13.5.3).
import { api } from '../core/api.js';
import { iniciarPagina, encabezado } from '../core/pagina.js';
import { h, tabla, modal, campo, leerForm, toast, manejarError, vaciar } from '../core/ui.js';
import { fmt } from '../core/format.js';

const { main } = await iniciarPagina({ roles: ['ADMIN'], titulo: 'Configuración' });
const form = h('form');
const terminos = h('div');
main.append(encabezado('Configuración', [['Opciones avanzadas', '/admin/opciones.html'], ['Configuración']]),
  h('div', { class: 'tarjeta' }, h('h2', {}, 'Parámetros'), form),
  h('div', { class: 'tarjeta' }, h('div', { class: 'fila entre' }, h('h2', {}, 'Términos y condiciones'), h('button', { class: 'btn chico', onclick: nuevaVersion }, 'Publicar nueva versión')), terminos));

async function cargar() {
  const params = await api.get('/configuracion');
  vaciar(form, h('div', { class: 'campos c2' }, params.filter(p => p.editable && !p.clave.startsWith('backup.')).map(p =>
    campo({ nombre: p.clave, etiqueta: p.descripcion || p.clave, valor: p.valor, ayuda: `Clave: ${p.clave}${p.updatedBy ? ` · modificado por ${p.updatedBy}` : ''}` }))),
  h('div', { class: 'acciones' }, h('button', { class: 'btn', type: 'submit' }, 'Guardar')));
}
form.addEventListener('submit', async (e) => {
  e.preventDefault();
  const valores = {};
  for (const el of form.elements) if (el.name) valores[el.name] = el.value;
  try { await api.put('/configuracion', valores); toast('Configuración guardada'); cargar(); } catch (er) { manejarError(er, form); }
});

async function cargarTerminos() {
  const lista = await api.get('/terminos');
  vaciar(terminos, tabla([{ titulo: 'Versión', campo: 'version' }, { titulo: 'Vigente desde', render: t => fmt.fechaHora(t.vigenteDesde) },
    { titulo: '', render: t => h('button', { class: 'btn chico secundario', onclick: () => modal({ titulo: `Versión ${t.version}`, cuerpo: h('div', { style: 'white-space:pre-wrap' }, t.texto) }) }, 'Ver') }], lista));
}

async function nuevaVersion() {
  const f = h('form', {}, campo({ nombre: 'version', etiqueta: 'Versión', requerido: true, maxLength: 10 }),
    campo({ nombre: 'texto', etiqueta: 'Texto', tipo: 'textarea', requerido: true, rows: 14 }),
    h('p', { class: 'suave' }, 'Todos los usuarios deberán aceptar la nueva versión en su próximo ingreso.'));
  await modal({ titulo: 'Nueva versión de términos', cuerpo: f, ancho: true, botones: [{ texto: 'Cancelar', clase: 'secundario' }, { texto: 'Publicar', accion: async () => {
    await api.post('/terminos', leerForm(f));
    toast('Nueva versión publicada');
    cargarTerminos();
  } }] });
}
cargar().catch(manejarError);
cargarTerminos().catch(manejarError);
