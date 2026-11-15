# Spec 10 · Base de datos de alimentos y ejercicios

| Campo | Valor |
|---|---|
| Paquete SDP | 10 – Base de datos de alimentos y ejercicios |
| RF | RF-11 · RF-17 |
| CU | CU-53A Alta de alimento · CU-53B Modificación · CU-53C Baja · CU-54A Alta de ejercicio · CU-54B Modificación · CU-54C Baja · CU-35 |
| Sprint | 3 · Backlog: Base de datos de ejercicios (4 h), Base de datos de alimentos (4 h). **Estructura (tablas + semilla mínima) adelantada a Sprint 2** porque specs 04/05 la usan (ver modelo de datos §13). |
| Depende de | 00, 14 |
| Decisiones | D-15, D-18 |

## Contexto
Catálogos editables que alimentan los planes (y sus equivalencias) y las rutinas (y sus alternativas). Los mantienen los profesionales con permisos de edición y el administrador.

## Historias — Alimentos (CU-53A/B/C)

### US-10.1 Alta de alimento (CU-53A)
- **CA-10.1.1** *Base de datos de alimentos › Nuevo alimento*: nombre, grupo de intercambio, valores por 100 g (kcal, HC, proteínas, grasas, fibra; sodio opcional), medida casera y sus gramos (opcional), aptitud por tipo de comida (desayuno/merienda, colación, almuerzo/cena), equivalencias manuales (alimento + gramos origen + gramos equivalente).
- **CA-10.1.2** Al guardar, el alimento queda disponible para planes y sugerencias (poscondición).
- **CA-10.1.3** Faltan obligatorios (nombre, grupo, kcal, HC, proteínas, grasas) → bloquea y muestra error (alterno 1.1).
- **CA-10.1.4** Validación de unidades (requerimiento especial): valores en g/100 g entre 0 y 100, kcal entre 0 y 900, suma HC+P+G+fibra ≤ 100; si las kcal difieren más de 15 % de 4·HC + 4·P + 9·G se advierte "Las calorías no coinciden con los macronutrientes" (permite confirmar).
- **CA-10.1.5** Nombre único (sin distinguir mayúsculas/acentos).
- **CA-10.1.6** Bitácora `CATALOGO/ALTA`.

### US-10.2 Modificación de alimento (CU-53B)
- **CA-10.2.1** Edición con las mismas validaciones; los planes existentes recalculan totales con los nuevos valores al visualizarse (los valores no se copian en el plan).
- **CA-10.2.2** Bitácora `MODIFICACION`.

### US-10.3 Baja de alimento (CU-53C)
- **CA-10.3.1** Baja lógica (`activo=false`) con confirmación; ya no se ofrece en editores, generador ni alternativas, pero los planes que lo contienen siguen mostrándolo.
- **CA-10.3.2** Se puede reactivar. Bitácora `BAJA`.

## Historias — Ejercicios (CU-54A/B/C)

### US-10.4 Alta de ejercicio (CU-54A)
- **CA-10.4.1** *Base de datos de ejercicios › Nuevo ejercicio*: nombre, descripción/técnica, grupo muscular principal, grupos secundarios, dificultad (baja/media/alta), equipamiento, enlace a video.
- **CA-10.4.2** Faltan obligatorios (nombre, grupo principal, dificultad) → bloquea y muestra error (alterno 1.1).
- **CA-10.4.3** El enlace de video, si se carga, debe ser una URL `https://` válida; se ofrece *Probar enlace* (abre en pestaña nueva).
- **CA-10.4.4** Queda disponible para rutinas; bitácora `ALTA` (CU-54 incluye CU-35).

### US-10.5 Modificación de ejercicio (CU-54B)
- **CA-10.5.1** Igual validación; las rutinas reflejan los cambios de nombre/video. Bitácora.

### US-10.6 Baja de ejercicio (CU-54C)
- **CA-10.6.1** Baja lógica igual que alimentos; no se ofrece en editor ni alternativas. Bitácora.

## Historias — Consulta
- **CA-10.7.1** Listados con búsqueda por nombre, filtro por grupo (alimentos) o grupo muscular/dificultad (ejercicios), estado activo/inactivo, paginados.
- **CA-10.7.2** Ayuda contextual en cada campo nutricional ("valores cada 100 g de alimento crudo/listo para consumir").

## Permisos (RF-11 "actualizada por los profesionales")
| | ADMIN | NUTRICIONISTA | ENTRENADOR |
|---|---|---|---|
| Alimentos | ABM | ABM | lectura |
| Ejercicios | ABM | lectura | ABM |

## Semilla inicial
- ≥ 150 alimentos de uso habitual en Argentina con valores de referencia de tablas públicas de composición química de alimentos (p. ej. tabla de composición de alimentos de la ANMAT/Argenfoods o USDA), en un CSV `seed/alimentos.csv` con la fuente citada.
- 9 grupos de intercambio y 14 grupos musculares (ver modelo de datos §4).
- ≥ 80 ejercicios (≥ 4 por grupo muscular principal para que haya alternativas) en `seed/ejercicios.csv`, con video cuando exista un enlace público y estable.
- La carga se hace por migración Flyway V12 (repetible `R__seed_catalogo.sql` para no duplicar).
