// Guion 7: el admin consulta la bitácora y los accesos de todo lo anterior, genera los 5 reportes en PDF y Excel,
// genera y descarga un backup y programa el backup semanal.
const { test, expect } = require('@playwright/test');
const u = require('./utilidades');

const REPORTES = ['Turnos', 'Pagos y deudas', 'Evolución del paciente', 'Rutinas', 'Planes alimentarios'];

test('Guion 7 · Bitácora, accesos, reportes y backup', async ({ page }, testInfo) => {
  test.setTimeout(15 * 60_000); // en Azure el backup es una exportación BACPAC de ~5 minutos
  const { paciente: pa, profesional: pr } = u.datos(testInfo);

  await test.step('Bitácora: altas y modificaciones de los guiones anteriores', async () => {
    await u.ingresar(page, u.ADMIN, u.PASSWORD_E2E);
    await page.goto('/admin/bitacora.html');
    await u.abrirFiltros(page);
    await page.getByRole('button', { name: 'Aplicar filtros' }).click();
    await expect(page.locator('tbody tr').first()).toBeVisible();
    // Con datos reales la primera página puede llenarse de otros eventos (p. ej. los recordatorios de deuda del job):
    // se filtra como lo haría el administrador para encontrar el alta del paciente del guion 2.
    await page.locator('[name=modulo]').selectOption('PACIENTES');
    await page.locator('[name=tipo]').selectOption('ALTA');
    await page.getByRole('button', { name: 'Aplicar filtros' }).click();
    await expect(page.locator('tbody')).toContainText(`Alta de paciente ${pa.apellido}, ${pa.nombre}`);
    await page.locator('[name=modulo]').selectOption('');
    await page.locator('[name=tipo]').selectOption('');
    await page.locator('[name=usuario]').fill(pr.dni);
    await page.getByRole('button', { name: 'Aplicar filtros' }).click();
    await expect(page.locator('tbody')).toContainText(pr.dni);
    await u.capturar(page, testInfo, '07-admin-bitacora');
    await u.descarga(page, () => page.getByRole('button', { name: 'Exportar a Excel' }).click(), 'xlsx');
  });

  await test.step('Registro de accesos del paciente', async () => {
    await page.goto('/admin/accesos.html');
    await u.abrirFiltros(page);
    await page.locator('[name=usuario]').fill(pa.dni);
    await page.getByRole('button', { name: 'Aplicar filtros' }).click();
    await expect(page.locator('tbody')).toContainText('login ok');
    await u.capturar(page, testInfo, '07-admin-accesos');
  });

  for (const titulo of REPORTES) {
    await test.step(`Reporte «${titulo}» en pantalla, PDF y Excel`, async () => {
      await page.goto('/comun/reportes.html');
      await page.getByRole('button', { name: new RegExp(`^${titulo}`) }).click();
      const form = page.locator('form.tarjeta').filter({ has: page.getByRole('heading', { name: titulo }) });
      await expect(form).toBeVisible();
      if (titulo === 'Evolución del paciente' || titulo === 'Planes alimentarios' || titulo === 'Rutinas') {
        await form.locator('input[type=search]').fill(pa.apellido);
        await form.getByRole('button', { name: new RegExp(pa.apellido) }).click();
      }
      await form.getByRole('button', { name: 'Generar' }).click();
      await expect(page.locator('main')).not.toContainText('Elegí un paciente');
      if (titulo === 'Turnos') await u.capturar(page, testInfo, '07-admin-reporte-turnos');
      await u.descarga(page, () => form.getByRole('button', { name: 'Descargar PDF' }).click(), 'pdf');
      await u.descarga(page, () => form.getByRole('button', { name: 'Descargar Excel' }).click(), 'xlsx');
    });
  }

  await test.step('Genera y descarga un backup', async () => {
    await page.goto('/admin/backups.html');
    await page.getByRole('button', { name: 'Generar backup' }).click();
    await page.getByRole('dialog', { name: 'Confirmar' }).getByRole('button', { name: 'Generar backup' }).click();
    await u.toast(page, 'Backup iniciado');
    const fila = page.locator('tbody tr').first();
    await expect(async () => {
      await page.reload();
      await expect(page.locator('tbody tr').first()).toContainText('OK', { timeout: 2000 });
    }).toPass({ timeout: 10 * 60_000, intervals: [5_000, 10_000, 15_000] });
    await u.capturar(page, testInfo, '07-admin-backups');
    await u.descarga(page, () => fila.getByRole('button', { name: /Descargar/ }).click(), '(bak|bacpac)');
  });

  await test.step('Programa el backup semanal', async () => {
    await page.locator('[name=habilitado]').check();
    await page.locator('[name=frecuencia]').selectOption('SEMANAL');
    await page.locator('[name=diaSemana]').selectOption('7');
    await page.locator('[name=hora]').fill('03:00');
    await page.getByRole('button', { name: 'Guardar programación' }).click();
    await u.toast(page, 'Programación');
  });
});
