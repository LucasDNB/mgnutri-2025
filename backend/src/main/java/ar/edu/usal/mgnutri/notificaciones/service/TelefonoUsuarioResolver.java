package ar.edu.usal.mgnutri.notificaciones.service;

import java.util.Optional;

/** Obtiene el teléfono de WhatsApp vigente de un usuario (paciente o profesional). */
public interface TelefonoUsuarioResolver {
    Optional<String> telefonoDe(Long usuarioId);
}
