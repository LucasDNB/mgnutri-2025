# Spec 08 · Consulta del plan alimentario y actividad nutricional del paciente

| Campo | Valor |
|---|---|
| Paquete SDP | 8 – Consulta de planes alimentarios |
| RF | RF-09 · RF-15 (nutrición) · RF-12 (contacto, home del paciente) · RF-04 (descarga) |
| CU | CU-15 Consultar plan · CU-16 Descargar (UI paciente) · CU-17 Registrar comidas realizadas · CU-18 Sugerencia de reemplazo · CU-41 Comentario sobre plan · CU-43 Observaciones de hambre/saciedad |
| Sprint | 3 · Backlog: Consultas y descargas de planes alimentarios (3 h), Testeo de planes (3 h) |
| Depende de | 04, 10 |
| Decisiones | D-15, D-23, D-24 |

## Contexto
El paciente entra desde el celular a *Nutrición*, ve su plan de la semana, marca lo que comió, cambia un alimento por un equivalente si lo necesita, deja comentarios y registra cómo se sintió. El nutricionista ve todo eso en la ficha del paciente.

## Historias de usuario

### US-08.0 Home del paciente y contacto (RF-12)
- **CA-08.0.1** Tras el login, el paciente ve el logo y tres botones grandes: **NUTRICIÓN**, **ENTRENAMIENTO**, **CONTACTO** (mockup ERS). Además: próximo turno (si tiene) y acceso a *Mi evolución*, *Mis turnos*, *Mi cuenta*, *Perfil*.
- **CA-08.0.2** *Contacto* lista: la central (nombre, dirección, horario, teléfono) y cada profesional asignado (nombre, especialidad); cada uno con botón **Escribir por WhatsApp** que abre `https://wa.me/{telefono}?text=Hola%20...` en una pestaña/app nueva.
- **CA-08.0.3** Si un profesional no tiene teléfono cargado, su botón no aparece.
- **CA-08.0.4** El botón *Contacto* del login (sin autenticar) muestra sólo los datos de la central.
- **CA-08.0.5** *Nutrición* muestra: Mi plan alimentario · Registro del día · Observaciones diarias · Turnos de nutrición.

### US-08.1 Consultar plan (CU-15)
- **CA-08.1.1** *Nutrición › Mi plan alimentario* muestra el plan activo: en escritorio la grilla semanal completa; en celular el día actual con pestañas L-M-M-J-V-S-D.
- **CA-08.1.2** Cada comida muestra la descripción y, si tiene ítems estructurados, la lista de alimentos con cantidades; los reemplazos activos aparecen con el alimento nuevo y la leyenda "reemplaza a {original}".
- **CA-08.1.3** Se muestran vigencia, profesional y observaciones generales.
- **CA-08.1.4** Sin plan asignado → mensaje "Todavía no tenés un plan alimentario asignado" y vuelve a la pantalla anterior (alterno 1.1).
- **CA-08.1.5** Botón *Descargar PDF* (CU-16, ver spec 04).

### US-08.2 Registrar comidas realizadas (CU-17, RF-15)
- **CA-08.2.1** En *Registro del día* (o en la vista del plan para el día de hoy) cada comida tiene un casillero ✔ "La hice como estaba propuesta".
- **CA-08.2.2** Además, por comida, las marcas opcionales "No me gustó" y "Me cayó mal".
- **CA-08.2.3** Al marcar/desmarcar se guarda inmediatamente (sin botón guardar) y se muestra feedback breve.
- **CA-08.2.4** Se puede registrar el día actual y hasta 2 días anteriores (para olvidos); fechas futuras no.
- **CA-08.2.5** Barra de cumplimiento de la semana (% de comidas marcadas).

### US-08.3 Sugerencia de reemplazo (CU-18)
- **CA-08.3.1** En una comida con ítems estructurados, el botón *Ver alternativa* junto a un alimento muestra la lista de equivalentes del mismo grupo con la cantidad equivalente calculada (D-15). Ej.: "100 g de carne vacuna ≈ 2 huevos (110 g) ≈ 120 g de pollo".
- **CA-08.3.2** Las equivalencias manuales cargadas por el nutricionista aparecen primero; luego las calculadas, ordenadas por cercanía en kcal; máximo 10.
- **CA-08.3.3** Al seleccionar una equivalencia, el sistema guarda el reemplazo para ese ítem (vigente hasta que el paciente lo revierta o el profesional edite el plan) y la vista del plan lo refleja (CU-18 paso 6).
- **CA-08.3.4** *Volver al original* elimina el reemplazo activo.
- **CA-08.3.5** Ítems cargados sólo como texto libre no ofrecen alternativas (se muestra "Consultá con tu nutricionista").
- **CA-08.3.6** El nutricionista ve en la ficha los reemplazos aplicados por el paciente.

### US-08.4 Comentario sobre el plan (CU-41)
- **CA-08.4.1** En el plan, por comida o general del día, *Comentar* abre un campo de texto (máx. 300 caracteres con contador).
- **CA-08.4.2** El comentario queda asociado al día y comida y es visible para el nutricionista asignado; genera un aviso en su bandeja *Novedades* (D-23).
- **CA-08.4.3** Sin plan asignado → la opción está bloqueada con mensaje (alterno 1.1).

### US-08.5 Observaciones diarias de hambre/saciedad (CU-43, RF-15)
- **CA-08.5.1** *Nutrición › Observaciones diarias*: escalas 1–5 de **hambre** y **saciedad** (con íconos/etiquetas), estado de ánimo 1–5 opcional, comentario (≤ 300).
- **CA-08.5.2** Datos mínimos: hambre y saciedad; si faltan, advertencia y no guarda (alterno 1.1).
- **CA-08.5.3** Una observación por día (si existe se edita); historial por fecha visible para el paciente y el nutricionista (requerimiento especial).

### US-08.6 Vista del nutricionista
- **CA-08.6.1** En la ficha, pestaña *Registros*: calendario/tabla de cumplimiento por día y comida, marcas "no me gustó/me cayó mal", observaciones diarias (gráfico de hambre/saciedad) y comentarios.
- **CA-08.6.2** Bandeja *Novedades* (home profesional) con comentarios y molestias no leídos de sus pacientes; marcar como leído.

## Reglas de negocio
- RN-08.1 El paciente sólo ve y registra sobre su plan `ACTIVO`.
- RN-08.2 Equivalencias sólo dentro del mismo grupo de intercambio y entre alimentos activos; cantidad redondeada a 5 g y expresada en medida casera si existe.
- RN-08.3 Registros, comentarios y observaciones del paciente no se auditan en bitácora como "eventos críticos" (son operación habitual), salvo el reemplazo de alimento (`NUTRICION/MODIFICACION`, entidad `ReemplazoAlimento`), porque modifica lo visible del plan.
