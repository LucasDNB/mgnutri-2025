// Spec 07: cuenta corriente del paciente, registrar pago (CU-44) / deuda (CU-45), historial y correcciones.
import { api, qs, descargar } from '../core/api.js';
import { iniciarPagina, encabezado } from '../core/pagina.js';
import { h, tabla, toast, modal, confirmar, pedirTexto, campo, leerForm, manejarError, vaciar, demorar, param, chip } from '../core/ui.js';
import { fmt, hoyISO, sumarDias } from '../core/format.js';

const { main } = await iniciarPagina({ roles: ['ADMIN'], titulo: 'Pagos' });
let pacienteId = param('pacienteId');
const MEDIOS = [['EFECTIVO', 'Efectivo'], ['TRANSFERENCIA', 'Transferencia'], ['DEBITO', 'Débito'], ['CREDITO', 'Crédito'], ['MERCADO_PAGO', 'Mercado Pago'], ['OTRO', 'Otro']];

const buscador = h('input', { type: 'search', placeholder: 'Buscar paciente por apellido, nombre o documento' });
const opciones = h('div', { class: 'fila', style: 'margin-top:.5rem' });
const cuentaEl = h('div');
main.append(encabezado('Pagos', [], h('a', { class: 'btn secundario', href: '/admin/deudores.html' }, 'Deudores'),
  h('a', { class: 'btn secundario', href: '/admin/aranceles.html' }, 'Aranceles'), h('button', { class: 'btn secundario', onclick: generarCuotas }, 'Generar cuotas del mes')),
h('div', { class: 'tarjeta' }, buscador, opciones), cuentaEl);

buscador.addEventListener('input', demorar(async () => {
  if (!buscador.value.trim()) { vaciar(opciones); return; }
  const r = await api.get(`/pacientes${qs({ q: buscador.value, size: 8 })}`);
  vaciar(opciones, r.content.map(p => h('button', { class: 'btn chico secundario', onclick: () => { pacienteId = p.id; vaciar(opciones); buscador.value = ''; cargar(); } },
    `${p.apellido}, ${p.nombre} · ${p.nroDocumento}`)));
}));

// Cada carga lleva un número: si llega la respuesta de un pedido viejo, se descarta para no pisar el resultado más reciente.
let pedidos = 0;
async function cargar() {
  const n = ++pedidos;
  if (!pacienteId) { vaciar(cuentaEl, h('div', { class: 'vacio' }, 'Buscá un paciente para ver su cuenta corriente.')); return; }
  history.replaceState(null, '', `?pacienteId=${pacienteId}`);
  try {
    const c = await api.get(`/pacientes/${pacienteId}/cuenta`);
    if (n !== pedidos) return;
    const situacion = { DEUDOR: ['Saldo deudor', 'ROJO'], A_FAVOR: ['Saldo a favor', 'OK'], AL_DIA: ['Al día', 'OK'] }[c.situacion];
    vaciar(cuentaEl,
      h('div', { class: 'tarjeta' },
        h('div', { class: 'fila entre' }, h('h2', {}, h('a', { href: `/comun/paciente.html?id=${c.pacienteId}` }, c.paciente)),
          h('div', { class: 'fila' }, h('button', { class: 'btn', onclick: nuevoPago }, '+ Nuevo pago'), h('button', { class: 'btn secundario', onclick: nuevaDeuda }, '+ Registrar deuda'))),
        h('div', { class: 'grilla g4' },
          h('div', {}, h('div', { class: 'suave' }, 'Saldo'), h('div', { style: 'font-size:1.5rem;font-weight:800' }, fmt.moneda(Math.abs(c.saldo))), chip(situacion[0], situacion[1])),
          h('div', {}, h('div', { class: 'suave' }, 'Total cargos'), h('strong', {}, fmt.moneda(c.totalCargos))),
          h('div', {}, h('div', { class: 'suave' }, 'Total pagos'), h('strong', {}, fmt.moneda(c.totalPagos))),
          h('div', {}, h('div', { class: 'suave' }, 'Fecha de la deuda'), h('strong', {}, c.fechaDeuda ? `${fmt.fecha(c.fechaDeuda)} (${c.diasAtraso} días)` : '—')))),
      h('div', { class: 'tarjeta' }, h('div', { class: 'fila entre' }, h('h2', {}, 'Movimientos'),
        h('div', { class: 'fila' },
          h('button', { class: 'btn chico secundario', onclick: () => descargar(`/pacientes/${pacienteId}/pagos?formato=pdf`) }, 'Historial de pagos PDF'),
          h('button', { class: 'btn chico secundario', onclick: () => descargar(`/pacientes/${pacienteId}/pagos?formato=xlsx`) }, 'Excel'))),
      tabla([
        { titulo: 'Fecha', render: m => fmt.fecha(m.fecha) },
        { titulo: 'Tipo', render: m => chip(m.tipo === 'PAGO' ? 'Pago' : 'Cargo', m.tipo === 'PAGO' ? 'OK' : 'AUSENTE') },
        { titulo: 'Concepto', render: m => [m.concepto, m.comprobante ? h('div', { class: 'suave' }, `Comprobante N.º ${m.comprobante}`) : null,
          m.vencimiento ? h('div', { class: 'suave' }, `Vence ${fmt.fecha(m.vencimiento)}`) : null, m.anulado ? h('div', { class: 'suave' }, `Anulado: ${m.motivoAnulacion}`) : null] },
        { titulo: 'Monto', render: m => (m.tipo === 'PAGO' ? '− ' : '+ ') + fmt.moneda(m.monto), clase: 'num' },
        { titulo: 'Saldo', render: m => fmt.moneda(m.saldo), clase: 'num' },
        { titulo: '', render: m => m.anulado ? '' : h('div', { class: 'fila' },
          m.tipo === 'PAGO' ? h('button', { class: 'btn chico secundario', onclick: () => descargar(`/pagos/${m.id}/comprobante.pdf`) }, 'Comprobante') : null,
          h('button', { class: 'btn chico secundario', onclick: () => (m.tipo === 'PAGO' ? editarPago(m) : editarCargo(m)) }, 'Corregir'),
          h('button', { class: 'btn chico peligro secundario', onclick: () => anular(m) }, 'Anular')) },
      ], c.movimientos, { vacio: 'El paciente no registra movimientos', claseFila: m => (m.anulado ? 'tachado' : '') })));
  } catch (e) { manejarError(e); }
}

