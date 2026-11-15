package ar.edu.usal.mgnutri.configuracion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class ConfiguracionDtos {

    private ConfiguracionDtos() {
    }

    public record ParametroResponse(String clave, String valor, String descripcion, Instant updatedAt,
                                    String updatedBy, boolean editable) {}

    public record ContactoCentral(String nombre, String direccion, String horario, String telefono,
                                  String whatsappUrl, String mantenimiento) {}

    public record ContactoProfesional(Long id, String nombre, String especialidad, String area, String whatsappUrl) {}

    public record MisContactos(ContactoCentral central, List<ContactoProfesional> profesionales) {}

    public record TerminosResponse(String version, String texto, Instant vigenteDesde) {}

    public record TerminosRequest(@NotBlank @Size(max = 10) String version, @NotBlank String texto) {}

    public record AceptarTerminosRequest(@NotBlank String version) {}
}
