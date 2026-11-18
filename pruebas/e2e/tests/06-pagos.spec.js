// Guion 6: el admin registra deudas y pagos, imprime el comprobante, lista deudores, dispara el recordatorio de deuda
// y ejecuta manualmente el job de recordatorio de turnos.
const { test, expect } = require('@playwright/test');
const u = require('./utilidades');

test('Guion 6 · Cuenta corriente, comprobante, deudores y recordatorios', async ({ page }, testInfo) => {
  const { paciente: pa } = u.datos(testInfo);
  const { pacienteId } = u.leerEstado(testInfo);

  await test.step('Registra dos deudas', async () => {
    await u.ingresar(page, u.ADMIN, u.PASSWORD_E2E);
    await page.goto(`/admin/pagos.html?pacienteId=${pacienteId}`);
    for (const [concepto, monto, vencimiento] of [['Cuota mensual', '25000', u.hoyArgentina(-15)], ['Evaluación inicial', '18000', u.hoyArgentina(10)]]) {
      await page.getByRole('button', { name: '+ Registrar deuda' }).click();
      const d = page.getByRole('dialog', { name: 'Registrar deuda' });
      await d.locator('[name=concepto]').fill(concepto);
      await d.locator('[name=monto]').fill(monto);
      await d.locator('[name=fecha]').fill(u.hoyArgentina(-20));
      await d.locator('[name=vencimiento]').fill(vencimiento);
      await d.getByRole('button', { name: 'Registrar' }).click();
      await expect(d).toBeHidden();
    }
    await expect(page.locator('table')).toContainText('Evaluación inicial');
  });

  await test.step('Registra un pago parcial e imprime el comprobante', async () => {
    await page.getByRole('button', { name: '+ Nuevo pago' }).click();
    const d = page.getByRole('dialog', { name: 'Nuevo pago' });
    await d.locator('[name=monto]').fill('20000');
    await d.locator('[name=medio]').selectOption('TRANSFERENCIA');
    await d.locator('[name=referencia]').fill('OP-778899');
    await d.getByRole('button', { name: 'Registrar pago' }).click();
    const pregunta = page.getByRole('dialog', { name: 'Confirmar' });
    await expect(pregunta).toContainText('imprimir el comprobante');
    await u.descarga(page, () => pregunta.getByRole('button', { name: 'Descargar comprobante' }).click(), 'pdf');
    await expect(page.locator('table')).toContainText('Comprobante N.º');
    await u.capturar(page, testInfo, '06-admin-cuenta-corriente');
  });

  await test.step('El paciente figura en el listado de deudores', async () => {
    await page.goto('/admin/deudores.html');
    await expect(page.locator('tr').filter({ hasText: pa.apellido })).toBeVisible();
    await u.capturar(page, testInfo, '06-admin-deudores');
    await u.descarga(page, () => page.getByRole('button', { name: 'Excel' }).click(), 'xlsx');
  });

  await test.step('Dispara el recordatorio de deudas y el de turnos de mañana', async () => {
    await page.goto('/admin/notificaciones.html');
    await page.getByRole('button', { name: 'Recordatorio de deudas' }).click();
    await page.getByRole('dialog', { name: 'Confirmar' }).getByRole('button', { name: 'Ejecutar' }).click();
    await u.toast(page, 'Recordatorio de deudas');
    await page.getByRole('button', { name: 'Recordatorio de turnos de mañana' }).click();
    await page.getByRole('dialog', { name: 'Confirmar' }).getByRole('button', { name: 'Ejecutar' }).click();
    await u.toast(page, 'Recordatorio de turnos de mañana');
    await page.getByRole('button', { name: 'Enviar pendientes' }).click();
    await page.getByRole('dialog', { name: 'Confirmar' }).getByRole('button', { name: 'Ejecutar' }).click();
    await u.toast(page, 'Enviar pendientes');
    await u.capturar(page, testInfo, '06-admin-notificaciones');
  });
});
