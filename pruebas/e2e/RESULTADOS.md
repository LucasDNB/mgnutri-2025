# Resultados de los guiones E2E (spec 16)

Última corrida: 2026-11-18 23:08 UTC · Playwright 1.49.1 sobre Google Chrome instalado · base `mgnutri_e2e` (perfil `local`).

**16 de 16 aprobados** en 2.9 min.

| Guion | Chrome escritorio (1366×860) | Chrome Android (Pixel 7, emulado) |
|---|---|---|
| Guion 1 · Alta de profesional integral, primer ingreso y agenda | ✔ aprobado (8 s) | ✔ aprobado (7 s) |
| Guion 2 · Alta de paciente y gestión de turnos | ✔ aprobado (12 s) | ✔ aprobado (12 s) |
| Guion 3 · Plan alimentario automático y uso por el paciente | ✔ aprobado (12 s) | ✔ aprobado (15 s) |
| Guion 4 · Rutina de entrenamiento, registro de cargas y molestia prioritaria | ✔ aprobado (8 s) | ✔ aprobado (11 s) |
| Guion 5 · Antropometría, gráficos y reporte de evolución | ✔ aprobado (7 s) | ✔ aprobado (10 s) |
| Guion 6 · Cuenta corriente, comprobante, deudores y recordatorios | ✔ aprobado (6 s) | ✔ aprobado (6 s) |
| Guion 7 · Bitácora, accesos, reportes y backup | ✔ aprobado (20 s) | ✔ aprobado (23 s) |
| Guion 8 · Bajas de paciente y profesional | ✔ aprobado (6 s) | ✔ aprobado (6 s) |

## Defectos encontrados y corregidos durante la ejecución

| Defecto | Severidad | Corrección |
|---|---|---|
| En celulares la ficha del paciente desbordaba la pantalla (emails largos en la tabla de datos) y el botón «Dar de baja» quedaba tapado por «Reenviar credenciales» | Mayor | `frontend/css/base.css`: hijos de `.grilla` con `min-width: 0` y corte de línea en tablas clave/valor |
| Las horas se mostraban en formato de 12 h («08:00 a. m.») | Menor | `frontend/js/core/format.js`: `hourCycle: 'h23'` |

## Evidencia

- Capturas de cada guion: `resultados/capturas/<proyecto>/` (se regeneran en cada corrida, no se versionan).
- Reporte HTML con trazas de las fallas: `npx playwright show-report resultados/reporte-html`.
- Safari iOS no se automatiza en este entorno (requiere macOS o un dispositivo): queda como prueba manual pendiente.
