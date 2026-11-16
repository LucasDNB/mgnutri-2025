// Validaciones de cliente, espejo de las del servidor (SDP §4.1). El servidor valida siempre.

/** Normaliza un teléfono argentino a E.164 (+549…) o devuelve null. */
export function telefonoAR(entrada) {
  if (!entrada) return null;
  let d = String(entrada).replace(/\D/g, '');
  if (d.startsWith('549') && d.length === 13) return `+${d}`;
  if (d.startsWith('54') && d.length === 12) return `+549${d.substring(2)}`;
  if (d.startsWith('0')) d = d.substring(1);
  if (d.length === 12) {
    for (let area = 2; area <= 4; area++) {
      if (d.substring(area, area + 2) === '15') {
        const sin15 = d.substring(0, area) + d.substring(area + 2);
        if (sin15.length === 10) return `+549${sin15}`;
      }
    }
  }
  if (d.length === 10) return `+549${d}`;
  return null;
}

export const validar = {
  requerido: (v) => (v === null || v === undefined || String(v).trim() === '' ? 'Este dato es obligatorio' : null),
  documento: (v) => (/^[A-Za-z0-9]{6,20}$/.test(v || '') ? null : 'Documento inválido (6 a 20 letras o números)'),
  nombre: (v) => (/^[\p{L} '\-]{2,80}$/u.test(v || '') ? null : 'Entre 2 y 80 letras'),
  email: (v) => (!v || /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v) ? null : 'Email inválido'),
  telefono: (v) => (telefonoAR(v) ? null : 'Ingresá código de área y número, por ejemplo 11 3456-7890'),
  fechaNacimiento: (v) => {
    if (!v) return 'Este dato es obligatorio';
    const edad = (Date.now() - new Date(v).getTime()) / (365.25 * 86400000);
    return edad >= 5 && edad <= 110 ? null : 'La edad debe estar entre 5 y 110 años';
  },
  password: (v, documento) => {
    if (!v || v.length < 8) return 'Mínimo 8 caracteres';
    if (!/[A-Za-zÁÉÍÓÚáéíóúÑñ]/.test(v) || !/\d/.test(v)) return 'Debe incluir al menos una letra y un número';
    if (documento && v.includes(documento)) return 'No puede contener tu documento';
    return null;
  },
  rango: (min, max) => (v) => (v === '' || v === null || v === undefined || (Number(v) >= min && Number(v) <= max) ? null : `Debe estar entre ${min} y ${max}`),
};

/** Fortaleza de contraseña 0..4 para el medidor visual. */
export function fortaleza(p) {
  let n = 0;
  if ((p || '').length >= 8) n++;
  if (/[a-z]/.test(p) && /[A-Z]/.test(p)) n++;
  if (/\d/.test(p)) n++;
  if (/[^A-Za-z0-9]/.test(p) || (p || '').length >= 12) n++;
  return n;
}
