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

/** Datos propios de cada proyecto (escritorio / android) para que ambos corran sobre la misma base sin chocar. */
function datos(testInfo) {
  const k = testInfo.project.name === 'chrome-android' ? 2 : 1;
  const c = corrida();
  // Los apellidos sólo admiten letras: la corrida se codifica como letras (0→A … 9→J).
  const letras = c.replace(/\d/g, (d) => 'ABCDEFGHIJ'[d]);
  const sufijo = `${k === 1 ? 'Escritorio' : 'Movil'} ${letras[0]}${letras.slice(1).toLowerCase()}`;
  return {
    profesional: { nombre: 'Valeria', apellido: `Integral ${sufijo}`, dni: `3${k}${c}`, telefono: `11 3${k}${c}` },
    profesional2: { nombre: 'Rodrigo', apellido: `Baja ${sufijo}`, dni: `4${k}${c}`, telefono: `11 4${k}${c}` },
    paciente: { nombre: 'Paula', apellido: `Prueba ${sufijo}`, dni: `5${k}${c}`, telefono: `11 5${k}${c}` },
  };
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

module.exports = { ADMIN, PASSWORD_E2E, datos, leerEstado, guardarEstado, capturar, ingresar, salir, primerIngreso, toast, descarga, api, hoyArgentina };
