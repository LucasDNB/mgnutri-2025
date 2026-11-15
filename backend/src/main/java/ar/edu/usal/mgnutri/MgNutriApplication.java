package ar.edu.usal.mgnutri;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Sistema de gestión del centro MG Nutrición y Entrenamiento.
 * Arquitectura MVC en capas controller → service → dao (constitución, art. IV).
 */
@SpringBootApplication
@org.springframework.cache.annotation.EnableCaching
@ConfigurationPropertiesScan
public class MgNutriApplication {

    public static void main(String[] args) {
        SpringApplication.run(MgNutriApplication.class, args);
    }
}
