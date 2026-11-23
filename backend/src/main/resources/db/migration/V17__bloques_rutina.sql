-- v1.1: bloques de ejercicios en la rutina (p. ej. «Entrada en calor» › «3 rondas», «WOD» › «21-15-9»).
-- Los ejercicios seguidos con el mismo bloque y esquema se muestran agrupados.
ALTER TABLE ejercicio_rutina ADD bloque NVARCHAR(60) NULL, esquema NVARCHAR(60) NULL;

-- Dentro de un bloque, series y repeticiones son opcionales (el esquema manda: «21-15-9», «5 min»).
ALTER TABLE ejercicio_rutina DROP CONSTRAINT ck_er_series;
ALTER TABLE ejercicio_rutina ALTER COLUMN series INT NULL;
ALTER TABLE ejercicio_rutina ALTER COLUMN repeticiones NVARCHAR(20) NULL;
ALTER TABLE ejercicio_rutina ADD CONSTRAINT ck_er_series CHECK (series IS NULL OR series BETWEEN 1 AND 20);
