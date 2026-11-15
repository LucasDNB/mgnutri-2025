# Spec 06 · Evolución del paciente

| Campo | Valor |
|---|---|
| Paquete SDP | 6 – Evolución del paciente |
| RF | RF-06 · RF-15 (cargas) · RF-17 |
| CU | CU-19 Registrar evolución · CU-20 Visualizar evolución · CU-35 |
| Sprint | 2 · Backlog: Carga y vistas de evolución de desempeño físico (2 h), Carga y vistas de evolución nutricional / composición corporal (2 h) |
| Depende de | 01; 09 para el gráfico de cargas (se habilita en S3) |
| Decisiones | D-16 |

## Contexto
En cada consulta el nutricionista registra la antropometría del paciente y el entrenador las evaluaciones de desempeño. Con esos datos el sistema muestra gráficos y tablas de evolución al profesional y al propio paciente, en pantalla o en PDF (reporte en spec 12).

## Historias de usuario

### US-06.1 Registrar antropometría (CU-19, nutricionista)
- **CA-06.1.1** *Pacientes › {paciente} › Cargar antropometría* muestra la planilla con: fecha (default hoy), peso (kg), talla (cm, precargada con la última), % grasa, % músculo, perímetros (cintura, cadera, brazo, muslo, pecho, pantorrilla) y observaciones.
- **CA-06.1.2** Obligatorios: fecha y peso. El IMC se calcula y muestra en vivo (peso / talla²).
- **CA-06.1.3** Valores fuera de rango (tabla RN-06.1) → el sistema muestra error en el campo y no guarda (alterno 1.1, `VALOR_FUERA_DE_RANGO`).
- **CA-06.1.4** Si la variación de peso respecto de la medición anterior supera 10 % se muestra advertencia "Variación inusual, verificá el dato" pero permite guardar.
- **CA-06.1.5** Al guardar, la medición queda registrada, visible en evolución y reportes; bitácora `EVOLUCION/ALTA`.
- **CA-06.1.6** Se puede editar o eliminar una medición dentro de las 48 h de cargada, por el mismo profesional (bitácora). Luego es inmutable.
- **CA-06.1.7** Sólo una medición por paciente y fecha por profesional (si existe, se ofrece editarla).

### US-06.2 Registrar evaluación de desempeño (entrenador)
- **CA-06.2.1** *Cargar evaluación física*: fecha, prueba (texto o elegir ejercicio del catálogo), valor, unidad (kg, reps, s, min, m, cm, km/h, otra), observaciones. Permite cargar varias pruebas en un mismo formulario.
- **CA-06.2.2** Validaciones: valor ≥ 0 y ≤ 10 000; prueba ≤ 80 caracteres. Mismas reglas de edición que CA-06.1.6.
- **CA-06.2.3** Bitácora `EVOLUCION/ALTA`.

### US-06.3 Visualizar evolución (CU-20)
- **CA-06.3.1** Profesional asignado (desde la ficha) o paciente (desde *Mi evolución*) acceden a la vista de evolución; requiere al menos una medición (si no hay: "Todavía no hay mediciones registradas").
- **CA-06.3.2** Filtros: período (último mes, 3 meses, 6 meses, 1 año, todo, rango personalizado).
- **CA-06.3.3** Gráficos (RF-06): **evolución de peso**; **composición corporal** (% grasa y % músculo, dos series); **tallas/medidas** (una serie por perímetro, seleccionables); **desempeño** por prueba; **progreso de cargas** por ejercicio a partir de los registros del paciente (spec 09).
- **CA-06.3.4** Tabla con todas las mediciones del período y diferencias contra la anterior y contra la primera (Δ y %).
- **CA-06.3.5** Legible en celular (gráficos responsivos, tabla con scroll horizontal) — requerimiento especial CU-20.
- **CA-06.3.6** Botón *Descargar PDF* → reporte de evolución (CU-32, spec 12).
- **CA-06.3.7** Nutricionista ve antropometría y desempeño; entrenador ve ambos (lectura de antropometría); el paciente ve ambos sobre sí mismo.

## Reglas de negocio
**RN-06.1 Rangos válidos (V-11)**
| Campo | Mín | Máx |
|---|---|---|
| Peso (kg) | 20 | 300 |
| Talla (cm) | 100 | 230 |
| % grasa | 3 | 60 |
| % músculo | 10 | 70 |
| Perímetros (cm) | 10 | 200 |
| Fecha | no futura | no anterior a 5 años |

- RN-06.2 `% grasa + % músculo ≤ 100`.
- RN-06.3 Antropometría: rol NUTRICIONISTA asignado. Evaluación física: rol ENTRENADOR asignado.
- RN-06.4 El sistema no interpreta los valores clínicamente (sin semáforos de "sobrepeso" etc.) — PdS: no diagnostica. Sólo muestra el IMC calculado.
