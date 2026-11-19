// CU-10 Calendario de turnos (profesional: propios; admin: todos con filtros) + gestión RF-03.
import { api, qs } from '../core/api.js';
import { iniciarPagina, encabezado } from '../core/pagina.js';
import { h, modal, toast, confirmar, pedirTexto, manejarError, vaciar, chip, tabla, param, campo, demorar } from '../core/ui.js';
import { fmt, hoyISO, etiquetaEstado, aIsoArgentina } from '../core/format.js';
import { renderCalendario, rangoVista, moverReferencia, tituloVista } from '../components/calendar.js';
import { selectorHuecos } from '../components/huecos.js';

const { usuario, main } = await iniciarPagina({ roles: ['ADMIN', 'NUTRICIONISTA', 'ENTRENADOR'], titulo: 'Calendario' });
const esAdmin = usuario.roles.includes('ADMIN');
let vista = 'dia';
let ref = hoyISO();
let modo = 'calendario';
let ordenLista = 'inicio';

const filtroProf = h('select', { 'aria-label': 'Profesional' }, h('option', { value: '' }, 'Todos los profesionales'));
const filtroArea = h('select', { 'aria-label': 'Área' }, h('option', { value: '' }, 'Todas las áreas'), h('option', { value: 'NUTRICION' }, 'Nutrición'), h('option', { value: 'ENTRENAMIENTO' }, 'Entrenamiento'));
const verCancelados = h('input', { type: 'checkbox' });
let profesionales = [];
if (esAdmin) {
  profesionales = (await api.get('/profesionales?size=100')).content;
  profesionales.forEach(p => filtroProf.append(h('option', { value: p.id }, `${p.apellido}, ${p.nombre}`)));
}
[filtroProf, filtroArea, verCancelados].forEach(x => x.addEventListener('change', cargar));

const titulo = h('span', { class: 'cal-titulo' });
const vistas = h('div', { class: 'pestanas', style: 'margin:0' }, [['dia', 'Día'], ['semana', 'Semana'], ['mes', 'Mes'], ['lista', 'Listado']].map(([v, t]) =>
  h('button', { dataset: { v }, onclick: () => { if (v === 'lista') modo = 'lista'; else { modo = 'calendario'; vista = v; } cargar(); } }, t)));
const cuerpo = h('div');
main.append(
  encabezado('Calendario', [], h('button', { class: 'btn', onclick: () => nuevoTurno() }, '+ Dar turno')),
  h('div', { class: 'tarjeta' },
    esAdmin ? h('div', { class: 'fila', style: 'margin-bottom:.6rem' }, filtroProf, filtroArea, h('label', { class: 'check' }, verCancelados, 'Ver cancelados')) :
      h('div', { class: 'fila', style: 'margin-bottom:.6rem' }, filtroArea, h('label', { class: 'check' }, verCancelados, 'Ver cancelados')),
    h('div', { class: 'cal-barra' },
      h('div', { class: 'fila' },
        h('button', { class: 'btn chico secundario', onclick: () => { ref = moverReferencia(vista, ref, -1); cargar(); } }, '‹'),
        h('button', { class: 'btn chico secundario', onclick: () => { ref = hoyISO(); cargar(); } }, 'Hoy'),
        h('button', { class: 'btn chico secundario', onclick: () => { ref = moverReferencia(vista, ref, 1); cargar(); } }, '›'),
        titulo),
      vistas),
    cuerpo));

