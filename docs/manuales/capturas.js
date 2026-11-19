// Toma las capturas del manual de usuario sobre una instancia con datos demo (perfil demo).
// Uso (desde pruebas/e2e, que tiene Playwright instalado):
//   node ../../docs/manuales/capturas.js http://localhost:8091
// Credenciales: cuentas demo con DEMO_PASSWORD (Demo2025!) y el admin con MANUAL_ADMIN_PASSWORD
// (si el admin todavía tiene la contraseña inicial, se hace su primer ingreso con esa nueva contraseña).
const path = require('path');
const fs = require('fs');
const { chromium, devices } = require(path.join(process.cwd(), 'node_modules', '@playwright/test'));

const BASE = process.argv[2] || 'http://localhost:8091';
const DEMO = process.env.DEMO_PASSWORD || 'Demo2025!';
const ADMIN_PASS = process.env.MANUAL_ADMIN_PASSWORD || 'ManualMg2025';
const SALIDA = path.join(__dirname, 'img');
const PACIENTE = { usuario: '36752000', id: 1, plan: 1, rutina: 1 }; // Ana Gómez: nutrición con Laura López, entrenamiento con Martín García
const NUTRI = '28456712';
const ENTRENADOR = '30117652';

async function token(usuario, password) {
  const r = await fetch(`${BASE}/api/v1/auth/login`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ username: usuario, password }) });
  return r.ok ? r.json() : null;
}

async function prepararAdmin() {
  if (await token('admin', ADMIN_PASS)) return;
  const inicial = await token('admin', process.env.ADMIN_PASSWORD_INICIAL || 'Admin2025!');
  if (!inicial) throw new Error('No se pudo ingresar como admin: definí MANUAL_ADMIN_PASSWORD');
  const h = { 'Content-Type': 'application/json', Authorization: `Bearer ${inicial.token}` };
  await fetch(`${BASE}/api/v1/auth/cambiar-password`, { method: 'POST', headers: h, body: JSON.stringify({ actual: process.env.ADMIN_PASSWORD_INICIAL || 'Admin2025!', nueva: ADMIN_PASS }) });
  const s = await token('admin', ADMIN_PASS);
  const h2 = { 'Content-Type': 'application/json', Authorization: `Bearer ${s.token}` };
  const t = await (await fetch(`${BASE}/api/v1/terminos/vigentes`, { headers: h2 })).json();
  await fetch(`${BASE}/api/v1/terminos/aceptar`, { method: 'POST', headers: h2, body: JSON.stringify({ version: t.version }) });
}

async function sesion(browser, dispositivo, usuario, password) {
  const ctx = await browser.newContext({ ...dispositivo, baseURL: BASE, locale: 'es-AR', timezoneId: 'America/Argentina/Buenos_Aires' });
  const page = await ctx.newPage();
  if (usuario) {
    const s = await token(usuario, password);
    if (!s) throw new Error(`No se pudo ingresar como ${usuario}`);
    await page.goto('/index.html');
    await page.evaluate((x) => { sessionStorage.setItem('mg.token', x.token); sessionStorage.setItem('mg.usuario', JSON.stringify(x.usuario)); }, s);
  }
  return { ctx, page };
}

async function capturar(page, nombre, url, accion) {
  if (url) await page.goto(url);
  await page.waitForLoadState('networkidle').catch(() => {});
  if (accion) await accion(page);
  await page.waitForTimeout(1200); // animaciones de Chart.js
  await page.screenshot({ path: path.join(SALIDA, `${nombre}.jpg`), type: 'jpeg', quality: 82 });
  console.log(nombre);
}

const ESCRITORIO = { viewport: { width: 1280, height: 800 }, deviceScaleFactor: 1 };
const CELULAR = { ...devices['Pixel 7'], deviceScaleFactor: 2 };

