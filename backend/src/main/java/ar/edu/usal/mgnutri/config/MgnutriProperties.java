package ar.edu.usal.mgnutri.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Parámetros de la aplicación (docs/02-arquitectura.md §8). */
@ConfigurationProperties(prefix = "mgnutri")
public record MgnutriProperties(
        String version,
        String zonaHoraria,
        Cors cors,
        Frontend frontend,
        Jwt jwt,
        Crypto crypto,
        AdminInicial adminInicial,
        Seguridad seguridad,
        WhatsApp whatsapp,
        Jobs jobs,
        Backup backup) {

    public record Cors(List<String> allowedOrigins) {}

    public record Frontend(String ruta) {}

    public record Jwt(String secret, int expiracionHoras) {}

    public record Crypto(String key) {}

    public record AdminInicial(String username, String password) {}

    public record Seguridad(int maxIntentos, int bloqueoMinutos, int recuperoHoras, int recuperoMaxPorHora,
                            boolean recuperoMensajeNeutro, int rateLimitPorMinuto) {}

    public record WhatsApp(boolean habilitado, String phoneNumberId, String token, String apiVersion,
                           String idiomaPlantillas, String credencialesModo) {}

    public record Jobs(boolean habilitados, String despachoCron, String recordatorioTurnosCron,
                       String recordatorioDeudaCron, String finalizarVencidosCron, String limpiezaTokensCron) {}

    public record Backup(String proveedor, Local local, Azure azure) {
        public record Local(String directorioServidor, String directorioDescarga, String baseDeDatos) {}

        public record Azure(String subscriptionId, String resourceGroup, String server, String database,
                            String adminUser, String adminPassword, String storageAccount, String storageKey,
                            String container) {}
    }
}
