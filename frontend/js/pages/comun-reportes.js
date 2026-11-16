// Spec 12 Reportes: tarjetas según rol, filtros, resultado en pantalla (tabla + gráficos) y descarga PDF/Excel.
import { api, qs, descargar } from '../core/api.js';
import { iniciarPagina, encabezado } from '../core/pagina.js';
import { h, tabla, campo, leerForm, manejarError, vaciar, demorar } from '../core/ui.js';
import { hoyISO, sumarDias, fmt } from '../core/format.js';
import { grafico } from '../components/graficos.js';

const { usuario, main } = await iniciarPagina({ roles: ['ADMIN', 'NUTRICIONISTA', 'ENTRENADOR'], titulo: 'Reportes' });
const esAdmin = usuario.roles.includes('ADMIN');
const R = [
  { tipo: 'turnos', titulo: 'Turnos', desc: 'Atendidos, ausentes, cancelados y tasas por profesional.', roles: ['ADMIN', 'NUTRICIONISTA', 'ENTRENADOR'],
    filtros: () => [selArea(), sel('estado', 'Estado', ['', 'RESERVADO', 'ATENDIDO', 'AUSENTE', 'CANCELADO'])] },
  { tipo: 'pagos', titulo: 'Pagos y deudas', desc: 'Recaudación por medio y deudas pendientes.', roles: ['ADMIN'],
    filtros: () => [sel('estado', 'Estado', ['', 'VIGENTE', 'ANULADO']), sel('medio', 'Medio', ['', 'EFECTIVO', 'TRANSFERENCIA', 'DEBITO', 'CREDITO', 'MERCADO_PAGO', 'OTRO'])] },
  { tipo: 'evolucion', titulo: 'Evolución del paciente', desc: 'Mediciones, gráficos comparativos y desempeño.', roles: ['ADMIN', 'NUTRICIONISTA', 'ENTRENADOR'], requierePaciente: true, filtros: () => [] },
  { tipo: 'rutinas', titulo: 'Rutinas', desc: 'Cumplimiento, reemplazos, comentarios y molestias.', roles: ['ADMIN', 'ENTRENADOR'],
    filtros: () => [sel('estado', 'Estado', ['', 'ACTIVA', 'FINALIZADA', 'ELIMINADA']), sel('agrupar', 'Comparar por', ['MES', 'SEMANA'])] },
  { tipo: 'planes', titulo: 'Planes alimentarios', desc: 'Comidas realizadas, reemplazos y hambre/saciedad.', roles: ['ADMIN', 'NUTRICIONISTA'],
    filtros: () => [sel('estado', 'Estado', ['', 'ACTIVO', 'BORRADOR', 'FINALIZADO', 'ELIMINADO']), sel('modo', 'Modo', ['', 'MANUAL', 'AUTOMATICO'])] },
].filter(r => r.roles.some(x => usuario.roles.includes(x)));

function sel(nombre, etiqueta, valores) {
  return campo({ nombre, etiqueta, tipo: 'select', opciones: valores.map(v => ({ valor: v, texto: v ? v.replaceAll('_', ' ').toLowerCase() : 'Todos' })) });
}
function selArea() {
  return campo({ nombre: 'area', etiqueta: 'Área', tipo: 'select', opciones: [{ valor: '', texto: 'Todas' }, { valor: 'NUTRICION', texto: 'Nutrición' }, { valor: 'ENTRENAMIENTO', texto: 'Entrenamiento' }] });
}

const zona = h('div');
main.append(encabezado('Reportes'), h('div', { class: 'grilla g3' }, R.map(r => h('button', { class: 'tarjeta', style: 'text-align:left;border:0;cursor:pointer;font:inherit', onclick: () => abrir(r) },
  h('h2', {}, r.titulo), h('p', { class: 'suave' }, r.desc)))), zona);

