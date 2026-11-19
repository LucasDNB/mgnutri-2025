# 01 · Registro de decisiones (ADR resumido)

Cada decisión resuelve una inconsistencia (I-xx) o un vacío (V-xx) de [`00-analisis-documental.md`](00-analisis-documental.md). Las marcadas **[Validar]** conviene confirmarlas con el tutor antes de implementar; el resto son interpretaciones directas del texto.

## Dominio y reglas de negocio

**D-01 · RF-05 trata rutinas, no planes de alimentación** (I-13). El texto de RF-05 repite por error "planes de alimentación". Se especifica como gestión de rutinas de entrenamiento por parte de entrenadores y profesionales integrales.

**D-03 · Modelo de roles.** Roles del sistema: `ADMIN`, `NUTRICIONISTA`, `ENTRENADOR`, `PACIENTE`. El *profesional integral* es un usuario con los roles `NUTRICIONISTA` y `ENTRENADOR` a la vez (no un rol aparte), de modo que hereda ambos permisos sin duplicar reglas. El tipo de profesional (`NUTRICIONISTA` | `ENTRENADOR` | `INTEGRAL`) se guarda en `profesional.tipo` y determina los roles asignados al usuario.

**D-04 · Credenciales iniciales.** El nombre de usuario es el número de documento (único). En el alta el sistema genera una contraseña temporal aleatoria (12 caracteres) que se envía por WhatsApp (CU-37). El usuario queda con `debe_cambiar_password = true` y en su primer ingreso se le exige cambiarla (CU-49) y aceptar los términos (D-25).

**D-05 · CU-02 no reenvía credenciales automáticamente** (I-19). Modificar un paciente no dispara CU-37. Se agrega la acción explícita "Reenviar credenciales" en la ficha (genera nueva contraseña temporal), útil cuando cambia el teléfono.

**D-06 · Bajas lógicas. [Validar]** CU-03/CU-06 dicen "elimina … irreversible salvo backup". Una eliminación física rompería el historial de turnos, pagos, reportes y bitácora (que exigen retención ≥ 6 meses). Se implementa baja lógica irreversible desde la UI: estado `BAJA`, credenciales eliminadas (usuario deshabilitado y contraseña invalidada), turnos futuros cancelados, el registro desaparece de listados y búsquedas. Los datos quedan sólo para reportes históricos y auditoría.

**D-07 · Asignación paciente↔profesional por área.** CU-29 vincula un paciente con uno o más profesionales indicando el área (`NUTRICION` o `ENTRENAMIENTO`; un integral puede quedar asignado en ambas). Sólo el profesional asignado en el área puede crear/editar planes (nutrición) o rutinas (entrenamiento) y cargar evolución de ese paciente. Los turnos, en cambio, pueden pedirse con cualquier profesional del área (CU-07 muestra primero el habitual).

**D-08 · Agenda de todos los profesionales** (I-17). Cualquier profesional puede tener agenda. El calendario se muestra en su home si tiene disponibilidad cargada. Esto concilia ERS 3.9.1 (entrenador sin calendario) con PdS/SDP (agendas de nutrición y entrenamiento): un entrenador sin disponibilidad simplemente no ve calendario.

**D-09 · Ciclo de vida del turno** (I-18, V-03). Estados: `RESERVADO` → `ATENDIDO` | `AUSENTE` | `CANCELADO`. La solicitud del paciente queda confirmada automáticamente (CU-07 paso 4). "Conceder" (RF-03) se interpreta como la potestad del profesional/admin de crear turnos para un paciente. Una reprogramación (CU-08) modifica fecha/hora del mismo turno y guarda la fecha original en `turno_historial`. El profesional o el admin marcan `ATENDIDO`/`AUSENTE` (necesario para el reporte de ausencias de RF-14).

**D-10 · Disponibilidad y duración** (V-02). Cada profesional tiene franjas semanales (`dia_semana`, `hora_desde`, `hora_hasta`, `ubicacion`) y una duración de turno (por defecto 30 min, configurable). Los huecos ofrecidos = franjas − turnos `RESERVADO` − bloqueos de agenda (vacaciones, feriados). La carga de disponibilidad la hace el admin o el propio profesional.

**Reglas de turnos derivadas de la ERS:**
- R-T1 (CU-07) Un paciente no puede solicitar turno con un profesional si ya tiene un turno `RESERVADO` con ese mismo profesional dentro de los próximos 7 días. **[Validar: se interpreta por profesional, porque un paciente puede tener nutrición y entrenamiento la misma semana.]**
- R-T2 (CU-08/09) El paciente no puede modificar ni cancelar un turno que empieza en menos de 24 h. Admin y profesional sí pueden.
- R-T3 (CU-08) La reprogramación ofrece sólo huecos del mismo profesional.
- R-T4 No se pueden reservar turnos en el pasado ni superpuestos para el mismo profesional (control de concurrencia optimista + índice único `profesional_id, fecha_hora_inicio` sobre turnos `RESERVADO`).