function formPago(m = {}, conMotivo = false) {
  return h('form', { novalidate: true }, h('div', { class: 'campos c2' },
    campo({ nombre: 'monto', etiqueta: 'Monto', tipo: 'number', min: 0.01, step: 0.01, requerido: true, valor: m.monto }),
    campo({ nombre: 'fecha', etiqueta: 'Fecha', tipo: 'date', max: hoyISO(), valor: m.fecha || hoyISO() }),
    campo({ nombre: 'medio', etiqueta: 'Medio de pago', tipo: 'select', valor: m.medio || 'EFECTIVO', opciones: MEDIOS.map(([v, t]) => ({ valor: v, texto: t })) }),
    campo({ nombre: 'referencia', etiqueta: 'Referencia', maxLength: 60, valor: m.referencia, ayuda: 'Número de transferencia u operación (opcional).' }),
    campo({ nombre: 'observaciones', etiqueta: 'Observaciones', tipo: 'textarea', maxLength: 300, ancho: true }),
    conMotivo ? campo({ nombre: 'motivo', etiqueta: 'Motivo de la corrección', requerido: true, maxLength: 200, ancho: true }) : null));
}

async function nuevoPago() {
  const form = formPago();
  await modal({ titulo: 'Nuevo pago', cuerpo: form, botones: [{ texto: 'Cancelar', clase: 'secundario' }, { texto: 'Registrar pago', accion: async () => {
    const d = leerForm(form);
    if (!d.monto || d.monto <= 0) { toast('Ingresá un monto mayor a 0', 'advertencia'); return false; }
    const r = await api.post(`/pacientes/${pacienteId}/pagos`, d);
    toast(`Pago registrado. Comprobante N.º ${r.nroComprobante}`);
    if (r.mensaje) toast(r.mensaje, 'advertencia', 7000);
    if (await confirmar('¿Querés imprimir el comprobante?', { texto: 'Descargar comprobante' })) descargar(`/pagos/${r.id}/comprobante.pdf`);
    cargar();
  } }] });
}

async function editarPago(m) {
  const form = formPago({ monto: m.monto, fecha: m.fecha, medio: m.medio, referencia: m.referencia }, true);
  await modal({ titulo: 'Corregir pago', cuerpo: form, botones: [{ texto: 'Cancelar', clase: 'secundario' }, { texto: 'Guardar', accion: async () => {
    await api.put(`/pagos/${m.id}`, { ...leerForm(form), version: m.version });
    toast('Pago corregido');
    cargar();
  } }] });
}

