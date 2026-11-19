// Utilidades compartidas por los guiones E2E.
const fs = require('fs');
const path = require('path');
const { expect } = require('@playwright/test');

const ADMIN = process.env.ADMIN_USERNAME || 'admin';
const PASSWORD_E2E = process.env.E2E_PASSWORD || 'E2eMgNutri2025';
const DIR_RESULTADOS = path.join(__dirname, '..', 'resultados');

/** Identificador de la corrida (6 dígitos, lo fija global-setup) para que cada ejecución use documentos nuevos. */
function corrida() {
  try { return JSON.parse(fs.readFileSync(path.join(DIR_RESULTADOS, 'corrida.json'), 'utf8')).id; } catch { return '000000'; }
}

const NOMBRES_F = ['Valeria', 'Lucía', 'Florencia', 'Agustina', 'Camila', 'Julieta', 'Romina', 'Paula', 'Carolina', 'Milagros'];
const NOMBRES_M = ['Rodrigo', 'Matías', 'Facundo', 'Gonzalo', 'Joaquín', 'Federico', 'Emiliano', 'Leandro', 'Franco', 'Ezequiel'];
const APELLIDOS = ['Ferreyra', 'Quiroga', 'Villalba', 'Cabrera', 'Godoy', 'Ledesma', 'Vega', 'Paz', 'Coronel', 'Arias', 'Ojeda', 'Bustos',
  'Maldonado', 'Figueroa', 'Mansilla', 'Correa', 'Peralta', 'Ríos', 'Toledo', 'Vera', 'Lucero', 'Navarro', 'Moreno', 'Cáceres', 'Domínguez',
  'Aguilar', 'Ponce', 'Carrizo', 'Ibáñez', 'Escobar', 'Benavídez', 'Zalazar', 'Montenegro', 'Barrios', 'Olmos', 'Insaurralde', 'Galván'];

/**
 * Datos propios de cada proyecto (escritorio / android) para que ambos corran sobre la misma base sin chocar.
 * Parecen reales (nombre + apellido doble, DNI acorde a la edad), pero son únicos por corrida: los guiones buscan las
 * filas por apellido o DNI. Mails en example.com y teléfonos 11 00… (no asignados) para no escribir a terceros.
 */
function datos(testInfo) {
  const k = testInfo.project.name === 'chrome-android' ? 2 : 1;
  const c = corrida();
  const n = Number(c);
  const persona = (r, nombres) => {
    const a = (n * 7 + r * 11 + k * 5) % APELLIDOS.length;
    let b = (Math.floor(n / APELLIDOS.length) * 13 + r * 3 + k * 17 + 1) % APELLIDOS.length;
    if (b === a) b = (b + 1) % APELLIDOS.length;
    return { nombre: nombres[(n + r * 3 + k) % nombres.length], apellido: `${APELLIDOS[a]} ${APELLIDOS[b]}` };
  };
  const conDocumento = (p, prefijo) => ({ ...p, dni: `${prefijo}${c}`, telefono: `11 00${prefijo}${c.slice(0, 4)}` });
  return {
    profesional: conDocumento(persona(0, NOMBRES_F), `2${5 + k}`),   // 26/27 M: nacida hacia 1978
    profesional2: conDocumento(persona(1, NOMBRES_M), `3${1 + k}`),  // 32/33 M: nacido hacia 1987
    paciente: conDocumento(persona(2, NOMBRES_F), `3${7 + k}`),      // 38/39 M: nacida hacia 1995
  };
}

/** Mail de prueba con el formato habitual nombre.apellido, en el dominio reservado example.com. */
function email(p) {
  const s = `${p.nombre}.${p.apellido}`.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase().replace(/\s+/g, '.');
  return `${s}.${p.dni.slice(-4)}@example.com`;
}

function archivoEstado(testInfo) {
  return path.join(DIR_RESULTADOS, `estado-${testInfo.project.name}.json`);
}

/** Estado que un guion deja para los siguientes (ids, contraseñas definitivas). */
function leerEstado(testInfo) {
  try { return JSON.parse(fs.readFileSync(archivoEstado(testInfo), 'utf8')); } catch { return {}; }
}

function guardarEstado(testInfo, cambios) {
  fs.mkdirSync(DIR_RESULTADOS, { recursive: true });
  fs.writeFileSync(archivoEstado(testInfo), JSON.stringify({ ...leerEstado(testInfo), ...cambios }, null, 2));
}

