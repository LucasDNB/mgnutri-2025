// Ficha del paciente (specs 01, 04, 05, 06, 08, 09): pestañas según rol.
import { api, qs, descargar } from '../core/api.js';
import { iniciarPagina, encabezado, usaAgenda } from '../core/pagina.js';
import { h, tabla, chip, toast, modal, confirmar, pedirTexto, campo, leerForm, manejarError, param, vaciar, cargando } from '../core/ui.js';
import { fmt, etiquetaEstado, hoyISO, sumarDias, DIAS } from '../core/format.js';

const { usuario, main } = await iniciarPagina({ roles: ['ADMIN', 'NUTRICIONISTA', 'ENTRENADOR'], titulo: 'Ficha del paciente' });
const id = Number(param('id'));
const esAdmin = usuario.roles.includes('ADMIN');
const esNutri = usuario.roles.includes('NUTRICIONISTA');
const esEntr = usuario.roles.includes('ENTRENADOR');
let p = await api.get(`/pacientes/${id}`);
const asignadoEn = (area) => p.asignaciones.some(a => a.profesionalId === usuario.profesionalId && a.area === area);
const puedeNutri = esNutri && asignadoEn('NUTRICION');
const puedeEntr = esEntr && asignadoEn('ENTRENAMIENTO');

const PESTANAS = [
  ['datos', 'Datos', datos],
  usaAgenda(usuario) ? ['turnos', 'Turnos', turnos] : null,
  ['planes', 'Planes', planes],
  ['rutinas', 'Rutinas', rutinas],
  ['evolucion', 'Evolución', evolucion],
  ['registros', 'Registros', registros],
].filter(Boolean);
if (esAdmin) PESTANAS.push(['cuenta', 'Cuenta', () => { location.href = `/admin/pagos.html?pacienteId=${id}`; }]);

const contenido = h('div');
const barra = h('div', { class: 'pestanas envolver', role: 'tablist' });
function abrir(clave) {
  barra.querySelectorAll('button').forEach(b => b.classList.toggle('activa', b.dataset.clave === clave));
  history.replaceState(null, '', `?id=${id}&tab=${clave}`);
  cargando(contenido);
  PESTANAS.find(x => x[0] === clave)[2]().catch(e => { manejarError(e); vaciar(contenido, h('div', { class: 'aviso error' }, e.message)); });
}
PESTANAS.forEach(([clave, texto]) => barra.append(h('button', { role: 'tab', dataset: { clave }, onclick: () => abrir(clave) }, texto)));

main.append(encabezado(`${p.apellido}, ${p.nombre}`, [['Pacientes', '/comun/pacientes.html'], [`${p.apellido}, ${p.nombre}`]]),
  h('p', { class: 'suave' }, `${p.edad} años · DNI ${p.nroDocumento}${p.objetivo ? ' · Objetivo: ' + p.objetivo : ''}`), barra, contenido);
abrir(PESTANAS.some(x => x[0] === param('tab')) ? param('tab') : 'datos');

