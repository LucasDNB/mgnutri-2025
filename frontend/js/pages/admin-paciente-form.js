// CU-01 Alta y CU-02 Modificación de paciente (con asignación opcional, CA-01.1.7).
import { api } from '../core/api.js';
import { iniciarPagina, encabezado } from '../core/pagina.js';
import { h, campo, leerForm, validarForm, manejarError, toast, modal, param } from '../core/ui.js';
import { validar, telefonoAR } from '../core/validators.js';

const { main } = await iniciarPagina({ roles: ['ADMIN'], titulo: 'Paciente' });
const id = param('id');
const p = id ? await api.get(`/pacientes/${id}`) : {};
const profesionales = id ? [] : (await api.get('/profesionales?size=100')).content;

const tiposDoc = ['DNI', 'PAS', 'LC', 'LE', 'CI'].map(v => ({ valor: v, texto: v }));
const asignacion = id ? null : h('fieldset', { class: 'tarjeta' }, h('legend', {}, 'Asignar profesionales (opcional)'),
  profesionales.length === 0 ? h('p', { class: 'suave' }, 'No hay profesionales disponibles.') :
    profesionales.flatMap(pr => {
      const areas = pr.tipo === 'INTEGRAL' ? ['NUTRICION', 'ENTRENAMIENTO'] : [pr.tipo === 'NUTRICIONISTA' ? 'NUTRICION' : 'ENTRENAMIENTO'];
      return areas.map(a => h('label', { class: 'check' }, h('input', { type: 'checkbox', class: 'asig', dataset: { prof: pr.id, area: a } }),
        `${pr.apellido}, ${pr.nombre} — ${a === 'NUTRICION' ? 'Nutrición' : 'Entrenamiento'}`));
    }));

const form = h('form', { novalidate: true },
  h('div', { class: 'tarjeta' }, h('h2', {}, 'Datos personales'), h('div', { class: 'campos c2' },
    campo({ nombre: 'nombre', etiqueta: 'Nombre', requerido: true, valor: p.nombre, maxLength: 80 }),
    campo({ nombre: 'apellido', etiqueta: 'Apellido', requerido: true, valor: p.apellido, maxLength: 80 }),
    campo({ nombre: 'tipoDocumento', etiqueta: 'Tipo de documento', tipo: 'select', opciones: tiposDoc, valor: p.tipoDocumento || 'DNI', requerido: true }),
    campo({ nombre: 'nroDocumento', etiqueta: 'Número de documento', requerido: true, valor: p.nroDocumento, ayuda: 'Sin puntos. Será el usuario para ingresar al sistema.' }),
    campo({ nombre: 'fechaNacimiento', etiqueta: 'Fecha de nacimiento', tipo: 'date', requerido: true, valor: p.fechaNacimiento }),
    campo({ nombre: 'sexo', etiqueta: 'Sexo', tipo: 'select', requerido: true, valor: p.sexo || '', opciones: [{ valor: '', texto: 'Elegí…' }, { valor: 'F', texto: 'Femenino' }, { valor: 'M', texto: 'Masculino' }, { valor: 'X', texto: 'X / No binario' }] }))),
  h('div', { class: 'tarjeta' }, h('h2', {}, 'Contacto'), h('div', { class: 'campos c2' },
    campo({ nombre: 'telefonoWhatsapp', etiqueta: 'Teléfono WhatsApp', tipo: 'tel', requerido: true, valor: p.telefonoWhatsapp,
      ayuda: 'Con código de área, por ejemplo 11 3456-7890. Se usa para enviar credenciales y recordatorios.' }),
    campo({ nombre: 'email', etiqueta: 'Email', tipo: 'email', valor: p.email }),
    campo({ nombre: 'domicilio', etiqueta: 'Domicilio', valor: p.domicilio, maxLength: 200, ancho: true }))),
  h('div', { class: 'tarjeta' }, h('h2', {}, 'Datos iniciales'), h('div', { class: 'campos' },
    campo({ nombre: 'objetivo', etiqueta: 'Objetivo', valor: p.objetivo, maxLength: 300, ayuda: 'Por ejemplo: descenso de peso, ganancia muscular.' }),
    campo({ nombre: 'antecedentesSalud', etiqueta: 'Antecedentes de salud informados', tipo: 'textarea', valor: p.antecedentesSalud, maxLength: 2000,
      ayuda: 'Información aportada por el paciente. No es un diagnóstico. Se guarda cifrada.' }),
    campo({ nombre: 'observaciones', etiqueta: 'Observaciones', tipo: 'textarea', valor: p.observaciones, maxLength: 2000 }))),
  asignacion,
  h('div', { class: 'acciones' }, h('a', { class: 'btn secundario', href: id ? `/comun/paciente.html?id=${id}` : '/comun/pacientes.html' }, 'Cancelar'),
    h('button', { class: 'btn', type: 'submit' }, id ? 'Guardar cambios' : 'Registrar paciente')));

form.addEventListener('submit', async (e) => {
  e.preventDefault();
  const ok = validarForm(form, {
    nombre: validar.nombre, apellido: validar.nombre, nroDocumento: validar.documento, fechaNacimiento: validar.fechaNacimiento,
    sexo: validar.requerido, telefonoWhatsapp: validar.telefono, email: validar.email,
  });
  if (!ok) return;
  const datos = leerForm(form);
  datos.telefonoWhatsapp = telefonoAR(datos.telefonoWhatsapp);
  delete datos.undefined;
  datos.asignaciones = [...form.querySelectorAll('input.asig:checked')].map(c => ({ profesionalId: Number(c.dataset.prof), area: c.dataset.area }));
  try {
    if (id) {
      datos.version = p.version;
      await api.put(`/pacientes/${id}`, datos);
      toast('Datos del paciente actualizados');
      location.href = `/comun/paciente.html?id=${id}`;
    } else {
      const r = await api.post('/pacientes', datos);
      await modal({
        titulo: 'Paciente registrado correctamente',
        cuerpo: h('div', {},
          h('p', {}, `Usuario: ${r.username}`),
          r.passwordTemporal
            ? h('div', { class: 'aviso advertencia' }, 'No se pudo enviar por WhatsApp. Entregá esta contraseña temporal por otro medio: ',
              h('strong', { style: 'font-size:1.2rem' }, r.passwordTemporal))
            : h('p', {}, 'Las credenciales se enviarán por WhatsApp.')),
        botones: [{ texto: 'Ver ficha', valor: true }],
      });
      location.href = `/comun/paciente.html?id=${r.id}`;
    }
  } catch (err) {
    manejarError(err, form);
  }
});

main.append(encabezado(id ? 'Modificar paciente' : 'Nuevo paciente', [['Pacientes', '/comun/pacientes.html'], [id ? `${p.apellido}, ${p.nombre}` : 'Nuevo']]), form);