async function formDeuda(m = {}, conMotivo = false) {
  const aranceles = (await api.get('/aranceles')).filter(a => a.activo);
  const form = h('form', { novalidate: true }, h('div', { class: 'campos c2' },
    campo({ nombre: 'arancelId', etiqueta: 'Arancel', tipo: 'select', valor: m.arancelId || '', opciones: [{ valor: '', texto: 'Concepto libre' }, ...aranceles.map(a => ({ valor: a.id, texto: `${a.concepto} (${fmt.moneda(a.monto)})` }))] }),
    campo({ nombre: 'concepto', etiqueta: 'Concepto', requerido: true, maxLength: 100, valor: m.concepto }),
    campo({ nombre: 'periodo', etiqueta: 'Período (AAAA-MM)', tipo: 'month', valor: m.periodo || hoyISO().substring(0, 7) }),
    campo({ nombre: 'monto', etiqueta: 'Monto', tipo: 'number', min: 0.01, step: 0.01, requerido: true, valor: m.monto }),
    campo({ nombre: 'fecha', etiqueta: 'Fecha', tipo: 'date', valor: m.fecha || hoyISO() }),
    campo({ nombre: 'vencimiento', etiqueta: 'Vencimiento', tipo: 'date', valor: m.vencimiento || sumarDias(hoyISO(), 10) }),
    conMotivo ? campo({ nombre: 'motivo', etiqueta: 'Motivo de la corrección', requerido: true, maxLength: 200, ancho: true })
      : campo({ nombre: 'notificarAhora', etiqueta: 'Notificar ahora por WhatsApp', tipo: 'checkbox', ancho: true })));
  form.arancelId.addEventListener('change', () => {
    const a = aranceles.find(x => String(x.id) === form.arancelId.value);
    if (a) { form.concepto.value = a.concepto; form.monto.value = a.monto; }
  });
  return form;
}

async function nuevaDeuda() {
  const form = await formDeuda();
  await modal({ titulo: 'Registrar deuda', cuerpo: form, botones: [{ texto: 'Cancelar', clase: 'secundario' }, { texto: 'Registrar', accion: async () => {
    const d = leerForm(form);
    d.arancelId = d.arancelId ? Number(d.arancelId) : null;
    const r = await api.post(`/pacientes/${pacienteId}/cargos`, d);
    toast('Deuda registrada');
    if (r.notificacion === 'OMITIDA_RECIENTE') toast('No se notificó: el paciente recibió un recordatorio en los últimos 6 días.', 'advertencia', 7000);
    cargar();
  } }] });
}

async function editarCargo(m) {
  const form = await formDeuda({ concepto: m.concepto.replace(` ${m.periodo || ''}`, '').trim(), periodo: m.periodo, monto: m.monto, fecha: m.fecha, vencimiento: m.vencimiento }, true);
  await modal({ titulo: 'Corregir deuda', cuerpo: form, botones: [{ texto: 'Cancelar', clase: 'secundario' }, { texto: 'Guardar', accion: async () => {
    const d = leerForm(form);
    d.arancelId = d.arancelId ? Number(d.arancelId) : null;
    await api.put(`/cargos/${m.id}`, { ...d, version: m.version });
    toast('Deuda corregida');
    cargar();
  } }] });
}

async function anular(m) {
  const motivo = await pedirTexto(m.tipo === 'PAGO' ? 'Anular pago' : 'Anular deuda', { ayuda: 'El movimiento quedará tachado y no sumará al saldo.', texto: 'Anular' });
  if (!motivo) return;
  try {
    await api.post(m.tipo === 'PAGO' ? `/pagos/${m.id}/anular` : `/cargos/${m.id}/anular`, { motivo });
    toast('Movimiento anulado');
    cargar();
  } catch (e) { manejarError(e); }
}

async function generarCuotas() {
  const aranceles = (await api.get('/aranceles')).filter(a => a.activo);
  if (!aranceles.length) { toast('Primero cargá un arancel', 'advertencia'); return; }
  const form = h('form', {}, h('div', { class: 'campos c2' },
    campo({ nombre: 'arancelId', etiqueta: 'Arancel', tipo: 'select', opciones: aranceles.map(a => ({ valor: a.id, texto: `${a.concepto} (${fmt.moneda(a.monto)})` })) }),
    campo({ nombre: 'periodo', etiqueta: 'Período', tipo: 'month', valor: hoyISO().substring(0, 7) }),
    campo({ nombre: 'vencimiento', etiqueta: 'Vencimiento', tipo: 'date', valor: sumarDias(hoyISO(), 10) })),
  h('p', { class: 'suave' }, 'Se crea el cargo para todos los pacientes activos. Los duplicados se omiten.'));
  await modal({ titulo: 'Generar cuotas del mes', cuerpo: form, botones: [{ texto: 'Cancelar', clase: 'secundario' }, { texto: 'Generar', accion: async () => {
    const d = leerForm(form);
    const r = await api.post('/cargos/generacion-masiva', { arancelId: Number(d.arancelId), periodo: d.periodo, vencimiento: d.vencimiento, todos: true });
    toast(`Cuotas creadas: ${r.creados}. Omitidas por duplicadas: ${r.omitidosDuplicados}`, 'ok', 7000);
    cargar();
  } }] });
}

cargar();
