// CU-51 Backup manual (estado cada 10 s) y CU-52 programación automática.
import { api, descargar } from '../core/api.js';
import { iniciarPagina, encabezado } from '../core/pagina.js';
import { h, tabla, campo, leerForm, toast, confirmar, manejarError, vaciar, chip } from '../core/ui.js';
import { fmt } from '../core/format.js';

const { main } = await iniciarPagina({ roles: ['ADMIN'], titulo: 'Backups' });
const lista = h('div');
const progForm = h('form');
let timer = null;
main.append(encabezado('Backups', [['Opciones avanzadas', '/admin/opciones.html'], ['Backups']], h('button', { class: 'btn', onclick: generar }, 'Generar backup')),
  h('div', { class: 'aviso' }, 'Además de estas copias, la base de datos en Azure tiene backups automáticos con restauración a un punto en el tiempo.'),
  h('div', { class: 'tarjeta' }, h('h2', {}, 'Copias generadas'), lista),
  h('div', { class: 'tarjeta' }, h('h2', {}, 'Programar backup automático'), progForm));

const tam = (b) => (b == null ? '' : b > 1048576 ? `${(b / 1048576).toFixed(1)} MB` : `${Math.round(b / 1024)} KB`);

async function cargar() {
  const r = await api.get('/backups?size=50');
  vaciar(lista, tabla([
    { titulo: 'Fecha', render: b => fmt.fechaHora(b.iniciadoAt) }, { titulo: 'Tipo', render: b => b.tipo.toLowerCase() },
    { titulo: 'Estado', render: b => [chip(b.estado, b.estado), b.expirado ? chip('expirado', 'FINALIZADO') : null] },
    { titulo: 'Archivo', campo: 'archivo' }, { titulo: 'Tamaño', render: b => tam(b.tamanoBytes), clase: 'num' },
    { titulo: 'Solicitado por', campo: 'solicitadoPor' }, { titulo: 'Error', render: b => h('small', {}, b.error || '') },
    { titulo: '', render: b => b.estado === 'OK' && !b.expirado ? h('button', { class: 'btn chico secundario', onclick: () => descargar(`/backups/${b.id}/descarga`, b.archivo).catch(manejarError) }, 'Descargar') : '' },
  ], r.content, { vacio: 'Todavía no se generaron backups' }));
  clearTimeout(timer);
  if (r.content.some(b => b.estado === 'EN_CURSO')) timer = setTimeout(cargar, 10000);
}

async function generar() {
  if (!(await confirmar('La operación puede tardar varios minutos. El sistema sigue disponible.', { texto: 'Generar backup' }))) return;
  try { await api.post('/backups'); toast('Backup iniciado'); cargar(); } catch (e) { manejarError(e); }
}

async function programacion() {
  const p = await api.get('/backups/programacion');
  const DIAS = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo'];
  vaciar(progForm, h('p', {}, h('strong', {}, p.descripcion)), h('div', { class: 'campos c3' },
    campo({ nombre: 'habilitado', etiqueta: 'Habilitado', tipo: 'checkbox', valor: p.habilitado }),
    campo({ nombre: 'frecuencia', etiqueta: 'Frecuencia', tipo: 'select', valor: p.frecuencia, opciones: [{ valor: 'DIARIA', texto: 'Diaria' }, { valor: 'SEMANAL', texto: 'Semanal' }, { valor: 'MENSUAL', texto: 'Mensual' }] }),
    campo({ nombre: 'diaSemana', etiqueta: 'Día de la semana', tipo: 'select', valor: p.diaSemana, opciones: DIAS.map((d, i) => ({ valor: i + 1, texto: d })) }),
    campo({ nombre: 'diaMes', etiqueta: 'Día del mes (1-28)', tipo: 'number', min: 1, max: 28, valor: p.diaMes }),
    campo({ nombre: 'hora', etiqueta: 'Hora', tipo: 'time', valor: p.hora }),
    campo({ nombre: 'retencionDias', etiqueta: 'Retención (días)', tipo: 'number', min: 7, max: 365, valor: p.retencionDias, ayuda: 'Los archivos más viejos se borran automáticamente.' })),
  h('div', { class: 'acciones' }, h('button', { class: 'btn', type: 'submit' }, 'Guardar programación')));
}
progForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  const d = leerForm(progForm);
  d.diaSemana = Number(d.diaSemana);
  try { await api.put('/backups/programacion', d); toast('Programación guardada'); programacion(); } catch (er) { manejarError(er, progForm); }
});

cargar().catch(manejarError);
programacion().catch(manejarError);
