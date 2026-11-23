// Guion 8: baja de paciente y de profesional → turnos futuros cancelados, acceso denegado e historial conservado.
const { test, expect } = require('@playwright/test');
const u = require('./utilidades');

test('Guion 8 · Bajas de paciente y profesional', async ({ page, playwright, baseURL }, testInfo) => {
  const { paciente: pa, profesional2: p2 } = u.datos(testInfo);
  const { pacienteId } = u.leerEstado(testInfo);
  const admin = await u.api(playwright, baseURL, u.ADMIN);

  await test.step('Baja del paciente desde su ficha', async () => {
    const antes = await admin.get(`/turnos?pacienteId=${pacienteId}&desde=${u.hoyArgentina()}&hasta=${u.hoyArgentina(60)}`);
    expect(antes.cuerpo.filter((t) => t.estado === 'RESERVADO').length).toBeGreaterThan(0);

    await u.ingresar(page, u.ADMIN, u.PASSWORD_E2E);
    await page.goto(`/comun/paciente.html?id=${pacienteId}&tab=datos`);
    await page.getByRole('button', { name: 'Dar de baja' }).click();
    const d = page.getByRole('dialog', { name: 'Dar de baja al paciente' });
    await d.locator('textarea').fill('Finaliza el tratamiento (prueba E2E)');
    await u.capturar(page, testInfo, '08-admin-baja-paciente');
    await d.getByRole('button', { name: 'Dar de baja' }).click();
    await page.waitForURL(/\/comun\/pacientes\.html/);
  });

  await test.step('Efectos: turnos futuros cancelados e historial conservado', async () => {
    const despues = await admin.get(`/turnos?pacienteId=${pacienteId}&desde=${u.hoyArgentina()}&hasta=${u.hoyArgentina(60)}&incluirCancelados=true`);
    expect(despues.cuerpo.length).toBeGreaterThan(0);
    expect(despues.cuerpo.every((t) => t.estado !== 'RESERVADO')).toBeTruthy();
    const ficha = await admin.get(`/pacientes/${pacienteId}`);
    expect(ficha.status).toBe(200);
    expect(ficha.cuerpo.estado).toBe('BAJA');
    expect((await admin.get(`/pacientes/${pacienteId}/planes-alimentarios`)).cuerpo.length).toBeGreaterThan(0);
    expect((await admin.get(`/pacientes/${pacienteId}/antropometria`)).cuerpo.length).toBe(3);
  });

  await test.step('El paciente dado de baja ya no puede ingresar', async () => {
    await u.salir(page);
    await page.locator('[name=username]').fill(pa.dni);
    await page.locator('[name=password]').fill(u.PASSWORD_E2E);
    await page.getByRole('button', { name: 'INGRESAR' }).click();
    await expect(page.locator('.toast.error')).toBeVisible();
    await expect(page).toHaveURL(/index\.html/);
  });

  await test.step('Baja de un profesional con turnos futuros', async () => {
    // Profesional y paciente auxiliares creados por API: el foco del paso es el efecto de la baja.
    const alta = await admin.post('/profesionales', { nombre: p2.nombre, apellido: p2.apellido, tipoDocumento: 'DNI', nroDocumento: p2.dni,
      tipo: 'NUTRICIONISTA', especialidad: 'Deportiva', telefonoWhatsapp: null, email: null, duracionTurnoMin: 45 });
    expect(alta.status, JSON.stringify(alta.cuerpo)).toBe(201);
    const profId = alta.cuerpo.id;
    const temporal = alta.cuerpo.passwordTemporal;
    const otro = await admin.post('/pacientes', { nombre: 'Tomás', apellido: p2.apellido.split(' ').reverse().join(' '), tipoDocumento: 'DNI', nroDocumento: `4${p2.dni.slice(1)}`,
      fechaNacimiento: '1988-03-02', sexo: 'M', telefonoWhatsapp: `+5491100${p2.dni.slice(2)}`, asignaciones: [{ profesionalId: profId, area: 'NUTRICION' }] });
    expect(otro.status, JSON.stringify(otro.cuerpo)).toBe(201);
    const inicio = new Date(Math.ceil((Date.now() + 5 * 864e5) / 3600e3) * 3600e3).toISOString();
    const turno = await admin.post('/turnos', { pacienteId: otro.cuerpo.id, profesionalId: profId, inicio, area: 'NUTRICION', forzar: true });
    expect(turno.status, JSON.stringify(turno.cuerpo)).toBe(201);

    await u.ingresar(page, u.ADMIN, u.PASSWORD_E2E);
    await page.goto(`/admin/profesional.html?id=${profId}`);
    await page.getByRole('button', { name: 'Dar de baja' }).click();
    await page.getByRole('dialog', { name: 'Dar de baja' }).getByRole('button', { name: 'Dar de baja' }).click();
    await page.waitForURL(/profesionales\.html/);

    const t = await admin.get(`/turnos?pacienteId=${otro.cuerpo.id}&desde=${u.hoyArgentina()}&hasta=${u.hoyArgentina(30)}&incluirCancelados=true`);
    expect(t.cuerpo.find((x) => x.id === turno.cuerpo.id).estado).toBe('CANCELADO');
    const ficha = await admin.get(`/pacientes/${otro.cuerpo.id}`);
    expect(ficha.cuerpo.asignaciones.some((a) => a.profesionalId === profId)).toBeFalsy();
    const login = await page.request.post('/api/v1/auth/login', { data: { username: p2.dni, password: temporal } });
    expect(login.status()).toBeGreaterThanOrEqual(400);
  });

  await admin.dispose();
});
