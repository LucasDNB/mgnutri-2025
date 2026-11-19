// US-08.6 / D-23 Bandeja de novedades: comentarios y molestias de pacientes asignados.
import { api } from '../core/api.js';
import { iniciarPagina, encabezado, actualizarContadorNovedades } from '../core/pagina.js';
import { h, tabla, chip, toast, manejarError, vaciar } from '../core/ui.js';
import { fmt } from '../core/format.js';

const { main } = await iniciarPagina({ roles: ['NUTRICIONISTA', 'ENTRENADOR', 'ADMIN'], titulo: 'Novedades' });
const todas = h('input', { type: 'checkbox' });
const res = h('div');
main.append(encabezado('Novedades de pacientes'), h('div', { class: 'tarjeta' }, h('label', { class: 'check' }, todas, 'Mostrar también las leídas'), res));
todas.addEventListener('change', cargar);

// Cada carga lleva un número: si llega la respuesta de un pedido viejo (p. ej. la carga inicial después de
// aplicar un filtro), se descarta para no pisar el resultado más reciente.
let pedidos = 0;
async function cargar() {
  const n = ++pedidos;
  try {
    const lista = await api.get(`/novedades?leidas=${todas.checked}`);
    if (n !== pedidos) return;
    vaciar(res, tabla([
      { titulo: 'Fecha', render: n => fmt.fechaHora(n.fecha) },
      { titulo: 'Tipo', render: n => [chip(n.tipo === 'MOLESTIA' ? 'Molestia' : 'Comentario', n.prioritaria ? 'prioritaria' : n.tipo === 'MOLESTIA' ? 'AUSENTE' : ''), n.prioritaria ? chip('Prioritaria', 'prioritaria') : null] },
      { titulo: 'Paciente', render: n => h('a', { href: `/comun/paciente.html?id=${n.pacienteId}&tab=registros` }, n.paciente) },
      { titulo: 'Detalle', render: n => [h('div', {}, n.texto), h('small', { class: 'suave' }, n.detalle)] },
      { titulo: '', render: n => n.leida ? chip('Leída') : h('button', { class: 'btn chico secundario', onclick: async () => {
        try { await api.post(`/novedades/${n.tipo}/${n.id}/leer`); toast('Marcada como leída'); cargar(); actualizarContadorNovedades(); } catch (e) { manejarError(e); }
      } }, 'Marcar leída') },
    ], lista, { vacio: 'No hay novedades pendientes', claseFila: n => (n.leida ? '' : '') }));
  } catch (e) { manejarError(e); }
}
cargar();
