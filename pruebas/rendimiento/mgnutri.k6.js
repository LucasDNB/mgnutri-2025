// Prueba de rendimiento (spec 16 · RNF 3.4): 50 usuarios concurrentes sobre un dataset de 500 pacientes / 1 año.
// Criterio: p90 < 5 s en servidor para login, mi plan, mi rutina, calendario semanal, ficha de paciente y reporte de turnos mensual.
//
// Dataset: backend con SPRING_PROFILES_ACTIVE=local,demo DEMO_PACIENTES=500 DEMO_MESES=12 sobre una base vacía;
// pacientes-activos.json = usuarios con plan y rutina activos (ver README.md).
// Ejecución (Docker, sin instalar k6):
//   docker run --rm -v "$PWD/pruebas/rendimiento:/scripts" grafana/k6:0.54.0 run \
//     -e BASE=http://host.docker.internal:8091/api/v1 --summary-export /scripts/resultados.json /scripts/mgnutri.k6.js
import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';

const BASE = __ENV.BASE || 'http://localhost:8090/api/v1';
const PASSWORD = __ENV.DEMO_PASSWORD || 'Demo2025!';
// Pacientes con plan y rutina activos (pacientes-activos.json, exportado de la base de prueba).
const PACIENTES = new SharedArray('pacientes', () => JSON.parse(open('./pacientes-activos.json')));
const PROFESIONALES = ['20111111', '20222222', '20333333', '20444444', '20555555'];
const OPERACIONES = ['login', 'mi_plan', 'mi_rutina', 'calendario_semanal', 'ficha_paciente', 'reporte_turnos_mensual'];

export const options = {
  scenarios: {
    carga: { executor: 'ramping-vus', startVUs: 0, stages: [
      { duration: '30s', target: 50 }, { duration: '3m', target: 50 }, { duration: '15s', target: 0 }] },
  },
  thresholds: Object.fromEntries([
    ...OPERACIONES.map((op) => [`http_req_duration{op:${op}}`, ['p(90)<5000']]),
    ['http_req_failed', ['rate<0.01']],
    ['checks', ['rate>0.99']],
  ]),
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'max'],
};

const json = { headers: { 'Content-Type': 'application/json' } };
let token = null;
let iteracion = 0;
let pacientesDelProfesional = [];

function esProfesional() { return __VU % 10 < 3; } // 30 % profesionales, 70 % pacientes

function usuario() {
  return esProfesional() ? PROFESIONALES[__VU % PROFESIONALES.length] : PACIENTES[(__VU * 37) % PACIENTES.length];
}

function login() {
  const r = http.post(`${BASE}/auth/login`, JSON.stringify({ username: usuario(), password: PASSWORD }), { ...json, tags: { op: 'login' } });
  check(r, { 'login 200': (x) => x.status === 200 });
  token = r.status === 200 ? r.json('token') : null;
}

function get(ruta, op) {
  const r = http.get(`${BASE}${ruta}`, { headers: { Authorization: `Bearer ${token}` }, tags: { op } });
  check(r, { [`${op} 200`]: (x) => x.status === 200 });
  return r;
}

function lunes(fecha) {
  const d = new Date(fecha);
  d.setUTCDate(d.getUTCDate() - ((d.getUTCDay() + 6) % 7));
  return d.toISOString().slice(0, 10);
}

export default function () {
  // Cada usuario inicia sesión al empezar y luego cada 10 iteraciones (mide login sin saturar BCrypt artificialmente).
  if (!token || iteracion % 10 === 0) login();
  iteracion++;
  if (!token) { sleep(1); return; }

  if (esProfesional()) {
    const desde = lunes(new Date());
    const hasta = new Date(Date.parse(desde) + 6 * 864e5).toISOString().slice(0, 10);
    get(`/turnos?desde=${desde}&hasta=${hasta}`, 'calendario_semanal');
    sleep(1 + Math.random() * 2);
    if (!pacientesDelProfesional.length) {
      pacientesDelProfesional = (get('/pacientes?size=50', 'listado_pacientes').json('content') || []).map((p) => p.id);
    }
    if (pacientesDelProfesional.length) {
      const id = pacientesDelProfesional[Math.floor(Math.random() * pacientesDelProfesional.length)];
      get(`/pacientes/${id}`, 'ficha_paciente');
      get(`/pacientes/${id}/antropometria`, 'ficha_paciente_evolucion');
    }
    if (iteracion % 5 === 0) {
      const hoy = new Date().toISOString().slice(0, 10);
      get(`/reportes/turnos?desde=${hoy.slice(0, 8)}01&hasta=${hoy}&formato=json`, 'reporte_turnos_mensual');
    }
  } else {
    get('/me/plan-alimentario', 'mi_plan');
    sleep(1 + Math.random() * 2);
    get('/me/rutina', 'mi_rutina');
  }
  sleep(1 + Math.random() * 3);
}
