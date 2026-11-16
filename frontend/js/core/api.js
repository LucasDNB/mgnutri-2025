// Cliente HTTP: agrega el token, maneja Problem Details (RFC 7807), 401 y 428.
import { API_BASE } from '../config.js';
import { token, limpiarSesion } from './auth.js';

export class ApiError extends Error {
  constructor(status, problema) {
    super(problema?.detail || 'Ocurrió un error inesperado.');
    this.status = status;
    this.code = problema?.code;
    this.errors = problema?.errors || [];
    this.data = problema || {};
  }
}

async function solicitud(metodo, ruta, cuerpo, opciones = {}) {
  const headers = { Accept: 'application/json, application/problem+json, */*' };
  const t = token();
  if (t) headers.Authorization = `Bearer ${t}`;
  const init = { method: metodo, headers };
  if (cuerpo !== undefined) {
    headers['Content-Type'] = 'application/json';
    init.body = JSON.stringify(cuerpo);
  }
  let resp;
  try {
    resp = await fetch(API_BASE + ruta, init);
  } catch {
    throw new ApiError(0, { detail: 'No hay conexión con el servidor. Revisá tu conexión e intentá de nuevo.', code: 'SIN_CONEXION' });
  }
  if (resp.status === 401 && !ruta.startsWith('/auth/login')) {
    limpiarSesion();
    sessionStorage.setItem('mg.mensaje', 'Tu sesión expiró. Volvé a iniciar sesión.');
    location.href = '/index.html';
    throw new ApiError(401, { detail: 'Sesión expirada' });
  }
  if (resp.status === 428) {
    const p = await resp.json().catch(() => ({}));
    location.href = p.code === 'DEBE_CAMBIAR_PASSWORD' ? '/cambiar-password.html?primer=1' : '/terminos.html';
    throw new ApiError(428, p);
  }
  if (!resp.ok) {
    const p = await resp.json().catch(() => ({ detail: `Error ${resp.status}` }));
    throw new ApiError(resp.status, p);
  }
  if (opciones.blob) return resp;
  if (resp.status === 204) return null;
  const tipo = resp.headers.get('content-type') || '';
  return tipo.includes('json') ? resp.json() : resp.text();
}

export const api = {
  get: (ruta) => solicitud('GET', ruta),
  post: (ruta, cuerpo) => solicitud('POST', ruta, cuerpo ?? {}),
  put: (ruta, cuerpo) => solicitud('PUT', ruta, cuerpo ?? {}),
  patch: (ruta, cuerpo) => solicitud('PATCH', ruta, cuerpo ?? {}),
  del: (ruta, cuerpo) => solicitud('DELETE', ruta, cuerpo),
};

/** Descarga un archivo con el token (fetch → Blob → enlace temporal). */
export async function descargar(ruta, nombrePorDefecto = 'archivo') {
  const resp = await solicitud('GET', ruta, undefined, { blob: true });
  const tipo = resp.headers.get('content-type') || '';
  if (tipo.includes('json')) {
    const datos = await resp.json();
    if (datos.url) { window.open(datos.url, '_blank', 'noopener'); return; }
  }
  const disp = resp.headers.get('content-disposition') || '';
  // Se prioriza filename* (RFC 5987) y se ignora un filename codificado en RFC 2047 (=?UTF-8?Q?...?=)
  const extendido = /filename\*\s*=\s*UTF-8''([^;]+)/i.exec(disp);
  const simple = /filename\s*=\s*"?([^";]+)"?/i.exec(disp);
  let nombre = nombrePorDefecto;
  if (extendido) nombre = decodeURIComponent(extendido[1].trim());
  else if (simple && !simple[1].startsWith('=?')) nombre = simple[1].trim();
  const blob = await resp.blob();
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = nombre;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 5000);
}

/** Construye un query string ignorando valores vacíos. */
export function qs(params) {
  const p = new URLSearchParams();
  Object.entries(params || {}).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') p.append(k, v);
  });
  const s = p.toString();
  return s ? `?${s}` : '';
}
