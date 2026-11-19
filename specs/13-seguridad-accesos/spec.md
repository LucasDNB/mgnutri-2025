# Spec 13 · Autenticación, seguridad y registro de accesos

| Campo | Valor |
|---|---|
| Paquete SDP | 13 – Registros de accesos |
| RF | RF-18 Gestión de acceso y autenticación · RF-16 Registro de accesos (ex RNF-1) · RNF 3.3 Seguridad · 3.10 Legales |
| CU | CU-47 Autenticación · CU-48 Recuperar contraseña · CU-49 Cambiar contraseña · CU-50 Cerrar sesión · CU-35 |
| Sprint | 4 · Backlog: Registro y seguridad de accesos al sistema (4 h). **Esqueleto (login JWT + roles) en Sprint 1** (D-27). |
| Depende de | 00, 14 |
| Decisiones | D-03, D-04, D-25, D-26, D-30 |

## Contexto
Todo usuario entra con usuario y contraseña y ve sólo lo que su rol permite. Los accesos quedan registrados de forma inalterable. El sistema cumple la Ley 25.326: aceptación de términos y protección de datos.

## Historias de usuario

### US-13.1 Autenticación (CU-47)
- **CA-13.1.1** La pantalla de login (mockup) tiene logo, *Usuario*, *Contraseña*, **INGRESAR**, **CONTACTO** y "¿Olvidaste tu contraseña?".
- **CA-13.1.2** Con credenciales correctas y usuario `ACTIVO`, el sistema verifica, registra `LOGIN_OK` y redirige al home de su rol (paciente / profesional / admin). Un integral va al home profesional con ambos módulos.
- **CA-13.1.3** Credenciales inválidas → "Usuario o contraseña incorrectos" (mismo mensaje para usuario inexistente y contraseña errónea), registra `LOGIN_FALLIDO` (alterno 1.1).
- **CA-13.1.4** Tras 5 intentos fallidos consecutivos la cuenta se bloquea 15 minutos: "Tu cuenta quedó bloqueada temporalmente. Probá de nuevo en 15 minutos o usá ¿Olvidaste tu contraseña?"; registra `BLOQUEO`. Un login exitoso reinicia el contador.
- **CA-13.1.5** Usuario dado de baja → mismo mensaje que credenciales inválidas.
- **CA-13.1.6** Si `debe_cambiar_password`, tras el login se exige el cambio (CU-49) antes de cualquier otra pantalla. Si no aceptó la versión vigente de T&C, se exige aceptarla (US-13.5).
- **CA-13.1.7** La sesión dura 8 h; al expirar, cualquier acción redirige al login con "Tu sesión expiró".
- **CA-13.1.8** El requerimiento especial "puede incluir captcha o doble autenticación" se cubre con el bloqueo por intentos y rate limit; el captcha/2FA queda como mejora opcional no incluida.

### US-13.2 Recuperar contraseña (CU-48)
- **CA-13.2.1** *¿Olvidaste tu contraseña?* pide documento o número de teléfono.
- **CA-13.2.2** Si existe un usuario activo con ese dato y teléfono válido, se envía por WhatsApp (y por email si está configurado) un enlace con token de un solo uso; se informa "Te enviamos un enlace a tu WhatsApp registrado terminado en ••{últimos 2 dígitos}". Registra `RECUPERO_SOLICITADO`.
- **CA-13.2.3** Si el dato no existe → "No encontramos un usuario con ese dato" (alterno 1.1, tal como pide la ERS). **[Nota de seguridad: esto permite enumerar usuarios; se puede cambiar por un mensaje neutro con `seguridad.recupero-mensaje-neutro=true`.]**
- **CA-13.2.4** El enlace abre *Nueva contraseña* (con confirmación y reglas de complejidad); al guardar se invalida el token, se revocan sesiones activas, se desbloquea la cuenta y se registra `PASSWORD_RESTABLECIDA` + bitácora `CAMBIO_PASSWORD`.
- **CA-13.2.5** El enlace expira a las 24 h y es de un solo uso; un token expirado o usado muestra "El enlace ya no es válido. Pedí uno nuevo."
- **CA-13.2.6** En ningún punto se muestra ni se envía la contraseña anterior (requerimiento especial).
- **CA-13.2.7** Máximo 3 solicitudes de recupero por usuario por hora.

