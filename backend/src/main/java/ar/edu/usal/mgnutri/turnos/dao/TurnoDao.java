package ar.edu.usal.mgnutri.turnos.dao;

import ar.edu.usal.mgnutri.turnos.domain.Turno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TurnoDao extends JpaRepository<Turno, Long>, JpaSpecificationExecutor<Turno> {

    @Query("select t from Turno t where t.profesionalId = :profesionalId and t.estado = ar.edu.usal.mgnutri.turnos.domain.Turno.Estado.RESERVADO "
            + "and t.inicio < :hasta and t.fin > :desde")
    List<Turno> reservadosDeProfesional(@Param("profesionalId") Long profesionalId, @Param("desde") Instant desde,
                                        @Param("hasta") Instant hasta);

    @Query("select t from Turno t where t.pacienteId = :pacienteId and t.estado = ar.edu.usal.mgnutri.turnos.domain.Turno.Estado.RESERVADO "
            + "and t.inicio < :hasta and t.fin > :desde")
    List<Turno> reservadosDePaciente(@Param("pacienteId") Long pacienteId, @Param("desde") Instant desde,
                                     @Param("hasta") Instant hasta);

    @Query("select t from Turno t where t.pacienteId = :pacienteId and t.profesionalId = :profesionalId "
            + "and t.estado = ar.edu.usal.mgnutri.turnos.domain.Turno.Estado.RESERVADO and t.inicio >= :desde and t.inicio < :hasta order by t.inicio")
    List<Turno> proximoConProfesional(@Param("pacienteId") Long pacienteId, @Param("profesionalId") Long profesionalId,
                                      @Param("desde") Instant desde, @Param("hasta") Instant hasta);

    @Query("select t from Turno t where t.pacienteId = :pacienteId and t.estado = ar.edu.usal.mgnutri.turnos.domain.Turno.Estado.RESERVADO "
            + "and t.inicio >= :desde order by t.inicio")
    List<Turno> futurosDePaciente(@Param("pacienteId") Long pacienteId, @Param("desde") Instant desde);

    @Query("select t from Turno t where t.profesionalId = :profesionalId and t.estado = ar.edu.usal.mgnutri.turnos.domain.Turno.Estado.RESERVADO "
            + "and t.inicio >= :desde order by t.inicio")
    List<Turno> futurosDeProfesional(@Param("profesionalId") Long profesionalId, @Param("desde") Instant desde);

    /** Turnos para recordatorio: RESERVADO en el rango y sin recordatorio enviado (idempotencia D-11). */
    @Query("select t from Turno t where t.estado = ar.edu.usal.mgnutri.turnos.domain.Turno.Estado.RESERVADO "
            + "and t.inicio >= :desde and t.inicio < :hasta and t.recordatorioEnviadoAt is null order by t.inicio")
    List<Turno> paraRecordatorio(@Param("desde") Instant desde, @Param("hasta") Instant hasta);

    Optional<Turno> findFirstByPacienteIdAndEstadoAndInicioAfterOrderByInicioAsc(Long pacienteId, Turno.Estado estado, Instant desde);
}
