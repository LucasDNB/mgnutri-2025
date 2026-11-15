package ar.edu.usal.mgnutri.config;

import ar.edu.usal.mgnutri.common.Fechas;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

/**
 * Las fechas-hora se intercambian en ISO-8601 con offset de Argentina (D-33).
 * Al deserializar se acepta con offset o sin offset (se interpreta hora Argentina).
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer fechasCustomizer() {
        SimpleModule modulo = new SimpleModule("mgnutri-fechas");
        modulo.addSerializer(Instant.class, new JsonSerializer<>() {
            @Override
            public void serialize(Instant value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeString(Fechas.offset(value).toString());
            }
        });
        modulo.addDeserializer(Instant.class, new JsonDeserializer<>() {
            @Override
            public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                return parsear(p.getValueAsString());
            }
        });
        return builder -> builder.modulesToInstall(modulo);
    }

    public static Instant parsear(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(texto).toInstant();
        } catch (DateTimeParseException e) {
            try {
                return Instant.parse(texto);
            } catch (DateTimeParseException e2) {
                return LocalDateTime.parse(texto).atZone(Fechas.ZONA).toInstant();
            }
        }
    }
}