### US-13.3 Cambiar contraseña (CU-49)
- **CA-13.3.1** Desde *Perfil › Cambiar contraseña*: contraseña actual, nueva y confirmación.
- **CA-13.3.2** Contraseña actual incorrecta → error y cancela (alterno 1.1).
- **CA-13.3.3** Verificación de complejidad (requerimiento especial): mínimo 8 caracteres, al menos una letra y un número, distinta de la actual y del número de documento. Indicador visual de fortaleza.
- **CA-13.3.4** Al guardar: nueva contraseña hasheada, `debe_cambiar_password=false`, se revocan las demás sesiones, registra `PASSWORD_CAMBIADA` y bitácora.

### US-13.4 Cerrar sesión (CU-50)
- **CA-13.4.1** *Perfil › Cerrar sesión* (también en el menú superior) cierra la sesión: el token se revoca en el servidor y se borra del navegador (requerimiento especial); redirige al login.
- **CA-13.4.2** Registra `LOGOUT` (RF-16).
- **CA-13.4.3** Un token revocado usado después → 401.

### US-13.5 Términos y condiciones (D-25)
- **CA-13.5.1** En el primer ingreso (o si hay nueva versión vigente) se muestra el texto de T&C y política de privacidad (Ley 25.326: finalidad, datos tratados, derechos de acceso, rectificación y supresión, responsable) con casilla "Leí y acepto".
- **CA-13.5.2** Sin aceptar no se accede; al aceptar se guarda versión y fecha.
- **CA-13.5.3** El admin carga nuevas versiones desde *Opciones avanzadas*.

### US-13.6 Control de acceso por rol (RF-18, RNF 3.3)
- **CA-13.6.1** Cada endpoint verifica rol (matriz `docs/02-arquitectura.md §6`) y pertenencia del recurso. Acceso indebido → 403 (o 404 en recursos de otros pacientes, para no revelar existencia).
- **CA-13.6.2** El frontend oculta lo no permitido y cada página verifica el rol al cargar (router-guard).

### US-13.7 Registro y consulta de accesos (RF-16)
- **CA-13.7.1** Se registran automáticamente: login exitoso, login fallido, bloqueo, logout, solicitud de recupero, restablecimiento y cambio de contraseña, con usuario (o usuario intentado), fecha y hora, rol, IP (si aplica, considerando `X-Forwarded-For` detrás de Azure) y navegador.
- **CA-13.7.2** Los registros no pueden modificarse ni eliminarse por ningún usuario (ni por la app: protección en BD).
- **CA-13.7.3** Sólo administradores pueden consultarlos en *Opciones avanzadas › Accesos*, con filtros por usuario, fecha, acción e IP, y exportar a Excel. **[RF-16 dice "administradores con privilegios especiales": se implementa como permiso `VER_AUDITORIA` que por defecto tienen todos los ADMIN; se puede restringir por configuración.]**
- **CA-13.7.4** Retención mínima: 6 meses (no existe depuración automática de registros con menos de 180 días).

## Reglas de seguridad (RNF 3.3, D-30)
- Contraseñas con BCrypt cost 10 (mínimo de OWASP; ver D-30); nunca se loguean ni se devuelven.
- Datos sensibles cifrados en columna (AES-256-GCM).
- HTTPS obligatorio; cabeceras de seguridad (HSTS, CSP, X-Content-Type-Options, X-Frame-Options).
- Token en `sessionStorage` (no `localStorage`) para que se borre al cerrar la pestaña.
- Rate limit en `/auth/**`: 10 solicitudes/min por IP.
