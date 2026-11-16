package ar.edu.usal.mgnutri.turnos;

import ar.edu.usal.mgnutri.common.Fechas;
import ar.edu.usal.mgnutri.common.error.BusinessException;
import ar.edu.usal.mgnutri.common.error.ErrorCode;
import ar.edu.usal.mgnutri.profesionales.domain.BloqueoAgenda;
import ar.edu.usal.mgnutri.profesionales.domain.Disponibilidad;
import ar.edu.usal.mgnutri.turnos.domain.Turno;
import ar.edu.usal.mgnutri.turnos.service.ReglasTurno;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Reglas R-T1 … R-T7 con reloj fijo (spec 03, plan de pruebas). */
class ReglasTurnoTest {

    // Lunes 18/08/2025 10:00 en Buenos Aires
    static final Instant AHORA = ZonedDateTime.of(2025, 8, 18, 10, 0, 0, 0, Fechas.ZONA).toInstant();
    final ReglasTurno reglas = new ReglasTurno(Clock.fixed(AHORA, Fechas.ZONA));

    static Turno turno(Instant inicio, int minutos) {
        Turno t = new Turno();
        t.setInicio(inicio);
        t.setFin(inicio.plus(Duration.ofMinutes(minutos)));
        return t;
    }

    @Test
    void regla24h_bordes() {
        assertThat(reglas.dentroDe24h(AHORA.plus(Duration.ofHours(23).plusMinutes(59)))).isTrue();
        assertThat(reglas.dentroDe24h(AHORA.plus(Duration.ofHours(24)))).isFalse();
        assertThatThrownBy(() -> reglas.validar24h(AHORA.plus(Duration.ofHours(5))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode()).isEqualTo(ErrorCode.TURNO_DENTRO_24H);
    }

    @Test
    void regla7Dias_conTurnoExistente_falla() {
        Turno existente = turno(AHORA.plus(Duration.ofDays(6).plusHours(23)), 30);
        assertThatThrownBy(() -> reglas.validarVentana7Dias(List.of(existente)))
                .extracting(e -> ((BusinessException) e).getCode()).isEqualTo(ErrorCode.TURNO_PROXIMO_EXISTENTE);
        reglas.validarVentana7Dias(List.of()); // sin turnos: OK
    }

    @Test
    void sinSolapamiento() {
        Turno t = turno(AHORA.plus(Duration.ofDays(2)), 30);
        assertThatThrownBy(() -> reglas.validarSinSolapamiento(List.of(t), t.getInicio().plusSeconds(600), t.getFin().plusSeconds(600), null, "x"))
                .extracting(e -> ((BusinessException) e).getCode()).isEqualTo(ErrorCode.HORARIO_NO_DISPONIBLE);
        // turno contiguo: no solapa
        reglas.validarSinSolapamiento(List.of(t), t.getFin(), t.getFin().plusSeconds(1800), null, "x");
    }

    @Test
    void huecos_respetaFranjasTurnosBloqueosYVentanaDe2h() {
        Disponibilidad lunes = new Disponibilidad(1L, 1, LocalTime.of(9, 0), LocalTime.of(12, 0), "Consultorio 1");
        Disponibilidad martes = new Disponibilidad(1L, 2, LocalTime.of(9, 0), LocalTime.of(10, 0), "Consultorio 2");
        // turno tomado el martes 9:00
        Turno tomado = turno(ZonedDateTime.of(2025, 8, 19, 9, 0, 0, 0, Fechas.ZONA).toInstant(), 30);
        // bloqueo el lunes de 11:00 a 12:00
        BloqueoAgenda bloqueo = new BloqueoAgenda(1L, ZonedDateTime.of(2025, 8, 18, 11, 0, 0, 0, Fechas.ZONA).toInstant(),
                ZonedDateTime.of(2025, 8, 18, 12, 0, 0, 0, Fechas.ZONA).toInstant(), "Reunión");
        List<ReglasTurno.Hueco> huecos = reglas.huecos(List.of(lunes, martes), List.of(tomado), List.of(bloqueo),
                LocalDate.of(2025, 8, 18), LocalDate.of(2025, 8, 19), 30);
        // lunes: 9:00–11:00 anteriores a ahora+2h (12:00) quedan fuera; 11–12 bloqueado → ninguno
        // martes: 9:00 tomado, 9:30 libre
        assertThat(huecos).hasSize(1);
        assertThat(Fechas.hora(huecos.get(0).inicio())).isEqualTo("09:30");
        assertThat(huecos.get(0).ubicacion()).isEqualTo("Consultorio 2");
    }

    @Test
    void franjaQueContiene_noPermiteSalirseDeLaFranja() {
        Disponibilidad d = new Disponibilidad(1L, 1, LocalTime.of(9, 0), LocalTime.of(10, 0), null);
        Instant inicio = ZonedDateTime.of(2025, 8, 25, 9, 45, 0, 0, Fechas.ZONA).toInstant();
        assertThat(reglas.franjaQueContiene(List.of(d), inicio, inicio.plus(Duration.ofMinutes(30)))).isEmpty();
        assertThat(reglas.franjaQueContiene(List.of(d), inicio.minus(Duration.ofMinutes(15)), inicio.plus(Duration.ofMinutes(15)))).isPresent();
    }
}
