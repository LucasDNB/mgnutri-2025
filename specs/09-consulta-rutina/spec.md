# Spec 09 · Consulta de la rutina y actividad física del paciente

| Campo | Valor |
|---|---|
| Paquete SDP | 9 – Consulta de rutina de entrenamiento |
| RF | RF-10 · RF-15 (entrenamiento) · RF-05 (descarga) |
| CU | CU-24 Consultar rutina · CU-25 Registrar rutina completada · CU-26 Alternativas de ejercicios · CU-27 Descargar (UI paciente) · CU-28 Video del ejercicio · CU-40 Comentario sobre rutina · CU-42 Molestias físicas |
| Sprint | 3 · Backlog: Consulta de rutina de entrenamiento (3 h), Testeo de rutinas (3 h) |
| Depende de | 05, 10 |
| Decisiones | D-18, D-23 |

## Contexto
El paciente usa *Entrenamiento* durante la sesión en el gimnasio: ve los ejercicios del día, abre el video si tiene dudas, tilda lo que va completando, anota la carga que usó, pide una alternativa si una máquina está ocupada y reporta molestias. El entrenador sigue todo desde la ficha.

## Historias de usuario

### US-09.0 Menú Entrenamiento
- **CA-09.0.1** *Entrenamiento* muestra: Mi rutina · Registrar molestia · Turnos de entrenamiento (si el entrenador tiene agenda).
- **CA-09.0.2** Sin rutina asignada, *Mi rutina* aparece bloqueado con la leyenda "Todavía no tenés una rutina asignada" (alterno 1.1 de CU-24/25/26/27/28).

### US-09.1 Consultar rutina (CU-24)
- **CA-09.1.1** *Mi rutina* muestra la rutina activa: nombre, objetivo, vigencia, entrenador, observaciones y los días de entrenamiento.
- **CA-09.1.2** Filtro **día / semana / mes**: *día* muestra la sesión de hoy (o la próxima), *semana* todos los días de la rutina, *mes* un calendario con los días programados y su estado de cumplimiento.
- **CA-09.1.3** Cada ejercicio muestra nombre, grupo muscular, series × repeticiones, descanso, carga sugerida, notas, ícono de video si lo tiene y la **última carga registrada** por el paciente para ese ejercicio (RF-15 "consultarlas cuando repita el ejercicio").
- **CA-09.1.4** Botón *Descargar PDF* (CU-27, spec 05).

### US-09.2 Registrar rutina completada (CU-25, RF-15)
- **CA-09.2.1** En la sesión del día, cada ejercicio tiene casilla ✔ "Completado" y campos opcionales: carga utilizada (kg), series y repeticiones realizadas.
- **CA-09.2.2** Se guarda al marcar/editar (sin botón guardar). Se puede registrar hoy y hasta 2 días anteriores.
- **CA-09.2.3** Al completar todos los ejercicios del día se muestra "¡Sesión completa!" y el día figura como cumplido en la vista mes.
- **CA-09.2.4** El historial de cargas por ejercicio alimenta el gráfico de progreso (spec 06).

### US-09.3 Alternativas de ejercicios (CU-26)
- **CA-09.3.1** En un ejercicio, *Pedir variante* muestra ejercicios del catálogo que trabajan el **mismo grupo muscular principal**, excluyendo el actual, ordenados por dificultad más cercana; con grupo, dificultad, equipamiento y video (D-18).
- **CA-09.3.2** Al seleccionar uno, se registra el reemplazo y la rutina se muestra con el ejercicio nuevo (manteniendo series/repeticiones del original) con la leyenda "variante de {original}" (CU-26 pasos 6–7).
- **CA-09.3.3** *Volver al original* revierte. El entrenador ve los reemplazos en la ficha.
- **CA-09.3.4** Si no hay alternativas: "No hay variantes cargadas para este grupo muscular".

### US-09.4 Video explicativo (CU-28)
- **CA-09.4.1** En el detalle del ejercicio, *Ver video* abre el enlace en una pestaña nueva / navegador predeterminado (`target="_blank" rel="noopener"`).
- **CA-09.4.2** Si el ejercicio no tiene video, el botón no se muestra (alterno 2.1).

### US-09.5 Comentario sobre la rutina (CU-40)
- **CA-09.5.1** En la rutina (general del día o por ejercicio), *Comentar* con texto ≤ 300 caracteres.
- **CA-09.5.2** El comentario queda asociado al día y ejercicio, visible para el entrenador y genera aviso en *Novedades*.
- **CA-09.5.3** Sin rutina activa → opción bloqueada con mensaje (alterno 1.1).

### US-09.6 Registrar molestias físicas (CU-42)
- **CA-09.6.1** *Registrar molestia*: descripción (obligatoria, ≤ 300), **intensidad 1–10** (obligatoria, escala visual), ubicación corporal (lista: cuello, hombro izq./der., espalda alta, zona lumbar, codo, muñeca, cadera, rodilla izq./der., tobillo, otra), contexto (entrenamiento / alimentación / general), ejercicio relacionado (opcional, de la rutina).
- **CA-09.6.2** También accesible desde un ejercicio de la rutina (precarga el ejercicio).
- **CA-09.6.3** Campos obligatorios faltantes → advertencia y no guarda (alterno 1.1).
- **CA-09.6.4** Al guardar, la molestia se notifica al profesional asignado del área del contexto (entrenador para entrenamiento, nutricionista para alimentación, ambos para general) mediante aviso en *Novedades*; con intensidad ≥ 7 el aviso se marca como prioritario (D-23).
- **CA-09.6.5** El paciente ve su historial de molestias.

### US-09.7 Vista del entrenador
- **CA-09.7.1** En la ficha, pestaña *Registros de entrenamiento*: cumplimiento por día, cargas por ejercicio, reemplazos, comentarios y molestias (con intensidad), filtrable por período.

## Reglas de negocio
- RN-09.1 Registros sólo sobre la rutina `ACTIVA` y ejercicios activos de ella.
- RN-09.2 Carga 0–500 kg (0,5 de precisión); repeticiones 0–200; series 0–20.
- RN-09.3 El sistema no interpreta las molestias ni sugiere tratamientos (PdS).
- RN-09.4 Reemplazo de ejercicio se audita (`ENTRENAMIENTO/MODIFICACION`, `ReemplazoEjercicio`).
