// CU-54A/B/C Base de datos de ejercicios (ABM: admin y entrenadores; lectura: nutricionistas).
import { api, qs } from '../core/api.js';
import { iniciarPagina, encabezado } from '../core/pagina.js';
import { h, tabla, paginador, campo, leerForm, modal, toast, confirmar, manejarError, vaciar, demorar, chip } from '../core/ui.js';

const { usuario, main } = await iniciarPagina({ roles: ['ADMIN', 'NUTRICIONISTA', 'ENTRENADOR'], titulo: 'Base de ejercicios' });
const puedeEditar = usuario.roles.some(r => ['ADMIN', 'ENTRENADOR'].includes(r));
const grupos = await api.get('/grupos-musculares');
let pagina = 0;
const q = h('input', { type: 'search', placeholder: 'Buscar ejercicio' });
const grupo = h('select', {}, h('option', { value: '' }, 'Todos los grupos'), grupos.map(g => h('option', { value: g.id }, g.nombre)));
const dif = h('select', {}, [['', 'Toda dificultad'], ['BAJA', 'Baja'], ['MEDIA', 'Media'], ['ALTA', 'Alta']].map(([v, t]) => h('option', { value: v }, t)));
const activo = h('select', {}, h('option', { value: 'true' }, 'Activos'), h('option', { value: 'false' }, 'Inactivos'), h('option', { value: '' }, 'Todos'));
const res = h('div');
main.append(encabezado('Base de ejercicios', [], puedeEditar ? h('button', { class: 'btn', onclick: () => editar() }, '+ Nuevo ejercicio') : null),
  h('div', { class: 'tarjeta' }, h('div', { class: 'fila' }, q, grupo, dif, activo), h('div', { style: 'margin-top:.8rem' }, res)));

// Cada carga lleva un número: si llega la respuesta de un pedido viejo (p. ej. la carga inicial después de
// aplicar un filtro), se descarta para no pisar el resultado más reciente.
let pedidos = 0;
async function cargar() {
  const n = ++pedidos;
  try {
    const r = await api.get(`/ejercicios${qs({ q: q.value, grupoMuscularId: grupo.value, dificultad: dif.value, activo: activo.value, page: pagina })}`);
    if (n !== pedidos) return;
    vaciar(res, tabla([
      { titulo: 'Ejercicio', render: e => [e.nombre, e.descripcion ? h('div', { class: 'suave' }, e.descripcion) : null] },
      { titulo: 'Grupo muscular', render: e => [e.grupoMuscular, e.gruposSecundarios.length ? h('div', { class: 'suave' }, e.gruposSecundarios.map(g => g.nombre).join(', ')) : null] },
      { titulo: 'Dificultad', render: e => chip(e.dificultad.toLowerCase()) }, { titulo: 'Equipamiento', campo: 'equipamiento' },
      { titulo: 'Video', render: e => e.videoUrl ? h('a', { href: e.videoUrl, target: '_blank', rel: 'noopener' }, 'Ver') : '' },
      { titulo: '', render: e => puedeEditar ? h('div', { class: 'fila' }, h('button', { class: 'btn chico secundario', onclick: () => editar(e) }, 'Modificar'),
        e.activo ? h('button', { class: 'btn chico peligro secundario', onclick: () => baja(e) }, 'Dar de baja')
          : h('button', { class: 'btn chico secundario', onclick: async () => { await api.post(`/ejercicios/${e.id}/reactivar`); toast('Ejercicio reactivado'); cargar(); } }, 'Reactivar')) : '' },
    ], r.content), paginador(r, (p) => { pagina = p; cargar(); }));
  } catch (e) { manejarError(e); }
}

async function baja(e) {
  if (!(await confirmar(`¿Dar de baja "${e.nombre}"? Las rutinas existentes lo conservan.`, { texto: 'Dar de baja', peligro: true }))) return;
  try { await api.del(`/ejercicios/${e.id}`); toast('Ejercicio dado de baja'); cargar(); } catch (er) { manejarError(er); }
}

async function editar(e = {}) {
  const sec = h('div', { class: 'fila' }, grupos.map(g => h('label', { class: 'check' },
    h('input', { type: 'checkbox', value: g.id, class: 'sec', checked: (e.gruposSecundarios || []).some(x => x.id === g.id) }), g.nombre)));
  const form = h('form', { novalidate: true }, h('div', { class: 'campos c2' },
    campo({ nombre: 'nombre', etiqueta: 'Nombre', requerido: true, maxLength: 120, valor: e.nombre }),
    campo({ nombre: 'grupoMuscularId', etiqueta: 'Grupo muscular principal', tipo: 'select', requerido: true, valor: e.grupoMuscularId, opciones: grupos.map(g => ({ valor: g.id, texto: g.nombre })) }),
    campo({ nombre: 'dificultad', etiqueta: 'Dificultad', tipo: 'select', requerido: true, valor: e.dificultad || 'MEDIA', opciones: [{ valor: 'BAJA', texto: 'Baja' }, { valor: 'MEDIA', texto: 'Media' }, { valor: 'ALTA', texto: 'Alta' }] }),
    campo({ nombre: 'equipamiento', etiqueta: 'Equipamiento', maxLength: 100, valor: e.equipamiento }),
    campo({ nombre: 'descripcion', etiqueta: 'Descripción / técnica', tipo: 'textarea', maxLength: 500, valor: e.descripcion, ancho: true }),
    campo({ nombre: 'videoUrl', etiqueta: 'Enlace a video', tipo: 'url', maxLength: 300, valor: e.videoUrl, ancho: true, placeholder: 'https://…', ayuda: 'Debe comenzar con https://' })),
  h('button', { type: 'button', class: 'btn chico secundario', onclick: () => { const u = form.videoUrl.value; if (u.startsWith('https://')) window.open(u, '_blank', 'noopener'); else toast('El enlace debe comenzar con https://', 'advertencia'); } }, 'Probar enlace'),
  h('h3', { style: 'margin-top:1rem' }, 'Grupos secundarios'), sec);
  await modal({ titulo: e.id ? 'Modificar ejercicio' : 'Nuevo ejercicio', cuerpo: form, ancho: true, botones: [{ texto: 'Cancelar', clase: 'secundario' }, { texto: 'Guardar', accion: async () => {
    const d = leerForm(form);
    d.grupoMuscularId = Number(d.grupoMuscularId);
    d.gruposSecundariosIds = [...sec.querySelectorAll('input.sec:checked')].map(c => Number(c.value));
    if (d.videoUrl && !d.videoUrl.startsWith('https://')) { toast('El enlace debe comenzar con https://', 'advertencia'); return false; }
    if (e.id) await api.put(`/ejercicios/${e.id}`, { ...d, version: e.version }); else await api.post('/ejercicios', d);
    toast('Ejercicio guardado');
    cargar();
  } }] });
}

q.addEventListener('input', demorar(() => { pagina = 0; cargar(); }));
[grupo, dif, activo].forEach(x => x.addEventListener('change', () => { pagina = 0; cargar(); }));
cargar();