// ------------------------------------------------------------------ Datos
async function datos() {
  p = await api.get(`/pacientes/${id}`);
  const fila = (k, v) => h('tr', {}, h('th', { style: 'width:35%' }, k), h('td', {}, v ?? '—'));
  const deBaja = p.estado === 'BAJA';
  const acciones = esAdmin && deBaja ? h('div', { class: 'acciones' }, h('button', { class: 'btn', onclick: reactivar }, 'Reactivar paciente'))
    : esAdmin ? h('div', { class: 'acciones' },
    h('a', { class: 'btn secundario', href: `/admin/paciente-form.html?id=${id}` }, 'Modificar datos'),
    h('button', { class: 'btn secundario', onclick: reenviar }, 'Reenviar credenciales'),
    h('button', { class: 'btn peligro', onclick: baja }, 'Dar de baja')) : null;
  vaciar(contenido,
    deBaja ? h('div', { class: 'aviso advertencia' }, h('strong', {}, 'Paciente dado de baja'),
      ` el ${fmt.fecha(p.fechaBaja)}${p.motivoBaja ? ' · Motivo: ' + p.motivoBaja : ''}. No tiene acceso al sistema.`) : null,
    h('div', { class: 'grilla g2' },
      h('div', { class: 'tarjeta' }, h('h2', {}, 'Datos personales'), h('table', {}, h('tbody', {},
        fila('Usuario', p.username), fila('Documento', `${p.tipoDocumento} ${p.nroDocumento}`), fila('Fecha de nacimiento', fmt.fecha(p.fechaNacimiento)),
        fila('Sexo', p.sexo), fila('WhatsApp', p.telefonoWhatsapp), fila('Email', p.email), fila('Domicilio', p.domicilio),
        esAdmin ? fila('Saldo', fmt.moneda(p.saldo)) : null))),
      h('div', { class: 'tarjeta' }, h('h2', {}, 'Salud y objetivo'),
        h('p', {}, h('strong', {}, 'Objetivo: '), p.objetivo || '—'),
        h('p', {}, h('strong', {}, 'Antecedentes informados: '), p.antecedentesSalud || '—'),
        h('p', {}, h('strong', {}, 'Observaciones: '), p.observaciones || '—'),
        h('p', { class: 'suave' }, 'Información aportada por el paciente; no constituye diagnóstico.'))),
    h('div', { class: 'tarjeta' }, h('div', { class: 'fila entre' }, h('h2', {}, 'Profesionales asignados'),
      esAdmin && !deBaja ? h('button', { class: 'btn chico', onclick: asignar }, '+ Asignar profesional') : null),
    tabla([
      { titulo: 'Profesional', campo: 'profesional' },
      { titulo: 'Área', render: a => a.area === 'NUTRICION' ? 'Nutrición' : 'Entrenamiento' },
      { titulo: 'Desde', render: a => fmt.fecha(a.fechaAsignacion) },
      { titulo: '', render: a => esAdmin && !deBaja ? h('button', { class: 'btn chico peligro secundario', onclick: () => desasignar(a) }, 'Desasignar') : '' },
    ], p.asignaciones, { vacio: deBaja ? 'Al reactivarlo, asignale de nuevo sus profesionales' : 'Sin profesionales asignados' })),
    acciones);
}

async function reenviar() {
  if (!(await confirmar('Se generará una nueva contraseña temporal y se enviará por WhatsApp. El paciente deberá cambiarla en su próximo ingreso.', { texto: 'Reenviar' }))) return;
  try {
    const r = await api.post(`/pacientes/${id}/reenviar-credenciales`);
    await modal({ titulo: 'Credenciales regeneradas', cuerpo: r.passwordTemporal
      ? h('div', { class: 'aviso advertencia' }, 'No se pudo enviar por WhatsApp. Contraseña temporal: ', h('strong', {}, r.passwordTemporal))
      : h('p', {}, 'Se enviaron por WhatsApp.') });
  } catch (e) { manejarError(e); }
}

async function reactivar() {
  if (!(await confirmar('El paciente vuelve a tener acceso con una contraseña temporal nueva, que se envía por WhatsApp. Conserva su historial; '
    + 'después asignale sus profesionales. Los turnos cancelados no se restauran.', { texto: 'Reactivar', titulo: 'Reactivar paciente' }))) return;
  try {
    const r = await api.post(`/pacientes/${id}/reactivar`);
    await modal({ titulo: 'Paciente reactivado', cuerpo: r.passwordTemporal
      ? h('div', { class: 'aviso advertencia' }, 'No se pudo enviar por WhatsApp. Usuario: ', h('strong', {}, r.username), ' · Contraseña temporal: ', h('strong', {}, r.passwordTemporal))
      : h('p', {}, 'Se le enviaron las credenciales por WhatsApp. Ahora asignale sus profesionales.') });
    datos();
  } catch (e) { manejarError(e); }
}

