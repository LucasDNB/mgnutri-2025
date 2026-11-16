// Utilidades de interfaz: construcción segura de DOM (sin innerHTML con datos), toasts, modales, formularios y tablas.

/** Crea un elemento. attrs: class, on<Evento>, dataset, style, y cualquier propiedad/atributo. */
export function h(tag, attrs = {}, ...hijos) {
  const el = document.createElement(tag);
  for (const [k, v] of Object.entries(attrs || {})) {
    if (v === null || v === undefined || v === false) continue;
    if (k === 'class') el.className = v;
    else if (k === 'dataset') Object.assign(el.dataset, v);
    else if (k === 'style') el.setAttribute('style', v);
    else if (k.startsWith('on') && typeof v === 'function') el.addEventListener(k.substring(2).toLowerCase(), v);
    else if (k in el && !['list', 'form', 'type'].includes(k)) el[k] = v;
    else el.setAttribute(k, v === true ? '' : v);
  }
  agregar(el, hijos);
  return el;
}

export function agregar(el, hijos) {
  for (const hijo of [hijos].flat(Infinity)) {
    if (hijo === null || hijo === undefined || hijo === false) continue;
    el.appendChild(hijo instanceof Node ? hijo : document.createTextNode(String(hijo)));
  }
  return el;
}

export function vaciar(el, ...hijos) {
  el.replaceChildren();
  return agregar(el, hijos);
}

// ------------------------------------------------------------------ toasts

function contenedorToasts() {
  let c = document.querySelector('.toasts');
  if (!c) {
    c = h('div', { class: 'toasts', role: 'status', 'aria-live': 'polite' });
    document.body.appendChild(c);
  }
  return c;
}

export function toast(mensaje, tipo = 'ok', ms = 4000) {
  const t = h('div', { class: `toast ${tipo}` }, mensaje);
  contenedorToasts().appendChild(t);
  setTimeout(() => t.remove(), ms);
}

/** Muestra el error de la API (Problem Details) y marca los campos del formulario si corresponde. */
export function manejarError(err, form) {
  console.warn(err);
  if (form && err?.errors?.length) mostrarErrores(form, err.errors);
  toast(err?.message || 'Ocurrió un error inesperado.', 'error', 6000);
}

// ------------------------------------------------------------------ modales

export function modal({ titulo, cuerpo, botones = [{ texto: 'Cerrar', valor: null, clase: 'secundario' }], ancho = false, alAbrir }) {
  return new Promise((resolve) => {
    const anterior = document.activeElement;
    const fondo = h('div', { class: 'fondo-modal', role: 'dialog', 'aria-modal': 'true', 'aria-label': titulo });
    const cerrar = (valor) => {
      fondo.remove();
      document.removeEventListener('keydown', esc);
      anterior?.focus?.();
      resolve(valor);
    };
    const esc = (e) => { if (e.key === 'Escape') cerrar(null); };
    document.addEventListener('keydown', esc);
    const acciones = h('div', { class: 'acciones' }, botones.map(b => h('button', {
      type: 'button', class: `btn ${b.clase || ''}`,
      onclick: async (ev) => {
        if (b.accion) {
          ev.currentTarget.disabled = true;
          try {
            const r = await b.accion();
            if (r === false) { ev.currentTarget.disabled = false; return; }
            cerrar(r === undefined ? b.valor : r);
          } catch (e) {
            ev.currentTarget.disabled = false;
            manejarError(e, fondo.querySelector('form'));
          }
        } else {
          cerrar(b.valor);
        }
      },
    }, b.texto)));
    const caja = h('div', { class: `modal ${ancho ? 'ancho' : ''}` }, h('h2', {}, titulo), cuerpo, acciones);
    fondo.appendChild(caja);
    fondo.addEventListener('click', (e) => { if (e.target === fondo) cerrar(null); });
    document.body.appendChild(fondo);
    (caja.querySelector('input, select, textarea, button') || caja).focus();
    alAbrir?.(caja, cerrar);
  });
}

export async function confirmar(mensaje, { texto = 'Confirmar', peligro = false, titulo = 'Confirmar' } = {}) {
  const r = await modal({
    titulo,
    cuerpo: h('p', {}, mensaje),
    botones: [{ texto: 'Cancelar', valor: false, clase: 'secundario' }, { texto, valor: true, clase: peligro ? 'peligro' : '' }],
  });
  return r === true;
}

/** Pide un texto (p. ej. motivo). Devuelve null si cancela. */
export async function pedirTexto(titulo, { etiqueta = 'Motivo', obligatorio = true, max = 200, texto = 'Aceptar', ayuda } = {}) {
  const input = h('textarea', { maxLength: max, required: obligatorio, rows: 3 });
  const cuerpo = h('div', {}, ayuda ? h('p', {}, ayuda) : null, h('div', { class: 'campo' }, h('label', { class: obligatorio ? 'obligatorio' : '' }, etiqueta), input));
  return modal({
    titulo, cuerpo,
    botones: [{ texto: 'Cancelar', valor: null, clase: 'secundario' }, {
      texto, accion: () => {
        if (obligatorio && !input.value.trim()) { toast('Completá el dato solicitado', 'advertencia'); return false; }
        return input.value.trim();
      },
    }],
  });
}

// ------------------------------------------------------------------ formularios

/** Ícono "?" con texto de ayuda contextual (ERS 3.8). */
export function ayuda(texto) {
  return h('button', { type: 'button', class: 'ayuda', 'aria-label': `Ayuda: ${texto}` }, '?', h('span', { class: 'texto-ayuda', role: 'tooltip' }, texto));
}

/**
 * Crea un campo de formulario.
 * tipo: text|number|date|time|email|tel|password|select|textarea|checkbox|datetime-local
 */