**D-11 · Recordatorio de turno** (I-14). Job diario a las 09:00 (hora Argentina) que envía recordatorio de todos los turnos `RESERVADO` del día siguiente. Así cada turno se recuerda entre 15 y 39 h antes, conciliando "día siguiente" (RF-07), "24 h" (CU-38) y "48 h" (RF-13). Hora configurable. Idempotente vía `turno.recordatorio_enviado_at`. Además se envían mensajes inmediatos al crear, reprogramar y cancelar (RF-13).

**D-12 · Recordatorio de deuda** (I-15). Job semanal, sábados 10:00, a pacientes con saldo deudor > 0 y teléfono registrado. Incluye monto y enlace de pago si `config.pagos.link` está definido.

**D-13 · Calendario** (I-16). Vistas día / semana / mes.

**D-14 · Generación automática de plan alimentario** (V-04). Ver algoritmo en spec 04. Entradas: kcal objetivo, % HC / proteínas / grasas (suma 100), comidas por día (≥ 4), distribución calórica por comida, alimentos a excluir. Salida: **borrador** editable que el nutricionista revisa y confirma (CU-14 paso 3-4). Respeta la exclusión del PdS: no reemplaza criterio profesional.

**D-15 · Equivalencias alimentarias** (V-05). Cada alimento pertenece a un *grupo de intercambio* (lácteos, carnes y huevos, cereales y legumbres, frutas, verduras, grasas, azúcares). Cada grupo define su macronutriente de referencia (p. ej. carnes → proteínas, cereales → HC). Equivalente de `q` gramos de A en B (mismo grupo) = `q × refA / refB` redondeado a 5 g. Además se admiten equivalencias manuales cargadas en CU-53 que tienen prioridad.

**D-16 · Evolución del paciente** (I-20). Dos registros: **antropometría** (nutricionista: peso, talla, % grasa, % músculo, perímetros) y **evaluación de desempeño físico** (entrenador: pruebas con valor y unidad, p. ej. "Sentadilla 1RM = 80 kg"). Una única pantalla de evolución con gráficos de peso, composición corporal, medidas y desempeño. La carga de pesos usados por el paciente (RF-15) alimenta además el gráfico de progreso de cargas por ejercicio.

**D-17 · Planes y rutinas: activo único e historial.** Al crear un plan/rutina para un paciente que ya tiene uno activo, el sistema advierte y, al confirmar, el anterior pasa a `FINALIZADO`. Eliminar (CU-13/CU-23) pone estado `ELIMINADO` (deja de ser accesible para el paciente) pero se conserva para el reporte de historial (RF-14). Un plan vencido por fecha pasa a `FINALIZADO` mediante job diario.

**D-18 · Alternativas de ejercicio** (V-06). Ejercicios activos con el mismo grupo muscular principal, excluyendo el actual, ordenados por cercanía de dificultad. El reemplazo elegido por el paciente se guarda como `reemplazo_ejercicio` (el original queda registrado y visible para el entrenador).

**D-19 · Cuenta corriente del paciente** (V-07, I-21). Cargos (deudas: cuotas, consultas) y pagos. Saldo = Σ cargos − Σ pagos (no anulados). "Fecha del saldo" = vencimiento del cargo impago más antiguo (imputación FIFO). Duplicado (CU-45 alterno) = mismo paciente + concepto + período + monto. Editar/eliminar pagos (SDP paquete 7) se registra en bitácora; eliminar = anulación lógica con motivo.

**D-20 · Aranceles** (V-13). Catálogo simple (concepto, monto, vigente desde). Un cambio de monto puede notificarse por WhatsApp a pacientes activos (RF-13) si el admin lo confirma.

**D-21 · Exportación de reportes** (I-22). Visualización en pantalla + exportación a PDF y Excel (.xlsx).

**D-23 · Comentarios y molestias.** Comentarios de texto ≤ 300 caracteres. Las molestias (CU-42) y comentarios generan un aviso **dentro del sistema** al profesional asignado (badge "Novedades de pacientes"); no se envían por WhatsApp porque RF-13 no lo incluye.

**D-24 · RF-12 Contacto.** Botones en el home del paciente que abren `https://wa.me/<telefono>` para cada profesional asignado y para la central (teléfono configurado en `configuracion_sistema`). El botón "Contacto" del login muestra los datos públicos de la central.

**D-25 · Términos y condiciones** (V-12). Ley 25.326: en el primer ingreso (y ante nueva versión de T&C) el usuario debe aceptar; se registra versión y fecha. Sin aceptación no se accede al sistema.