async function baja() {
  const notificar = h('input', { type: 'checkbox' });
  const motivo = h('textarea', { maxLength: 300 });
  const ok = await modal({
    titulo: 'Dar de baja al paciente',
    cuerpo: h('div', {}, h('div', { class: 'aviso error' }, 'El paciente perderá el acceso al sistema. Sus turnos futuros se cancelarán y se quitarán sus profesionales asignados. Podés reactivarlo más adelante.'),
      h('div', { class: 'campo' }, h('label', {}, 'Motivo (opcional)'), motivo),
      h('label', { class: 'check', style: 'margin-top:.6rem' }, notificar, 'Avisar por WhatsApp la cancelación de sus turnos')),
    botones: [{ texto: 'Cancelar', valor: false, clase: 'secundario' }, { texto: 'Dar de baja', valor: true, clase: 'peligro' }],
  });
  if (!ok) return;
  try {
    await api.post(`/pacientes/${id}/baja`, { motivo: motivo.value, notificarCancelaciones: notificar.checked });
    toast('Paciente dado de baja');
    location.href = '/comun/pacientes.html';
  } catch (e) { manejarError(e); }
}

async function asignar() {
  const profs = (await api.get('/profesionales?size=100')).content;
  if (!profs.length) { toast('No hay profesionales disponibles', 'advertencia'); return; }
  const checks = profs.flatMap(pr => (pr.tipo === 'INTEGRAL' ? ['NUTRICION', 'ENTRENAMIENTO'] : [pr.tipo === 'NUTRICIONISTA' ? 'NUTRICION' : 'ENTRENAMIENTO'])
    .map(a => h('label', { class: 'check' }, h('input', { type: 'checkbox', dataset: { prof: pr.id, area: a } }),
      `${pr.apellido}, ${pr.nombre} (${pr.tipo.toLowerCase()}) — ${a === 'NUTRICION' ? 'Nutrición' : 'Entrenamiento'}`)));
  const cuerpo = h('div', { class: 'columna' }, checks);
  await modal({
    titulo: 'Asignar profesional', cuerpo,
    botones: [{ texto: 'Cancelar', clase: 'secundario' }, { texto: 'Asignar', accion: async () => {
      const asignaciones = [...cuerpo.querySelectorAll('input:checked')].map(c => ({ profesionalId: Number(c.dataset.prof), area: c.dataset.area }));
      if (!asignaciones.length) { toast('Elegí al menos un profesional', 'advertencia'); return false; }
      await api.post(`/pacientes/${id}/profesionales`, { asignaciones });
      toast('Asignación registrada');
      datos();
    } }],
  });
}

async function desasignar(a) {
  if (!(await confirmar(`¿Desasignar a ${a.profesional}? El historial de planes y rutinas se conserva.`, { texto: 'Desasignar', peligro: true }))) return;
  try { await api.del(`/pacientes/${id}/profesionales/${a.id}`); toast('Profesional desasignado'); datos(); } catch (e) { manejarError(e); }
}

// ------------------------------------------------------------------ Turnos
async function turnos() {
  const lista = await api.get(`/turnos${qs({ pacienteId: id, desde: sumarDias(hoyISO(), -183), hasta: sumarDias(hoyISO(), 180), incluirCancelados: true })}`);
  vaciar(contenido, h('div', { class: 'tarjeta' }, h('div', { class: 'fila entre' }, h('h2', {}, 'Turnos'),
    h('a', { class: 'btn chico', href: `/comun/calendario.html?pacienteId=${id}&nuevo=1` }, '+ Dar turno')),
  tabla([
    { titulo: 'Fecha', render: t => `${fmt.diaFecha(t.inicio)} ${fmt.hora(t.inicio)}` },
    { titulo: 'Profesional', render: t => t.profesional.nombre },
    { titulo: 'Área', render: t => t.area === 'NUTRICION' ? 'Nutrición' : 'Entrenamiento' },
    { titulo: 'Estado', render: t => chip(etiquetaEstado(t.estado), t.estado) },
    { titulo: 'Motivo', render: t => t.motivoCancelacion || '' },
  ], lista.reverse(), { vacio: 'Sin turnos en el período' })));
}

