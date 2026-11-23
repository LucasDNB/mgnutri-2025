-- v1.1: «La hice con cambios» en el registro de comidas del paciente.
ALTER TABLE registro_comida ADD con_cambios BIT NOT NULL CONSTRAINT df_registro_comida_con_cambios DEFAULT 0;

-- v1.1: los turnos son sólo de nutrición; los entrenadores dejan de tener agenda.
UPDATE d SET d.activo = 0
FROM disponibilidad d
JOIN profesional p ON p.id = d.profesional_id
WHERE p.tipo = 'ENTRENADOR' AND d.activo = 1;
