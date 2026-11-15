# Spec 04 · Gestión de planes alimentarios

| Campo | Valor |
|---|---|
| Paquete SDP | 4 – Gestión de planes alimentarios |
| RF | RF-04 · RF-17 |
| CU | CU-11 Crear · CU-12 Editar · CU-13 Eliminar · CU-14 Generación automática · CU-16 Descargar PDF · CU-35 |
| Sprint | 2 · Backlog: BackEnd (3 h), FrontEnd (2 h), Testeo (1 h) · la consulta del paciente es spec 08 (Sprint 3) |
| Depende de | 01 (asignación), 10 (estructura de alimentos; semilla mínima para Sprint 2) |
| Decisiones | D-07, D-14, D-17, D-22 |

## Contexto
El nutricionista (o integral) diseña para cada paciente asignado un plan semanal personalizado: grilla de 7 días × comidas (desayuno, colación, almuerzo, merienda, cena…), como el "Ejemplo de vista del plan alimentario" de la ERS. Puede cargarlo manualmente comida por comida o pedir al sistema un borrador automático a partir de porcentajes de nutrientes. El paciente sólo puede tener un plan activo.

## Historias de usuario

### US-04.1 Crear plan manual (CU-11)
Como **nutricionista** quiero diseñar un plan para un paciente asignado.
- **CA-04.1.1** Desde *Pacientes › {paciente} › Asignar nuevo plan*, el sistema muestra el editor con los datos del paciente (nombre, edad, objetivo, último peso si existe) y elige modo: **Cargar manualmente** o **Generar automáticamente** (CU-14).
- **CA-04.1.2** Encabezado del plan: nombre (default "Plan {mes año}"), fecha de inicio (default hoy), duración en semanas (1–52), observaciones generales.
- **CA-04.1.3** Editor en grilla: filas = tipos de comida activados por el profesional; columnas = Lunes…Domingo. Cada celda tiene descripción libre y, opcionalmente, ítems estructurados (alimento del catálogo + gramos + medida casera). Si hay ítems, la celda muestra kcal y macros estimados.
- **CA-04.1.4** Acciones de productividad: *Copiar celda/columna*, *Replicar este día a toda la semana*, *Agregar tipo de comida*.
- **CA-04.1.5** Panel de totales por día: kcal, HC, proteínas, grasas (sólo con ítems estructurados).
- **CA-04.1.6** Al **confirmar**, se valida que cada día tenga **al menos 4 comidas** con contenido; el plan queda `ACTIVO` y accesible para el paciente; se registra en bitácora.
- **CA-04.1.7** Si faltan datos requeridos (fecha, duración, comidas mínimas) no se guarda y se indica el error (alterno 1.1, código `PLAN_MIN_4_COMIDAS`).
- **CA-04.1.8** Se puede **guardar borrador** sin validar mínimos; el borrador no es visible para el paciente.
- **CA-04.1.9** Si el paciente ya tiene un plan `ACTIVO`, al confirmar se advierte "El plan actual (desde {fecha}) se finalizará" y, al aceptar, el anterior pasa a `FINALIZADO` (D-17).
- **CA-04.1.10** Un nutricionista no asignado al paciente en el área NUTRICION recibe `PACIENTE_NO_ASIGNADO`.

### US-04.2 Generar plan automático (CU-14)
Como **nutricionista** quiero que el sistema proponga un plan a partir de parámetros, para ahorrar tiempo, y revisarlo antes de asignarlo.
- **CA-04.2.1** Parámetros: kcal diarias objetivo (800–5000), % HC, % proteínas, % grasas (cada uno 5–80 y suma = 100), tipos de comida (≥ 4) con % de distribución calórica (suma = 100; defaults según cantidad), alimentos a excluir (búsqueda en catálogo), variedad: "menú distinto cada día" o "mismo menú toda la semana".
- **CA-04.2.2** Al pedir generación, el sistema devuelve un **borrador** en el editor (mismo formato que el manual) con totales por día y un indicador de desvío por comida y por día (verde ≤ 10 % kcal y ≤ 15 % macros; amarillo hasta el doble; rojo más).
- **CA-04.2.3** El profesional puede regenerar (otra combinación), editar celdas a mano y finalmente confirmar; recién al confirmar se guarda y se otorga acceso al paciente (CU-14 pasos 3–4).
- **CA-04.2.4** Si el catálogo no tiene alimentos suficientes para algún grupo/comida, la generación igual completa lo posible y marca las celdas vacías "Completar manualmente".
- **CA-04.2.5** El plan guarda `modo = AUTOMATICO` y los parámetros usados.

