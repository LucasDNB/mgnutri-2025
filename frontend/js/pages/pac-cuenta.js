// CA-07.1.2 Mi cuenta: saldo y últimos movimientos (sólo lectura).
import { api } from '../core/api.js';
import { iniciarPagina, encabezado } from '../core/pagina.js';
import { h, tabla, chip } from '../core/ui.js';
import { fmt } from '../core/format.js';

const { main } = await iniciarPagina({ roles: ['PACIENTE'], titulo: 'Mi cuenta' });
const c = await api.get('/me/cuenta');
const contacto = await api.get('/publico/contacto');
const texto = { DEUDOR: 'Saldo pendiente', A_FAVOR: 'Saldo a favor', AL_DIA: 'Estás al día' }[c.situacion];
main.append(encabezado('Mi cuenta', [['Inicio', '/paciente/home.html'], ['Mi cuenta']]),
  h('div', { class: 'tarjeta', style: 'text-align:center' }, h('div', { class: 'suave' }, texto),
    h('div', { style: `font-size:2rem;font-weight:800;color:${c.situacion === 'DEUDOR' ? 'var(--rojo)' : 'var(--verde)'}` }, fmt.moneda(Math.abs(c.saldo))),
    c.fechaDeuda ? h('p', {}, `Deuda desde el ${fmt.fecha(c.fechaDeuda)}`) : null,
    c.situacion === 'DEUDOR' ? h('p', { class: 'suave' }, `Podés regularizarlo en la central${contacto.direccion ? ' (' + contacto.direccion + ')' : ''}.`) : null),
  h('div', { class: 'tarjeta' }, h('h2', {}, 'Últimos movimientos'), tabla([
    { titulo: 'Fecha', render: m => fmt.fecha(m.fecha) }, { titulo: 'Concepto', campo: 'concepto' },
    { titulo: 'Tipo', render: m => chip(m.tipo === 'PAGO' ? 'Pago' : 'Cargo', m.tipo === 'PAGO' ? 'OK' : 'AUSENTE') },
    { titulo: 'Monto', render: m => fmt.moneda(m.monto), clase: 'num' },
  ], c.movimientos.filter(m => !m.anulado).slice(0, 20), { vacio: 'Sin movimientos' })));
