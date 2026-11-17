// Guion 1: admin da de alta un profesional integral → credenciales (WhatsApp o entrega manual) → primer ingreso,
// cambio de contraseña y T&C → carga su agenda semanal.
const { test, expect } = require('@playwright/test');
const u = require('./utilidades');

test('Guion 1 · Alta de profesional integral, primer ingreso y agenda', async ({ page, playwright, baseURL }, testInfo) => {
  const { profesional: pr } = u.datos(testInfo);

  await test.step('Admin registra al profesional integral', async () => {
    await u.ingresar(page, u.ADMIN, u.PASSWORD_E2E);
    await page.goto('/admin/profesional.html');
    await page.locator('[name=nombre]').fill(pr.nombre);
    await page.locator('[name=apellido]').fill(pr.apellido);
    await page.locator('[name=nroDocumento]').fill(pr.dni);
    await page.locator('[name=tipo]').selectOption('INTEGRAL');
    await page.locator('[name=matricula]').fill('MN 7777');
    await page.locator('[name=especialidad]').fill('Nutrición y entrenamiento');
    await page.locator('[name=telefonoWhatsapp]').fill(pr.telefono);
    await page.locator('[name=email]').fill(`integral${pr.dni}@e2e.mgnutri`);
    await u.capturar(page, testInfo, '01-admin-alta-profesional');
    await page.getByRole('button', { name: 'Registrar profesional' }).click();
    const dialogo = page.getByRole('dialog', { name: 'Profesional registrado correctamente' });
    await expect(dialogo).toContainText(`Usuario: ${pr.dni}`);
    // Sin WhatsApp configurado (entorno local) el sistema muestra la contraseña temporal para entregarla por otro medio.
    const temporal = (await dialogo.locator('strong').textContent()).trim();
    expect(temporal.length).toBeGreaterThanOrEqual(8);
    await dialogo.getByRole('button', { name: 'Cerrar' }).click();
    await page.waitForURL(/\/admin\/profesional\.html\?id=\d+/);
    const id = Number(new URL(page.url()).searchParams.get('id'));
    u.guardarEstado(testInfo, { profesionalId: id, temporalProfesional: temporal });
  });

  await test.step('Se encoló el mensaje de credenciales (outbox de WhatsApp)', async () => {
    const admin = await u.api(playwright, baseURL, u.ADMIN);
    const prof = await admin.get(`/profesionales/${u.leerEstado(testInfo).profesionalId}`);
    const r = await admin.get('/notificaciones?tipo=CREDENCIALES&size=50');
    expect(r.status).toBe(200);
    expect(r.cuerpo.content.some((n) => n.usuarioId === prof.cuerpo.usuarioId)).toBeTruthy();
    await admin.dispose();
  });

  await test.step('Primer ingreso: cambio de contraseña y aceptación de T&C', async () => {
    await u.salir(page);
    await u.primerIngreso(page, pr.dni, u.leerEstado(testInfo).temporalProfesional);
    await expect(page).toHaveURL(/\/profesional\/home\.html/);
    await u.capturar(page, testInfo, '01-profesional-home');
  });

  await test.step('El profesional carga su agenda (lunes a sábado, 8 a 20 h)', async () => {
    await page.goto('/profesional/agenda.html');
    for (let dia = 1; dia <= 6; dia++) {
      await page.getByRole('button', { name: '+ Agregar franja' }).click();
      const fila = page.locator('tbody tr').nth(dia - 1);
      await fila.locator('select').selectOption(String(dia));
      await fila.locator('input[type=time]').nth(0).fill('08:00');
      await fila.locator('input[type=time]').nth(1).fill('20:00');
      await fila.locator('input[type=text]').fill('Consultorio E2E');
      await fila.locator('input[type=text]').dispatchEvent('change');
    }
    await page.getByRole('button', { name: 'Guardar agenda' }).click();
    await u.toast(page, 'Agenda guardada');
    await expect(page.locator('tbody tr')).toHaveCount(6);
    await u.capturar(page, testInfo, '01-profesional-agenda');
  });
});
