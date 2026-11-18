// Guion 3: el nutricionista genera un plan automático, lo ajusta y lo confirma; el paciente lo ve, marca comidas,
// aplica un reemplazo, comenta, registra hambre/saciedad y descarga el PDF.
const { test, expect } = require('@playwright/test');
const u = require('./utilidades');

test('Guion 3 · Plan alimentario automático y uso por el paciente', async ({ page }, testInfo) => {
  const { profesional: pr, paciente: pa } = u.datos(testInfo);
  const { pacienteId } = u.leerEstado(testInfo);

  await test.step('Nutricionista genera el borrador automático', async () => {
    await u.ingresar(page, pr.dni, u.PASSWORD_E2E);
    await page.goto(`/profesional/plan-editor.html?pacienteId=${pacienteId}&modo=generar`);
    const generador = page.getByRole('dialog', { name: 'Generar plan automáticamente' });
    await expect(generador).toBeVisible();
    await u.capturar(page, testInfo, '03-nutricionista-generador');
    await generador.getByRole('button', { name: 'Generar borrador' }).click();
    await u.toast(page, 'Borrador generado');
    await expect(page.locator('td.celda-plan .kcal').first()).toBeVisible();
  });

  await test.step('Ajusta una celda y confirma el plan', async () => {
    await page.locator('td.celda-plan').first().click();
    const celda = page.getByRole('dialog');
    await celda.locator('textarea').fill('Infusión con leche descremada + 2 tostadas integrales con queso untable + 1 fruta de estación');
    await celda.getByRole('button', { name: 'Aceptar' }).click();
    await expect(page.locator('td.celda-plan').first()).toContainText('tostadas integrales');
    await u.capturar(page, testInfo, '03-nutricionista-plan-borrador');
    await page.getByRole('button', { name: 'Confirmar plan' }).click();
    await page.waitForURL(/plan-editor\.html\?id=\d+/);
    await expect(page.getByText('ACTIVO').first()).toBeVisible();
  });

  await test.step('El paciente ve su plan y aplica un reemplazo', async () => {
    await u.salir(page);
    await u.ingresar(page, pa.dni, u.PASSWORD_E2E);
    await page.goto('/paciente/mi-plan.html');
    await expect(page.getByRole('heading', { level: 2 }).first()).toBeVisible();
    await u.capturar(page, testInfo, '03-paciente-mi-plan');
    // Se prueban ítems hasta encontrar uno con alternativas equivalentes del mismo grupo.
    const botones = page.getByRole('button', { name: 'Ver alternativa' });
    let aplicado = false;
    for (let i = 0; i < Math.min(await botones.count(), 8) && !aplicado; i++) {
      await botones.nth(i).click();
      const dialogo = page.getByRole('dialog');
      if (await dialogo.waitFor({ state: 'visible', timeout: 4000 }).then(() => true, () => false)) {
        await u.capturar(page, testInfo, '03-paciente-alternativas');
        await dialogo.locator('button.bloque').first().click();
        await u.toast(page, 'Reemplazo aplicado');
        aplicado = true;
      }
    }
    expect(aplicado, 'algún alimento del plan tiene alternativas').toBeTruthy();
    await expect(page.locator('.reemplazo').first()).toContainText('reemplaza a');
  });

  await test.step('Comenta el plan y descarga el PDF', async () => {
    await page.getByRole('button', { name: 'Comentar sobre el plan' }).click();
    const dialogo = page.getByRole('dialog', { name: 'Comentario sobre el plan' });
    await dialogo.locator('textarea').fill('Me cuesta la colación de la tarde, ¿puedo cambiarla por yogur?');
    await dialogo.getByRole('button', { name: 'Enviar' }).click();
    await u.toast(page, 'Comentario enviado');
    await u.descarga(page, () => page.getByRole('button', { name: 'Descargar PDF' }).click(), 'pdf');
  });

  await test.step('Marca comidas realizadas del día', async () => {
    await page.goto('/paciente/registro-dia.html');
    await page.getByLabel('La hice como estaba propuesta').first().check();
    await u.toast(page, 'Guardado');
    await expect(page.locator('.ejercicio-card.hecho').first()).toBeVisible();
    await u.capturar(page, testInfo, '03-paciente-registro-dia');
  });

  await test.step('Registra hambre y saciedad', async () => {
    await page.goto('/paciente/observaciones.html');
    await page.getByRole('radiogroup', { name: /hambre/ }).getByRole('radio').nth(2).click();
    await page.getByRole('radiogroup', { name: /saciado/ }).getByRole('radio').nth(3).click();
    await page.getByRole('radiogroup', { name: /ánimo/ }).getByRole('radio').nth(3).click();
    await page.getByRole('button', { name: 'Guardar' }).click();
    await u.toast(page, 'Observación guardada');
    await expect(page.locator('table')).toContainText('3');
    await u.capturar(page, testInfo, '03-paciente-observaciones');
  });
});
