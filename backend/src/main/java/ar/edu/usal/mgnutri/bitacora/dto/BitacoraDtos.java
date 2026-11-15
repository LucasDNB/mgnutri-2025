package ar.edu.usal.mgnutri.bitacora.dto;

import ar.edu.usal.mgnutri.bitacora.domain.BitacoraEvento;
import ar.edu.usal.mgnutri.bitacora.domain.Modulo;
import ar.edu.usal.mgnutri.bitacora.domain.TipoEvento;

import java.time.Instant;
import java.time.LocalDate;

public final class BitacoraDtos {

    private BitacoraDtos() {
    }

    public record EventoResponse(Long id, Instant fechaHora, Long usuarioId, String username, String roles,
                                 Modulo modulo, TipoEvento tipoEvento, String entidad, String entidadId,
                                 String descripcion, BitacoraEvento.Resultado resultado, String ip, String detalle) {}

    public record Filtro(LocalDate desde, LocalDate hasta, Long usuarioId, String usuario, Modulo modulo,
                         TipoEvento tipo, String entidad, String entidadId, BitacoraEvento.Resultado resultado) {}
}
