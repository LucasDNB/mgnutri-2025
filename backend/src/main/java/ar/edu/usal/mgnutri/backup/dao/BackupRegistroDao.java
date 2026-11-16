package ar.edu.usal.mgnutri.backup.dao;

import ar.edu.usal.mgnutri.backup.domain.BackupRegistro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface BackupRegistroDao extends JpaRepository<BackupRegistro, Long> {
    boolean existsByEstado(BackupRegistro.Estado estado);

    List<BackupRegistro> findByEstadoAndExpiradoFalseAndIniciadoAtBefore(BackupRegistro.Estado estado, Instant antes);
}
