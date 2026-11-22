package ar.edu.usal.mgnutri.notificaciones.service;

import ar.edu.usal.mgnutri.common.Textos;
import ar.edu.usal.mgnutri.config.MgnutriProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/** Adaptador WhatsApp Cloud API de Meta (spec 11). */
@Component
@ConditionalOnProperty(prefix = "mgnutri.whatsapp", name = "habilitado", havingValue = "true")
public class WhatsAppCloudAdapter implements MensajeriaPort {

    private final RestClient client;
    private final MgnutriProperties.WhatsApp config;

    // Hay dos constructores (el otro es para pruebas): sin @Autowired Spring busca uno sin argumentos y falla al iniciar.
    @Autowired
    public WhatsAppCloudAdapter(MgnutriProperties props, RestClient.Builder builder) {
        this.config = props.whatsapp();
        if (config.phoneNumberId() == null || config.phoneNumberId().isBlank()
                || config.token() == null || config.token().isBlank()) {
            throw new IllegalStateException("WhatsApp habilitado sin WA_PHONE_ID / WA_TOKEN");
        }
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(10_000);
        this.client = builder.baseUrl("https://graph.facebook.com/" + config.apiVersion())
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.token())
                .build();
    }

    WhatsAppCloudAdapter(RestClient client, MgnutriProperties.WhatsApp config) {
        this.client = client;
        this.config = config;
    }

    @Override
    public ResultadoEnvio enviar(String plantilla, String telefonoE164, List<String> parametros) {
        Map<String, Object> cuerpo = cuerpo(plantilla, telefonoE164, parametros, config.idiomaPlantillas());
        try {
            JsonNode resp = client.post().uri("/{id}/messages", config.phoneNumberId())
                    .contentType(MediaType.APPLICATION_JSON).body(cuerpo).retrieve().body(JsonNode.class);
            String id = resp == null ? null : resp.path("messages").path(0).path("id").asText(null);
            return id == null ? ResultadoEnvio.falla(true, "Respuesta sin id de mensaje") : ResultadoEnvio.exito(id);
        } catch (HttpClientErrorException e) {
            // Errores de plantilla o número: no se reintenta
            return ResultadoEnvio.falla(false, "HTTP " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString());
        } catch (HttpServerErrorException | ResourceAccessException e) {
            return ResultadoEnvio.falla(true, e.getMessage());
        }
    }

    /**
     * Celulares argentinos (+54 9 …) se envían sin el 9: Meta los resuelve al mismo WhatsApp (devuelve wa_id 549…), y
     * con el 9 rechaza a los destinatarios verificados del número de prueba con #131030 (verificado el 22/11/2026).
     */
    static String destino(String telefonoE164) {
        String d = Textos.digitos(telefonoE164);
        return d != null && d.length() == 13 && d.startsWith("549") ? "54" + d.substring(3) : d;
    }

    static Map<String, Object> cuerpo(String plantilla, String telefonoE164, List<String> parametros, String idioma) {
        List<Map<String, Object>> params = parametros.stream()
                .map(p -> Map.<String, Object>of("type", "text", "text", p == null || p.isBlank() ? "-" : p))
                .toList();
        return Map.of(
                "messaging_product", "whatsapp",
                "to", destino(telefonoE164),
                "type", "template",
                "template", Map.of(
                        "name", plantilla,
                        "language", Map.of("code", idioma),
                        "components", List.of(Map.of("type", "body", "parameters", params))));
    }
}
