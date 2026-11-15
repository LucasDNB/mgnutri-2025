package ar.edu.usal.mgnutri.common;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Utilidades de fecha: se persiste en UTC y se muestra en hora Argentina (D-33). */
public final class Fechas {

    public static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");
    public static final Locale ES_AR = Locale.forLanguageTag("es-AR");
    public static final DateTimeFormatter DIA_FECHA = DateTimeFormatter.ofPattern("EEEE dd/MM", ES_AR);
    public static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy", ES_AR);
    public static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", ES_AR);
    public static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm", ES_AR);

    private Fechas() {
    }

    public static ZonedDateTime local(Instant instante) {
        return instante.atZone(ZONA);
    }

    public static OffsetDateTime offset(Instant instante) {
        return instante == null ? null : instante.atZone(ZONA).toOffsetDateTime();
    }

    public static Instant instante(LocalDate fecha, LocalTime hora) {
        return LocalDateTime.of(fecha, hora).atZone(ZONA).toInstant();
    }

    public static Instant inicioDelDia(LocalDate fecha) {
        return fecha.atStartOfDay(ZONA).toInstant();
    }

    public static LocalDate fecha(Instant instante) {
        return instante.atZone(ZONA).toLocalDate();
    }

    public static String diaFecha(Instant instante) {
        return DIA_FECHA.format(local(instante));
    }

    public static String hora(Instant instante) {
        return HORA.format(local(instante));
    }

    public static String fechaHora(Instant instante) {
        return instante == null ? "" : FECHA_HORA.format(local(instante));
    }

    public static String fecha(LocalDate fecha) {
        return fecha == null ? "" : FECHA.format(fecha);
    }
}
