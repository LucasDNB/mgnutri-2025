// Guion 5: el nutricionista carga 3 antropometrías → paciente y profesional ven los gráficos y descargan el reporte de evolución.
const { test, expect } = require('@playwright/test');
const u = require('./utilidades');

const MEDICIONES = [
  { dias: -60, peso: '82.4', grasa: '31.5', musculo: '33.0', cintura: '94' },
  { dias: -30, peso: '80.1', grasa: '30.2', musculo: '33.6', cintura: '91' },
  { dias: 0, peso: '78.6', grasa: '29.1', musculo: '34.1', cintura: '89' },
];

test('Guion 5 · Antropometría, gráficos y reporte de evolución', async ({ page }, testInfo) => {
  const { profesional: pr, paciente: pa } = u.datos(testInfo);
  const { pacienteId } = u.leerEstado(testInfo);

  await test.step('El nutricionista carga 3 mediciones', async () => {
    await u.ingresar(page, pr.dni, u.PASSWORD_E2E);
    await page.goto(`/comun/paciente.html?id=${pacienteId}&tab=evolucion`);
    for (const m of MEDICIONES) {
      await page.getByRole('button', { name: 'Cargar antropometría' }).click();
      const d = page.getByRole('dialog', { name: 'Cargar antropometría' });
      await d.locator('[name=fecha]').fill(u.hoyArgentina(m.dias));
      await d.locator('[name=pesoKg]').fill(m.peso);
      await d.locator('[name=tallaCm]').fill('168');
      await d.locator('[name=pctGrasa]').fill(m.grasa);
      await d.locator('[name=pctMusculo]').fill(m.musculo);
      await d.locator('[name=circCintura]').fill(m.cintura);
      await expect(d.getByText('IMC calculado:')).toContainText(/\d+\.\d/);
      if (m.dias === 0) await u.capturar(page, testInfo, '05-nutricionista-antropometria');
      await d.getByRole('button', { name: 'Guardar' }).click();
      await u.toast(page, 'Medición guardada');
      await expect(d).toBeHidden();
    }
    await expect(page.locator('table').first().locator('tbody tr')).toHaveCount(3);
  });

  await test.step('El profesional ve los gráficos y descarga el reporte', async () => {
    await page.getByRole('link', { name: 'Ver gráficos de evolución' }).click();
    await page.waitForURL(/evolucion\.html/);
    await expect(page.locator('canvas').first()).toBeVisible();
    await u.capturar(page, testInfo, '05-profesional-evolucion');
    await u.descarga(page, () => page.getByRole('button', { name: 'Descargar PDF' }).click(), 'pdf');
  });

  await test.step('El paciente ve sus gráficos y descarga el reporte', async () => {
    await u.salir(page);
    await u.ingresar(page, pa.dni, u.PASSWORD_E2E);
    await page.goto('/comun/evolucion.html');
    await expect(page.locator('canvas').first()).toBeVisible();
    await expect(page.locator('table')).toContainText('78,6');
    await u.capturar(page, testInfo, '05-paciente-evolucion');
    await u.descarga(page, () => page.getByRole('button', { name: 'Descargar PDF' }).click(), 'pdf');
  });
});
