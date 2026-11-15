package ar.edu.usal.mgnutri.turnos.service;

import ar.edu.usal.mgnutri.common.Fechas;
import ar.edu.usal.mgnutri.common.error.BusinessException;
import ar.edu.usal.mgnutri.common.error.ErrorCode;
import ar.edu.usal.mgnutri.profesionales.domain.BloqueoAgenda;
import ar.edu.usal.mgnutri.profesionales.domain.Disponibilidad;
import ar.edu.usal.mgnutri.turnos.domain.Turno;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

/** Reglas puras de turnos (R-T1 … R-T7) con Clock inyectable, testeables sin BD. */
@Component
public class ReglasTurno {

    public static final Duration VENTANA_7_DIAS = Duration.ofDays(7);
    public static final Duration ANTICIPACION_PACIENTE = Duration.ofHours(24);
    public static final Duration MINIMO_HUECO = Duration.ofHours(2);

    private final Clock clock;

    public ReglasTurno(Clock clock) {
        this.clock = clock;
    }

    public Instant ahora() {
        return clock.instant();
    }

    /** R-T1: un turno RESERVADO por paciente y profesional dentro de los próximos 7 días. */
    public void validarVentana7Dias(List<Turno> reservadosConProfesionalProximos7) {
        if (!reservadosConProfesionalProximos7.isEmpty()) {
            Turno t = reservadosConProfesionalProximos7.get(0);
            throw new BusinessException(ErrorCode.TURNO_PROXIMO_EXISTENTE,
                    "Ya tenés un turno el " + Fechas.diaFecha(t.getInicio()) + " a las " + Fechas.hora(t.getInicio())
                            + ". Podés modificarlo o cancelarlo.").con("turnoId", t.getId());
        }
    }

    /** R-T2: el paciente no modifica ni cancela con menos de 24 h. */
    public boolean dentroDe24h(Instant inicio) {
        return Duration.between(ahora(), inicio).compareTo(ANTICIPACION_PACIENTE) < 0;
    }

    public void validar24h(Instant inicio) {
        if (dentroDe24h(inicio)) {
            throw new BusinessException(ErrorCode.TURNO_DENTRO_24H,
                    "No se puede modificar con menos de 24 h de anticipación. Comunicate con el centro.");
        }
    }

    /** R-T4: sin turnos en el pasado. */
    public void validarFuturo(Instant inicio) {
        if (!inicio.isAfter(ahora())) {
            throw new BusinessException(ErrorCode.HORARIO_NO_DISPONIBLE, "No se pueden reservar turnos en el pasado.");
        }
    }

    /** R-T4: el turno debe caer completo dentro de una franja. Devuelve la franja (para la ubicación). */
    public Optional<Disponibilidad> franjaQueContiene(List<Disponibilidad> franjas, Instant inicio, Instant fin) {
        ZonedDateTime i = inicio.atZone(Fechas.ZONA);
        ZonedDateTime f = fin.atZone(Fechas.ZONA);
        if (!i.toLocalDate().equals(f.toLocalDate()) && !f.toLocalTime().equals(java.time.LocalTime.MIDNIGHT)) {
            return Optional.empty(); // no se permiten turnos que crucen medianoche
        }
        int dia = i.getDayOfWeek().getValue();
        return franjas.stream()
                .filter(d -> d.isActivo() && d.getDiaSemana() == dia)
                .filter(d -> !i.toLocalTime().isBefore(d.getHoraDesde()) && !f.toLocalTime().isAfter(d.getHoraHasta()))
                .findFirst();
    }

    /** R-T4 / R-T5: sin superposición. */
    public void validarSinSolapamiento(List<Turno> existentes, Instant inicio, Instant fin, Long excluirId, String mensaje) {
        boolean solapa = existentes.stream()
                .filter(t -> excluirId == null || !t.getId().equals(excluirId))
                .anyMatch(t -> t.solapa(inicio, fin));
        if (solapa) {
            throw new BusinessException(ErrorCode.HORARIO_NO_DISPONIBLE, mensaje);
        }
    }

    public boolean bloqueado(List<BloqueoAgenda> bloqueos, Instant inicio, Instant fin) {
        return bloqueos.stream().anyMatch(b -> b.solapa(inicio, fin));
    }

    /**
     * Algoritmo de huecos (plan 03): por cada día y franja, slots de duración fija desde hora_desde,
     * que empiecen ≥ ahora + 2 h y no solapen turnos reservados ni bloqueos (R-T7).
     */
    public List<Hueco> huecos(List<Disponibilidad> franjas, List<Turno> reservados, List<BloqueoAgenda> bloqueos,
                              LocalDate desde, LocalDate hasta, int duracionMin) {
        Instant minimo = ahora().plus(MINIMO_HUECO);
        Duration dur = Duration.ofMinutes(duracionMin);
        java.util.ArrayList<Hueco> r = new java.util.ArrayList<>();
        for (LocalDate d = desde; !d.isAfter(hasta); d = d.plusDays(1)) {
            int dia = d.getDayOfWeek().getValue();
            for (Disponibilidad f : franjas) {
                if (!f.isActivo() || f.getDiaSemana() != dia) {
                    continue;
                }
                ZonedDateTime t = d.atTime(f.getHoraDesde()).atZone(Fechas.ZONA);
                ZonedDateTime limite = d.atTime(f.getHoraHasta()).atZone(Fechas.ZONA);
                while (!t.plus(dur).isAfter(limite)) {
                    Instant i = t.toInstant();
                    Instant fi = t.plus(dur).toInstant();
                    if (!i.isBefore(minimo) && reservados.stream().noneMatch(x -> x.solapa(i, fi)) && !bloqueado(bloqueos, i, fi)) {
                        r.add(new Hueco(i, fi, f.getUbicacion()));
                    }
                    t = t.plus(dur);
                }
            }
        }
        r.sort(java.util.Comparator.comparing(Hueco::inicio));
        return r;
    }

    public record Hueco(Instant inicio, Instant fin, String ubicacion) {}
}
