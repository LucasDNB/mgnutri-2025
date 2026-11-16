package ar.edu.usal.mgnutri.nutricion;

import ar.edu.usal.mgnutri.catalogo.domain.Alimento;
import ar.edu.usal.mgnutri.catalogo.domain.GrupoAlimento;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Catálogo fijo para pruebas del generador y de equivalencias. */
public final class CatalogoFijo {

    public static final GrupoAlimento LACTEOS = grupo(1L, "Lácteos", GrupoAlimento.Macro.PROTEINAS);
    public static final GrupoAlimento CARNES = grupo(2L, "Carnes y huevos", GrupoAlimento.Macro.PROTEINAS);
    public static final GrupoAlimento CEREALES = grupo(3L, "Cereales, legumbres y derivados", GrupoAlimento.Macro.HC);
    public static final GrupoAlimento FRUTAS = grupo(4L, "Frutas", GrupoAlimento.Macro.HC);
    public static final GrupoAlimento VERDURAS = grupo(5L, "Verduras", GrupoAlimento.Macro.HC);
    public static final GrupoAlimento ACEITES = grupo(6L, "Aceites y grasas", GrupoAlimento.Macro.GRASAS);
    public static final GrupoAlimento FRUTOS = grupo(7L, "Frutos secos y semillas", GrupoAlimento.Macro.GRASAS);

    private CatalogoFijo() {
    }

    static GrupoAlimento grupo(Long id, String nombre, GrupoAlimento.Macro macro) {
        GrupoAlimento g = new GrupoAlimento(nombre, macro);
        ReflectionTestUtils.setField(g, "id", id);
        return g;
    }

    public static Alimento alimento(long id, String nombre, GrupoAlimento g, double kcal, double hc, double p, double gr,
                                    Double gramosUnidad, String aptitud) {
        Alimento a = new Alimento();
        ReflectionTestUtils.setField(a, "id", id);
        a.setNombre(nombre);
        a.setGrupo(g);
        a.setKcal(BigDecimal.valueOf(kcal));
        a.setHc(BigDecimal.valueOf(hc));
        a.setProteinas(BigDecimal.valueOf(p));
        a.setGrasas(BigDecimal.valueOf(gr));
        a.setFibra(BigDecimal.ZERO);
        if (gramosUnidad != null) {
            a.setUnidadCasera("1 unidad");
            a.setGramosUnidadCasera(BigDecimal.valueOf(gramosUnidad));
        }
        a.setAptoDesayuno(aptitud.contains("D"));
        a.setAptoColacion(aptitud.contains("C"));
        a.setAptoAlmuerzo(aptitud.contains("A"));
        a.setActivo(true);
        return a;
    }

    public static List<Alimento> catalogo() {
        List<Alimento> l = new ArrayList<>();
        l.add(alimento(1, "Leche descremada", LACTEOS, 35, 5, 3.4, 0.1, 200.0, "DC"));
        l.add(alimento(2, "Yogur descremado", LACTEOS, 40, 5.5, 4, 0.2, 190.0, "DC"));
        l.add(alimento(3, "Pechuga de pollo", CARNES, 120, 0, 22.5, 2.6, null, "A"));
        l.add(alimento(4, "Carne vacuna magra", CARNES, 143, 0, 22, 6, null, "A"));
        l.add(alimento(5, "Merluza", CARNES, 82, 0, 17, 1.3, null, "A"));
        l.add(alimento(6, "Huevo", CARNES, 143, 0.7, 12.6, 9.5, 55.0, "DA"));
        l.add(alimento(7, "Arroz integral cocido", CEREALES, 123, 25.6, 2.7, 1, null, "A"));
        l.add(alimento(8, "Fideos cocidos", CEREALES, 158, 31, 5.8, 0.9, null, "A"));
        l.add(alimento(9, "Pan integral", CEREALES, 250, 43, 10, 3.4, 25.0, "DA"));
        l.add(alimento(10, "Avena", CEREALES, 379, 57.7, 13.2, 6.5, 10.0, "DC"));
        l.add(alimento(11, "Manzana", FRUTAS, 52, 13.8, 0.3, 0.2, 150.0, "DC"));
        l.add(alimento(12, "Banana", FRUTAS, 89, 22.8, 1.1, 0.3, 120.0, "DC"));
        l.add(alimento(13, "Brócoli", VERDURAS, 34, 6.6, 2.8, 0.4, null, "A"));
        l.add(alimento(14, "Zapallo", VERDURAS, 26, 6.5, 1, 0.1, null, "A"));
        l.add(alimento(15, "Aceite de oliva", ACEITES, 884, 0, 0, 100, 10.0, "DA"));
        l.add(alimento(16, "Nueces", FRUTOS, 654, 7, 15.2, 65.2, 15.0, "DC"));
        l.add(alimento(17, "Almendras", FRUTOS, 579, 9.1, 21.2, 49.9, 12.0, "DC"));
        return l;
    }
}
