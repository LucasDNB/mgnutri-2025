package ar.edu.usal.mgnutri.notificaciones.service;

import ar.edu.usal.mgnutri.bitacora.domain.Modulo;
import ar.edu.usal.mgnutri.bitacora.domain.TipoEvento;
import ar.edu.usal.mgnutri.bitacora.service.BitacoraService;
import ar.edu.usal.mgnutri.common.Fechas;
import ar.edu.usal.mgnutri.pacientes.dao.PacienteDao;
import ar.edu.usal.mgnutri.pacientes.domain.Paciente;
import ar.edu.usal.mgnutri.pagos.dao.SaldoDao;
import ar.edu.usal.mgnutri.pagos.service.CargoService;
import ar.edu.usal.mgnutri.profesionales.dao.ProfesionalDao;
import ar.edu.usal.mgnutri.profesionales.domain.Profesional;
import ar.edu.usal.mgnutri.turnos.dao.TurnoDao;
import ar.edu.usal.mgnutri.turnos.domain.Turno;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Recordatorios automáticos de turnos (CU-38, D-11) y de deuda (CU-39, D-12). */
@Service
public class RecordatoriosService {

    private final TurnoDao turnoDao;
    private final PacienteDao pacienteDao;
    private final ProfesionalDao profesionalDao;
    private final SaldoDao saldoDao;
    private final CargoService cargos;
    private final Notificador notificador;
    private final BitacoraService bitacora;
    private final Clock clock;

    public RecordatoriosService(TurnoDao turnoDao, PacienteDao pacienteDao, ProfesionalDao profesionalDao, SaldoDao saldoDao,
                                CargoService cargos, Notificador notificador, BitacoraService bitacora, Clock clock) {
        this.turnoDao = turnoDao;
        this.pacienteDao = pacienteDao;
        this.profesionalDao = profesionalDao;
        this.saldoDao = saldoDao;
        this.cargos = cargos;
        this.notificador = notificador;
        this.bitacora = bitacora;
        this.clock = clock;
    }

    /** Encola el recordatorio de cada turno RESERVADO de mañana sin recordatorio previo (idempotente). */
    @Transactional
    public int recordarTurnosDeManiana() {
        LocalDate maniana = LocalDate.now(clock.withZone(Fechas.ZONA)).plusDays(1);
        Instant ahora = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        int n = 0;
        for (Turno t : turnoDao.paraRecordatorio(Fechas.inicioDelDia(maniana), Fechas.inicioDelDia(maniana.plusDays(1)))) {
            Paciente p = pacienteDao.findById(t.getPacienteId()).orElse(null);
            Profesional pr = profesionalDao.findById(t.getProfesionalId()).orElse(null);
            if (p == null || pr == null || !p.activo()) {
                continue;
            }
            notificador.recordatorioTurno(p.getUsuarioId(), p.getNombre(), p.getTelefonoWhatsapp(), pr.nombreVisible(),
                    t.getInicio(), t.getUbicacion(), t.getId());
            t.setRecordatorioEnviadoAt(ahora);
            n++;
        }
        bitacora.ok(Modulo.NOTIFICACIONES, TipoEvento.NOTIFICACION, "Turno", null, "Recordatorio de turnos de mañana: " + n + " encolados");
        return n;
    }

    /** Pacientes activos con saldo deudor, sin recordatorio en los últimos 6 días (CA-11.3.1/3). */
    @Transactional
    public int recordarDeudas() {
        int n = 0;
        for (Paciente p : pacienteDao.findAllById(saldoDao.deudores().keySet())) {
            if (p.activo() && p.getTelefonoWhatsapp() != null && cargos.recordarDeuda(p)) {
                n++;
            }
        }
        bitacora.ok(Modulo.NOTIFICACIONES, TipoEvento.NOTIFICACION, "Paciente", null, "Recordatorio de deudas: " + n + " encolados");
        return n;
    }
}