export function campo({ nombre, etiqueta, tipo = 'text', valor, requerido = false, ayuda: textoAyuda, opciones, ancho = false, ...attrs }) {
  const id = `c-${nombre}-${Math.random().toString(36).slice(2, 7)}`;
  let control;
  if (tipo === 'select') {
    control = h('select', { id, name: nombre, required: requerido, ...attrs },
      (opciones || []).map(o => h('option', { value: o.valor ?? o.value ?? o, selected: String(o.valor ?? o.value ?? o) === String(valor ?? '') }, o.texto ?? o.label ?? o)));
  } else if (tipo === 'textarea') {
    control = h('textarea', { id, name: nombre, required: requerido, ...attrs });
    control.value = valor ?? '';
  } else if (tipo === 'checkbox') {
    control = h('input', { id, name: nombre, type: 'checkbox', checked: !!valor, ...attrs });
    return h('div', { class: `campo ${ancho ? 'ancho' : ''}` }, h('label', { class: 'check', for: id }, control, etiqueta, textoAyuda ? ayuda(textoAyuda) : null),
      h('span', { class: 'error' }));
  } else {
    control = h('input', { id, name: nombre, type: tipo, required: requerido, ...attrs });
    if (valor !== undefined && valor !== null) control.value = valor;
  }
  const extra = [];
  if ((tipo === 'textarea' || tipo === 'text') && attrs.maxLength) {
    const cont = h('div', { class: 'contador-caracteres' });
    const act = () => { cont.textContent = `${control.value.length}/${attrs.maxLength}`; };
    control.addEventListener('input', act);
    act();
    extra.push(cont);
  }
  return h('div', { class: `campo ${ancho ? 'ancho' : ''}` },
    h('label', { for: id, class: requerido ? 'obligatorio' : '' }, etiqueta, textoAyuda ? ayuda(textoAyuda) : null),
    control, extra, h('span', { class: 'error', 'aria-live': 'polite' }));
}

/** Lee un formulario a objeto: números → Number, vacíos → null, checkbox → boolean. */
export function leerForm(form) {
  const o = {};
  for (const el of form.elements) {
    if (!el.name || el.disabled) continue;
    if (el.type === 'checkbox') o[el.name] = el.checked;
    else if (el.type === 'number') o[el.name] = el.value === '' ? null : Number(el.value);
    else o[el.name] = el.value.trim() === '' ? null : el.value.trim();
  }
  return o;
}

export function limpiarErrores(form) {
  form.querySelectorAll('.campo.invalido').forEach(c => c.classList.remove('invalido'));
  form.querySelectorAll('.campo .error').forEach(e => { e.textContent = ''; });
}

export function mostrarErrores(form, errores) {
  limpiarErrores(form);
  let primero = null;
  for (const e of errores || []) {
    const el = form.querySelector(`[name="${CSS.escape(e.campo)}"]`);
    if (!el) continue;
    const c = el.closest('.campo');
    c?.classList.add('invalido');
    const span = c?.querySelector('.error');
    if (span) span.textContent = e.mensaje;
    primero = primero || el;
  }
  primero?.focus();
}

/** Valida en cliente con un mapa {campo: fn(valor, datos) → mensaje|null}. Devuelve true si es válido. */
export function validarForm(form, reglas) {
  const datos = leerForm(form);
  const errores = [];
  for (const [nombre, fns] of Object.entries(reglas)) {
    for (const fn of [fns].flat()) {
      const m = fn(datos[nombre], datos);
      if (m) { errores.push({ campo: nombre, mensaje: m }); break; }
    }
  }
  mostrarErrores(form, errores);
  if (errores.length) toast('Revisá los datos marcados', 'advertencia');
  return errores.length === 0;
}

// ------------------------------------------------------------------ tablas

/**
 * columnas: [{ titulo, campo, render(fila) → nodo|texto, clase, orden }]
 */
export function tabla(columnas, filas, { vacio = 'Sin resultados', claseFila } = {}) {
  if (!filas || filas.length === 0) return h('div', { class: 'vacio' }, vacio);
  return h('div', { class: 'tabla-scroll' }, h('table', {},
    h('thead', {}, h('tr', {}, columnas.map(c => h('th', { scope: 'col' }, c.titulo)))),
    h('tbody', {}, filas.map(f => h('tr', { class: claseFila ? claseFila(f) : '' },
      columnas.map(c => h('td', { class: c.clase || '' }, c.render ? c.render(f) : f[c.campo])))))));
}

export function paginador(pagina, alCambiar) {
  if (!pagina || pagina.totalPages <= 1) return h('div', { class: 'paginacion' }, pagina ? `${pagina.totalElements} resultado(s)` : '');
  return h('div', { class: 'paginacion' },
    h('button', { class: 'btn chico secundario', disabled: pagina.page === 0, onclick: () => alCambiar(pagina.page - 1) }, '‹ Anterior'),
    `Página ${pagina.page + 1} de ${pagina.totalPages} · ${pagina.totalElements} resultado(s)`,
    h('button', { class: 'btn chico secundario', disabled: pagina.page + 1 >= pagina.totalPages, onclick: () => alCambiar(pagina.page + 1) }, 'Siguiente ›'));
}

export function chip(texto, clase = '') {
  return h('span', { class: `chip ${clase}` }, texto);
}

export function param(nombre) {
  return new URLSearchParams(location.search).get(nombre);
}

export function cargando(el, texto = 'Cargando…') {
  vaciar(el, h('div', { class: 'vacio' }, texto));
}

/** Debounce para búsquedas. */
export function demorar(fn, ms = 300) {
  let t;
  return (...args) => { clearTimeout(t); t = setTimeout(() => fn(...args), ms); };
}
