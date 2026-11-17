// Formatos para Argentina (hora local de Buenos Aires, moneda ARS).
const ZONA = 'America/Argentina/Buenos_Aires';
const moneda = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' });
const numero = new Intl.NumberFormat('es-AR', { maximumFractionDigits: 1 });

export const DIAS = ['', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo'];
export const DIAS_CORTOS = ['', 'L', 'M', 'M', 'J', 'V', 'S', 'D'];

export const fmt = {
  moneda: (v) => (v === null || v === undefined || v === '' ? '' : moneda.format(Number(v))),
  numero: (v) => (v === null || v === undefined || v === '' ? '' : numero.format(Number(v))),
  fecha: (iso) => {
    if (!iso) return '';
    const [a, m, d] = String(iso).substring(0, 10).split('-');
    return `${d}/${m}/${a}`;
  },
  fechaHora: (iso) => (iso ? new Date(iso).toLocaleString('es-AR', { timeZone: ZONA, dateStyle: 'short', timeStyle: 'short', hourCycle: 'h23' }) : ''),
  hora: (iso) => (iso ? new Date(iso).toLocaleTimeString('es-AR', { timeZone: ZONA, hour: '2-digit', minute: '2-digit', hourCycle: 'h23' }) : ''),
  diaFecha: (iso) => (iso ? new Date(iso).toLocaleDateString('es-AR', { timeZone: ZONA, weekday: 'long', day: '2-digit', month: '2-digit' }) : ''),
  porcentaje: (v) => (v === null || v === undefined ? '' : `${numero.format(v)} %`),
};

/** Fecha local (Buenos Aires) en formato YYYY-MM-DD. */
export function hoyISO(desplazamientoDias = 0) {
  const d = new Date(Date.now() + desplazamientoDias * 86400000);
  const partes = new Intl.DateTimeFormat('en-CA', { timeZone: ZONA, year: 'numeric', month: '2-digit', day: '2-digit' }).format(d);
  return partes;
}

export function sumarDias(iso, n) {
  const d = new Date(`${iso}T12:00:00Z`);
  d.setUTCDate(d.getUTCDate() + n);
  return d.toISOString().substring(0, 10);
}

/** Día de la semana ISO (1 = lunes) de una fecha YYYY-MM-DD. */
export function diaSemana(iso) {
  const d = new Date(`${iso}T12:00:00Z`).getUTCDay();
  return d === 0 ? 7 : d;
}

/** Fecha-hora local Argentina → ISO con offset -03:00 (Argentina no usa horario de verano). */
export function aIsoArgentina(fechaISO, hhmm) {
  return `${fechaISO}T${hhmm}:00-03:00`;
}

export function fechaLocalDeIso(iso) {
  return new Intl.DateTimeFormat('en-CA', { timeZone: ZONA, year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date(iso));
}

export function etiquetaEstado(e) {
  return ({ RESERVADO: 'Reservado', ATENDIDO: 'Atendido', AUSENTE: 'Ausente', CANCELADO: 'Cancelado', ACTIVO: 'Activo', ACTIVA: 'Activa',
    BORRADOR: 'Borrador', FINALIZADO: 'Finalizado', FINALIZADA: 'Finalizada', ELIMINADO: 'Eliminado', ELIMINADA: 'Eliminada' })[e] || e;
}
