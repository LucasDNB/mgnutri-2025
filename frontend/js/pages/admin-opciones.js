// Menú de Opciones avanzadas del administrador.
import { iniciarPagina, encabezado } from '../core/pagina.js';
import { h } from '../core/ui.js';

const { main } = await iniciarPagina({ roles: ['ADMIN'], titulo: 'Opciones avanzadas' });
const op = (titulo, href, desc) => h('a', { class: 'tarjeta', href, style: 'text-decoration:none;color:inherit;display:block' }, h('h2', {}, titulo), h('p', { class: 'suave' }, desc));
main.append(encabezado('Opciones avanzadas'), h('div', { class: 'grilla g3' },
  op('Aranceles', '/admin/aranceles.html', 'Conceptos y montos que se cobran.'),
  op('Base de alimentos', '/comun/alimentos.html', 'Catálogo usado en los planes alimentarios.'),
  op('Base de ejercicios', '/comun/ejercicios.html', 'Catálogo usado en las rutinas.'),
  op('Notificaciones', '/admin/notificaciones.html', 'Mensajes de WhatsApp enviados, fallidos y omitidos.'),
  op('Bitácora', '/admin/bitacora.html', 'Registro inalterable de las acciones del sistema.'),
  op('Registro de accesos', '/admin/accesos.html', 'Ingresos, salidas e intentos fallidos.'),
  op('Backups', '/admin/backups.html', 'Copias de seguridad manuales y programadas.'),
  op('Configuración', '/admin/configuracion.html', 'Datos de la central, recordatorios, términos y condiciones.')));
