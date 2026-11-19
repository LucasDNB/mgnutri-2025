package ar.edu.usal.mgnutri.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * En local/demo el backend sirve el frontend desde disco (FRONTEND_PATH) para ver los cambios sin recompilar.
 * Sin FRONTEND_PATH (Azure) se usa la copia empaquetada en el jar (classpath:/static), que Spring Boot
 * sirve por defecto con las mismas cabeceras de seguridad de SecurityConfig.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String ruta;

    public WebConfig(MgnutriProperties props) {
        this.ruta = props.frontend() == null ? null : props.frontend().ruta();
    }

    private boolean activo() {
        return ruta != null && !ruta.isBlank() && Files.isDirectory(Path.of(ruta));
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (activo()) {
            String ubicacion = Path.of(ruta).toAbsolutePath().normalize().toUri().toString();
            registry.addResourceHandler("/**").addResourceLocations(ubicacion).setCachePeriod(0);
        }
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        if (activo()) {
            registry.addViewController("/").setViewName("forward:/index.html");
        }
    }
}