// Cada carga lleva un número: si llega la respuesta de un pedido viejo, se descarta para no pisar el resultado más reciente.
let pedidos = 0;
async function cargar() {
  const n = ++pedidos;
  vistas.querySelectorAll('button').forEach(b => b.classList.toggle('activa', modo === 'lista' ? b.dataset.v === 'lista' : b.dataset.v === vista));
  titulo.textContent = tituloVista(vista, ref);
  const [desde, hasta] = rangoVista(vista, ref);
  try {
    const turnos = await api.get(`/turnos${qs({ profesionalId: filtroProf.value, area: filtroArea.value, desde, hasta, incluirCancelados: verCancelados.checked })}`);
    if (n !== pedidos) return;
    if (modo === 'lista') {
      const ordenados = [...turnos].sort((a, b) => ordenLista === 'paciente' ? a.paciente.nombre.localeCompare(b.paciente.nombre)
        : ordenLista === 'profesional' ? a.profesional.nombre.localeCompare(b.profesional.nombre) : a.inicio.localeCompare(b.inicio));
      vaciar(cuerpo,
        h('div', { class: 'fila' }, 'Ordenar por: ', ['inicio', 'paciente', 'profesional'].map(o => h('button', { class: `btn chico ${ordenLista === o ? '' : 'secundario'}`, onclick: () => { ordenLista = o; cargar(); } }, o === 'inicio' ? 'Fecha' : o[0].toUpperCase() + o.slice(1)))),
        tabla([
          { titulo: 'Fecha', render: t => `${fmt.fecha(t.inicio.substring(0, 10))} ${fmt.hora(t.inicio)}` },
          { titulo: 'Paciente', render: t => t.paciente.nombre }, { titulo: 'Profesional', render: t => t.profesional.nombre },
          { titulo: 'Área', render: t => (t.area === 'NUTRICION' ? 'Nutrición' : 'Entrenamiento') },
          { titulo: 'Estado', render: t => chip(etiquetaEstado(t.estado), t.estado) },
          { titulo: '', render: t => h('button', { class: 'btn chico secundario', onclick: () => detalle(t) }, 'Ver') },
        ], ordenados, { vacio: 'No hay turnos en el período' }));
    } else {
      const eventos = turnos.map(t => ({ ...t, titulo: esAdmin ? `${t.paciente.nombre} · ${t.profesional.nombre}` : t.paciente.nombre, detalle: t.ubicacion }));
      vaciar(cuerpo, renderCalendario({ vista, ref, eventos, onClick: detalle, onDia: (f) => { vista = 'dia'; ref = f; cargar(); } }));
    }
  } catch (e) {
    manejarError(e);
  }
}

async function detalle(t) {
  const acciones = [];
  if (t.estado === 'RESERVADO' && !t.pasado) {
    acciones.push({ texto: 'Reprogramar', clase: 'secundario', valor: 'reprogramar' }, { texto: 'Cancelar turno', clase: 'peligro secundario', valor: 'cancelar' });
  }
  if (['RESERVADO', 'ATENDIDO', 'AUSENTE'].includes(t.estado) && t.pasado) {
    acciones.push({ texto: 'Atendido', valor: 'ATENDIDO' }, { texto: 'Ausente', clase: 'secundario', valor: 'AUSENTE' });
  }
  acciones.push({ texto: 'Cerrar', clase: 'secundario', valor: null });
  const r = await modal({
    titulo: 'Detalle del turno',
    cuerpo: h('div', {},
      h('p', {}, h('strong', {}, 'Paciente: '), h('a', { href: `/comun/paciente.html?id=${t.paciente.id}` }, t.paciente.nombre)),
      t.paciente.telefono ? h('p', {}, h('strong', {}, 'Teléfono: '), t.paciente.telefono) : null,
      h('p', {}, h('strong', {}, 'Profesional: '), t.profesional.nombre),
      h('p', {}, h('strong', {}, 'Fecha: '), `${fmt.diaFecha(t.inicio)} · ${fmt.hora(t.inicio)} a ${fmt.hora(t.fin)}`),
      h('p', {}, h('strong', {}, 'Ubicación: '), t.ubicacion || '—', t.fueraDeAgenda ? chip('fuera de agenda', 'AUSENTE') : null),
      h('p', {}, h('strong', {}, 'Estado: '), chip(etiquetaEstado(t.estado), t.estado)),
      t.motivoCancelacion ? h('p', {}, h('strong', {}, 'Motivo: '), t.motivoCancelacion) : null),
    botones: acciones,
  });
  try {
    if (r === 'cancelar') {
      const motivo = await pedirTexto('Cancelar turno', { ayuda: 'El paciente recibirá un aviso por WhatsApp.' });
      if (!motivo) return;
      await api.post(`/turnos/${t.id}/cancelar`, { motivo, version: t.version });
      toast('Turno cancelado');
    } else if (r === 'reprogramar') {
      await reprogramar(t);
    } else if (r === 'ATENDIDO' || r === 'AUSENTE') {
      await api.post(`/turnos/${t.id}/estado`, { estado: r, version: t.version });
      toast(`Turno marcado como ${r.toLowerCase()}`);
    }
    if (r) cargar();
  } catch (e) {
    manejarError(e);
  }
}

async function reprogramar(t) {
  let elegido = null;
  const info = h('div', { class: 'aviso' }, 'Elegí un nuevo horario.');
  const motivo = h('input', { type: 'text', maxLength: 200, placeholder: 'Motivo (obligatorio)' });
  const ok = await modal({
    titulo: `Reprogramar turno de ${t.paciente.nombre}`, ancho: true,
    cuerpo: h('div', {}, selectorHuecos(t.profesional.id, (x) => { elegido = x; info.textContent = `Nuevo horario: ${fmt.diaFecha(x.inicio)} ${fmt.hora(x.inicio)}`; }), info, motivo),
    botones: [{ texto: 'Cancelar', clase: 'secundario' }, { texto: 'Reprogramar', accion: async () => {
      if (!elegido) { toast('Elegí un horario', 'advertencia'); return false; }
      if (!motivo.value.trim()) { toast('Indicá el motivo', 'advertencia'); return false; }
      await api.post(`/turnos/${t.id}/reprogramar`, { inicio: elegido.inicio, motivo: motivo.value.trim(), version: t.version });
      toast('Turno reprogramado');
      return true;
    } }],
  });
  return ok;
}

