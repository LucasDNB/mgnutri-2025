package ar.edu.usal.mgnutri.common;

/** Área de atención (D-07). */
public enum Area {
    NUTRICION, ENTRENAMIENTO;

    public Rol rolRequerido() {
        return this == NUTRICION ? Rol.NUTRICIONISTA : Rol.ENTRENADOR;
    }

    public String etiqueta() {
        return this == NUTRICION ? "Nutrición" : "Entrenamiento";
    }
}
