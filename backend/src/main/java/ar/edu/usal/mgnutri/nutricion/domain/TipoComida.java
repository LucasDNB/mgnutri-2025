package ar.edu.usal.mgnutri.nutricion.domain;

/** Tipos de comida del plan semanal (mockup ERS). */
public enum TipoComida {
    DESAYUNO("Desayuno", 1),
    COLACION_MANANA("Colación mañana", 2),
    ALMUERZO("Almuerzo", 3),
    MERIENDA("Merienda", 4),
    COLACION_TARDE("Colación tarde", 5),
    CENA("Cena", 6);

    private final String etiqueta;
    private final int orden;

    TipoComida(String etiqueta, int orden) {
        this.etiqueta = etiqueta;
        this.orden = orden;
    }

    public String etiqueta() {
        return etiqueta;
    }

    public int orden() {
        return orden;
    }

    public boolean esPrincipal() {
        return this == ALMUERZO || this == CENA;
    }

    public boolean esDesayunoOMerienda() {
        return this == DESAYUNO || this == MERIENDA;
    }

    public boolean esColacion() {
        return this == COLACION_MANANA || this == COLACION_TARDE;
    }
}
