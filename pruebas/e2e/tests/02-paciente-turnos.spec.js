// Guion 2: admin da de alta un paciente y lo asigna → el paciente ingresa, pide turno de nutrición, prueba la regla
// de 7 días, lo reprograma e intenta cancelar un turno con menos de 24 h.
const { test, expect } = require('@playwright/test');
const u = require('./utilidades');

test('Guion 2 · Alta de paciente y gestión de turnos', async ({ page, playwright, baseURL }, testInfo) => {
  const { paciente: pa, profesional: pr } = u.datos(testInfo);

  await test.step('Admin registra al paciente y lo asigna al profesional integral', async () => {
    await u.ingresar(page, u.ADMIN, u.PASSWORD_E2E);
    await page.goto('/admin/paciente-form.html');
    await page.locator('[name=nombre]').fill(pa.nombre);
    await page.locator('[name=apellido]').fill(pa.apellido);
    await page.locator('[name=nroDocumento]').fill(pa.dni);
    await page.locator('[name=fechaNacimiento]').fill('1990-05-14');
    await page.locator('[name=sexo]').selectOption('F');
    await page.locator('[name=telefonoWhatsapp]').fill(pa.telefono);
    await page.locator('[name=email]').fill(`paciente${pa.dni}@e2e.mgnutri`);
    await page.locator('[name=objetivo]').fill('Descenso de peso y mejora de la condición física');
    for (const area of ['Nutrición', 'Entrenamiento']) {
      await page.locator('label.check').filter({ hasText: `${pr.apellido}, ${pr.nombre} — ${area}` }).locator('input').check();
    }
    await u.capturar(page, testInfo, '02-admin-alta-paciente');
    await page.getByRole('button', { name: 'Registrar paciente' }).click();
    const dialogo = page.getByRole('dialog', { name: 'Paciente registrado correctamente' });
    const temporal = (await dialogo.locator('strong').textContent()).trim();
    await dialogo.getByRole('button', { name: 'Ver ficha' }).click();
    await page.waitForURL(/\/comun\/paciente\.html\?id=\d+/);
    const id = Number(new URL(page.url()).searchParams.get('id'));
    await expect(page.locator('table').filter({ hasText: 'Nutrición' })).toContainText(pr.apellido);
    u.guardarEstado(testInfo, { pacienteId: id, temporalPaciente: temporal });
  });

  await test.step('Primer ingreso del paciente', async () => {
    await u.salir(page);
    await u.primerIngreso(page, pa.dni, u.leerEstado(testInfo).temporalPaciente);
    await expect(page).toHaveURL(/\/paciente\/home\.html/);
    await u.capturar(page, testInfo, '02-paciente-home');
  });

  const elegirProfesional = async () => {
    await page.goto('/paciente/turno-nuevo.html?area=NUTRICION');
    await page.getByRole('button', { name: new RegExp(pr.apellido) }).click();
    // Semana siguiente: un turno a más de 24 h, que el paciente todavía puede modificar.
    await page.getByRole('button', { name: 'Semana siguiente ›' }).click();
    await expect(page.getByRole('button', { name: /^\d{2}:\d{2}$/ }).first()).toBeVisible();
  };

  await test.step('Pide un turno de nutrición', async () => {
    await elegirProfesional();
    await u.capturar(page, testInfo, '02-paciente-elegir-horario');
    await page.getByRole('button', { name: /^\d{2}:\d{2}$/ }).first().click();
    await page.getByRole('dialog').getByRole('button', { name: 'Confirmar turno' }).click();
    await expect(page.locator('.aviso')).toContainText('Turno confirmado');
  });

  await test.step('Regla de 7 días: no puede pedir otro turno de nutrición cercano', async () => {
    await elegirProfesional();
    await page.getByRole('button', { name: /^\d{2}:\d{2}$/ }).nth(2).click();
    await page.getByRole('dialog').getByRole('button', { name: 'Confirmar turno' }).click();
    await expect(page.locator('.aviso.advertencia')).toBeVisible();
    await u.capturar(page, testInfo, '02-paciente-regla-7-dias');
  });

  await test.step('Reprograma el turno a la semana siguiente', async () => {
    await page.goto('/paciente/turnos.html');
    await page.getByRole('button', { name: 'Modificar' }).first().click();
    const dialogo = page.getByRole('dialog', { name: 'Modificar turno' });
    await dialogo.getByRole('button', { name: 'Semana siguiente ›' }).click();
    await dialogo.getByRole('button', { name: 'Semana siguiente ›' }).click();
    await dialogo.getByRole('button', { name: /^\d{2}:\d{2}$/ }).first().click();
    await dialogo.getByRole('button', { name: 'Confirmar cambio' }).click();
    await u.toast(page, 'Turno modificado');
  });

  await test.step('Turno con menos de 24 h: no se puede cancelar', async () => {
    const { pacienteId, profesionalId } = u.leerEstado(testInfo);
    const admin = await u.api(playwright, baseURL, u.ADMIN);
    const inicio = new Date(Math.ceil((Date.now() + 3 * 3600e3) / 1800e3) * 1800e3).toISOString();
    const t = await admin.post('/turnos', { pacienteId, profesionalId, inicio, area: 'ENTRENAMIENTO', forzar: true });
    expect(t.status, JSON.stringify(t.cuerpo)).toBe(201);
    await admin.dispose();

    await page.goto('/paciente/turnos.html');
    await expect(page.getByText('No se puede modificar con menos de 24 h de anticipación').first()).toBeVisible();
    await u.capturar(page, testInfo, '02-paciente-turnos');
    const paciente = await u.api(playwright, baseURL, pa.dni);
    const r = await paciente.post(`/me/turnos/${t.cuerpo.id}/cancelar`, { version: t.cuerpo.version });
    expect(r.status).toBeGreaterThanOrEqual(400);
    await paciente.dispose();
  });
});
