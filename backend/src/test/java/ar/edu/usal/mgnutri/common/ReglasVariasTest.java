package ar.edu.usal.mgnutri.common;

import ar.edu.usal.mgnutri.backup.dto.BackupDtos.ProgramacionDto;
import ar.edu.usal.mgnutri.backup.service.BackupService;
import ar.edu.usal.mgnutri.common.error.BusinessException;
import ar.edu.usal.mgnutri.evolucion.service.EvolucionService;
import ar.edu.usal.mgnutri.pagos.domain.Cargo;
import ar.edu.usal.mgnutri.pagos.service.CuentaCorrienteService;
import ar.edu.usal.mgnutri.profesionales.dto.ProfesionalDtos.FranjaDto;
import ar.edu.usal.mgnutri.profesionales.service.AgendaService;
import ar.edu.usal.mgnutri.seguridad.service.PasswordPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Reglas puras de varios specs: teléfonos, contraseñas, FIFO de deuda, IMC, franjas, cron de backup. */
class ReglasVariasTest {

    @Test
    void telefonoArgentino_seNormalizaAE164() {
        assertThat(Textos.telefonoE164("011 15-3456-7890")).isEqualTo("+5491134567890");
        assertThat(Textos.telefonoE164("11 3456 7890")).isEqualTo("+5491134567890");
        assertThat(Textos.telefonoE164("+54 9 11 3456-7890")).isEqualTo("+5491134567890");
        assertThat(Textos.telefonoE164("0351 15 412 3456")).isEqualTo("+5493514123456");
        assertThat(Textos.telefonoE164("1234")).isNull();
        assertThat(Textos.normalizar("  Gómez  PÉREZ ")).isEqualTo("gomez perez");
    }

    @Test
    void politicaDeContrasenas() {
        PasswordPolicy p = new PasswordPolicy();
        assertThatThrownBy(() -> p.validar("corta1", "123")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> p.validar("sinnumeros", "123")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> p.validar("Clave40123456", "40123456")).isInstanceOf(BusinessException.class);
        assertThatCode(() -> p.validar("ClaveSegura1", "40123456")).doesNotThrowAnyException();
        String temporal = p.temporal();
        assertThat(temporal).hasSize(12).matches(".*\\d.*").matches(".*[A-Za-z].*");
    }

    static Cargo cargo(long id, String monto, LocalDate venc) {
        Cargo c = new Cargo();
        ReflectionTestUtils.setField(c, "id", id);
        c.setMonto(new BigDecimal(monto));
        c.setVencimiento(venc);
        c.setFecha(venc);
        return c;
    }

    @Test
    void fechaDeDeuda_imputacionFifo() {
        // cargos 01/07 $10k y 01/08 $10k, pago $12k → fecha deuda = vencimiento del de agosto
        List<Cargo> cargos = List.of(cargo(1, "10000", LocalDate.of(2025, 7, 1)), cargo(2, "10000", LocalDate.of(2025, 8, 1)));
        assertThat(CuentaCorrienteService.fechaDeuda(cargos, new BigDecimal("12000"))).isEqualTo(LocalDate.of(2025, 8, 1));
        assertThat(CuentaCorrienteService.fechaDeuda(cargos, new BigDecimal("20000"))).isNull();
        Cargo anulado = cargo(3, "5000", LocalDate.of(2025, 6, 1));
        anulado.setAnulado(true);
        assertThat(CuentaCorrienteService.fechaDeuda(List.of(anulado, cargos.get(0)), BigDecimal.ZERO)).isEqualTo(LocalDate.of(2025, 7, 1));
    }

    @Test
    void imc() {
        assertThat(EvolucionService.imc(new BigDecimal("70"), new BigDecimal("175"))).isEqualByComparingTo("22.9");
        assertThat(EvolucionService.imc(new BigDecimal("70"), null)).isNull();
    }

    @Test
    void franjasSuperpuestas_fallan() {
        List<FranjaDto> ok = List.of(new FranjaDto(1, LocalTime.of(9, 0), LocalTime.of(12, 0), null),
                new FranjaDto(1, LocalTime.of(14, 0), LocalTime.of(18, 0), null));
        assertThatCode(() -> AgendaService.validarFranjas(ok, 30)).doesNotThrowAnyException();
        List<FranjaDto> mal = List.of(new FranjaDto(1, LocalTime.of(9, 0), LocalTime.of(12, 0), null),
                new FranjaDto(1, LocalTime.of(11, 0), LocalTime.of(13, 0), null));
        assertThatThrownBy(() -> AgendaService.validarFranjas(mal, 30)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> AgendaService.validarFranjas(List.of(new FranjaDto(2, LocalTime.of(9, 0), LocalTime.of(9, 15), null)), 30))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void conversionDeProgramacionACron() {
        assertThat(BackupService.cron(new ProgramacionDto(true, "DIARIA", null, null, "03:30", 30, null, null))).isEqualTo("0 30 3 * * *");
        assertThat(BackupService.cron(new ProgramacionDto(true, "SEMANAL", 7, null, "03:00", 30, null, null))).isEqualTo("0 0 3 * * SUN");
        assertThat(BackupService.cron(new ProgramacionDto(true, "MENSUAL", null, 15, "22:05", 30, null, null))).isEqualTo("0 5 22 15 * *");
        assertThat(BackupService.describir(new ProgramacionDto(true, "SEMANAL", 7, null, "03:00", 30, null, null)))
                .isEqualTo("Todos los domingos a las 03:00, se conservan 30 días");
    }
}