async function abrir(r) {
  let pacienteId = null;
  const buscador = h('input', { type: 'search', placeholder: 'Buscar paciente…' });
  const opciones = h('div', { class: 'fila' });
  const pacienteSel = h('div', { class: 'campo ancho' }, h('label', { class: r.requierePaciente ? 'obligatorio' : '' }, 'Paciente'), buscador, opciones);
  buscador.addEventListener('input', demorar(async () => {
    pacienteId = null;
    const res = await api.get(`/pacientes${qs({ q: buscador.value, size: 6 })}`);
    vaciar(opciones, res.content.map(p => h('button', { type: 'button', class: 'btn chico secundario', onclick: () => { pacienteId = p.id; buscador.value = `${p.apellido}, ${p.nombre}`; vaciar(opciones); } }, `${p.apellido}, ${p.nombre}`)));
  }));
  const profs = esAdmin && ['turnos', 'rutinas', 'planes'].includes(r.tipo) ? (await api.get('/profesionales?size=100')).content : [];
  const campoProf = { turnos: 'profesionalId', rutinas: 'entrenadorId', planes: 'nutricionistaId' }[r.tipo];
  const form = h('form', { class: 'tarjeta' }, h('h2', {}, r.titulo), h('div', { class: 'campos c3' },
    campo({ nombre: 'desde', etiqueta: 'Desde', tipo: 'date', valor: r.tipo === 'evolucion' ? '' : sumarDias(hoyISO(), -30) }),
    campo({ nombre: 'hasta', etiqueta: 'Hasta', tipo: 'date', valor: hoyISO() }),
    profs.length ? campo({ nombre: campoProf, etiqueta: 'Profesional', tipo: 'select', opciones: [{ valor: '', texto: 'Todos' }, ...profs.map(p => ({ valor: p.id, texto: `${p.apellido}, ${p.nombre}` }))] }) : null,
    r.filtros(), r.tipo !== 'pagos' || true ? pacienteSel : null),
  h('div', { class: 'acciones' },
    h('button', { type: 'button', class: 'btn secundario', onclick: () => bajar('pdf') }, 'Descargar PDF'),
    h('button', { type: 'button', class: 'btn secundario', onclick: () => bajar('xlsx') }, 'Descargar Excel'),
    h('button', { class: 'btn', type: 'submit' }, 'Generar')));
  const resultado = h('div');
  vaciar(zona, form, resultado);
  form.scrollIntoView({ behavior: 'smooth' });

  const ruta = (formato) => {
    const f = leerForm(form);
    if (r.tipo === 'evolucion') return `/reportes/evolucion/${pacienteId}${qs({ desde: f.desde, hasta: f.hasta, formato })}`;
    return `/reportes/${r.tipo}${qs({ ...f, pacienteId, formato })}`;
  };
  const listo = () => { if (r.requierePaciente && !pacienteId) { manejarError(new Error('Elegí un paciente')); return false; } return true; };
  async function bajar(formato) { if (listo()) descargar(ruta(formato), `reporte.${formato}`).catch(manejarError); }

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    if (!listo()) return;
    try {
      const rep = await api.get(ruta('json'));
      if (rep.vacio) { vaciar(resultado, h('div', { class: 'tarjeta vacio' }, 'No hay resultados para los filtros elegidos')); return; }
      const graficos = Object.values(rep.graficos).map(g => {
        const c = h('div');
        setTimeout(() => grafico(c, { tipo: g.tipo, titulo: g.titulo, labels: g.labels, series: g.series.map(s => ({ nombre: s.nombre, datos: s.datos })) }), 0);
        return h('div', { class: 'tarjeta' }, c);
      });
      const val = (v) => (typeof v === 'number' && !Number.isInteger(v) ? fmt.numero(v)
        : typeof v === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(v) ? fmt.fecha(v) : v);
      const tablaDe = (t) => h('div', { class: 'tarjeta' }, h('h2', {}, t.titulo), tabla(t.columnas.map((c, i) => ({ titulo: c, render: f => val(f[i]) })), t.filas));
      vaciar(resultado,
        h('div', { class: 'tarjeta' }, h('h2', {}, rep.titulo), h('div', { class: 'grilla g4' }, Object.entries(rep.resumen).map(([k, v]) =>
          h('div', {}, h('div', { class: 'suave' }, k), h('strong', {}, typeof v === 'number' && k.toLowerCase().includes('total') && !k.includes('turnos') && r.tipo === 'pagos' ? fmt.moneda(v) : val(v)))))),
        h('div', { class: 'grilla g2' }, graficos), tablaDe(rep.tabla), rep.secciones.map(tablaDe));
    } catch (er) { manejarError(er); }
  });
}