**D-26 · Recupero de contraseña** (V-09). El usuario ingresa su documento o teléfono; se envía por WhatsApp un enlace con token de un solo uso válido 24 h (CU-48). Si hay SMTP configurado, también por email. El token se guarda hasheado.

## Arquitectura y tecnología

**D-02 · Spring Boot sobre Java 17.** El backlog dice "inicialización con Spring y Maven". Se usa Spring Boot 3.x respetando literalmente las restricciones de diseño:
- *Capas controller / service / DAO*: `@RestController` → `@Service` → interfaces DAO (`*Dao`) implementadas con Spring Data JPA sobre **Hibernate**.
- *Patrón Singleton para acceso a BD*: el `DataSource`/`EntityManagerFactory` es un bean singleton gestionado por Spring (scope por defecto).
- *Factory*: fábrica de generadores de reportes (PDF/Excel) y de adaptadores de notificación.
- *MVC*: el "Modelo" son entidades + servicios, la "Vista" es el frontend desacoplado, el "Controlador" son los REST controllers.

**D-22 · Generación de documentos.** PDFs en servidor con OpenPDF (planes, rutinas, reportes, comprobantes); gráficos para PDF con JFreeChart; Excel con Apache POI. En el navegador, gráficos con Chart.js. Todas gratuitas (SDP §3.1).

**D-27 · Ajuste de secuencia de sprints** (I-25). Sin cambiar la asignación de horas:
- Sprint 1 incluye un *esqueleto de seguridad* (login JWT y roles) porque todos los endpoints lo necesitan; el paquete 13 (S4) completa recupero, cambio de contraseña, bloqueo y registro de accesos.
- Sprint 1 define el puerto `NotificacionService` con un adaptador "log" que encola mensajes; el paquete 11 (S4) conecta el adaptador WhatsApp Cloud API y los jobs.

**D-28 · Bitácora inmutable.** Registro por anotación `@Auditable` (AOP) en servicios + llamadas explícitas para eventos de seguridad. Inmutabilidad doble: la app no expone update/delete y el usuario de BD de la aplicación tiene `DENY UPDATE, DELETE` sobre `bitacora_evento` y `registro_acceso` (más trigger `INSTEAD OF UPDATE, DELETE`). Retención: no se depura nada con menos de 180 días.

**D-29 · Backups** (V-10). Azure SQL Database ya hace backups automáticos con restauración a un punto en el tiempo (cubre "backup diario" y RTO ≤ 2 h). CU-51/52 se implementan sobre un puerto `BackupService` con dos adaptadores: **Azure** (exportación BACPAC a Blob Storage mediante la API de gestión de Azure, descarga por URL SAS temporal) y **local** (`BACKUP DATABASE … TO DISK` para desarrollo). Cada ejecución queda en `backup_registro`.

**D-30 · Seguridad técnica.** Contraseñas con BCrypt (cost 10; era 12 hasta el 19/11/2026, cuando la prueba de carga en Azure mostró ~1 s de CPU por login: los hashes viejos se rehashean al ingresar). JWT firmado HS256, vigencia 8 h, con `jti` revocable al cerrar sesión (CU-50 "borrar token"). Bloqueo tras 5 intentos fallidos durante 15 min. Datos personales sensibles (teléfono, email, domicilio, antecedentes y observaciones de salud) cifrados en columna con AES-256-GCM vía `AttributeConverter` JPA; clave en Azure App Settings / Key Vault. HTTPS obligatorio, CORS restringido al dominio del frontend.

**D-31 · Frontend.** HTML + CSS + JavaScript (módulos ES nativos), sin framework, multipágina por rol, mobile-first. Única librería: Chart.js. Validación en cliente espejo de la del servidor (SDP §4.1).

**D-32 · Tareas programadas.** `@Scheduled` de Spring con zona `America/Argentina/Buenos_Aires`. Requiere App Service con "Always On" (plan B1 o superior; el plan F1 gratuito no lo ofrece). **[Validar presupuesto Azure for Students.]**

**D-33 · Fechas y zona horaria.** Se persiste en `datetime2` en UTC; la API intercambia ISO-8601 con offset; la UI muestra hora Argentina.

**D-34 · Migraciones.** Flyway versiona el esquema (cumple "formato de datos estable" y "gestión de versiones" de RNF 3.5).

**D-35 · Envío confiable de notificaciones.** Patrón *outbox*: cada notificación se inserta en `notificacion` dentro de la misma transacción del evento de negocio y un job la despacha (reintentos: 3, backoff 1/5/15 min). Si falla definitivamente o el destinatario no tiene teléfono, queda `FALLIDA`/`OMITIDA` y se genera alerta para el administrador (CU-37/38/39 flujo alterno).
