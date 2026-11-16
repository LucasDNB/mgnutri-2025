package ar.edu.usal.mgnutri.nutricion;

import ar.edu.usal.mgnutri.common.error.BusinessException;
import ar.edu.usal.mgnutri.nutricion.domain.TipoComida;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.BorradorGenerado;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.ComidaParam;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.ComidaResponse;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.GenerarRequest;
import ar.edu.usal.mgnutri.nutricion.dto.PlanDtos.TotalDia;
import ar.edu.usal.mgnutri.nutricion.service.GeneradorPlanService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** CU-14 / D-14: algoritmo de generación con catálogo fijo y semilla. */
class GeneradorPlanServiceTest {

    final GeneradorPlanService generador = new GeneradorPlanService(null, null);

    static GenerarRequest nominal(String variedad, List<Long> excluir) {
        return new GenerarRequest(1L, 2000, 50.0, 20.0, 30.0, List.of(
                new ComidaParam(TipoComida.DESAYUNO, 20.0), new ComidaParam(TipoComida.COLACION_MANANA, 10.0),
                new ComidaParam(TipoComida.ALMUERZO, 30.0), new ComidaParam(TipoComida.MERIENDA, 15.0),
                new ComidaParam(TipoComida.CENA, 25.0)), excluir, variedad, 42L);
    }

    @Test
    void casoNominal_totalesDentroDel10PorCientoDeKcal() {
        BorradorGenerado b = generador.generar(nominal("DIARIA", List.of()), CatalogoFijo.catalogo());
        assertThat(b.comidas()).hasSize(35);
        for (TotalDia t : b.totalesPorDia()) {
            assertThat(Math.abs(t.kcal() - 2000) / 2000).as("día %d: %s kcal", t.diaSemana(), t.kcal()).isLessThanOrEqualTo(0.10);
        }
        assertThat(b.semillaUsada()).isEqualTo(42L);
    }

    @Test
    void respetaExclusiones() {
        BorradorGenerado b = generador.generar(nominal("DIARIA", List.of(3L, 4L, 11L)), CatalogoFijo.catalogo());
        assertThat(b.comidas()).flatExtracting(ComidaResponse::items).extracting(i -> i.alimentoId()).doesNotContain(3L, 4L, 11L);
    }

    @Test
    void variedadSemanal_replicaElPrimerDia() {
        BorradorGenerado b = generador.generar(nominal("SEMANAL", List.of()), CatalogoFijo.catalogo());
        List<String> lunes = b.comidas().stream().filter(c -> c.diaSemana() == 1).map(ComidaResponse::descripcion).toList();
        List<String> domingo = b.comidas().stream().filter(c -> c.diaSemana() == 7).map(ComidaResponse::descripcion).toList();
        assertThat(domingo).isEqualTo(lunes);
    }

    @Test
    void mismaSemilla_mismoResultado() {
        var a = generador.generar(nominal("DIARIA", List.of()), CatalogoFijo.catalogo());
        var b = generador.generar(nominal("DIARIA", List.of()), CatalogoFijo.catalogo());
        assertThat(a.comidas()).extracting(ComidaResponse::descripcion).isEqualTo(b.comidas().stream().map(ComidaResponse::descripcion).toList());
    }

    @Test
    void porcentajesQueNoSuman100_fallan() {
        GenerarRequest r = new GenerarRequest(1L, 2000, 50.0, 30.0, 30.0, nominal("DIARIA", List.of()).comidas(), List.of(), "DIARIA", 1L);
        assertThatThrownBy(() -> generador.generar(r, CatalogoFijo.catalogo())).isInstanceOf(BusinessException.class);
    }

    @Test
    void catalogoInsuficiente_marcaCompletarManualmente() {
        BorradorGenerado b = generador.generar(nominal("DIARIA", List.of()), CatalogoFijo.catalogo().subList(0, 2));
        assertThat(b.advertencias()).isNotEmpty();
        assertThat(b.comidas()).anyMatch(c -> "Completar manualmente".equals(c.descripcion()) || c.items().size() < 3);
    }
}
