package ar.edu.usal.mgnutri.nutricion;

import ar.edu.usal.mgnutri.catalogo.domain.Alimento;
import ar.edu.usal.mgnutri.catalogo.domain.EquivalenciaManual;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.Alternativa;
import ar.edu.usal.mgnutri.nutricion.service.EquivalenciaService;
import ar.edu.usal.mgnutri.nutricion.service.ValidadorPlan;
import ar.edu.usal.mgnutri.nutricion.domain.TipoComida;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.ComidaDto;
import ar.edu.usal.mgnutri.common.error.BusinessException;
import ar.edu.usal.mgnutri.common.error.ErrorCode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** D-15 equivalencias y RN-04.3 validación de plan. */
class EquivalenciaServiceTest {

    final List<Alimento> cat = CatalogoFijo.catalogo();

    Alimento porId(long id) {
        return cat.stream().filter(a -> a.getId() == id).findFirst().orElseThrow();
    }

    @Test
    void calculaPorMacroDeReferenciaDentroDelMismoGrupo() {
        // 100 g de carne magra (22 g prot) ≈ 98 g de pollo (22,5 g prot) → 100 g redondeado a 5
        List<Alternativa> alts = EquivalenciaService.calcular(porId(4), 100, cat, List.of());
        assertThat(alts).extracting(Alternativa::nombre).contains("Pechuga de pollo", "Merluza").doesNotContain("Leche descremada", "Arroz integral cocido");
        Alternativa pollo = alts.stream().filter(a -> a.nombre().equals("Pechuga de pollo")).findFirst().orElseThrow();
        assertThat(pollo.gramos()).isEqualByComparingTo("100");
        assertThat(pollo.origen()).isEqualTo("CALCULADA");
    }

    @Test
    void manualesTienenPrioridad() {
        EquivalenciaManual m = new EquivalenciaManual(4L, 6L, BigDecimal.valueOf(100), BigDecimal.valueOf(110), "2 huevos");
        List<Alternativa> alts = EquivalenciaService.calcular(porId(4), 100, cat, List.of(m));
        assertThat(alts.get(0).nombre()).isEqualTo("Huevo");
        assertThat(alts.get(0).origen()).isEqualTo("MANUAL");
        assertThat(alts.get(0).gramos()).isEqualByComparingTo("110");
    }

    @Test
    void descartaProporcionesPocoPracticasEInactivos() {
        porId(5).setActivo(false);
        List<Alternativa> alts = EquivalenciaService.calcular(porId(4), 100, cat, List.of());
        assertThat(alts).extracting(Alternativa::nombre).doesNotContain("Merluza");
        // huevo: 100 × 22 / 12,6 = 175 g → práctico (< 3×)
        assertThat(alts).extracting(Alternativa::nombre).contains("Huevo");
    }

    @Test
    void validadorPlan_minimo4ComidasPorDia() {
        List<ComidaDto> comidas = new ArrayList<>();
        TipoComida[] tipos = {TipoComida.DESAYUNO, TipoComida.ALMUERZO, TipoComida.MERIENDA, TipoComida.CENA};
        for (int d = 1; d <= 7; d++) {
            for (TipoComida t : tipos) {
                comidas.add(new ComidaDto(d, t, "algo", List.of()));
            }
        }
        ValidadorPlan.validarConfirmacion(comidas);
        comidas.removeIf(c -> c.diaSemana() == 3 && c.tipo() == TipoComida.CENA);
        assertThatThrownBy(() -> ValidadorPlan.validarConfirmacion(comidas))
                .extracting(e -> ((BusinessException) e).getCode()).isEqualTo(ErrorCode.PLAN_MIN_4_COMIDAS);
    }
}
