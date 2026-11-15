# Spec 16 · Pruebas y validación del sistema

| Campo | Valor |
|---|---|
| Paquete SDP | 16 – Pruebas (16.1.1 unitarias, 16.1.2 integración, 16.1.3 funcionales, 16.1.4 revisión de cumplimiento, 16.1.5 corrección) |
| RF / RNF | Todos · RNF 3.1 Usabilidad, 3.4 Eficiencia · Constitución art. VII |
| Sprint | 6 (backlog: "Testeo de sistema end-to-end", 12 h). Los testeos por módulo de S1–S3 se definen en cada plan. |
| Depende de | 01–15 |

## Objetivo
Demostrar, con evidencia, que cada RF y CU de la ERS está implementado y que se cumplen los RNF medibles, antes del despliegue final y la demo.

## Niveles de prueba
| Nivel | Herramienta | Alcance | Criterio de salida |
|---|---|---|---|
| Unitarias | JUnit 5 + Mockito + `Clock` fijo | Reglas puras: `ReglasTurno`, `ValidadorPlan`, `GeneradorPlanService`, `EquivalenciaService`, `CuentaCorrienteService`, `PasswordPolicy`, rangos antropométricos, jobs | 100 % en verde; cobertura `service` ≥ 70 % (JaCoCo) |
| Integración | Spring Boot Test + Testcontainers (SQL Server) | Controllers → BD real con Flyway; seguridad; bitácora; outbox | 100 % en verde |
| API / contrato | Postman + Newman en CI | Una carpeta por spec, un test por CA de API | 100 % en verde |
| Arquitectura | ArchUnit | Capas (controller ↛ dao), `@Auditable` en servicios de escritura, entidades no expuestas | 0 violaciones |
| Funcionales / E2E | Guiones manuales por rol (paciente, nutricionista, entrenador, integral, admin) | Flujos completos de cada CU en Chrome escritorio, Chrome Android, Safari iOS | Sin defectos bloqueantes ni críticos abiertos |
| Rendimiento | k6 o JMeter (gratuitos) contra `dev` | 50 usuarios virtuales, dataset 500 pacientes / 1 año | p90 < 5 s en servidor para: login, mi plan, mi rutina, calendario semanal, ficha de paciente, reporte de turnos mensual |
| Seguridad | Tests de matriz de permisos + revisión OWASP Top 10 básica (inyección, auth, exposición de datos, control de acceso) | Todos los endpoints | 0 accesos indebidos |
| Usabilidad | 3 personas con mínima experiencia, tarea guiada | Home y funciones básicas de su rol | Comprenden lo básico en ≤ 30 min (RNF 3.1) |

## Datos de prueba
- `seed/demo/` (perfil `demo`): 1 admin, 2 nutricionistas, 2 entrenadores, 1 integral, 30 pacientes con turnos, planes, rutinas, evolución de 6 meses, pagos y deudas; credenciales documentadas para la demo.
- Generador sintético para rendimiento (500 pacientes × 1 año).

## Revisión de cumplimiento (16.1.4)
Se completa la matriz de [`docs/05-trazabilidad.md`](../../docs/05-trazabilidad.md) agregando por cada CU: estado (OK / Parcial / No), evidencia (test, caso Postman, captura) y observaciones. Ningún CU de prioridad Alta puede quedar en "No".

## Gestión de defectos (16.1.5)
GitHub Issues con etiquetas `bug`, severidad (`bloqueante`, `critico`, `mayor`, `menor`) y spec (`spec-03`…). Cada corrección referencia el issue y agrega un test de regresión.

## Guiones E2E mínimos
1. Admin da de alta profesional integral → recibe WhatsApp → primer ingreso, cambio de contraseña, T&C → carga agenda.
2. Admin da de alta paciente, lo asigna → paciente ingresa, pide turno de nutrición (y prueba la regla de 7 días), lo reprograma, intenta cancelar con < 24 h.
3. Nutricionista genera plan automático, lo ajusta, lo confirma; paciente lo ve en el celular, marca comidas, aplica un reemplazo, comenta, registra hambre/saciedad, descarga PDF.
4. Entrenador arma rutina; paciente registra sesión con cargas, pide variante, mira video, reporta molestia intensidad 8 → entrenador ve novedad prioritaria.
5. Nutricionista carga 3 antropometrías → paciente y profesional ven gráficos y descargan reporte de evolución.
6. Admin registra deudas y pagos, imprime comprobante, lista deudores, dispara recordatorio de deuda manual; job de recordatorio de turnos ejecutado manualmente.
7. Admin consulta bitácora y accesos de todo lo anterior; genera los 5 reportes en PDF y Excel; genera y descarga un backup; programa backup semanal.
8. Baja de paciente y de profesional: verificación de efectos (turnos cancelados, acceso denegado, historial conservado).

## Tareas
- [ ] T-16.1 Completar unitarias faltantes y cobertura
- [ ] T-16.2 Integración y Newman en CI
- [ ] T-16.3 Guiones E2E por rol y ejecución en 3 navegadores
- [ ] T-16.4 Pruebas de rendimiento y de usabilidad
- [ ] T-16.5 Matriz de cumplimiento con evidencia + corrección de defectos (12 h en total)