// ------------------------------------------------------------------ Planes
async function planes() {
  const lista = await api.get(`/pacientes/${id}/planes-alimentarios`);
  const activo = lista.find(x => x.estado === 'ACTIVO');
  vaciar(contenido, h('div', { class: 'tarjeta' },
    h('div', { class: 'fila entre' }, h('h2', {}, 'Planes alimentarios'), puedeNutri ? h('div', { class: 'fila' },
      h('a', { class: 'btn', href: `/profesional/plan-editor.html?pacienteId=${id}&modo=generar` }, 'Generar automáticamente'),
      h('a', { class: 'btn secundario', href: `/profesional/plan-editor.html?pacienteId=${id}` }, 'Cargar manualmente')) : null),
    activo ? h('div', { class: 'aviso' }, `Plan activo: ${activo.nombre} (${fmt.fecha(activo.fechaInicio)} al ${fmt.fecha(activo.fechaFin)})`) : h('div', { class: 'aviso advertencia' }, 'El paciente no tiene plan activo.'),
    tabla([
      { titulo: 'Plan', campo: 'nombre' },
      { titulo: 'Vigencia', render: x => `${fmt.fecha(x.fechaInicio)} – ${fmt.fecha(x.fechaFin)}` },
      { titulo: 'Estado', render: x => chip(etiquetaEstado(x.estado), x.estado) },
      { titulo: 'Modo', render: x => x.modo === 'AUTOMATICO' ? 'Automático' : 'Manual' },
      { titulo: 'Profesional', campo: 'nutricionista' },
      { titulo: 'Acciones', render: x => h('div', { class: 'fila' },
        h('a', { class: 'btn chico secundario', href: `/profesional/plan-editor.html?id=${x.id}${['ACTIVO', 'BORRADOR'].includes(x.estado) && puedeNutri ? '' : '&ver=1'}` },
          ['ACTIVO', 'BORRADOR'].includes(x.estado) && puedeNutri ? 'Editar' : 'Ver'),
        h('button', { class: 'btn chico secundario', onclick: () => descargar(`/planes-alimentarios/${x.id}/pdf`, 'plan.pdf') }, 'PDF'),
        puedeNutri ? h('button', { class: 'btn chico secundario', onclick: () => accionPlan(x, 'duplicar') }, 'Duplicar') : null,
        puedeNutri && x.estado !== 'ELIMINADO' ? h('button', { class: 'btn chico peligro secundario', onclick: () => accionPlan(x, 'eliminar') }, 'Eliminar') : null) },
    ], lista, { vacio: 'Sin planes registrados' })));
}

async function accionPlan(x, accion) {
  try {
    if (accion === 'eliminar') {
      if (!(await confirmar(`¿Eliminar el plan "${x.nombre}"? Dejará de estar disponible para el paciente.`, { texto: 'Eliminar', peligro: true }))) return;
      await api.del(`/planes-alimentarios/${x.id}`);
      toast('Plan eliminado');
      planes();
    } else {
      const r = await api.post(`/planes-alimentarios/${x.id}/duplicar`);
      location.href = `/profesional/plan-editor.html?id=${r.id}`;
    }
  } catch (e) { manejarError(e); }
}

