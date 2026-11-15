package ar.edu.usal.mgnutri.pagos.service;

import ar.edu.usal.mgnutri.common.error.BusinessException;
import ar.edu.usal.mgnutri.common.security.CurrentUser;
import ar.edu.usal.mgnutri.pacientes.dao.PacienteDao;
import ar.edu.usal.mgnutri.pacientes.domain.Paciente;
import ar.edu.usal.mgnutri.pagos.dao.CargoDao;
import ar.edu.usal.mgnutri.pagos.dao.PagoDao;
import ar.edu.usal.mgnutri.pagos.dao.SaldoDao;
import ar.edu.usal.mgnutri.pagos.domain.Cargo;
import ar.edu.usal.mgnutri.pagos.domain.Pago;
import ar.edu.usal.mgnutri.pagos.dto.PagoDtos.CuentaResponse;
import ar.edu.usal.mgnutri.pagos.dto.PagoDtos.DeudorResponse;
import ar.edu.usal.mgnutri.pagos.dto.PagoDtos.Movimiento;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Cuenta corriente del paciente (D-19): saldo, movimientos y fecha de deuda FIFO. */
@Service
public class CuentaCorrienteService {

    private final CargoDao cargoDao;
    private final PagoDao pagoDao;
    private final SaldoDao saldoDao;
    private final PacienteDao pacienteDao;
    private final Clock clock;

    public CuentaCorrienteService(CargoDao cargoDao, PagoDao pagoDao, SaldoDao saldoDao, PacienteDao pacienteDao, Clock clock) {
        this.cargoDao = cargoDao;
        this.pagoDao = pagoDao;
        this.saldoDao = saldoDao;
        this.pacienteDao = pacienteDao;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public CuentaResponse cuenta(Long pacienteId, LocalDate desde, LocalDate hasta) {
        Paciente p = pacienteDao.findById(pacienteId).orElseThrow(() -> BusinessException.noEncontrado("Paciente"));
        return armar(p, cargoDao.findByPacienteIdOrderByFechaAscIdAsc(pacienteId), pagoDao.findByPacienteIdOrderByFechaAscIdAsc(pacienteId),
                desde, hasta);
    }

    @Transactional(readOnly = true)
    public CuentaResponse miCuenta() {
        return cuenta(CurrentUser.pacienteId(), null, null);
    }

    CuentaResponse armar(Paciente p, List<Cargo> cargos, List<Pago> pagos, LocalDate desde, LocalDate hasta) {
        List<Movimiento> movs = new ArrayList<>();
        for (Cargo c : cargos) {
            movs.add(new Movimiento("CARGO", c.getId(), c.getFecha(), c.getConcepto() + (c.getPeriodo() == null ? "" : " " + c.getPeriodo()),
                    c.getMonto(), null, c.isAnulado(), null, null, c.getVencimiento(), c.getPeriodo(), null, c.getMotivoAnulacion(), c.getVersion()));
        }
        for (Pago pg : pagos) {
            movs.add(new Movimiento("PAGO", pg.getId(), pg.getFecha(), "Pago " + pg.getMedio().etiqueta(), pg.getMonto(), null,
                    pg.isAnulado(), pg.getNroComprobante(), pg.getMedio().name(), null, null, pg.getReferencia(),
                    pg.getMotivoAnulacion(), pg.getVersion()));
        }
        movs.sort(Comparator.comparing(Movimiento::fecha).thenComparing(m -> "CARGO".equals(m.tipo()) ? 0 : 1).thenComparing(Movimiento::id));
        BigDecimal acumulado = BigDecimal.ZERO;
        List<Movimiento> conSaldo = new ArrayList<>();
        for (Movimiento m : movs) {
            if (!m.anulado()) {
                acumulado = "CARGO".equals(m.tipo()) ? acumulado.add(m.monto()) : acumulado.subtract(m.monto());
            }
            if ((desde == null || !m.fecha().isBefore(desde)) && (hasta == null || !m.fecha().isAfter(hasta))) {
                conSaldo.add(new Movimiento(m.tipo(), m.id(), m.fecha(), m.concepto(), m.monto(), acumulado, m.anulado(),
                        m.comprobante(), m.medio(), m.vencimiento(), m.periodo(), m.referencia(), m.motivoAnulacion(), m.version()));
            }
        }
        BigDecimal totalCargos = cargos.stream().filter(c -> !c.isAnulado()).map(Cargo::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPagos = pagos.stream().filter(x -> !x.isAnulado()).map(Pago::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal saldo = totalCargos.subtract(totalPagos).setScale(2, RoundingMode.HALF_EVEN);
        LocalDate fechaDeuda = fechaDeuda(cargos, totalPagos);
        Long dias = fechaDeuda == null ? null : Math.max(0, ChronoUnit.DAYS.between(fechaDeuda, LocalDate.now(clock)));
        String situacion = saldo.signum() > 0 ? "DEUDOR" : saldo.signum() < 0 ? "A_FAVOR" : "AL_DIA";
        java.util.Collections.reverse(conSaldo); // más reciente primero
        return new CuentaResponse(p.getId(), p.nombreCompleto(), saldo, situacion, totalCargos, totalPagos, fechaDeuda, dias, conSaldo);
    }

    /** Imputación FIFO (RN-07.4): vencimiento del cargo impago más antiguo. */
    public static LocalDate fechaDeuda(List<Cargo> cargos, BigDecimal totalPagos) {
        BigDecimal disponible = totalPagos;
        List<Cargo> ordenados = cargos.stream().filter(c -> !c.isAnulado())
                .sorted(Comparator.comparing(Cargo::getVencimiento).thenComparing(Cargo::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        for (Cargo c : ordenados) {
            if (disponible.compareTo(c.getMonto()) >= 0) {
                disponible = disponible.subtract(c.getMonto());
            } else {
                return c.getVencimiento();
            }
        }
        return null;
    }

    /** Listado de deudores (US-07.6) con saldo > 0. */
    @Transactional(readOnly = true)
    public List<DeudorResponse> deudores(String orden) {
        Map<Long, BigDecimal> saldos = saldoDao.deudores();
        LocalDate hoy = LocalDate.now(clock);
        List<DeudorResponse> lista = new ArrayList<>();
        for (Paciente p : pacienteDao.findAllById(saldos.keySet())) {
            BigDecimal totalPagos = pagoDao.findByPacienteIdOrderByFechaAscIdAsc(p.getId()).stream().filter(x -> !x.isAnulado())
                    .map(Pago::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
            LocalDate fd = fechaDeuda(cargoDao.findByPacienteIdOrderByFechaAscIdAsc(p.getId()), totalPagos);
            lista.add(new DeudorResponse(p.getId(), p.nombreCompleto() + (p.activo() ? "" : " (baja)"), p.getNroDocumento(),
                    p.getTelefonoWhatsapp(), saldos.get(p.getId()), fd, fd == null ? 0 : Math.max(0, ChronoUnit.DAYS.between(fd, hoy)),
                    !p.activo()));
        }
        Comparator<DeudorResponse> cmp = "antiguedad".equalsIgnoreCase(orden)
                ? Comparator.comparing(DeudorResponse::diasAtraso).reversed()
                : Comparator.comparing(DeudorResponse::saldo).reversed();
        lista.sort(cmp);
        return lista;
    }

    @Transactional(readOnly = true)
    public BigDecimal saldo(Long pacienteId) {
        return saldoDao.saldo(pacienteId);
    }
}
