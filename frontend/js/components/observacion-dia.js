// CU-43 Observaciones del día (hambre, saciedad y ánimo), en una ventana desde «Mi plan alimentario» (D-38).
import { api } from '../core/api.js';
import { h, toast, modal, vaciar } from '../core/ui.js';
import { fmt } from '../core/format.js';

const NIVELES = ['Nada', 'Poca', 'Moderada', 'Bastante', 'Mucha'];
const ANIMO = ['😞', '🙁', '😐', '🙂', '😄'];

/** Abre la ventana para la fecha; devuelve la observación guardada o null si se canceló. */
export async function observacionDelDia(fecha, existente) {
  const valores = { nivelHambre: existente?.nivelHambre ?? null, nivelSaciedad: existente?.nivelSaciedad ?? null, estadoAnimo: existente?.estadoAnimo ?? null };
  const comentario = h('textarea', { maxLength: 300, rows: 3, placeholder: 'Comentario (opcional)' });
  comentario.value = existente?.comentario || '';
  const escala = (campo, titulo, etiquetas, ayuda) => {
    const cont = h('div', { class: 'escala', role: 'radiogroup', 'aria-label': titulo });
    const dibujar = () => vaciar(cont, [1, 2, 3, 4, 5].map(n => h('button', { type: 'button', role: 'radio', 'aria-checked': String(valores[campo] === n),
      class: valores[campo] === n ? 'sel' : '', title: etiquetas[n - 1], onclick: () => { valores[campo] = n; dibujar(); } }, campo === 'estadoAnimo' ? etiquetas[n - 1] : n)));
    dibujar();
    return h('div', { class: 'campo' }, h('label', {}, titulo), cont, ayuda ? h('small', { class: 'suave' }, ayuda) : null);
  };
  let guardada = null;
  await modal({
    titulo: `Observaciones del día ${fmt.fecha(fecha)}`,
    cuerpo: h('div', { class: 'columna' },
      escala('nivelHambre', '¿Cuánta hambre sentiste? *', NIVELES, '1 = nada · 5 = mucha'),
      escala('nivelSaciedad', '¿Qué tan saciado/a quedaste? *', NIVELES, '1 = nada · 5 = mucha'),
      escala('estadoAnimo', 'Estado de ánimo (opcional)', ANIMO),
      comentario,
      h('a', { class: 'suave', href: '/paciente/observaciones.html' }, 'Ver historial de observaciones')),
    botones: [{ texto: 'Cancelar', clase: 'secundario', valor: null }, { texto: 'Guardar', accion: async () => {
      if (!valores.nivelHambre || !valores.nivelSaciedad) { toast('Indicá tu nivel de hambre y de saciedad', 'advertencia'); return false; }
      guardada = await api.put('/me/observaciones-diarias', { fecha, ...valores, comentario: comentario.value });
      toast('Observaciones guardadas');
    } }],
  });
  return guardada;
}
