# Spec 05 · Gestión de rutinas de entrenamiento

| Campo | Valor |
|---|---|
| Paquete SDP | 5 – Gestión de rutinas de entrenamiento |
| RF | RF-05 (ver D-01) · RF-17 |
| CU | CU-21 Crear · CU-22 Editar · CU-23 Eliminar · CU-27 Descargar PDF · CU-35 |
| Sprint | 2 · Backlog: BackEnd (3 h), FrontEnd (2 h), Testeo (1 h) |
| Depende de | 01, 10 (estructura de ejercicios) |
| Decisiones | D-01, D-07, D-17, D-22 |

## Contexto
El entrenador (o integral) arma rutinas personalizadas para sus pacientes asignados: días de entrenamiento con ejercicios del catálogo, grupo muscular, series, repeticiones y enlace a video. Un paciente tiene una sola rutina activa.

## Historias de usuario

### US-05.1 Crear rutina (CU-21)
- **CA-05.1.1** Desde *Pacientes › {paciente} › Nueva rutina* se abre el formulario con los datos del paciente.
- **CA-05.1.2** Encabezado: nombre, objetivo (texto), fecha de inicio, duración en semanas (1–52), observaciones.
- **CA-05.1.3** Días: el entrenador agrega días de entrenamiento (día de la semana + nombre, p. ej. "Lunes – Tren superior"); 1 a 7 días.
- **CA-05.1.4** En cada día agrega ejercicios **sólo desde el catálogo** (requerimiento especial CU-21) buscando por nombre o filtrando por grupo muscular; por cada uno: series (1–20), repeticiones (texto corto: "12", "8-10", "30 s", "al fallo"), descanso en segundos (opcional), carga sugerida (opcional), notas. Se muestran grupo muscular y si tiene video.
- **CA-05.1.5** Reordenar ejercicios (subir/bajar) y duplicar un día.
- **CA-05.1.6** Al confirmar: cada día con ≥ 1 ejercicio, al menos 1 día; la rutina queda `ACTIVA` y accesible para el paciente; bitácora `ALTA`.
- **CA-05.1.7** Si el paciente ya tiene rutina activa, misma confirmación y finalización que D-17 (`RUTINA_ACTIVA_EXISTENTE`).
- **CA-05.1.8** Entrenador no asignado en área ENTRENAMIENTO → `PACIENTE_NO_ASIGNADO`.

### US-05.2 Editar rutina (CU-22)
- **CA-05.2.1** Si el paciente no tiene rutina asignada, la opción *Editar rutina* aparece bloqueada (alterno 1.1).
- **CA-05.2.2** Se edita con el mismo formulario; al confirmar, la rutina editada queda disponible; bitácora `MODIFICACION`.
- **CA-05.2.3** Los registros del paciente (cumplimiento, cargas, comentarios) de ejercicios que se quitan se conservan (ejercicio_rutina inactivo).
- **CA-05.2.4** Si el paciente aplicó un reemplazo (CU-26) sobre un ejercicio que el entrenador cambia, el reemplazo se desactiva con aviso previo.

### US-05.3 Eliminar rutina (CU-23)
- **CA-05.3.1** Advertencia y confirmación; al confirmar, `ELIMINADA`, el paciente pierde acceso; bitácora `BAJA`.
- **CA-05.3.2** Cancelar → no se elimina (alterno 1.1).

### US-05.4 Descargar rutina en PDF (CU-27)
- **CA-05.4.1** Paciente (desde *Mi rutina*) y entrenador descargan la rutina activa en PDF.
- **CA-05.4.2** El PDF muestra la rutina semanal por día: ejercicio, grupo muscular, series × repeticiones, descanso, carga sugerida, notas y **enlace clickeable al video** cuando exista (requerimiento especial CU-27). Reemplazos activos marcados.
- **CA-05.4.3** Sin rutina → el acceso a "Mi rutina" está bloqueado (alterno 1.1).
- **CA-05.4.4** Bitácora `ENTRENAMIENTO/DESCARGA`.

### US-05.5 Historial
- **CA-05.5.1** Pestaña *Rutinas* en la ficha: activa, historial (finalizadas/eliminadas) en sólo lectura, con opción *Duplicar como base*.

## Reglas de negocio
- RN-05.1 Sólo rol ENTRENADOR asignado al paciente en el área ENTRENAMIENTO gestiona su rutina (RF-05).
- RN-05.2 Una rutina `ACTIVA` por paciente (RF-05).
- RN-05.3 Fechas y vencimiento igual que RN-04.4/RN-04.5 (job diario finaliza vencidas).
- RN-05.4 Un mismo ejercicio puede repetirse en distintos días pero no dos veces seguidas en el mismo día (advertencia, no bloqueo).
- RN-05.5 Ejercicios inactivos del catálogo no se pueden agregar; si un ejercicio se inactiva, las rutinas existentes lo conservan.

## Bitácora
`ENTRENAMIENTO` · ALTA / MODIFICACION / BAJA / DESCARGA sobre `Rutina`.
