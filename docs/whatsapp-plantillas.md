# Plantillas de WhatsApp para cargar en Meta

Cargar en *WhatsApp Manager › Plantillas de mensajes › Crear plantilla*, una por una:

- **Categoría:** Utilidad (*Utility*).
- **Nombre:** exactamente el de la tabla (el sistema las pide por ese nombre).
- **Idioma:** Español (ARG), código `es_AR`.
- **Tipo de variable:** número (`{{1}}`, `{{2}}`…), solo en el cuerpo, sin encabezado ni botones.

El orden de las variables es el que arma `Notificador.java`: si se cambia un texto en Meta hay que mantener la misma
cantidad y el mismo orden.

Meta rechaza un cuerpo que empieza o termina con una variable (error 2388299). Por eso todos los textos cierran con una
frase fija, y no hay dos variables seguidas sin texto entre ellas.

## 1. `mg_credenciales`

> Hola {{1}}, ya estás registrado en MG Nutrición y Entrenamiento con el usuario {{2}}. Para empezar a usar el sistema completá tu alta en {{3}} (el enlace vence en 72 h). ¡Te esperamos!

Ejemplos: `Valeria` · `38043292` · `https://app-mgnutri-1dfb81.azurewebsites.net/recupero-confirmar.html?token=abc123`

Requiere `WA_CREDENCIALES_MODO=ENLACE` (enlace de activación de un solo uso, sin contraseña). La versión con la
contraseña temporal («…tu contraseña temporal es {{3}}…», 4 variables, modo `PASSWORD`) fue rechazada por Meta el
19/11/2026 con `INCORRECT_CATEGORY`: la considera de *Autenticación*, categoría que solo admite códigos de verificación.

## 2. `mg_turno_confirmado`

> Hola {{1}}, tu turno con {{2}} quedó confirmado para el {{3}} a las {{4}} en {{5}}. Si no podés asistir, cancelalo desde el sistema.

Ejemplos: `Valeria` · `Lic. Carolina Paz` · `martes 25/11/2026` · `10:30` · `Consultorio 2`

## 3. `mg_turno_reprogramado`

> Hola {{1}}, tu turno con {{2}} del {{3}} fue reprogramado para el {{4}} a las {{5}} en {{6}}. Podés ver el detalle en el sistema.

Ejemplos: `Valeria` · `Lic. Carolina Paz` · `martes 25/11/2026 10:30` · `jueves 27/11/2026` · `11:00` · `Consultorio 2`

## 4. `mg_turno_cancelado`

> Hola {{1}}, tu turno con {{2}} del {{3}} a las {{4}} fue cancelado. Motivo: {{5}}. Podés pedir uno nuevo desde el sistema.

Ejemplos: `Valeria` · `Lic. Carolina Paz` · `martes 25/11/2026` · `10:30` · `licencia del profesional`

## 5. `mg_recordatorio_turno`

> Hola {{1}}, te recordamos tu turno de mañana {{2}} a las {{3}} con {{4}} en {{5}}. Si no podés asistir, avisanos con anticipación.

Ejemplos: `Valeria` · `martes 25/11/2026` · `10:30` · `Lic. Carolina Paz` · `Consultorio 2`

## 6. `mg_recordatorio_deuda`

> Hola {{1}}, registrás un saldo pendiente de {{2}} en MG Nutrición y Entrenamiento. {{3}} Muchas gracias.

Ejemplos: `Valeria` · `$ 18.500,00` · `Acercate a la central para regularizarlo.`

## 7. `mg_cambio_arancel`

> Hola {{1}}, te informamos que desde el {{2}} el valor de {{3}} será de {{4}}. Ante cualquier duda, consultá en la central.

Ejemplos: `Valeria` · `01/12/2026` · `Consulta nutricional` · `$ 25.000,00`

## 8. `mg_recupero_password`

> Hola {{1}}, este es el enlace que pediste en MG Nutrición y Entrenamiento: {{2}}. Vence en 24 h. Si no lo pediste, ignorá este mensaje.

Ejemplos: `Valeria` · `https://app-mgnutri-1dfb81.azurewebsites.net/recupero-confirmar.html?token=…`

Con «restablecer tu contraseña» en el texto Meta la rechazó por la misma razón (`INCORRECT_CATEGORY`).