// ------------------------------------------------------------------ Rutinas
async function rutinas() {
  const lista = await api.get(`/pacientes/${id}/rutinas`);
  const activa = lista.find(x => x.estado === 'ACTIVA');
  vaciar(contenido, h('div', { class: 'tarjeta' },
    h('div', { class: 'fila entre' }, h('h2', {}, 'Rutinas'), puedeEntr ? h('div', { class: 'fila' },
      h('a', { class: 'btn', href: `/profesional/rutina-editor.html?pacienteId=${id}` }, 'Nueva rutina'),
      activa ? h('a', { class: 'btn secundario', href: `/profesional/rutina-editor.html?id=${activa.id}` }, 'Editar rutina actual')
        : h('button', { class: 'btn secundario', disabled: true, title: 'El paciente no tiene rutina asignada' }, 'Editar rutina')) : null),
    tabla([
      { titulo: 'Rutina', campo: 'nombre' },
      { titulo: 'Vigencia', render: x => `${fmt.fecha(x.fechaInicio)} – ${fmt.fecha(x.fechaFin)}` },
      { titulo: 'Estado', render: x => chip(etiquetaEstado(x.estado), x.estado) },
      { titulo: 'Entrenador', campo: 'entrenador' },
      { titulo: 'Acciones', render: x => h('div', { class: 'fila' },
        h('a', { class: 'btn chico secundario', href: `/profesional/rutina-editor.html?id=${x.id}${x.estado === 'ACTIVA' && puedeEntr ? '' : '&ver=1'}` }, x.estado === 'ACTIVA' && puedeEntr ? 'Editar' : 'Ver'),
        h('button', { class: 'btn chico secundario', onclick: () => descargar(`/rutinas/${x.id}/pdf`, 'rutina.pdf') }, 'PDF'),
        puedeEntr ? h('button', { class: 'btn chico secundario', onclick: async () => {
          try { const r = await api.post(`/rutinas/${x.id}/duplicar`); location.href = `/profesional/rutina-editor.html?id=${r.id}`; } catch (e) { manejarError(e); }
        } }, 'Duplicar') : null,
        puedeEntr && x.estado === 'ACTIVA' ? h('button', { class: 'btn chico peligro secundario', onclick: async () => {
          if (!(await confirmar('¿Eliminar la rutina? El paciente dejará de verla.', { texto: 'Eliminar', peligro: true }))) return;
          try { await api.del(`/rutinas/${x.id}`); toast('Rutina eliminada'); rutinas(); } catch (e) { manejarError(e); }
        } }, 'Eliminar') : null) },
    ], lista, { vacio: 'Sin rutinas registradas' })));
}

// ------------------------------------------------------------------ Evolución
async function evolucion() {
  const [meds, evs] = await Promise.all([api.get(`/pacientes/${id}/antropometria`), api.get(`/pacientes/${id}/evaluaciones-fisicas`)]);
  vaciar(contenido,
    h('div', { class: 'fila', style: 'margin-bottom:1rem' },
      h('a', { class: 'btn', href: `/comun/evolucion.html?pacienteId=${id}` }, 'Ver gráficos de evolución'),
      puedeNutri ? h('button', { class: 'btn secundario', onclick: () => formMedicion() }, 'Cargar antropometría') : null,
      puedeEntr ? h('button', { class: 'btn secundario', onclick: formEvaluacion }, 'Cargar evaluación física') : null),
    h('div', { class: 'tarjeta' }, h('h2', {}, 'Antropometría'), tabla([
      { titulo: 'Fecha', render: m => fmt.fecha(m.fecha) }, { titulo: 'Peso', render: m => `${fmt.numero(m.pesoKg)} kg`, clase: 'num' },
      { titulo: 'Talla', render: m => m.tallaCm ? `${fmt.numero(m.tallaCm)} cm` : '', clase: 'num' }, { titulo: 'IMC', render: m => fmt.numero(m.imc), clase: 'num' },
      { titulo: '% grasa', render: m => fmt.numero(m.pctGrasa), clase: 'num' }, { titulo: '% músculo', render: m => fmt.numero(m.pctMusculo), clase: 'num' },
      { titulo: 'Cintura', render: m => fmt.numero(m.circCintura), clase: 'num' }, { titulo: 'Profesional', campo: 'profesional' },
      { titulo: '', render: m => m.editable ? h('div', { class: 'fila' },
        h('button', { class: 'btn chico secundario', onclick: () => formMedicion(m) }, 'Editar'),
        h('button', { class: 'btn chico peligro secundario', onclick: async () => {
          if (!(await confirmar('¿Eliminar la medición?', { peligro: true, texto: 'Eliminar' }))) return;
          try { await api.del(`/antropometria/${m.id}`); evolucion(); } catch (e) { manejarError(e); }
        } }, 'Eliminar')) : '' },
    ], [...meds].reverse(), { vacio: 'Todavía no hay mediciones registradas' })),
    h('div', { class: 'tarjeta' }, h('h2', {}, 'Evaluaciones de desempeño'), tabla([
      { titulo: 'Fecha', render: e => fmt.fecha(e.fecha) }, { titulo: 'Prueba', campo: 'prueba' },
      { titulo: 'Valor', render: e => `${fmt.numero(e.valor)} ${e.unidad}`, clase: 'num' }, { titulo: 'Observaciones', campo: 'observaciones' },
      { titulo: 'Profesional', campo: 'profesional' },
      { titulo: '', render: e => e.editable ? h('button', { class: 'btn chico peligro secundario', onclick: async () => {
        if (!(await confirmar('¿Eliminar la evaluación?', { peligro: true, texto: 'Eliminar' }))) return;
        try { await api.del(`/evaluaciones-fisicas/${e.id}`); evolucion(); } catch (er) { manejarError(er); }
      } }, 'Eliminar') : '' },
    ], [...evs].reverse(), { vacio: 'Sin evaluaciones registradas' })));
}

