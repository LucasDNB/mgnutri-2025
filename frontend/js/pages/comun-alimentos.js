// CU-53A/B/C Base de datos de alimentos (ABM: admin y nutricionistas; lectura: entrenadores).
import { api, qs, ApiError } from '../core/api.js';
import { iniciarPagina, encabezado } from '../core/pagina.js';
import { h, tabla, paginador, campo, leerForm, modal, toast, confirmar, manejarError, vaciar, demorar, chip } from '../core/ui.js';

const { usuario, main } = await iniciarPagina({ roles: ['ADMIN', 'NUTRICIONISTA', 'ENTRENADOR'], titulo: 'Base de alimentos' });
const puedeEditar = usuario.roles.some(r => ['ADMIN', 'NUTRICIONISTA'].includes(r));
const grupos = await api.get('/grupos-alimento');
let pagina = 0;
const q = h('input', { type: 'search', placeholder: 'Buscar alimento' });
const grupo = h('select', {}, h('option', { value: '' }, 'Todos los grupos'), grupos.map(g => h('option', { value: g.id }, g.nombre)));
const activo = h('select', {}, h('option', { value: 'true' }, 'Activos'), h('option', { value: 'false' }, 'Inactivos'), h('option', { value: '' }, 'Todos'));
const res = h('div');
main.append(encabezado('Base de alimentos', [], puedeEditar ? h('button', { class: 'btn', onclick: () => editar() }, '+ Nuevo alimento') : null),
  h('div', { class: 'tarjeta' }, h('div', { class: 'fila' }, q, grupo, activo), h('p', { class: 'suave' }, 'Valores cada 100 g de alimento crudo o listo para consumir.'), res));

async function cargar() {
  try {
    const r = await api.get(`/alimentos${qs({ q: q.value, grupoId: grupo.value, activo: activo.value, page: pagina })}`);
    vaciar(res, tabla([
      { titulo: 'Alimento', campo: 'nombre' }, { titulo: 'Grupo', campo: 'grupo' },
      { titulo: 'kcal', campo: 'kcal', clase: 'num' }, { titulo: 'HC', campo: 'hc', clase: 'num' }, { titulo: 'Prot.', campo: 'proteinas', clase: 'num' },
      { titulo: 'Grasas', campo: 'grasas', clase: 'num' }, { titulo: 'Medida casera', render: a => a.unidadCasera ? `${a.unidadCasera} (${a.gramosUnidadCasera} g)` : '' },
      { titulo: 'Estado', render: a => chip(a.activo ? 'Activo' : 'Inactivo', a.activo ? 'OK' : 'FINALIZADO') },
      { titulo: '', render: a => puedeEditar ? h('div', { class: 'fila' }, h('button', { class: 'btn chico secundario', onclick: () => editar(a.id) }, 'Modificar'),
        a.activo ? h('button', { class: 'btn chico peligro secundario', onclick: () => baja(a) }, 'Dar de baja')
          : h('button', { class: 'btn chico secundario', onclick: async () => { await api.post(`/alimentos/${a.id}/reactivar`); toast('Alimento reactivado'); cargar(); } }, 'Reactivar')) : '' },
    ], r.content), paginador(r, (p) => { pagina = p; cargar(); }));
  } catch (e) { manejarError(e); }
}

async function baja(a) {
  if (!(await confirmar(`¿Dar de baja "${a.nombre}"? No se ofrecerá en editores, generador ni alternativas; los planes existentes lo conservan.`, { texto: 'Dar de baja', peligro: true }))) return;
  try { await api.del(`/alimentos/${a.id}`); toast('Alimento dado de baja'); cargar(); } catch (e) { manejarError(e); }
}

