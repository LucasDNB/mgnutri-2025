-- Índices para las claves foráneas que se recorren desde la entidad padre (colecciones de JPA y listados por paciente
-- o profesional). SQL Server no los crea solo: sin ellos, «mi plan» (≈35 consultas a item_comida por plan) recorría la
-- tabla completa en cada consulta. Con 500 pacientes en Azure SQL la base llegaba al 100 % de CPU (prueba k6, 19/11/2026).
-- Idempotente (IF NOT EXISTS): se aplicó a mano sobre la base de la prueba antes de desplegarlo.

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_item_comida_comida' AND object_id = OBJECT_ID('item_comida'))
    CREATE INDEX ix_item_comida_comida ON item_comida (comida_plan_id, orden);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_comida_plan_plan' AND object_id = OBJECT_ID('comida_plan'))
    CREATE INDEX ix_comida_plan_plan ON comida_plan (plan_id, dia_semana, orden);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_dia_rutina_rutina' AND object_id = OBJECT_ID('dia_rutina'))
    CREATE INDEX ix_dia_rutina_rutina ON dia_rutina (rutina_id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_ejercicio_rutina_dia' AND object_id = OBJECT_ID('ejercicio_rutina'))
    CREATE INDEX ix_ejercicio_rutina_dia ON ejercicio_rutina (dia_rutina_id, orden);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_rutina_paciente' AND object_id = OBJECT_ID('rutina'))
    CREATE INDEX ix_rutina_paciente ON rutina (paciente_id, estado);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_rutina_entrenador' AND object_id = OBJECT_ID('rutina'))
    CREATE INDEX ix_rutina_entrenador ON rutina (entrenador_id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_plan_nutricionista' AND object_id = OBJECT_ID('plan_alimentario'))
    CREATE INDEX ix_plan_nutricionista ON plan_alimentario (nutricionista_id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_registro_entrenamiento_ejercicio_rutina' AND object_id = OBJECT_ID('registro_entrenamiento'))
    CREATE INDEX ix_registro_entrenamiento_ejercicio_rutina ON registro_entrenamiento (ejercicio_rutina_id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_cargo_paciente' AND object_id = OBJECT_ID('cargo'))
    CREATE INDEX ix_cargo_paciente ON cargo (paciente_id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_paciente_profesional_profesional' AND object_id = OBJECT_ID('paciente_profesional'))
    CREATE INDEX ix_paciente_profesional_profesional ON paciente_profesional (profesional_id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_paciente_profesional_paciente' AND object_id = OBJECT_ID('paciente_profesional'))
    CREATE INDEX ix_paciente_profesional_paciente ON paciente_profesional (paciente_id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_disponibilidad_profesional' AND object_id = OBJECT_ID('disponibilidad'))
    CREATE INDEX ix_disponibilidad_profesional ON disponibilidad (profesional_id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_bloqueo_agenda_profesional' AND object_id = OBJECT_ID('bloqueo_agenda'))
    CREATE INDEX ix_bloqueo_agenda_profesional ON bloqueo_agenda (profesional_id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_medicion_profesional' AND object_id = OBJECT_ID('medicion_antropometrica'))
    CREATE INDEX ix_medicion_profesional ON medicion_antropometrica (profesional_id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_turno_historial_turno' AND object_id = OBJECT_ID('turno_historial'))
    CREATE INDEX ix_turno_historial_turno ON turno_historial (turno_id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_registro_comida_comida' AND object_id = OBJECT_ID('registro_comida'))
    CREATE INDEX ix_registro_comida_comida ON registro_comida (comida_plan_id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_comentario_plan' AND object_id = OBJECT_ID('comentario'))
    CREATE INDEX ix_comentario_plan ON comentario (plan_id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_comentario_rutina' AND object_id = OBJECT_ID('comentario'))
    CREATE INDEX ix_comentario_rutina ON comentario (rutina_id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_molestia_ejercicio_rutina' AND object_id = OBJECT_ID('molestia'))
    CREATE INDEX ix_molestia_ejercicio_rutina ON molestia (ejercicio_rutina_id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_reemplazo_alimento_paciente' AND object_id = OBJECT_ID('reemplazo_alimento'))
    CREATE INDEX ix_reemplazo_alimento_paciente ON reemplazo_alimento (paciente_id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_reemplazo_ejercicio_paciente' AND object_id = OBJECT_ID('reemplazo_ejercicio'))
    CREATE INDEX ix_reemplazo_ejercicio_paciente ON reemplazo_ejercicio (paciente_id);