async function formMedicion(m = null) {
  const ultimaTalla = m?.tallaCm;
  const n = (nombre, etiqueta, min, max, valor, extra = {}) => campo({ nombre, etiqueta, tipo: 'number', step: '0.1', min, max, valor, ...extra });
  const imc = h('strong', {}, '—');
  const form = h('form', { novalidate: true }, h('div', { class: 'campos c3' },
    campo({ nombre: 'fecha', etiqueta: 'Fecha', tipo: 'date', requerido: true, valor: m?.fecha || hoyISO(), max: hoyISO() }),
    n('pesoKg', 'Peso (kg)', 20, 300, m?.pesoKg, { requerido: true }),
    n('tallaCm', 'Talla (cm)', 100, 230, m?.tallaCm ?? ultimaTalla, { ayuda: 'Si la dejás vacía se usa la última registrada.' }),
    n('pctGrasa', '% grasa', 3, 60, m?.pctGrasa), n('pctMusculo', '% músculo', 10, 70, m?.pctMusculo),
    n('circCintura', 'Cintura (cm)', 10, 200, m?.circCintura), n('circCadera', 'Cadera (cm)', 10, 200, m?.circCadera),
    n('circBrazo', 'Brazo (cm)', 10, 200, m?.circBrazo), n('circMuslo', 'Muslo (cm)', 10, 200, m?.circMuslo),
    n('circPecho', 'Pecho (cm)', 10, 200, m?.circPecho), n('circPantorrilla', 'Pantorrilla (cm)', 10, 200, m?.circPantorrilla),
    campo({ nombre: 'observaciones', etiqueta: 'Observaciones', tipo: 'textarea', valor: m?.observaciones, ancho: true, maxLength: 2000 })),
  h('p', {}, 'IMC calculado: ', imc));
  const calc = () => { const pe = Number(form.pesoKg.value); const t = Number(form.tallaCm.value) / 100; imc.textContent = pe && t ? (pe / (t * t)).toFixed(1) : '—'; };
  form.pesoKg.addEventListener('input', calc);
  form.tallaCm.addEventListener('input', calc);
  calc();
  await modal({
    titulo: m ? 'Editar medición' : 'Cargar antropometría', cuerpo: form, ancho: true,
    botones: [{ texto: 'Cancelar', clase: 'secundario' }, { texto: 'Guardar', accion: async () => {
      const d = leerForm(form);
      const r = m ? await api.put(`/antropometria/${m.id}`, d) : await api.post(`/pacientes/${id}/antropometria`, d);
      (r.advertencias || []).forEach(a => toast(a, 'advertencia', 7000));
      toast('Medición guardada');
      evolucion();
    } }],
  });
}

