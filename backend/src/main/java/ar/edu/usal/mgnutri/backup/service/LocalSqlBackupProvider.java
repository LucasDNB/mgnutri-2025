package ar.edu.usal.mgnutri.backup.service;

import ar.edu.usal.mgnutri.config.MgnutriProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Backup local con BACKUP DATABASE ... TO DISK (entorno local / Docker). La descarga lee el archivo
 * desde la carpeta compartida (BACKUP_DIR_DESCARGA) o, si no está configurada, con OPENROWSET(BULK).
 */
@Component
@ConditionalOnProperty(prefix = "mgnutri.backup", name = "proveedor", havingValue = "local", matchIfMissing = true)
public class LocalSqlBackupProvider implements BackupProvider {

    private static final Logger log = LoggerFactory.getLogger(LocalSqlBackupProvider.class);

    private final JdbcTemplate jdbc;
    private final MgnutriProperties.Backup.Local config;

    public LocalSqlBackupProvider(JdbcTemplate jdbc, MgnutriProperties props) {
        this.jdbc = jdbc;
        this.config = props.backup().local();
    }

    @Override
    public String extension() {
        return "bak";
    }

    private String rutaServidor(String archivo) {
        String dir = config.directorioServidor();
        String sep = dir.contains("\\") ? "\\" : "/";
        return dir.endsWith(sep) ? dir + archivo : dir + sep + archivo;
    }

    @Override
    public Resultado ejecutar(String nombreArchivo) {
        String ruta = rutaServidor(nombreArchivo).replace("'", "''");
        String db = config.baseDeDatos().replace("]", "]]");
        String base = "BACKUP DATABASE [" + db + "] TO DISK = N'" + ruta + "' WITH COPY_ONLY, INIT";
        try {
            jdbc.execute(base + ", COMPRESSION");
        } catch (DataAccessException e) {
            log.info("BACKUP con compresión no disponible ({}); se reintenta sin compresión", e.getMostSpecificCause().getMessage());
            jdbc.execute(base);
        }
        Long tam = jdbc.queryForObject("SELECT TOP 1 CAST(backup_size AS BIGINT) FROM msdb.dbo.backupset bs "
                + "JOIN msdb.dbo.backupmediafamily mf ON mf.media_set_id = bs.media_set_id WHERE mf.physical_device_name = ? "
                + "ORDER BY bs.backup_finish_date DESC", Long.class, rutaServidor(nombreArchivo));
        return new Resultado(tam == null ? 0 : tam, rutaServidor(nombreArchivo));
    }

    @Override
    public Descarga descargar(String archivo, Duration vigencia) {
        String local = config.directorioDescarga();
        if (local != null && !local.isBlank()) {
            try {
                return new Descarga(null, Files.readAllBytes(Path.of(local, archivo)));
            } catch (IOException e) {
                throw new IllegalStateException("No se pudo leer el archivo de backup", e);
            }
        }
        byte[] contenido = jdbc.queryForObject("SELECT BulkColumn FROM OPENROWSET(BULK N'"
                + rutaServidor(archivo).replace("'", "''") + "', SINGLE_BLOB) AS x", byte[].class);
        return new Descarga(null, contenido);
    }

    @Override
    public void eliminar(String archivo) {
        try {
            jdbc.execute("EXEC master.sys.xp_delete_files N'" + rutaServidor(archivo).replace("'", "''") + "'");
        } catch (DataAccessException e) {
            log.warn("No se pudo eliminar el backup {}: {}", archivo, e.getMostSpecificCause().getMessage());
        }
    }
}
