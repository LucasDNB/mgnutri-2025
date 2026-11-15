package ar.edu.usal.mgnutri.profesionales.service;

import ar.edu.usal.mgnutri.common.Area;

/** Cuenta planes/rutinas activos de un profesional (lo implementan nutrición y entrenamiento). */
public interface PlanesActivosConsulta {
    long activosDe(Long profesionalId, Area area);
}
