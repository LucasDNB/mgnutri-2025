// Guiones E2E mínimos (spec 16) sobre Chrome de escritorio y Chrome Android (emulado).
// Requiere el backend sirviendo el frontend (FRONTEND_PATH) sobre una base vacía con perfil "local":
//   E2E_URL=http://localhost:8092 npx playwright test
// Usa el Chrome instalado en la máquina (channel: 'chrome'); no descarga navegadores.
const { defineConfig, devices } = require('@playwright/test');

module.exports = defineConfig({
  testDir: './tests',
  outputDir: './resultados/artefactos',
  fullyParallel: false,
  workers: 1,
  retries: 0,
  timeout: 120_000,
  expect: { timeout: 15_000 },
  reporter: [['list'], ['html', { outputFolder: 'resultados/reporte-html', open: 'never' }], ['json', { outputFile: 'resultados/resultados.json' }]],
  globalSetup: require.resolve('./global-setup.js'),
  use: {
    baseURL: process.env.E2E_URL || 'http://localhost:8092',
    channel: 'chrome',
    locale: 'es-AR',
    timezoneId: 'America/Argentina/Buenos_Aires',
    acceptDownloads: true,
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
  },
  projects: [
    { name: 'chrome-escritorio', use: { ...devices['Desktop Chrome'], channel: 'chrome', viewport: { width: 1366, height: 860 } } },
    // Pixel 7 = Chrome Android emulado (viewport, user agent, touch). Corre después del de escritorio, con sus propios datos.
    { name: 'chrome-android', use: { ...devices['Pixel 7'], channel: 'chrome' }, dependencies: ['chrome-escritorio'] },
  ],
});
