# Prueba de rendimiento (spec 16 · RNF 3.4)

**Criterio:** con 50 usuarios concurrentes sobre un dataset de 500 pacientes y 1 año de historia, el p90 en servidor
debe ser menor a 5 s para login, mi plan, mi rutina, calendario semanal, ficha de paciente y reporte de turnos mensual.

**Resultado (18/11/2026): cumple.** El peor p90 fue 541 ms (mi plan), un 11 % del límite.

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
  Windows 11; k6 0.54.0 en Docker en la misma máquina. No es el entorno `dev` de Azure que pide la spec: hay que repetir la
  prueba allí (mismo script, `-e BASE=https://<app>.azurewebsites.net/api/v1`) cuando esté desplegado.
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
