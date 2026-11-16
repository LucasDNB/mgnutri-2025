// Sesión del usuario: token en sessionStorage (se borra al cerrar la pestaña, spec 13).
const CLAVE_TOKEN = 'mg.token';
const CLAVE_USUARIO = 'mg.usuario';

export function guardarSesion(login) {
  sessionStorage.setItem(CLAVE_TOKEN, login.token);
  sessionStorage.setItem(CLAVE_USUARIO, JSON.stringify(login.usuario));
}

export function actualizarUsuario(usuario) {
  sessionStorage.setItem(CLAVE_USUARIO, JSON.stringify(usuario));
}

export function token() {
  return sessionStorage.getItem(CLAVE_TOKEN);
}

export function usuarioActual() {
  try {
    return JSON.parse(sessionStorage.getItem(CLAVE_USUARIO) || 'null');
  } catch {
    return null;
  }
}

export function tieneRol(...roles) {
  const u = usuarioActual();
  return !!u && roles.some(r => u.roles.includes(r));
}

export function limpiarSesion() {
  sessionStorage.removeItem(CLAVE_TOKEN);
  sessionStorage.removeItem(CLAVE_USUARIO);
}

/** Página a la que debe ir el usuario según su estado (primer ingreso, T&C, home). */
export function destinoInicial(u) {
  if (!u) return '/index.html';
  if (u.debeCambiarPassword) return '/cambiar-password.html?primer=1';
  if (u.debeAceptarTerminos) return '/terminos.html';
  return u.home;
}
