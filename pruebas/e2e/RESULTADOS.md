# Resultados de los guiones E2E (spec 16)

Última corrida: 2026-11-19 11:17 UTC · Playwright 1.49.1 sobre Google Chrome instalado · **Azure (staging)**:
`app-mgnutri-staging-1dfb81.azurewebsites.net`, mismo plan de App Service y mismo servidor SQL que producción.

**16 de 16 aprobados** en 7,9 min.

## Datos con los que se probó

La base de staging se cargó con el mismo cargador que producción (`DemoDataLoader`, datos realistas), así que los
guiones trabajan entre datos parecidos a los reales y no sobre una base vacía:

| Dato | Cantidad |
|---|---|
| Profesionales (2 nutricionistas, 2 entrenadores, 1 integral) con agenda semanal | 5 |
| Pacientes (DNI acorde a la edad, domicilios y objetivos variados) | 60 |
| Turnos de 6 meses (asistidos, ausentes, cancelados y futuros) | 1.001 |
| Planes alimentarios / rutinas | 60 / 60 |
| Mediciones antropométricas | 420 |
| Pagos (y deudas pendientes) | 306 |
| Registros de entrenamiento | 2.005 |

Los guiones crean además sus propios profesionales y pacientes con nombre y apellido doble realistas, únicos por
corrida (p. ej. «Milagros Godoy Carrizo», DNI 38.043.292). Mails en `example.com` y teléfonos `11 00…` (no asignados).

## Resultado por guion

| Guion | Chrome escritorio (1366×860) | Chrome Android (Pixel 7, emulado) |
|---|---|---|
| Guion 1 · Alta de profesional integral, primer ingreso y agenda | ✔ aprobado (18 s) | ✔ aprobado (17 s) |
| Guion 2 · Alta de paciente y gestión de turnos | ✔ aprobado (21 s) | ✔ aprobado (22 s) |
| Guion 3 · Plan alimentario automático y uso por el paciente | ✔ aprobado (20 s) | ✔ aprobado (24 s) |
| Guion 4 · Rutina de entrenamiento, registro de cargas y molestia prioritaria | ✔ aprobado (13 s) | ✔ aprobado (17 s) |
| Guion 5 · Antropometría, gráficos y reporte de evolución | ✔ aprobado (12 s) | ✔ aprobado (17 s) |
| Guion 6 · Cuenta corriente, comprobante, deudores y recordatorios | ✔ aprobado (10 s) | ✔ aprobado (11 s) |
| Guion 7 · Bitácora, accesos, reportes y backup (BACPAC a Blob Storage) | ✔ aprobado (120 s) | ✔ aprobado (107 s) |
| Guion 8 · Bajas de paciente y profesional | ✔ aprobado (18 s) | ✔ aprobado (16 s) |

La misma suite pasó también 16/16 en local (2,9 min) sobre una base cargada igual.

## Defectos encontrados y corregidos

| Defecto | Severidad | Corrección |
|---|---|---|
| Listados con filtros o búsqueda (12 pantallas): si la carga inicial respondía después que el filtro, la tabla mostraba resultados sin filtrar. Apareció con datos realistas, cuando la carga inicial se vuelve más lenta | Mayor | Cada carga lleva un número y se descarta la respuesta de un pedido viejo |
| En Azure (base Basic, 5 DTU) el backup BACPAC lleva la base al 100 % unos 5 minutos y el sistema casi no responde; el aviso decía «El sistema sigue disponible» | Mayor | El aviso de *Generar backup* advierte la demora y recomienda hacerlo fuera del horario de atención (el backup automático se programa de noche) |
| En celulares la ficha del paciente desbordaba la pantalla y el botón «Dar de baja» quedaba tapado | Mayor | `frontend/css/base.css` (corrida del 18/11) |
| Las horas se mostraban en formato de 12 h («08:00 a. m.») | Menor | `frontend/js/core/format.js` (corrida del 18/11) |

Ajustes de los guiones para datos reales: el guion 7 busca el alta del paciente filtrando por módulo y tipo (la primera
página de la bitácora puede llenarse con los recordatorios del job) y espera hasta 10 minutos el backup BACPAC.

## Cómo correrlos

Contra una instancia con base propia y `RATE_LIMIT_AUTH=1000` (los guiones ingresan muchas veces por minuto desde la
misma IP; con el límite de producción, 10, el servidor responde 429):

```bash
E2E_URL=https://<staging>.azurewebsites.net ADMIN_PASSWORD_INICIAL=<admin temporal> E2E_PASSWORD=<contraseña de prueba> npx playwright test
```

Nunca contra producción: crean registros y cambian la contraseña del administrador.

## Evidencia

- Capturas de cada guion: `resultados/capturas/<proyecto>/` (se regeneran en cada corrida, no se versionan).
- Reporte HTML con trazas: `npx playwright show-report resultados/reporte-html`.
- Safari iOS no se automatiza en este entorno (requiere macOS o un dispositivo): se probó a mano (ver abajo).

## Pruebas manuales en producción (19/11/2026)

Sobre `app-mgnutri-1dfb81.azurewebsites.net`, con los datos demo realistas (sin crear registros de prueba):

| Prueba | Resultado |
|---|---|
| Safari iOS (iPhone): ingreso como paciente y como profesional, menú, listados, formularios, gráficos y PDF | ✔ aprobada |
| Usabilidad (RNF 3.1): 3 personas con poca experiencia, una tarea básica de su rol cada una | ✔ aprobada: comprendieron lo básico en ≤ 30 min |
| Recorrido del administrador: primer ingreso, home, bitácora, reportes y opciones avanzadas | ✔ aprobada |