(async () => {
  fs.mkdirSync(SALIDA, { recursive: true });
  await prepararAdmin();
  const browser = await chromium.launch({ channel: 'chrome' });

  // ---- Acceso (sin sesión)
  let { ctx, page } = await sesion(browser, CELULAR);
  await capturar(page, 'acceso-login', '/index.html');
  await capturar(page, 'acceso-contacto', null, (p) => p.getByRole('button', { name: 'CONTACTO' }).click());
  await capturar(page, 'acceso-recupero', '/recupero.html');
  await ctx.close();

  // ---- Paciente (celular)
  ({ ctx, page } = await sesion(browser, CELULAR, PACIENTE.usuario, DEMO));
  for (const [n, u] of [['paciente-home', '/paciente/home.html'], ['paciente-nutricion', '/paciente/nutricion.html'], ['paciente-mi-plan', '/paciente/mi-plan.html'],
    ['paciente-registro-dia', '/paciente/registro-dia.html'], ['paciente-observaciones', '/paciente/observaciones.html'], ['paciente-entrenamiento', '/paciente/entrenamiento.html'],
    ['paciente-mi-rutina', '/paciente/mi-rutina.html'], ['paciente-molestia', '/paciente/molestia.html'], ['paciente-turnos', '/paciente/turnos.html'],
    ['paciente-evolucion', '/comun/evolucion.html'], ['paciente-cuenta', '/paciente/cuenta.html'], ['paciente-perfil', '/paciente/perfil.html']]) {
    await capturar(page, n, u);
  }
  await capturar(page, 'paciente-alternativas', '/paciente/mi-plan.html', async (p) => {
    await p.getByRole('button', { name: 'Ver alternativa' }).first().click();
    await p.getByRole('dialog').waitFor();
  });
  await capturar(page, 'paciente-turno-nuevo', '/paciente/turno-nuevo.html?area=NUTRICION', async (p) => {
    await p.locator('.tarjeta .btn.bloque').first().click();
    await p.getByRole('button', { name: 'Semana siguiente ›' }).click();
    await p.getByRole('button', { name: /^\d{2}:\d{2}$/ }).first().waitFor();
  });
  await capturar(page, 'paciente-menu', '/paciente/home.html', (p) => p.locator('.btn-menu').click());
  await ctx.close();

  // ---- Nutricionista (escritorio)
  ({ ctx, page } = await sesion(browser, ESCRITORIO, NUTRI, DEMO));
  for (const [n, u] of [['nutri-home', '/profesional/home.html'], ['nutri-pacientes', '/comun/pacientes.html'],
    ['nutri-ficha-datos', `/comun/paciente.html?id=${PACIENTE.id}&tab=datos`], ['nutri-ficha-planes', `/comun/paciente.html?id=${PACIENTE.id}&tab=planes`],
    ['nutri-ficha-evolucion', `/comun/paciente.html?id=${PACIENTE.id}&tab=evolucion`], ['nutri-ficha-registros', `/comun/paciente.html?id=${PACIENTE.id}&tab=registros`],
    ['nutri-plan-editor', `/profesional/plan-editor.html?id=${PACIENTE.plan}`], ['nutri-evolucion', `/comun/evolucion.html?pacienteId=${PACIENTE.id}`],
    ['nutri-calendario', '/comun/calendario.html'], ['nutri-agenda', '/profesional/agenda.html'], ['nutri-novedades', '/profesional/novedades.html'],
    ['nutri-alimentos', '/comun/alimentos.html']]) {
    await capturar(page, n, u);
  }
  await capturar(page, 'nutri-generador', `/profesional/plan-editor.html?pacienteId=${PACIENTE.id}&modo=generar`, (p) => p.getByRole('dialog').waitFor());
  await capturar(page, 'nutri-antropometria', `/comun/paciente.html?id=${PACIENTE.id}&tab=evolucion`, async (p) => {
    await p.getByRole('button', { name: 'Cargar antropometría' }).click();
    await p.getByRole('dialog').waitFor();
  });
  await ctx.close();

  // ---- Entrenador (escritorio)
  ({ ctx, page } = await sesion(browser, ESCRITORIO, ENTRENADOR, DEMO));
  for (const [n, u] of [['entrenador-home', '/profesional/home.html'], ['entrenador-ficha-rutinas', `/comun/paciente.html?id=${PACIENTE.id}&tab=rutinas`],
    ['entrenador-rutina-editor', `/profesional/rutina-editor.html?id=${PACIENTE.rutina}`], ['entrenador-ejercicios', '/comun/ejercicios.html']]) {
    await capturar(page, n, u);
  }
  await ctx.close();

  // ---- Administrador (escritorio)
  ({ ctx, page } = await sesion(browser, ESCRITORIO, 'admin', ADMIN_PASS));
  for (const [n, u] of [['admin-home', '/admin/home.html'], ['admin-pacientes', '/comun/pacientes.html'], ['admin-paciente-form', '/admin/paciente-form.html'],
    ['admin-profesionales', '/admin/profesionales.html'], ['admin-profesional-form', '/admin/profesional.html'], ['admin-calendario', '/comun/calendario.html'],
    ['admin-pagos', `/admin/pagos.html?pacienteId=${PACIENTE.id}`], ['admin-deudores', '/admin/deudores.html'], ['admin-aranceles', '/admin/aranceles.html'],
    ['admin-opciones', '/admin/opciones.html'], ['admin-bitacora', '/admin/bitacora.html'], ['admin-accesos', '/admin/accesos.html'],
    ['admin-notificaciones', '/admin/notificaciones.html'], ['admin-backups', '/admin/backups.html'], ['admin-configuracion', '/admin/configuracion.html']]) {
    await capturar(page, n, u);
  }
  await capturar(page, 'admin-reporte-turnos', '/comun/reportes.html', async (p) => {
    await p.getByRole('button', { name: /^Turnos/ }).click();
    await p.getByRole('button', { name: 'Generar' }).click();
    await p.locator('canvas').first().waitFor();
    await p.locator('canvas').first().scrollIntoViewIfNeeded();
  });
  await ctx.close();
  await browser.close();
})().catch((e) => { console.error(e); process.exit(1); });
