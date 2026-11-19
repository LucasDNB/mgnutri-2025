// Listado de pacientes (CU-02/03/29). Admin: todos + alta. Profesional: sólo asignados (CA-01.5.2).
import { api, qs } from '../core/api.js';
import { iniciarPagina, encabezado } from '../core/pagina.js';
import { h, tabla, paginador, demorar, manejarError, chip } from '../core/ui.js';
import { fmt } from '../core/format.js';

const { usuario, main } = await iniciarPagina({ roles: ['ADMIN', 'NUTRICIONISTA', 'ENTRENADOR'], titulo: 'Pacientes' });
const esAdmin = usuario.roles.includes('ADMIN');
let pagina = 0;

const buscador = h('input', { type: 'search', placeholder: 'Buscar por apellido, nombre o documento', 'aria-label': 'Buscar paciente' });
const resultados = h('div');
main.append(
  encabezado(esAdmin ? 'Pacientes' : 'Mis pacientes', [], esAdmin ? h('a', { class: 'btn', href: '/admin/paciente-form.html' }, '+ Nuevo paciente') : null),
  h('div', { class: 'tarjeta' }, buscador, h('div', { style: 'margin-top:.8rem' }, resultados)));

// Cada carga lleva un número: si llega la respuesta de un pedido viejo (p. ej. la carga inicial después de
// aplicar un filtro), se descarta para no pisar el resultado más reciente.
let pedidos = 0;
async function cargar() {
  const n = ++pedidos;
  try {
    const r = await api.get(`/pacientes${qs({ q: buscador.value, page: pagina, size: 20 })}`);
    if (n !== pedidos) return;
    const cols = [
      { titulo: 'Apellido y nombre', render: p => h('a', { href: `/comun/paciente.html?id=${p.id}` }, `${p.apellido}, ${p.nombre}`) },
      { titulo: 'Documento', campo: 'nroDocumento' },
      { titulo: 'Edad', campo: 'edad', clase: 'num' },
      { titulo: 'Teléfono', campo: 'telefono' },
      { titulo: 'Profesionales', render: p => p.profesionales.length ? p.profesionales.map(x => h('div', {}, x)) : chip('Sin asignar', 'AUSENTE') },
    ];
    if (esAdmin) cols.push({ titulo: 'Saldo', clase: 'num', render: p => h('span', { style: Number(p.saldo) > 0 ? 'color:var(--rojo);font-weight:700' : '' }, fmt.moneda(p.saldo)) });
    else cols.push({ titulo: 'Objetivo', campo: 'objetivo' });
    resultados.replaceChildren(tabla(cols, r.content, { vacio: 'No se encontraron pacientes' }), paginador(r, (p) => { pagina = p; cargar(); }));
  } catch (e) {
    manejarError(e);
  }
}
buscador.addEventListener('input', demorar(() => { pagina = 0; cargar(); }));
cargar();
