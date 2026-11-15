package ar.edu.usal.mgnutri.pacientes.service;

import ar.edu.usal.mgnutri.notificaciones.service.TelefonoUsuarioResolver;
import ar.edu.usal.mgnutri.pacientes.dao.PacienteDao;
import ar.edu.usal.mgnutri.pacientes.domain.Paciente;
import ar.edu.usal.mgnutri.profesionales.dao.ProfesionalDao;
import ar.edu.usal.mgnutri.profesionales.domain.Profesional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class TelefonoUsuarioResolverImpl implements TelefonoUsuarioResolver {

    private final PacienteDao pacienteDao;
    private final ProfesionalDao profesionalDao;

    public TelefonoUsuarioResolverImpl(PacienteDao pacienteDao, ProfesionalDao profesionalDao) {
        this.pacienteDao = pacienteDao;
        this.profesionalDao = profesionalDao;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> telefonoDe(Long usuarioId) {
        Optional<String> p = pacienteDao.findByUsuarioId(usuarioId).map(Paciente::getTelefonoWhatsapp);
        return p.isPresent() ? p : profesionalDao.findByUsuarioId(usuarioId).map(Profesional::getTelefonoWhatsapp);
    }
}
