// Primer ingreso del administrador inicial: reemplaza la contraseña temporal y acepta los T&C (por API, una sola vez).
const { request } = require('@playwright/test');
const { ADMIN, PASSWORD_E2E } = require('./tests/utilidades');

module.exports = async (config) => {
  const fs = require('fs');
  const path = require('path');
  const dir = path.join(__dirname, 'resultados');
  fs.mkdirSync(dir, { recursive: true });
  for (const f of fs.readdirSync(dir).filter((x) => x.startsWith('estado-'))) fs.rmSync(path.join(dir, f));
  fs.writeFileSync(path.join(dir, 'corrida.json'), JSON.stringify({ id: String(Math.floor(Date.now() / 1000) % 1e6).padStart(6, '0'), inicio: new Date().toISOString() }));
  const baseURL = config.projects[0].use.baseURL;
  const api = await request.newContext({ baseURL });
  const login = (password) => api.post('/api/v1/auth/login', { data: { username: ADMIN, password } });
  let r = await login(PASSWORD_E2E);
  if (!r.ok()) {
    r = await login(process.env.ADMIN_PASSWORD_INICIAL || 'Admin2025!');
    if (!r.ok()) throw new Error(`No se pudo ingresar como ${ADMIN}: ${r.status()} ${await r.text()}`);
    const { token } = await r.json();
    const c = await api.post('/api/v1/auth/cambiar-password', { headers: { Authorization: `Bearer ${token}` },
      data: { actual: process.env.ADMIN_PASSWORD_INICIAL || 'Admin2025!', nueva: PASSWORD_E2E } });
    if (!c.ok()) throw new Error(`Cambio de contraseña del admin: ${c.status()} ${await c.text()}`);
    r = await login(PASSWORD_E2E);
  }
  const { token } = await r.json();
  const t = await (await api.get('/api/v1/terminos/vigentes', { headers: { Authorization: `Bearer ${token}` } })).json();
  await api.post('/api/v1/terminos/aceptar', { headers: { Authorization: `Bearer ${token}` }, data: { version: t.version } });
  await api.dispose();
};
