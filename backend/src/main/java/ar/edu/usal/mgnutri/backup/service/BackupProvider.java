package ar.edu.usal.mgnutri.backup.service;

import java.time.Duration;

/** Puerto de backup (D-29): adaptador local (BACKUP DATABASE) o Azure (BACPAC en Blob Storage). */
public interface BackupProvider {

    /** Ejecuta el backup (bloqueante). */
    Resultado ejecutar(String nombreArchivo);

    /** Descarga: URL temporal (Azure) o contenido directo (local). */
    Descarga descargar(String archivo, Duration vigencia);

    void eliminar(String archivo);

    String extension();

    record Resultado(long tamanoBytes, String ubicacion) {}

    record Descarga(String url, byte[] contenido) {}
}
