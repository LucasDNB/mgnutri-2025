// Marcas de una comida del plan en una fecha (CU-17, D-38): se usan en «Mi plan alimentario» y en «Registro del día».
//  · Completada → «La hice como estaba propuesta», o «La hice con cambios» si la comida tiene un alimento reemplazado.
//  · Saltada / reemplazada → «No completada».
//  · «No me gustó» y «Me cayó mal» se guardan aparte.
import { api } from '../core/api.js';
import { h, toast, manejarError } from '../core/ui.js';

const VACIO = { realizada: false, conCambios: false, omitida: false, noLeGusto: false, leCayoMal: false };

export const conReemplazo = (c) => c.items.some(i => i.reemplazo);
export const completada = (r) => !!(r && (r.realizada || r.conCambios));

export function estadoTexto(r) {
  if (r?.realizada) return 'La hice como estaba propuesta';
  if (r?.conCambios) return 'La hice con cambios';
  if (r?.omitida) return 'No completada';
  return 'Pendiente';
}

/**
 * @param c comida del plan
 * @param o { fecha, registro, editable, aviso, alGuardar(registroNuevo) }
 */
export function marcasComida(c, { fecha, registro, editable, aviso, alGuardar }) {
  const r = { ...VACIO, ...(registro || {}) };
  const hecha = completada(r);
  async function guardar(cambios) {
    const n = { ...r, ...cambios };
    try {
      const res = await api.put('/me/registro-comidas', { comidaPlanId: c.id, fecha, realizada: n.realizada, conCambios: n.conCambios,
        omitida: n.omitida, noLeGusto: n.noLeGusto, leCayoMal: n.leCayoMal });
      toast('Guardado ✔', 'ok', 1500);
      alGuardar(res);
    } catch (e) { manejarError(e); }
  }
  const cambios = conReemplazo(c);
  const check = (campo, texto) => h('label', { class: 'check' },
    h('input', { type: 'checkbox', checked: r[campo], disabled: !editable, onchange: (e) => guardar({ [campo]: e.target.checked }) }), texto);
  return h('div', { class: 'marcas-comida' },
    h('div', { class: 'fila' },
      h('button', { type: 'button', class: `btn chico ${hecha ? '' : 'secundario'}`, 'aria-pressed': String(hecha), disabled: !editable,
        title: cambios ? 'Tiene un alimento reemplazado: se registra «La hice con cambios»' : 'Se registra «La hice como estaba propuesta»',
        onclick: () => guardar(hecha ? { realizada: false, conCambios: false } : { realizada: !cambios, conCambios: cambios, omitida: false }) },
      hecha ? '✔ Completada' : 'Completada'),
      h('button', { type: 'button', class: `btn chico ${r.omitida ? 'peligro' : 'secundario'}`, 'aria-pressed': String(r.omitida), disabled: !editable,
        onclick: () => guardar(r.omitida ? { omitida: false } : { omitida: true, realizada: false, conCambios: false }) },
      r.omitida ? '✔ Saltada / reemplazada' : 'Saltada / reemplazada')),
    h('div', { class: 'fila' }, check('noLeGusto', 'No me gustó'), check('leCayoMal', 'Me cayó mal')),
    h('small', { class: 'suave' }, editable ? `Registro: ${estadoTexto(r)}` : aviso));
}
