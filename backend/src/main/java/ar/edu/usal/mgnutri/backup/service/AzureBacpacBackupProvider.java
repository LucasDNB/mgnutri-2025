package ar.edu.usal.mgnutri.backup.service;

import ar.edu.usal.mgnutri.config.MgnutriProperties;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.azure.storage.common.StorageSharedKeyCredential;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Exportación BACPAC de Azure SQL a Blob Storage mediante la API de gestión de Azure (D-29).
 * Autenticación con identidad administrada / service principal (DefaultAzureCredential).
 */
@Component
@ConditionalOnProperty(prefix = "mgnutri.backup", name = "proveedor", havingValue = "azure")
public class AzureBacpacBackupProvider implements BackupProvider {

    private static final String API = "2021-11-01";
    private final MgnutriProperties.Backup.Azure cfg;
    private final RestClient rest = RestClient.create();

    public AzureBacpacBackupProvider(MgnutriProperties props) {
        this.cfg = props.backup().azure();
    }

    @Override
    public String extension() {
        return "bacpac";
    }

    private String token() {
        return new DefaultAzureCredentialBuilder().build()
                .getToken(new TokenRequestContext().addScopes("https://management.azure.com/.default")).block().getToken();
    }

    private BlobClient blob(String archivo) {
        return new BlobServiceClientBuilder()
                .endpoint("https://" + cfg.storageAccount() + ".blob.core.windows.net")
                .credential(new StorageSharedKeyCredential(cfg.storageAccount(), cfg.storageKey()))
                .buildClient().getBlobContainerClient(cfg.container()).getBlobClient(archivo);
    }

    @Override
    public Resultado ejecutar(String nombreArchivo) {
        String storageUri = "https://" + cfg.storageAccount() + ".blob.core.windows.net/" + cfg.container() + "/" + nombreArchivo;
        String url = "https://management.azure.com/subscriptions/" + cfg.subscriptionId() + "/resourceGroups/" + cfg.resourceGroup()
                + "/providers/Microsoft.Sql/servers/" + cfg.server() + "/databases/" + cfg.database() + "/export?api-version=" + API;
        String token = token();
        ResponseEntity<Void> resp = rest.post().uri(url).header("Authorization", "Bearer " + token)
                .body(Map.of("storageKeyType", "StorageAccessKey", "storageKey", cfg.storageKey(), "storageUri", storageUri,
                        "administratorLogin", cfg.adminUser(), "administratorLoginPassword", cfg.adminPassword(),
                        "authenticationType", "Sql"))
                .retrieve().toBodilessEntity();
        String operacion = resp.getHeaders().getFirst("Azure-AsyncOperation");
        if (operacion == null) {
            operacion = resp.getHeaders().getFirst("Location");
        }
        Instant limite = Instant.now().plus(Duration.ofHours(2));
        while (operacion != null && Instant.now().isBefore(limite)) {
            try {
                Thread.sleep(15_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Backup interrumpido", e);
            }
            JsonNode estado = rest.get().uri(operacion).header("Authorization", "Bearer " + token()).retrieve().body(JsonNode.class);
            String s = estado == null ? "" : estado.path("status").asText("");
            if ("Succeeded".equalsIgnoreCase(s)) {
                break;
            }
            if ("Failed".equalsIgnoreCase(s) || "Canceled".equalsIgnoreCase(s)) {
                throw new IllegalStateException("La exportación BACPAC falló: " + estado.path("error").toString());
            }
        }
        long tam = blob(nombreArchivo).getProperties().getBlobSize();
        return new Resultado(tam, storageUri);
    }

    @Override
    public Descarga descargar(String archivo, Duration vigencia) {
        BlobClient b = blob(archivo);
        String sas = b.generateSas(new BlobServiceSasSignatureValues(OffsetDateTime.now().plus(vigencia),
                new BlobSasPermission().setReadPermission(true)));
        return new Descarga(b.getBlobUrl() + "?" + sas, null);
    }

    @Override
    public void eliminar(String archivo) {
        blob(archivo).deleteIfExists();
    }
}
