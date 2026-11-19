# Prueba de rendimiento (spec 16 · RNF 3.4)

**Criterio:** con 50 usuarios concurrentes sobre un dataset de 500 pacientes y 1 año de historia, el p90 en servidor
debe ser menor a 5 s para login, mi plan, mi rutina, calendario semanal, ficha de paciente y reporte de turnos mensual.

## Resultado en Azure (19/11/2026): cumple

Peor p90: **2,39 s** (login), un 48 % del límite. Errores: 0,04 % (2 de 4.488 requests).

| Operación | Promedio | Mediana | p90 | p95 | Máximo | Criterio p90 < 5 s |
|---|---:|---:|---:|---:|---:|:---:|
| Login | 1,30 s | 909 ms | 2,39 s | 3,84 s | 7,38 s | ✔ |
| Mi plan (paciente) | 572 ms | 300 ms | 1,24 s | 1,99 s | 8,39 s | ✔ |
| Mi rutina (paciente) | 430 ms | 228 ms | 785 ms | 1,62 s | 6,82 s | ✔ |
| Calendario semanal (profesional) | 223 ms | 117 ms | 348 ms | 599 ms | 4,09 s | ✔ |
| Ficha de paciente (profesional) | 328 ms | 136 ms | 399 ms | 821 ms | 8,59 s | ✔ |
| Reporte de turnos mensual | 417 ms | 232 ms | 766 ms | 1,27 s | 4,30 s | ✔ |

- 4.488 requests en 3 min 45 s (19,7 req/s con 50 usuarios virtuales). Resultados: [`resultados-azure.json`](resultados-azure.json).
- **Entorno:** staging (`app-mgnutri-staging-1dfb81`, jar de producción) en App Service **B2** (2 vCPU, compartido con
  producción) y Azure SQL **S2** (50 DTU) en Chile Central. Producción usa B1 y Basic: la base Basic tiene una décima
  parte de las DTU, así que con 50 usuarios simultáneos reales conviene subir de nivel.
- **Datos:** copia de `mgnutri_perf` (500 pacientes, 12 meses: 16.121 turnos, 41.421 ítems de comida, 12.495 registros
  de entrenamiento) cargada con `bcp` en una base temporal `mgnutri-perf`, borrada al terminar.
- **Cliente:** k6 0.54.0 en Docker desde una notebook en Argentina: los tiempos incluyen la latencia de internet hasta
  Chile (≈ 50 ms por request), así que son más exigentes que el criterio «en servidor».

### Defectos encontrados en Azure y corregidos

La primera corrida no cumplía: p90 de 30 a 60 s y 28 % de errores. En la notebook no se veían, porque hay mucha más
CPU que en Azure.

| # | Defecto | Efecto medido | Corrección |
|---|---|---|---|
| 1 | Claves foráneas sin índice (`item_comida.comida_plan_id`, `comida_plan.plan_id`, `dia_rutina.rutina_id`, `ejercicio_rutina.dia_rutina_id` y otras): «mi plan» hace ~35 consultas que recorrían `item_comida` completa | Azure SQL al 100 % de CPU | Migración `V15__indices_claves_foraneas.sql` |
| 2 | El login calculaba BCrypt dentro de la transacción y retenía una conexión del pool (15) | Con ingresos simultáneos se agotaba el pool y el resto esperaba 30 s (timeout de Hikari) | BCrypt antes de abrir la transacción (`AuthService.login`) |
| 3 | BCrypt con costo 12: ~1 s de CPU por login en B2 | Login p90 25,7 s; App Service al 95 % de CPU | Costo 10 (mínimo de OWASP); los hashes anteriores se rehashean en el siguiente login |

Evolución: 1.ª corrida p90 de 30-60 s → con índices y pool, todo cumple salvo el login (25,7 s) → con costo 10, cumple.

## Resultado local (18/11/2026)

**Cumple.** El peor p90 fue 541 ms (mi plan), un 11 % del límite.

