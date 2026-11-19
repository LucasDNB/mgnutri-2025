---
titulo: Informe final
subtitulo: Sistema de gestión MG Nutrición y Entrenamiento
version: 1.0 · noviembre de 2026
---

# Resumen

El sistema está **desplegado en producción** en Azure (https://app-mgnutri-1dfb81.azurewebsites.net), con datos de
demostración realistas, monitoreo de disponibilidad y backups probados.

- **Funcionalidad:** se implementaron los **54 casos de uso** de la ERS y los 18 requerimientos funcionales, y todos
  tienen evidencia de prueba en la matriz de trazabilidad.
- **Calidad:** se cumplen los requerimientos no funcionales medibles:
  - Usabilidad: 3 personas aprendieron lo básico en 30 minutos o menos.
  - Rendimiento: p90 máximo de 2,39 s con 50 usuarios y 500 pacientes, contra un límite de 5 s.
  - Restauración: 8 a 15 minutos, contra un objetivo de 2 horas.
  - Disponibilidad: monitoreada cada 5 minutos, con alerta por mail.
- **Pendiente:** solo queda el envío real por WhatsApp a pacientes. El código, la cuenta de Meta y las plantillas están
  listos, y las plantillas esperan la aprobación de Meta.

Durante las pruebas finales en Azure aparecieron **10 defectos** que no se veían en el entorno local. Todos se
corrigieron y volvieron a probarse (sección «Defectos»).

| Entregable (SDP) | Estado | Dónde |
|---|---|---|
| Prototipo funcional (DEMO) | Entregado | Producción en Azure, `v1.0.0` y posteriores |
| Manual de usuario | Entregado | `docs/manuales/manual-usuario.pdf` |
| Manual del sistema | Entregado | `docs/manuales/manual-sistema.pdf` |
| Informe final | Este documento | `docs/manuales/informe-final.pdf` |

# Objetivos y resultados

| Objetivo (PdS / ERS) | Resultado | Evidencia |
|---|---|---|
| Gestión del centro: pacientes, profesionales, agenda y turnos | Cumplido: altas, bajas y modificaciones, agenda semanal por profesional, turnos con reprogramación y cancelación | CU-01 a CU-10 · guiones E2E 1, 2 y 8 |
| Módulo Nutrición: planes alimentarios, generación automática, alternativas, uso por el paciente | Cumplido: plan semanal, generación automática por kcal y macros, reemplazos, registro de comidas, PDF | CU-11 a CU-18 · guion 3 |
| Módulo Entrenamiento: rutinas, registro de cargas, progreso, molestias | Cumplido: rutinas por día, registro de series y cargas, molestias prioritarias, alternativas | CU-21 a CU-28 · guion 4 |
| Seguimiento de la evolución | Cumplido: antropometría, gráficos y reporte de evolución | CU-19, 20, 32 · guion 5 |
| Pagos: cuenta corriente, comprobantes, deudores | Cumplido: cargos, pagos con saldo FIFO, comprobante PDF, deudores y recordatorios | CU-44 a CU-46 · guion 6 |
| Comunicación por WhatsApp | Implementado y conectado a Meta; envío real a la espera de la aprobación de plantillas | CU-37 a CU-39 · sección «Pendientes» |
| Reportes en pantalla, PDF y Excel | Cumplido: 5 reportes | CU-30 a CU-34 · guion 7 |
| Seguridad, registro de accesos y bitácora | Cumplido: roles, bloqueo por intentos, recupero, cifrado de datos sensibles, bitácora inmutable | CU-35, 36, 47 a 50 · guiones 1 y 7 |
| Backups y restauración (RTO ≤ 2 h) | Cumplido: manual, programado y restauración ensayada | CU-51, 52 · ensayo del 19/11/2026 |
| Uso desde el celular | Cumplido: Chrome Android (automatizado) y Safari iOS (manual) | RNF 3.1 |

**Fuera de alcance** (estaba en la PdS pero no se bajó a la ERS): recetario, hidratación, listas de compra automáticas, mensajería interna y pasarela de pago
(las dos últimas se cubren en parte con el enlace a WhatsApp, los comentarios y un enlace de pago configurable). Ver `docs/00-analisis-documental.md` §5.

# Matriz de cumplimiento

La matriz completa, caso de uso por caso de uso, está en `docs/05-trazabilidad.md`. Resumen:

| Alcance | Total | Con evidencia | Estado |
|---|---:|---:|---|
| Casos de uso (ERS) | 54 | 54 | OK |
| Requerimientos funcionales (RF-01 a RF-18) | 18 | 18 | OK |
| Requerimientos no funcionales medibles | 6 | 6 | Cumplen (detalle abajo) |

| RNF | Criterio | Resultado medido |
|---|---|---|
| 3.1 Usabilidad | Aprender lo básico en ≤ 30 min; responsive | 3 personas con poca experiencia: cumple. Chrome escritorio y Android (16/16 E2E) y Safari iOS (manual): cumple |
| 3.2 Disponibilidad 99 % | Monitoreo y alerta | Prueba estándar de Application Insights cada 5 min desde 2 regiones, alerta por mail |
| 3.2 Backup diario / RTO ≤ 2 h | Restaurar en ≤ 2 h | PITR ≈ 8 min y BACPAC ≈ 15 min hasta la app funcionando |
| 3.3 Seguridad | Usuario y contraseña, roles, cifrado, accesos | BCrypt, JWT con revocación, AES-256-GCM en datos personales, bloqueo tras 5 intentos, registro de accesos |
| 3.4 Eficiencia | p90 < 5 s con 50 concurrentes y 500 pacientes | p90 máximo 2,39 s (login) en Azure |
| 3.5 Mantenibilidad | Módulos, versionado, migraciones | Módulos por dominio, 15 migraciones Flyway, tags de versión, CI/CD |

# Pruebas realizadas

| Tipo | Herramienta | Alcance | Resultado |
|---|---|---|---|
| Unitarias y de arquitectura | JUnit 5 | Reglas de negocio, reglas de dependencias entre módulos | 27 de 27 aprobadas (CI) |
| Integración | JUnit 5 + Testcontainers (SQL Server) | Flujos con base real: salud, bloqueo tras intentos fallidos, alta de paciente con permisos y bitácora, inmutabilidad de bitácora y accesos, revocación del token al salir | 5 de 5 aprobadas (CI, con SQL Server real en contenedor) |
| API | Postman / Newman | 33 requests y 45 aserciones sobre la API, en cada push | 45 de 45 aprobadas (CI) |
| Extremo a extremo | Playwright | 8 guiones por rol en Chrome escritorio y Chrome Android, sobre staging en Azure | 16 de 16 aprobados (7,9 min) |
| Navegador Safari iOS | Manual, iPhone | Paciente y profesional: menú, listados, formularios, gráficos y PDF | Aprobada |
| Usabilidad | Manual, 3 personas | Una tarea básica por rol | Aprobada (≤ 30 min) |
| Recorrido del administrador | Manual, producción | Primer ingreso, bitácora, reportes y opciones avanzadas | Aprobado |
| Rendimiento | k6 | 50 usuarios, 500 pacientes y 12 meses de historia, sobre Azure | p90 máximo 2,39 s; 0,04 % de errores |
| Restauración | Azure SQL | PITR y BACPAC hasta tener la app funcionando | 8 y 15 min |

Detalle y forma de repetirlas: `pruebas/e2e/RESULTADOS.md` y `pruebas/rendimiento/README.md`.

**Desvío respecto de la spec 17 (CA-17.8):** los guiones E2E no se corrieron sobre producción sino sobre un staging
idéntico: mismo plan, mismo servidor SQL y mismo jar. Los guiones crean registros y cambian la contraseña del
administrador, así que correrlos en producción habría ensuciado los datos reales. Sobre producción se hicieron las
pruebas manuales.

# Defectos encontrados en las pruebas finales

| # | Defecto | Severidad | Cómo se detectó | Corrección |
|---|---|---|---|---|
| 1 | Listados con filtros (12 pantallas) mostraban resultados sin filtrar si la carga inicial respondía después del filtro | Mayor | E2E con datos realistas | Se descarta la respuesta de un pedido viejo |
| 2 | El backup BACPAC satura la base Basic ~5 min y el aviso decía que el sistema seguía disponible | Mayor | E2E en Azure | Aviso de demora y recomendación de hacerlo fuera de horario |
| 3 | Ficha del paciente desbordada en celulares | Mayor | E2E Android | CSS |
| 4 | Horas en formato de 12 h | Menor | E2E | Formato de 24 h |
| 5 | Con WhatsApp habilitado la aplicación no arrancaba (constructor ambiguo del adaptador) | Crítica | Activación en producción | `@Autowired` en el constructor |
| 6 | Plantillas de WhatsApp que Meta rechaza (variable al final; contraseña en el texto) | Mayor | Envío a revisión de Meta | Textos nuevos y modo ENLACE para credenciales |
| 7 | Claves foráneas sin índice: «mi plan» recorría tablas completas | Crítica | k6 en Azure (base al 100 % de CPU) | Migración V15 |
| 8 | BCrypt dentro de la transacción agotaba el pool de conexiones | Crítica | k6 en Azure (esperas de 30 s) | Cálculo antes de la transacción |
| 9 | BCrypt con costo 12: ~1 s de CPU por login | Mayor | k6 en Azure (login p90 25,7 s) | Costo 10 con rehash automático |
| 10 | Las pruebas de integración quedaban omitidas en CI sin que el build fallara | Mayor | Revisión de los reportes de CI para este informe | Versión de la API de Docker y control que falla si se omiten |

Los defectos 7 a 9 no aparecían en local: la notebook de desarrollo tiene mucha más CPU que el plan B2 y la base S2.
Sin la prueba de carga en Azure habrían llegado a producción.

# Burndown real y planificado

Planificación del Sprint Planning v1.0.0: **124 horas** en 6 sprints.

| Sprint | Contenido | Horas planificadas | Restante planificado | Horas reales | Restante real |
|---|---|---:|---:|---:|---:|
| 1 | Entorno, bitácora, seguridad mínima, pacientes, profesionales, turnos | 32 | 92 | _a completar_ | _a completar_ |
| 2 | Planes, rutinas, evolución, pagos | 24 | 68 | _a completar_ | _a completar_ |
| 3 | Catálogos, consulta de plan y de rutina, PDFs | 20 | 48 | _a completar_ | _a completar_ |
| 4 | WhatsApp, reportes, seguridad completa | 20 | 28 | _a completar_ | _a completar_ |
| 5 | Backup | 8 | 20 | _a completar_ | _a completar_ |
| 6 | Pruebas E2E, despliegue y documentación | 20 | 0 | _a completar_ | _a completar_ |
| **Total** | | **124** | | _a completar_ | |

> **Nota para el equipo:** el repositorio no registra horas. El historial de git solo tiene commits entre el 15 y el
> 19/11/2026. Completar las columnas reales con el registro del equipo.

Trabajo que no estaba en el plan de 124 h y se agregó en el sprint 6:
- Ajustes por las restricciones de la suscripción de Azure.
- Ensayo de restauración.
- Monitoreo y alertas.
- Corrección de los 10 defectos.
- Carga de datos realistas.

# Riesgos

| Riesgo previsto (roadmap) | ¿Se materializó? | Qué pasó |
|---|---|---|
| Rechazo de plantillas de WhatsApp | **Sí** | Meta rechazó como «Autenticación» la plantilla con la contraseña y la de recupero. Se aplicó la mitigación prevista (modo ENLACE) y se reformularon los textos |
| Plan F1 sin Always On, sin jobs | No | Se usó B1 con Always On desde el inicio |
| Exportación BACPAC sin permisos | No | Funcionó con identidad administrada y rol *SQL DB Contributor*. Apareció un efecto no previsto: en Basic satura la base unos 5 minutos |
| Datos de catálogo sin fuente confiable | No | Catálogo semilla con validación de coherencia de kcal |
| Subestimación de las 124 h | Sin datos | Ver «Burndown» |

| Riesgo no previsto | Impacto | Respuesta |
|---|---|---|
| La suscripción Azure for Students solo permite 5 regiones y no admite Static Web Apps | No se podía publicar el frontend como estaba diseñado | Frontend empaquetado en el jar y servido por el mismo App Service (un solo despliegue, sin CORS) |
| Rendimiento en Azure muy inferior al local | 3 defectos críticos o mayores invisibles en local | Prueba de carga sobre Azure antes de cerrar (defectos 7 a 9) |
| CI en verde con pruebas omitidas | Falsa sensación de cobertura | Control explícito en el workflow (defecto 10) |
| Plan B1 sin memoria para dos aplicaciones Java | Staging y producción no podían correr juntos | Subida temporal a B2 solo durante las pruebas |

# Lecciones aprendidas

1. **Probar en el entorno real.** Los defectos más graves de rendimiento solo aparecieron en Azure. La prueba de
   carga local daba un p90 de 541 ms; la primera corrida en Azure dio de 30 a 60 s.
2. **Datos realistas, no de prueba.** Con datos parecidos a los reales aparecieron condiciones de carrera en los
   listados (defecto 1) que con una base casi vacía no se veían.
3. **Un CI en verde no prueba nada si no se revisa qué corrió.** Las pruebas de integración estaban omitidas y nadie
   lo notaba. Hoy el workflow falla si eso vuelve a pasar.
4. **Las integraciones externas tienen reglas propias.** Meta rechaza plantillas por categoría o por variables al
   final. Conviene enviarlas a revisión al principio del proyecto, no al final.
5. **Las restricciones de la suscripción condicionan la arquitectura.** Revisar al inicio qué regiones y servicios
   permite evita rediseños tardíos.
6. **Ensayar la restauración.** El ensayo mostró que el BACPAC pierde las contraseñas de los usuarios de la base. Eso
   no está en ninguna guía y en una emergencia real habría costado tiempo.

# Pendientes

| Pendiente | Responsable | Estado |
|---|---|---|
| Primer envío real por WhatsApp | Equipo (automático al aprobarse) | 8 plantillas en revisión en Meta; la aplicación ya tiene WhatsApp habilitado |
| Número propio de WhatsApp (hoy es el número de prueba de Meta) | Centro | Requiere un número dedicado y un medio de pago en Meta |
| Completar el burndown real | Equipo | Columnas marcadas «a completar» |
| Rotar el token de WhatsApp | Equipo | El token actual circuló por un canal no seguro |
| Subir de nivel la base de producción si se esperan 50 usuarios simultáneos | Centro | Producción usa Basic (5 DTU); la prueba de carga se hizo con S2 (50 DTU) |