async function formEvaluacion() {
  const filas = h('div', { class: 'columna' });
  const unidades = ['kg', 'reps', 's', 'min', 'm', 'cm', 'km/h', 'otra'].map(u => ({ valor: u, texto: u }));
  const agregarFila = () => filas.append(h('div', { class: 'campos c3 prueba', style: 'border-bottom:1px dashed var(--borde);padding-bottom:.6rem' },
    campo({ nombre: 'prueba', etiqueta: 'Prueba', requerido: true, maxLength: 80, placeholder: 'Ej.: Sentadilla 1RM' }),
    campo({ nombre: 'valor', etiqueta: 'Valor', tipo: 'number', step: '0.01', min: 0, max: 10000, requerido: true }),
    campo({ nombre: 'unidad', etiqueta: 'Unidad', tipo: 'select', opciones: unidades }),
    campo({ nombre: 'observaciones', etiqueta: 'Observaciones', maxLength: 300, ancho: true })));
  agregarFila();
  const fecha = campo({ nombre: 'fecha', etiqueta: 'Fecha', tipo: 'date', valor: hoyISO(), max: hoyISO() });
  const cuerpo = h('form', {}, fecha, filas, h('button', { type: 'button', class: 'btn chico secundario', onclick: agregarFila }, '+ Agregar prueba'));
  await modal({
    titulo: 'Cargar evaluación física', cuerpo, ancho: true,
    botones: [{ texto: 'Cancelar', clase: 'secundario' }, { texto: 'Guardar', accion: async () => {
      const pruebas = [...filas.querySelectorAll('.prueba')].map(f => ({
        prueba: f.querySelector('[name=prueba]').value.trim(), valor: Number(f.querySelector('[name=valor]').value),
        unidad: f.querySelector('[name=unidad]').value, observaciones: f.querySelector('[name=observaciones]').value.trim() || null,
      })).filter(x => x.prueba);
      if (!pruebas.length) { toast('Cargá al menos una prueba', 'advertencia'); return false; }
      await api.post(`/pacientes/${id}/evaluaciones-fisicas`, { fecha: cuerpo.querySelector('[name=fecha]').value, pruebas });
      toast('Evaluación guardada');
      evolucion();
    } }],
  });
}

