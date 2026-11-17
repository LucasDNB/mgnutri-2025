// Guion 4: el entrenador arma la rutina; el paciente registra la sesión con cargas, pide una variante, accede al video
// y reporta una molestia de intensidad 8 → el entrenador la ve como novedad prioritaria.
const { test, expect } = require('@playwright/test');
const u = require('./utilidades');

const NOMBRES_DIA = ['', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo'];

test('Guion 4 · Rutina de entrenamiento, registro de cargas y molestia prioritaria', async ({ page }, testInfo) => {
  const { profesional: pr, paciente: pa } = u.datos(testInfo);
  const { pacienteId } = u.leerEstado(testInfo);
  const diaHoy = ((new Date(`${u.hoyArgentina()}T12:00:00Z`).getUTCDay() + 6) % 7) + 1; // 1 = lunes … 7 = domingo

  await test.step('Entrenador arma la rutina (incluye el día de hoy)', async () => {
    await u.ingresar(page, pr.dni, u.PASSWORD_E2E);
    await page.goto(`/profesional/rutina-editor.html?pacienteId=${pacienteId}`);
    await page.locator('[name=nombre]').fill('Fuerza general');
    await page.locator('[name=objetivo]').fill('Ganar fuerza y mejorar la técnica');
    await page.getByRole('button', { name: '+ Agregar día' }).click();
    const dia = page.getByRole('dialog', { name: 'Agregar día' });
    await dia.locator('select').selectOption(String(diaHoy));
    await dia.getByRole('button', { name: 'Agregar' }).click();
    await page.getByRole('button', { name: '+ Agregar ejercicio' }).click();
    const buscador = page.getByRole('dialog', { name: new RegExp(`Agregar ejercicio al ${NOMBRES_DIA[diaHoy]}`) });
    await expect(buscador.locator('.columna button').first()).toBeVisible();
    await buscador.locator('.columna button').filter({ hasText: '🎬' }).first().click();
    await buscador.locator('.columna button').filter({ hasNotText: '🎬' }).first().click();
    await buscador.getByRole('button', { name: 'Listo' }).click();
    await expect(page.locator('.ejercicio-card')).toHaveCount(2);
    await u.capturar(page, testInfo, '04-entrenador-rutina');
    await page.getByRole('button', { name: 'Confirmar rutina' }).click();
    await page.waitForURL(/tab=rutinas/);
  });

  await test.step('El paciente registra la sesión con cargas', async () => {
    await u.salir(page);
    await u.ingresar(page, pa.dni, u.PASSWORD_E2E);
    await page.goto('/paciente/mi-rutina.html');
    const tarjeta = page.locator('.ejercicio-card').first();
    await tarjeta.getByPlaceholder('kg').fill('40');
    await tarjeta.getByPlaceholder('kg').blur();
    await u.toast(page, 'Guardado');
    await tarjeta.getByPlaceholder('series').fill('3');
    await tarjeta.getByPlaceholder('series').blur();
    await tarjeta.getByPlaceholder('reps').fill('10');
    await tarjeta.getByPlaceholder('reps').blur();
    await tarjeta.locator('input[type=checkbox]').check();
    await expect(tarjeta).toHaveClass(/hecho/);
    await u.capturar(page, testInfo, '04-paciente-mi-rutina');
  });

  await test.step('Accede al video del ejercicio', async () => {
    const video = page.getByRole('link', { name: '🎬 Ver video' }).first();
    await expect(video).toHaveAttribute('href', /^https?:\/\//);
    await expect(video).toHaveAttribute('target', '_blank');
  });

  await test.step('Pide una variante del segundo ejercicio', async () => {
    await page.locator('.ejercicio-card').nth(1).getByRole('button', { name: 'Pedir variante' }).click();
    const dialogo = page.getByRole('dialog');
    await dialogo.locator('button.bloque').first().click();
    await u.toast(page, 'Variante aplicada');
    await expect(page.locator('.reemplazo').first()).toContainText('variante de');
  });

  await test.step('Reporta una molestia de intensidad 8', async () => {
    await page.locator('.ejercicio-card').first().getByRole('link', { name: 'Molestia' }).click();
    await page.waitForURL(/molestia\.html/);
    await page.locator('textarea').fill('Dolor en la rodilla derecha al bajar en la sentadilla');
    await page.getByRole('radiogroup', { name: 'Intensidad' }).getByRole('radio', { name: '8', exact: true }).click();
    await page.locator('select').first().selectOption('Rodilla der.');
    await u.capturar(page, testInfo, '04-paciente-molestia');
    await page.getByRole('button', { name: 'Enviar a mi profesional' }).click();
    await u.toast(page, 'Tu profesional fue avisado');
    await expect(page.locator('table')).toContainText('8/10');
  });

  await test.step('El entrenador ve la novedad prioritaria', async () => {
    await u.salir(page);
    await u.ingresar(page, pr.dni, u.PASSWORD_E2E);
    await page.goto('/profesional/novedades.html');
    const fila = page.locator('tr').filter({ hasText: pa.apellido }).filter({ hasText: 'Prioritaria' });
    await expect(fila.first()).toBeVisible();
    await u.capturar(page, testInfo, '04-entrenador-novedades');
  });
});
