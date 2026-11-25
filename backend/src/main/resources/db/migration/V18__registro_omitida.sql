-- D-38: «Saltada / reemplazada» desde el plan alimentario → la comida queda registrada como «No completada».
-- Excluyente con realizada y con_cambios (se valida en el servicio).
ALTER TABLE registro_comida ADD omitida BIT NOT NULL CONSTRAINT df_registro_comida_omitida DEFAULT 0;