// ------------------------------------------------------------------ Registros
async function registros() {
  const desde = sumarDias(hoyISO(), -27);
  const [nut, ent, comentarios, molestias] = await Promise.all([
    api.get(`/pacientes/${id}/registros-nutricion${qs({ desde })}`),
    api.get(`/pacientes/${id}/registros-entrenamiento${qs({ desde })}`),
    api.get(`/pacientes/${id}/comentarios`),
    api.get(`/pacientes/${id}/molestias`),
  ]);
  const canvas = h('canvas', { 'aria-label': 'Gráfico de hambre y saciedad' });
  vaciar(contenido,
    h('div', { class: 'grilla g2' },
      h('div', { class: 'tarjeta' }, h('h2', {}, 'Nutrición (últimas 4 semanas)'),
        h('p', {}, `Cumplimiento del plan: ${fmt.porcentaje(nut.cumplimientoPct)}`),
        h('div', { class: 'barra' }, h('span', { style: `width:${Math.min(100, nut.cumplimientoPct)}%` })),
        h('h3', { style: 'margin-top:1rem' }, 'Hambre y saciedad'), h('div', { class: 'grafico' }, canvas),
        h('h3', {}, 'Comidas con marcas'), tabla([
          { titulo: 'Fecha', render: r => fmt.fecha(r.fecha) },
          { titulo: 'Realizada', render: r => (r.realizada ? '✔' : r.conCambios ? 'Con cambios' : '—') },
          { titulo: 'No le gustó', render: r => (r.noLeGusto ? '✔' : '') }, { titulo: 'Le cayó mal', render: r => (r.leCayoMal ? '✔' : '') },
        ], nut.registros.filter(r => r.conCambios || r.noLeGusto || r.leCayoMal), { vacio: 'Sin marcas' }),
        h('h3', {}, 'Reemplazos aplicados'), tabla([
          { titulo: 'Comida', render: r => `${DIAS[r.diaSemana]} · ${r.tipoComida}` }, { titulo: 'Original', campo: 'original' },
          { titulo: 'Reemplazo', render: r => `${r.reemplazo} (${fmt.numero(r.gramos)} g)` }, { titulo: 'Desde', render: r => fmt.fecha(r.desde) },
        ], nut.reemplazos, { vacio: 'Sin reemplazos' })),
      h('div', { class: 'tarjeta' }, h('h2', {}, 'Entrenamiento (últimas 4 semanas)'),
        h('p', {}, `Cumplimiento de la rutina: ${fmt.porcentaje(ent.cumplimientoPct)}`),
        h('div', { class: 'barra' }, h('span', { style: `width:${Math.min(100, ent.cumplimientoPct)}%` })),
        h('h3', { style: 'margin-top:1rem' }, 'Registros y cargas'), tabla([
          { titulo: 'Fecha', render: r => fmt.fecha(r.fecha) }, { titulo: 'Ejercicio', campo: 'ejercicio' },
          { titulo: 'Hecho', render: r => (r.completado ? '✔' : '—') }, { titulo: 'Carga', render: r => r.cargaKg ? `${fmt.numero(r.cargaKg)} kg` : '' },
          { titulo: 'Series × reps', render: r => r.seriesRealizadas ? `${r.seriesRealizadas} × ${r.repeticionesRealizadas ?? ''}` : '' },
        ], [...ent.registros].reverse(), { vacio: 'Sin registros' }),
        h('h3', {}, 'Variantes elegidas'), tabla([
          { titulo: 'Original', campo: 'original' }, { titulo: 'Variante', campo: 'variante' }, { titulo: 'Fecha', render: r => fmt.fecha(r.fecha) },
        ], ent.reemplazos, { vacio: 'Sin variantes' }))),
    h('div', { class: 'grilla g2' },
      h('div', { class: 'tarjeta' }, h('h2', {}, 'Comentarios'), tabla([
        { titulo: 'Fecha', render: c => fmt.fechaHora(c.createdAt) }, { titulo: 'Sobre', render: c => c.referencia },
        { titulo: 'Comentario', campo: 'texto' }, { titulo: '', render: c => (c.leidoAt ? chip('Leído') : chip('Nuevo', 'AUSENTE')) },
      ], comentarios, { vacio: 'Sin comentarios' })),
      h('div', { class: 'tarjeta' }, h('h2', {}, 'Molestias'), tabla([
        { titulo: 'Fecha', render: m => fmt.fecha(m.fecha) }, { titulo: 'Descripción', campo: 'descripcion' },
        { titulo: 'Intensidad', render: m => chip(`${m.intensidad}/10`, m.prioritaria ? 'prioritaria' : '') },
        { titulo: 'Zona', campo: 'ubicacionCorporal' }, { titulo: 'Contexto', render: m => m.contexto.toLowerCase() },
        { titulo: 'Ejercicio', campo: 'ejercicio' },
      ], molestias, { vacio: 'Sin molestias registradas' }))));
  if (window.Chart && nut.observaciones.length) {
    new window.Chart(canvas, {
      type: 'line',
      data: { labels: nut.observaciones.map(o => fmt.fecha(o.fecha)), datasets: [
        { label: 'Hambre', data: nut.observaciones.map(o => o.nivelHambre), borderColor: '#B7791F', tension: .3 },
        { label: 'Saciedad', data: nut.observaciones.map(o => o.nivelSaciedad), borderColor: '#2E5E3A', tension: .3 }] },
      options: { responsive: true, maintainAspectRatio: false, scales: { y: { min: 1, max: 5, ticks: { stepSize: 1 } } } },
    });
  } else {
    canvas.replaceWith(h('div', { class: 'vacio' }, 'Sin observaciones diarias'));
  }
}