/** Captura de evidencia: resultados/capturas/<proyecto>/<nombre>.png */
async function capturar(page, testInfo, nombre) {
  const dir = path.join(DIR_RESULTADOS, 'capturas', testInfo.project.name);
  fs.mkdirSync(dir, { recursive: true });
  await page.waitForLoadState('networkidle').catch(() => {});
  await page.screenshot({ path: path.join(dir, `${nombre}.png`), fullPage: true });
}

async function ingresar(page, usuario, password) {
  await page.goto('/index.html');
  await page.locator('[name=username]').fill(usuario);
  await page.locator('[name=password]').fill(password);
  await page.getByRole('button', { name: 'INGRESAR' }).click();
  await page.waitForURL((u) => !u.pathname.endsWith('/index.html'));
}

async function salir(page) {
  await page.evaluate(() => { sessionStorage.clear(); localStorage.clear(); });
  await page.goto('/index.html');
}

/** Primer ingreso: cambio de la contraseña temporal + aceptación de T&C. */
async function primerIngreso(page, usuario, temporal, definitiva = PASSWORD_E2E) {
  await ingresar(page, usuario, temporal);
  await page.waitForURL(/cambiar-password/);
  await page.locator('[name=actual]').fill(temporal);
  await page.locator('[name=nueva]').fill(definitiva);
  await page.locator('[name=confirmacion]').fill(definitiva);
  await page.getByRole('button', { name: 'Cambiar contraseña' }).click();
  await page.waitForURL(/terminos/);
  await page.locator('#acepto').check();
  await page.getByRole('button', { name: 'Continuar' }).click();
  await page.waitForURL(/home\.html/);
}

/** En pantallas angostas los filtros vienen plegados en un <details>: se despliegan si hace falta. */
async function abrirFiltros(page) {
  const resumen = page.locator('summary', { hasText: 'Filtros' }).first();
  if (!(await resumen.waitFor({ timeout: 5000 }).then(() => true, () => false))) return;
  if (!(await resumen.evaluate((s) => s.parentElement.open))) await resumen.click();
}

async function toast(page, texto) {
  await expect(page.locator('.toast').filter({ hasText: texto }).first()).toBeVisible();
}

/** Espera la descarga disparada por `accion` y verifica nombre y tamaño. */
async function descarga(page, accion, extension) {
  const [d] = await Promise.all([page.waitForEvent('download'), accion()]);
  const nombre = d.suggestedFilename();
  expect(nombre, 'nombre de archivo sin codificación RFC 2047').not.toMatch(/^=_|=\?/);
  expect(nombre.toLowerCase()).toMatch(new RegExp(`\\.${extension}$`));
  const ruta = await d.path();
  expect(fs.statSync(ruta).size).toBeGreaterThan(500);
  return nombre;
}

/** Cliente de API autenticado, para preparar datos o verificar efectos que no se ven en pantalla. */
async function api(playwright, baseURL, usuario, password = PASSWORD_E2E) {
  const ctx = await playwright.request.newContext({ baseURL });
  const r = await ctx.post('/api/v1/auth/login', { data: { username: usuario, password } });
  expect(r.ok(), `login API de ${usuario}`).toBeTruthy();
  const { token } = await r.json();
  const headers = { Authorization: `Bearer ${token}` };
  const llamar = async (metodo, ruta, data) => {
    const resp = await ctx.fetch(`/api/v1${ruta}`, { method: metodo, headers, data });
    const texto = await resp.text();
    let cuerpo = null;
    try { cuerpo = texto ? JSON.parse(texto) : null; } catch { cuerpo = texto; }
    return { status: resp.status(), cuerpo };
  };
  return { get: (r2) => llamar('GET', r2), post: (r2, d) => llamar('POST', r2, d ?? {}), dispose: () => ctx.dispose() };
}

function hoyArgentina(deltaDias = 0) {
  const d = new Date(Date.now() - 3 * 3600e3 + deltaDias * 864e5);
  return d.toISOString().slice(0, 10);
}

module.exports = { ADMIN, PASSWORD_E2E, datos, email, leerEstado, guardarEstado, capturar, ingresar, salir, primerIngreso, abrirFiltros, toast, descarga, api, hoyArgentina };