### US-04.3 Editar plan (CU-12)
- **CA-04.3.1** *Modificar plan actual* abre el editor con el plan activo (o un borrador existente).
- **CA-04.3.2** Al confirmar se aplican las mismas validaciones que en la creación; queda disponible para el paciente con los cambios; bitácora `MODIFICACION` con resumen (celdas cambiadas).
- **CA-04.3.3** Datos incompletos → no guarda (alterno 1.1).
- **CA-04.3.4** Los registros de cumplimiento del paciente (spec 08) asociados a comidas que se modifican se conservan; si una comida se quita, sus registros históricos se mantienen ligados a la versión anterior (la comida queda inactiva, no se borra).
- **CA-04.3.5** Los reemplazos que el paciente haya aplicado (CU-18) sobre ítems que el profesional cambia se desactivan y se avisa al profesional antes de confirmar.

### US-04.4 Eliminar plan (CU-13)
- **CA-04.4.1** *Eliminar plan actual* pide confirmación; al confirmar el plan pasa a `ELIMINADO`, deja de ser accesible para el paciente y se registra en bitácora.
- **CA-04.4.2** El plan eliminado no se muestra al profesional en la ficha salvo en *Historial de planes* (filtrable) y en reportes (RF-14).

### US-04.5 Descargar plan en PDF (CU-16)
- **CA-04.5.1** El paciente (desde *Mi plan alimentario*, spec 08) y el profesional (desde la ficha) pueden descargar el plan activo en PDF.
- **CA-04.5.2** El PDF (A4 apaisado) contiene: logo y datos del centro, nombre del paciente, profesional y matrícula, vigencia, grilla semanal como en el mockup, reemplazos activos elegidos por el paciente señalados con "(reemplazo)", observaciones y la leyenda "Este plan fue elaborado por un profesional del centro. No reemplaza la consulta médica."
- **CA-04.5.3** Sin plan asignado → "No tenés un plan alimentario asignado" y vuelve a la pantalla anterior (alterno 1.1).
- **CA-04.5.4** Cada descarga registra `NUTRICION/DESCARGA` en bitácora.
- **CA-04.5.5** Tiempo de generación < 3 s para un plan completo.

### US-04.6 Historial
- **CA-04.6.1** En la ficha del paciente, pestaña *Planes*: plan activo, borradores e historial (finalizados y eliminados), con fechas y profesional. Los finalizados/eliminados son de sólo lectura y se pueden **duplicar como base** de un nuevo plan.

## Reglas de negocio
- RN-04.1 Sólo usuarios con rol NUTRICIONISTA asignados al paciente en el área NUTRICION gestionan sus planes (RF-04).
- RN-04.2 Un único plan `ACTIVO` por paciente (RF-04); sin límite de borradores.
- RN-04.3 Cada día del plan debe tener ≥ 4 comidas con contenido (RF-04). Tipos de comida distintos por día.
- RN-04.4 `fecha_fin = fecha_inicio + duracion_semanas·7 − 1`. Un job diario pasa a `FINALIZADO` los planes con `fecha_fin < hoy`.
- RN-04.5 Fecha de inicio no anterior a 30 días atrás.
- RN-04.6 Ítems: gramos 1–2000; alimento activo del catálogo.
- RN-04.7 La generación automática nunca asigna sin confirmación humana (Constitución art. II).

## Algoritmo de generación (D-14)
Ver plan técnico §Generador. Resumen: distribuye kcal y macros por comida; para cada comida usa una plantilla de grupos (desayuno/merienda: lácteo + cereal + fruta; colación: fruta o lácteo o frutos secos; almuerzo/cena: carne o huevo + cereal/legumbre + verdura + aceite); elige alimentos aptos para ese tipo de comida, no excluidos, rotando para dar variedad; dimensiona porciones en orden proteína → hidratos → grasas para aproximar los objetivos; redondea a 5 g y a medidas caseras cuando existen.

## Bitácora
`NUTRICION` · ALTA (confirmación de plan) / MODIFICACION / BAJA (eliminación) / DESCARGA (PDF) sobre `PlanAlimentario`. Generar borrador no se audita (no persiste).
