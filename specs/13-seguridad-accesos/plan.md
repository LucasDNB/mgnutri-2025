# Plan 13 · Autenticación, seguridad y accesos

## Datos
`usuario`, `usuario_rol`, `token_revocado`, `token_recupero` (V13), `registro_acceso` (V2), `terminos_condiciones` (V1).

## API
| Método | Ruta | Acceso | CU |
|---|---|---|---|
| POST | `/auth/login` `{username, password}` → `{token, expiraEn, usuario:{id, nombre, roles, home, debeCambiarPassword, debeAceptarTerminos}}` | público | CU-47 |
| POST | `/auth/logout` | autenticado | CU-50 |
| POST | `/auth/recupero` `{documentoOTelefono}` | público | CU-48 |
| GET | `/auth/recupero/{token}` (valida) | público | CU-48 |
| POST | `/auth/recupero/confirmar` `{token, nuevaPassword}` | público | CU-48 |
| POST | `/auth/cambiar-password` `{actual, nueva}` | autenticado (permitido aun con `debeCambiarPassword`) | CU-49 |
| GET | `/me` | autenticado | — |
| GET | `/terminos/vigentes` · POST `/terminos/aceptar` `{version}` | autenticado | D-25 |
| POST | `/terminos` | ADMIN | D-25 |
| GET | `/accesos?usuario=&accion=&desde=&hasta=&ip=&formato=json|xlsx` | ADMIN (`VER_AUDITORIA`) | RF-16 |

Errores: `CREDENCIALES_INVALIDAS` (401), `USUARIO_BLOQUEADO` (423, con `bloqueadoHasta`), `DEBE_CAMBIAR_PASSWORD` / `DEBE_ACEPTAR_TERMINOS` (428 para cualquier otra ruta), `TOKEN_EXPIRADO` (400 en recupero), `PASSWORD_DEBIL` (400).

## Backend
- `config.SecurityConfig`: `SecurityFilterChain` stateless, `JwtAuthenticationFilter`, `PrimerIngresoFilter` (428), CORS, headers, `@EnableMethodSecurity`.
- `seguridad.service.AuthService` (login con contador de intentos y bloqueo; usa `PasswordEncoder` BCrypt costo 10, calculado fuera de la transacción).
- `seguridad.service.JwtService` (jjwt, HS256, claims `sub, roles, pacienteId, profesionalId, jti, exp`).
- `seguridad.service.TokenRevocacionService` (+ caché en memoria de `jti` revocados con TTL).
- `seguridad.service.RecuperoService` (token aleatorio 32 bytes Base64URL, se guarda SHA-256; envía `RECUPERO_PASSWORD` por outbox; límite 3/h).
- `seguridad.service.PasswordPolicy`.
- `seguridad.service.RegistroAccesoService` (escritura en `REQUIRES_NEW`, IP de `X-Forwarded-For` primer valor confiable).
- `seguridad.service.UsuarioService` (crear con contraseña temporal `SecureRandom` 12 caracteres, dar de baja, reenviar credenciales) — usado por specs 01/02.
- `common.security.CurrentUser` (acceso al usuario autenticado en servicios).
- `RateLimitFilter` (bucket en memoria por IP para `/auth/**`).
- Job `LimpiezaTokensJob` (03:00).

## Frontend
- `index.html` (login + modal contacto + link recupero), `recupero.html`, `recupero-confirmar.html?token=`, `cambiar-password.html` (con medidor de fortaleza), `terminos.html`.
- `js/core/auth.js` (login, logout, `sessionStorage`, decodificación de claims para UI), `js/core/router-guard.js`.
- `admin/accesos.html` (tabla filtrable + exportar).

## Pruebas
- Unit `PasswordPolicyTest`, `JwtServiceTest` (expiración, firma inválida).
- Integración: 5 fallos → 423 y luego, pasado el bloqueo (Clock), login OK; logout → token revocado → 401; `debeCambiarPassword` → 428 en `/pacientes`; recupero: token usado/expirado → error; 4ª solicitud en 1 h → 429.
- Integración: `registro_acceso` registra cada acción; intento de `UPDATE` directo con el usuario de app falla (trigger/DENY).
- Seguridad: matriz de permisos completa como test parametrizado (rol × endpoint representativo → código esperado).
- Postman "13-seguridad" (login de cada rol guarda token en variables de entorno para el resto de la colección).

## Tareas
- [ ] T-13.0 (S1) SecurityConfig, login JWT, roles, `UsuarioService`, `/me`, login.html + guard
- [ ] T-13.1 Bloqueo por intentos, logout con revocación, registro de accesos + consulta admin
- [ ] T-13.2 Recupero por WhatsApp, cambio de contraseña, primer ingreso, T&C
- [ ] T-13.3 Tests de matriz de permisos (total S4: 4 h)
