// CU-04 Alta / CU-05 Modificación / CU-06 Baja de profesional.
import { api } from '../core/api.js';
import { iniciarPagina, encabezado } from '../core/pagina.js';
import { h, campo, leerForm, validarForm, manejarError, toast, modal, confirmar, param } from '../core/ui.js';
import { validar, telefonoAR } from '../core/validators.js';

const { main } = await iniciarPagina({ roles: ['ADMIN'], titulo: 'Profesional' });
const id = param('id');
const p = id ? await api.get(`/profesionales/${id}`) : {};

const form = h('form', { novalidate: true }, h('div', { class: 'tarjeta' }, h('div', { class: 'campos c2' },
  campo({ nombre: 'nombre', etiqueta: 'Nombre', requerido: true, valor: p.nombre, maxLength: 80 }),
  campo({ nombre: 'apellido', etiqueta: 'Apellido', requerido: true, valor: p.apellido, maxLength: 80 }),
  campo({ nombre: 'tipoDocumento', etiqueta: 'Tipo de documento', tipo: 'select', valor: p.tipoDocumento || 'DNI', opciones: ['DNI', 'PAS', 'LC', 'LE', 'CI'] }),
  campo({ nombre: 'nroDocumento', etiqueta: 'Número de documento', requerido: true, valor: p.nroDocumento, ayuda: 'Será su usuario para ingresar.' }),
  campo({ nombre: 'tipo', etiqueta: 'Tipo de profesional', tipo: 'select', requerido: true, valor: p.tipo || '',
    opciones: [{ valor: '', texto: 'Elegí…' }, { valor: 'NUTRICIONISTA', texto: 'Nutricionista' }, { valor: 'ENTRENADOR', texto: 'Entrenador' }, { valor: 'INTEGRAL', texto: 'Integral (nutrición y entrenamiento)' }],
    ayuda: 'Define los permisos: el integral accede a nutrición y entrenamiento.' }),
  campo({ nombre: 'matricula', etiqueta: 'Matrícula', valor: p.matricula, maxLength: 30, ayuda: 'Obligatoria para nutricionistas e integrales.' }),
  campo({ nombre: 'especialidad', etiqueta: 'Especialidad', valor: p.especialidad, maxLength: 100 }),
  campo({ nombre: 'telefonoWhatsapp', etiqueta: 'Teléfono WhatsApp', tipo: 'tel', valor: p.telefonoWhatsapp }),
  campo({ nombre: 'email', etiqueta: 'Email', tipo: 'email', valor: p.email }),
  campo({ nombre: 'duracionTurnoMin', etiqueta: 'Duración del turno (min)', tipo: 'number', min: 15, max: 120, step: 5, valor: p.duracionTurnoMin || 30 }))),
h('div', { class: 'acciones' },
  id && p.tipo !== 'ENTRENADOR' ? h('a', { class: 'btn secundario', href: `/profesional/agenda.html?id=${id}` }, 'Agenda y bloqueos') : null,
  id ? h('button', { type: 'button', class: 'btn secundario', onclick: reenviar }, 'Reenviar credenciales') : null,
  id ? h('button', { type: 'button', class: 'btn peligro', onclick: baja }, 'Dar de baja') : null,
  h('a', { class: 'btn secundario', href: '/admin/profesionales.html' }, 'Volver'),
  h('button', { class: 'btn', type: 'submit' }, id ? 'Guardar cambios' : 'Registrar profesional')));

form.addEventListener('submit', async (e) => {
  e.preventDefault();
  const ok = validarForm(form, {
    nombre: validar.nombre, apellido: validar.nombre, nroDocumento: validar.documento, tipo: validar.requerido, email: validar.email,
    telefonoWhatsapp: (v) => (v && !telefonoAR(v) ? 'Teléfono inválido' : null),
    matricula: (v, d) => (d.tipo !== 'ENTRENADOR' && !v ? 'La matrícula es obligatoria para este tipo' : null),
    duracionTurnoMin: (v) => (v < 15 || v > 120 || v % 5 ? 'Entre 15 y 120 minutos, múltiplo de 5' : null),
  });
  if (!ok) return;
  const d = leerForm(form);
  d.telefonoWhatsapp = d.telefonoWhatsapp ? telefonoAR(d.telefonoWhatsapp) : null;
  try {
    if (id) {
      d.version = p.version;
      const r = await api.put(`/profesionales/${id}`, d);
      for (const a of r.advertencias) toast(a, 'advertencia', 8000);
      toast('Profesional actualizado');
      location.reload();
    } else {
      const r = await api.post('/profesionales', d);
      await modal({ titulo: 'Profesional registrado correctamente', cuerpo: h('div', {}, h('p', {}, `Usuario: ${r.username}`),
        r.passwordTemporal ? h('div', { class: 'aviso advertencia' }, 'No se pudo enviar por WhatsApp. Entregá esta contraseña temporal: ', h('strong', {}, r.passwordTemporal))
          : h('p', {}, 'Las credenciales se enviarán por WhatsApp.'), d.tipo === 'ENTRENADOR' ? null : h('p', {}, 'Recordá cargar su agenda para que pueda recibir turnos.')) });
      location.href = `/admin/profesional.html?id=${r.id}`;
    }
  } catch (err) { manejarError(err, form); }
});

async function reenviar() {
  if (!(await confirmar('Se generará una nueva contraseña temporal.', { texto: 'Reenviar' }))) return;
  try {
    const r = await api.post(`/profesionales/${id}/reenviar-credenciales`);
    await modal({ titulo: 'Credenciales regeneradas', cuerpo: r.passwordTemporal ? h('div', { class: 'aviso advertencia' }, 'Contraseña temporal: ', h('strong', {}, r.passwordTemporal)) : h('p', {}, 'Se enviaron por WhatsApp.') });
  } catch (e) { manejarError(e); }
}

async function baja() {
  if (!(await confirmar('El profesional ya no podrá acceder. Sus turnos futuros se cancelarán y sus pacientes quedarán sin profesional asignado en el área.', { texto: 'Dar de baja', peligro: true, titulo: 'Dar de baja' }))) return;
  try { await api.post(`/profesionales/${id}/baja`, {}); toast('Profesional dado de baja'); location.href = '/admin/profesionales.html'; } catch (e) { manejarError(e); }
}

main.append(encabezado(id ? `${p.apellido}, ${p.nombre}` : 'Nuevo profesional', [['Profesionales', '/admin/profesionales.html'], [id ? p.apellido : 'Nuevo']]),
  id ? h('p', { class: 'suave' }, `Usuario: ${p.username} · ${p.pacientesAsignados} paciente(s) asignado(s) · ${p.tipo === 'ENTRENADOR' ? 'sin turnos (entrenador)' : p.tieneAgenda ? 'con agenda' : 'sin agenda cargada'}`) : null, form);
