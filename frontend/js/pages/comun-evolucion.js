// CU-20 Visualizar evolución: profesional (?pacienteId=) o paciente (propia). Gráficos responsivos y tabla con deltas.
import { api, qs, descargar } from '../core/api.js';
import { iniciarPagina, encabezado } from '../core/pagina.js';
import { h, tabla, manejarError, vaciar, param } from '../core/ui.js';
import { fmt, hoyISO, sumarDias } from '../core/format.js';
import { grafico, alinear } from '../components/graficos.js';

const { usuario, main } = await iniciarPagina({ roles: ['ADMIN', 'NUTRICIONISTA', 'ENTRENADOR', 'PACIENTE'], titulo: 'Evolución' });
const esPaciente = usuario.roles.includes('PACIENTE');
const pacienteId = esPaciente ? null : param('pacienteId');
const paciente = pacienteId ? await api.get(`/pacientes/${pacienteId}`) : null;
const base = esPaciente ? '/me/evolucion' : `/pacientes/${pacienteId}/evolucion`;

const periodo = h('select', { 'aria-label': 'Período' }, [['30', 'Último mes'], ['90', '3 meses'], ['180', '6 meses'], ['365', '1 año'], ['', 'Todo'], ['custom', 'Personalizado']].map(([v, t]) => h('option', { value: v, selected: v === '' }, t)));
const desdeIn = h('input', { type: 'date', class: 'oculto' });
const hastaIn = h('input', { type: 'date', class: 'oculto', value: hoyISO() });
const cont = h('div');
periodo.addEventListener('change', () => { const c = periodo.value === 'custom'; desdeIn.classList.toggle('oculto', !c); hastaIn.classList.toggle('oculto', !c); cargar(); });
[desdeIn, hastaIn].forEach(x => x.addEventListener('change', cargar));

function rango() {
  if (periodo.value === 'custom') return { desde: desdeIn.value, hasta: hastaIn.value };
  if (!periodo.value) return {};
  return { desde: sumarDias(hoyISO(), -Number(periodo.value)), hasta: hoyISO() };
}

main.append(encabezado(esPaciente ? 'Mi evolución' : `Evolución de ${paciente.nombre} ${paciente.apellido}`,
  esPaciente ? [] : [['Pacientes', '/comun/pacientes.html'], [`${paciente.apellido}`, `/comun/paciente.html?id=${pacienteId}&tab=evolucion`], ['Evolución']],
  h('button', { class: 'btn secundario', onclick: () => descargar(esPaciente ? `/me/reportes/evolucion${qs({ ...rango(), formato: 'pdf' })}` : `/reportes/evolucion/${pacienteId}${qs({ ...rango(), formato: 'pdf' })}`, 'evolucion.pdf').catch(manejarError) }, 'Descargar PDF')),
h('div', { class: 'tarjeta fila' }, h('label', {}, 'Período: '), periodo, desdeIn, hastaIn), cont);

async function cargar() {
  try {
    const ev = await api.get(`${base}${qs(rango())}`);
    if (ev.sinDatos) {
      vaciar(cont, h('div', { class: 'tarjeta vacio' }, 'Todavía no hay mediciones registradas'));
      return;
    }
    const gPeso = h('div');
    const gComp = h('div');
    const gMed = h('div');
    const gDes = h('div');
    const gCar = h('div');
    const selMedidas = h('div', { class: 'fila' });
    const medidasDisponibles = Object.entries(ev.medidas).filter(([, v]) => v.length);
    let elegidas = new Set(medidasDisponibles.slice(0, 2).map(([k]) => k));
    const dibujarMedidas = () => {
      const d = alinear(medidasDisponibles.filter(([k]) => elegidas.has(k)).map(([k, v]) => ({ nombre: k, puntos: v })));
      grafico(gMed, { titulo: 'Perímetros (cm)', ...d });
    };
    medidasDisponibles.forEach(([k]) => {
      const c = h('input', { type: 'checkbox', checked: elegidas.has(k), onchange: () => { if (c.checked) elegidas.add(k); else elegidas.delete(k); dibujarMedidas(); } });
      selMedidas.append(h('label', { class: 'check' }, c, k));
    });
    const selCarga = h('select', {}, ev.cargas.map((c, i) => h('option', { value: i }, c.nombre)));
    const selPrueba = h('select', {}, ev.desempeno.map((c, i) => h('option', { value: i }, `${c.nombre} (${c.unidad})`)));
    vaciar(cont,
      h('div', { class: 'grilla g2' },
        h('div', { class: 'tarjeta' }, h('h2', {}, 'Peso'), gPeso),
        h('div', { class: 'tarjeta' }, h('h2', {}, 'Composición corporal'), gComp),
        h('div', { class: 'tarjeta' }, h('h2', {}, 'Medidas'), selMedidas, gMed),
        h('div', { class: 'tarjeta' }, h('h2', {}, 'Desempeño físico'), ev.desempeno.length ? selPrueba : null, gDes),
        h('div', { class: 'tarjeta' }, h('h2', {}, 'Progreso de cargas'), ev.cargas.length ? selCarga : null, gCar)),
      h('div', { class: 'tarjeta' }, h('h2', {}, 'Mediciones'), tabla([
        { titulo: 'Fecha', render: f => fmt.fecha(f.fecha) }, { titulo: 'Peso (kg)', render: f => fmt.numero(f.peso), clase: 'num' },
        { titulo: 'IMC', render: f => fmt.numero(f.imc), clase: 'num' }, { titulo: '% grasa', render: f => fmt.numero(f.pctGrasa), clase: 'num' },
        { titulo: '% músculo', render: f => fmt.numero(f.pctMusculo), clase: 'num' }, { titulo: 'Cintura', render: f => fmt.numero(f.cintura), clase: 'num' },
        { titulo: 'Δ anterior', render: f => (f.deltaAnterior == null ? '' : `${f.deltaAnterior > 0 ? '+' : ''}${fmt.numero(f.deltaAnterior)}`), clase: 'num' },
        { titulo: 'Δ inicial', render: f => `${f.deltaInicial > 0 ? '+' : ''}${fmt.numero(f.deltaInicial)} (${fmt.numero(f.deltaInicialPct)} %)`, clase: 'num' },
      ], [...ev.tabla].reverse(), { vacio: 'Sin mediciones en el período' })),
      h('p', { class: 'suave' }, 'El sistema muestra los datos registrados; la interpretación corresponde a tu profesional.'));
    grafico(gPeso, { titulo: 'Peso (kg)', ...alinear([{ nombre: 'Peso', puntos: ev.peso }]) });
    grafico(gComp, { titulo: 'Composición (%)', ...alinear([{ nombre: '% grasa', puntos: ev.composicion.grasa }, { nombre: '% músculo', puntos: ev.composicion.musculo }]) });
    dibujarMedidas();
    const des = () => { const s = ev.desempeno[selPrueba.value || 0]; grafico(gDes, s ? { titulo: `${s.nombre} (${s.unidad})`, ...alinear([{ nombre: s.nombre, puntos: s.serie }]) } : { labels: [] }); };
    const car = () => { const s = ev.cargas[selCarga.value || 0]; grafico(gCar, s ? { titulo: `${s.nombre} (kg)`, ...alinear([{ nombre: s.nombre, puntos: s.serie }]) } : { labels: [] }); };
    selPrueba.addEventListener('change', des);
    selCarga.addEventListener('change', car);
    des();
    car();
  } catch (e) { manejarError(e); }
}
cargar();