| Operación | Promedio | Mediana | p90 | p95 | Máximo | Criterio p90 < 5 s |
|---|---:|---:|---:|---:|---:|:---:|
| Login | 351 ms | 348 ms | 405 ms | 415 ms | 470 ms | ✔ |
| Mi plan (paciente) | 397 ms | 383 ms | 541 ms | 599 ms | 1.010 ms | ✔ |
| Mi rutina (paciente) | 154 ms | 145 ms | 199 ms | 222 ms | 416 ms | ✔ |
| Calendario semanal (profesional) | 53 ms | 49 ms | 73 ms | 83 ms | 161 ms | ✔ |
| Ficha de paciente (profesional) | 76 ms | 72 ms | 101 ms | 114 ms | 149 ms | ✔ |
| Reporte de turnos mensual | 95 ms | 91 ms | 127 ms | 137 ms | 157 ms | ✔ |

- 5.080 requests en 3 min 45 s (22 req/s sostenidos con 50 usuarios virtuales, pausas de 1 a 4 s entre acciones).
- Checks OK: 99,96 % (5.078 de 5.080). Las 2 fallas son logins cortados por k6 al bajar los usuarios virtuales al final de
  la prueba: el servidor los registró como `LOGIN_OK` (231 en `registro_acceso`, ninguno fallido).
- Resultados completos: [`resultados.json`](resultados.json) (`--summary-export` de k6).

## Escenario

- 50 usuarios virtuales (rampa de 30 s, 3 min sostenidos, bajada de 15 s): 70 % pacientes (mi plan → mi rutina) y
  30 % profesionales (calendario semanal → listado → ficha y antropometría; cada 5 iteraciones, reporte de turnos del mes).
- Cada usuario inicia sesión al empezar y luego cada 10 iteraciones (BCrypt incluido en el tiempo de login).

## Dataset

Base `mgnutri_perf` generada con el perfil `demo` y `DEMO_PACIENTES=500 DEMO_MESES=12`:
500 pacientes, 16.121 turnos, 6.500 mediciones antropométricas, 5.485 pagos y 12.495 registros de entrenamiento en 12 meses.
374 pacientes tienen plan y rutina activos ([`pacientes-activos.json`](pacientes-activos.json)) y son los que usa la prueba
(la carga de planes se interrumpió en ese punto; el volumen de historia —lo que pesa en las consultas— está completo).

## Entorno y limitaciones

- Backend (jar de producción, Java 17, `-Xmx1g`) y SQL Server 2022 en Docker, ambos en una notebook de desarrollo con
  Windows 11; k6 0.54.0 en Docker en la misma máquina. La prueba en Azure del 19/11/2026 está arriba (mismo script,
  `-e BASE=https://<app>.azurewebsites.net/api/v1`).
- Al compartir la máquina, cliente y servidor compiten por CPU; los tiempos medidos son conservadores.

## Cómo repetirla

```bash
# 1. Backend con el dataset (base vacía, tarda ~40 min en generarlo)
SPRING_PROFILES_ACTIVE=local,demo DEMO_PACIENTES=500 DEMO_MESES=12 PORT=8091 java -jar backend/target/mgnutri.jar
# 2. Exportar pacientes con plan y rutina activos (consulta más abajo) a pacientes-activos.json
# 3. Ejecutar k6
docker run --rm -v "$PWD/pruebas/rendimiento:/scripts" grafana/k6:0.54.0 run \
  -e BASE=http://host.docker.internal:8091/api/v1 --summary-export /scripts/resultados.json /scripts/mgnutri.k6.js
```

Consulta usada en el paso 2:

```sql
SELECT u.username FROM paciente p JOIN usuario u ON u.id = p.usuario_id
WHERE EXISTS (SELECT 1 FROM plan_alimentario x WHERE x.paciente_id = p.id AND x.estado = 'ACTIVO')
  AND EXISTS (SELECT 1 FROM rutina r WHERE r.paciente_id = p.id AND r.estado = 'ACTIVA')
ORDER BY p.id;
```