async function nuevoTurno(pacienteInicial) {
  let paciente = pacienteInicial || null;
  let profesionalId = esAdmin ? null : usuario.profesionalId;
  let elegido = null;
  const pacienteSel = h('div');
  const buscador = h('input', { type: 'search', placeholder: 'Buscar paciente…' });
  const opciones = h('div', { class: 'columna' });
  const zonaHuecos = h('div');
  const info = h('div', { class: 'aviso' }, 'Elegí paciente, profesional y horario.');
  const forzarFecha = campo({ nombre: 'fecha', etiqueta: 'Fecha', tipo: 'date', min: hoyISO() });
  const forzarHora = campo({ nombre: 'hora', etiqueta: 'Hora', tipo: 'time' });
  const forzar = h('input', { type: 'checkbox' });

  const mostrarPaciente = () => vaciar(pacienteSel, paciente ? h('p', {}, h('strong', {}, 'Paciente: '), paciente.nombre, ' ',
    h('button', { type: 'button', class: 'btn link', onclick: () => { paciente = null; mostrarPaciente(); } }, 'cambiar')) : h('div', {}, buscador, opciones));
  buscador.addEventListener('input', demorar(async () => {
    const r = await api.get(`/pacientes${qs({ q: buscador.value, size: 8 })}`);
    vaciar(opciones, r.content.map(p => h('button', { type: 'button', class: 'btn chico secundario', onclick: () => { paciente = { id: p.id, nombre: `${p.apellido}, ${p.nombre}` }; mostrarPaciente(); } }, `${p.apellido}, ${p.nombre} · ${p.nroDocumento}`)));
  }));
  mostrarPaciente();
  const cargarHuecos = () => vaciar(zonaHuecos, profesionalId ? selectorHuecos(profesionalId, (x) => { elegido = x; info.textContent = `Horario: ${fmt.diaFecha(x.inicio)} ${fmt.hora(x.inicio)}`; }) : null);
  const profSel = esAdmin ? h('select', { onchange: (e) => { profesionalId = Number(e.target.value) || null; cargarHuecos(); } },
    h('option', { value: '' }, 'Elegí profesional…'), profesionales.map(p => h('option', { value: p.id }, `${p.apellido}, ${p.nombre} (${p.tipo.toLowerCase()})`))) : null;
  cargarHuecos();
  await modal({
    titulo: 'Dar turno', ancho: true,
    cuerpo: h('div', { class: 'columna' }, pacienteSel, profSel, zonaHuecos, info,
      esAdmin ? h('details', {}, h('summary', {}, 'Forzar un horario fuera de agenda (sólo administrador)'),
        h('div', { class: 'aviso advertencia' }, 'El turno se creará aunque esté fuera de las franjas del profesional.'),
        h('div', { class: 'campos c2' }, forzarFecha, forzarHora), h('label', { class: 'check' }, forzar, 'Usar este horario')) : null),
    botones: [{ texto: 'Cancelar', clase: 'secundario' }, { texto: 'Confirmar turno', accion: async () => {
      if (!paciente || !profesionalId) { toast('Elegí paciente y profesional', 'advertencia'); return false; }
      let inicio = elegido?.inicio;
      if (forzar.checked) {
        const f = forzarFecha.querySelector('input').value;
        const hh = forzarHora.querySelector('input').value;
        if (!f || !hh) { toast('Completá fecha y hora', 'advertencia'); return false; }
        inicio = aIsoArgentina(f, hh);
      }
      if (!inicio) { toast('Elegí un horario', 'advertencia'); return false; }
      const t = await api.post('/turnos', { pacienteId: paciente.id, profesionalId, inicio, forzar: forzar.checked });
      toast(`Turno confirmado para el ${fmt.diaFecha(t.inicio)} a las ${fmt.hora(t.inicio)}`);
      cargar();
    } }],
  });
}

cargar();
if (param('nuevo') === '1' && param('pacienteId')) {
  const p = await api.get(`/pacientes/${param('pacienteId')}`);
  nuevoTurno({ id: p.id, nombre: `${p.apellido}, ${p.nombre}` });
}
