// Listado de profesionales (CU-05/06).
import { api, qs } from '../core/api.js';
import { iniciarPagina, encabezado } from '../core/pagina.js';
import { h, tabla, paginador, demorar, manejarError, chip } from '../core/ui.js';

const { main } = await iniciarPagina({ roles: ['ADMIN'], titulo: 'Profesionales' });
let pagina = 0;
const buscador = h('input', { type: 'search', placeholder: 'Buscar por apellido, nombre o documento', 'aria-label': 'Buscar' });
const tipo = h('select', { 'aria-label': 'Tipo' }, [['', 'Todos los tipos'], ['NUTRICIONISTA', 'Nutricionistas'], ['ENTRENADOR', 'Entrenadores'], ['INTEGRAL', 'Integrales']].map(([v, t]) => h('option', { value: v }, t)));
const res = h('div');
main.append(encabezado('Profesionales', [], h('a', { class: 'btn', href: '/admin/profesional.html' }, '+ Nuevo profesional')),
  h('div', { class: 'tarjeta' }, h('div', { class: 'fila' }, buscador, tipo), h('div', { style: 'margin-top:.8rem' }, res)));

// Cada carga lleva un número: si llega la respuesta de un pedido viejo (p. ej. la carga inicial después de
// aplicar un filtro), se descarta para no pisar el resultado más reciente.
let pedidos = 0;
async function cargar() {
  const n = ++pedidos;
  try {
    const r = await api.get(`/profesionales${qs({ q: buscador.value, tipo: tipo.value, page: pagina })}`);
    if (n !== pedidos) return;
    res.replaceChildren(tabla([
      { titulo: 'Apellido y nombre', render: p => h('a', { href: `/admin/profesional.html?id=${p.id}` }, `${p.apellido}, ${p.nombre}`) },
      { titulo: 'Tipo', render: p => chip(p.tipo.toLowerCase()) },
      { titulo: 'Matrícula', campo: 'matricula' },
      { titulo: 'Especialidad', campo: 'especialidad' },
      { titulo: 'Teléfono', campo: 'telefono' },
      { titulo: 'Agenda', render: p => (p.tipo === 'ENTRENADOR' ? '—' : p.tieneAgenda ? chip('Cargada', 'OK') : chip('Sin agenda', 'AUSENTE')) },
      { titulo: 'Pacientes', campo: 'pacientesAsignados', clase: 'num' },
    ], r.content, { vacio: 'No se encontraron profesionales' }), paginador(r, (p) => { pagina = p; cargar(); }));
  } catch (e) { manejarError(e); }
}
buscador.addEventListener('input', demorar(() => { pagina = 0; cargar(); }));
tipo.addEventListener('change', () => { pagina = 0; cargar(); });
cargar();