async function editar(id) {
  const a = id ? await api.get(`/alimentos/${id}`) : { aptoDesayuno: true, aptoColacion: false, aptoAlmuerzo: true, equivalencias: [] };
  const equivalencias = [...(a.equivalencias || [])];
  const eqEl = h('div');
  const dibujarEq = () => vaciar(eqEl, tabla([{ titulo: 'Equivale a', campo: 'alimentoEquivalente' },
    { titulo: 'Gramos origen', campo: 'gramosOrigen', clase: 'num' }, { titulo: 'Gramos equivalente', campo: 'gramosEquivalente', clase: 'num' },
    { titulo: '', render: (e) => h('button', { type: 'button', class: 'btn chico secundario', onclick: () => { equivalencias.splice(equivalencias.indexOf(e), 1); dibujarEq(); } }, 'Quitar') }],
  equivalencias, { vacio: 'Sin equivalencias manuales' }));
  dibujarEq();
  const busqEq = h('input', { type: 'search', placeholder: 'Buscar alimento equivalente…' });
  const opcEq = h('div', { class: 'fila' });
  busqEq.addEventListener('input', demorar(async () => {
    const r = await api.get(`/alimentos${qs({ q: busqEq.value, activo: true, size: 6 })}`);
    vaciar(opcEq, r.content.filter(x => x.id !== id).map(x => h('button', { type: 'button', class: 'btn chico secundario', onclick: () => {
      const go = Number(prompt(`Gramos de ${a.nombre || 'este alimento'}`, '100'));
      const ge = Number(prompt(`Equivalen a cuántos gramos de ${x.nombre}?`, '100'));
      if (go > 0 && ge > 0) { equivalencias.push({ alimentoEquivalenteId: x.id, alimentoEquivalente: x.nombre, gramosOrigen: go, gramosEquivalente: ge }); dibujarEq(); }
    } }, x.nombre)));
  }));
  const n = (nombre, etiqueta, max, extra = {}) => campo({ nombre, etiqueta, tipo: 'number', step: '0.01', min: 0, max, valor: a[nombre], ...extra });
  const form = h('form', { novalidate: true }, h('div', { class: 'campos c3' },
    campo({ nombre: 'nombre', etiqueta: 'Nombre', requerido: true, maxLength: 120, valor: a.nombre, ancho: true }),
    campo({ nombre: 'grupoId', etiqueta: 'Grupo de intercambio', tipo: 'select', requerido: true, valor: a.grupoId, opciones: grupos.map(g => ({ valor: g.id, texto: `${g.nombre} (ref. ${g.macroReferencia.toLowerCase()})` })),
      ayuda: 'Las equivalencias calculadas se hacen dentro del mismo grupo, según su macronutriente de referencia.' }),
    n('kcal', 'kcal / 100 g', 900, { requerido: true }), n('hc', 'HC (g)', 100, { requerido: true }), n('proteinas', 'Proteínas (g)', 100, { requerido: true }),
    n('grasas', 'Grasas (g)', 100, { requerido: true }), n('fibra', 'Fibra (g)', 100), n('sodioMg', 'Sodio (mg)', 100000),
    campo({ nombre: 'unidadCasera', etiqueta: 'Medida casera', maxLength: 60, valor: a.unidadCasera, placeholder: '1 unidad mediana' }),
    n('gramosUnidadCasera', 'Gramos de la medida', 2000),
    campo({ nombre: 'aptoDesayuno', etiqueta: 'Desayuno / merienda', tipo: 'checkbox', valor: a.aptoDesayuno }),
    campo({ nombre: 'aptoColacion', etiqueta: 'Colación', tipo: 'checkbox', valor: a.aptoColacion }),
    campo({ nombre: 'aptoAlmuerzo', etiqueta: 'Almuerzo / cena', tipo: 'checkbox', valor: a.aptoAlmuerzo })),
  h('h3', { style: 'margin-top:1rem' }, 'Equivalencias manuales'), h('p', { class: 'suave' }, 'Tienen prioridad sobre las calculadas.'), eqEl, busqEq, opcEq);
  const guardar = async (confirmarAdvertencias) => {
    const d = leerForm(form);
    d.grupoId = Number(d.grupoId);
    d.equivalencias = equivalencias.map(e => ({ alimentoEquivalenteId: e.alimentoEquivalenteId, gramosOrigen: e.gramosOrigen, gramosEquivalente: e.gramosEquivalente, nota: e.nota }));
    d.confirmarAdvertencias = confirmarAdvertencias;
    if (id) d.version = a.version;
    return id ? api.put(`/alimentos/${id}`, d) : api.post('/alimentos', d);
  };
  await modal({ titulo: id ? 'Modificar alimento' : 'Nuevo alimento', cuerpo: form, ancho: true, botones: [{ texto: 'Cancelar', clase: 'secundario' }, { texto: 'Guardar', accion: async () => {
    try {
      await guardar(false);
    } catch (e) {
      if (e instanceof ApiError && e.code === 'KCAL_INCOHERENTES') {
        if (!(await confirmar(e.message, { texto: 'Guardar igual' }))) return false;
        await guardar(true);
      } else throw e;
    }
    toast('Alimento guardado');
    cargar();
  } }] });
}

[q].forEach(x => x.addEventListener('input', demorar(() => { pagina = 0; cargar(); })));
[grupo, activo].forEach(x => x.addEventListener('change', () => { pagina = 0; cargar(); }));
cargar();
