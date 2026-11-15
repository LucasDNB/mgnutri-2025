package ar.edu.usal.mgnutri.pagos.dto;

import ar.edu.usal.mgnutri.pagos.domain.Pago;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class PagoDtos {

    private PagoDtos() {
    }

    public record PagoRequest(
            @NotNull(message = "El monto es obligatorio") @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
            @DecimalMax(value = "9999999.99", message = "Monto demasiado alto") @Digits(integer = 7, fraction = 2) BigDecimal monto,
            LocalDate fecha,
            @NotNull(message = "El medio de pago es obligatorio") Pago.Medio medio,
            @Size(max = 60) String referencia,
            @Size(max = 300) String observaciones,
            @Size(max = 200) String motivo,
            Integer version) {}

    public record CargoRequest(
            @NotBlank(message = "El concepto es obligatorio") @Size(max = 100) String concepto,
            Long arancelId,
            @Pattern(regexp = "^$|^\\d{4}-(0[1-9]|1[0-2])$", message = "Período inválido (AAAA-MM)") String periodo,
            @NotNull(message = "El monto es obligatorio") @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
            @DecimalMax(value = "9999999.99") @Digits(integer = 7, fraction = 2) BigDecimal monto,
            LocalDate fecha,
            LocalDate vencimiento,
            Boolean notificarAhora,
            @Size(max = 200) String motivo,
            Integer version) {}

    public record AnularRequest(@NotBlank(message = "Indicá el motivo") @Size(max = 200) String motivo) {}

    public record PagoResponse(Long id, Long pacienteId, String paciente, LocalDate fecha, BigDecimal monto, String medio,
                               String referencia, String observaciones, Long nroComprobante, boolean anulado,
                               String motivoAnulacion, Integer version, BigDecimal saldoResultante, String mensaje) {}

    public record CargoResponse(Long id, Long pacienteId, String concepto, String periodo, BigDecimal monto, LocalDate fecha,
                                LocalDate vencimiento, Long arancelId, boolean anulado, String motivoAnulacion,
                                Integer version, String notificacion) {}

    public record Movimiento(String tipo, Long id, LocalDate fecha, String concepto, BigDecimal monto, BigDecimal saldo,
                             boolean anulado, Long comprobante, String medio, LocalDate vencimiento, String periodo,
                             String referencia, String motivoAnulacion, Integer version) {}

    public record CuentaResponse(Long pacienteId, String paciente, BigDecimal saldo, String situacion,
                                 BigDecimal totalCargos, BigDecimal totalPagos, LocalDate fechaDeuda, Long diasAtraso,
                                 List<Movimiento> movimientos) {}

    public record DeudorResponse(Long pacienteId, String paciente, String documento, String telefono, BigDecimal saldo,
                                 LocalDate fechaDeuda, long diasAtraso, boolean baja) {}

    public record GeneracionMasivaRequest(@NotNull Long arancelId,
                                          @NotBlank @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "Período inválido (AAAA-MM)") String periodo,
                                          List<Long> pacienteIds, Boolean todos, LocalDate vencimiento) {}

    public record GeneracionMasivaResponse(int creados, int omitidosDuplicados) {}

    public record ArancelRequest(@NotBlank @Size(max = 100) String concepto,
                                 @NotNull @DecimalMin("0.01") @DecimalMax("9999999.99") BigDecimal monto,
                                 @NotNull LocalDate vigenteDesde, Boolean activo, Boolean notificarPacientes,
                                 Integer version) {}

    public record ArancelResponse(Long id, String concepto, BigDecimal monto, LocalDate vigenteDesde, boolean activo,
                                  Integer version, Integer notificados) {}
}
