-- Spec 06: evolución del paciente
CREATE TABLE medicion_antropometrica (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    paciente_id         BIGINT         NOT NULL REFERENCES paciente(id),
    profesional_id      BIGINT         NOT NULL REFERENCES profesional(id),
    fecha               DATE           NOT NULL,
    peso_kg             DECIMAL(5,2)   NOT NULL,
    talla_cm            DECIMAL(5,1)   NULL,
    imc                 DECIMAL(4,1)   NULL,
    pct_grasa           DECIMAL(4,1)   NULL,
    pct_musculo         DECIMAL(4,1)   NULL,
    circ_cintura_cm     DECIMAL(5,1)   NULL,
    circ_cadera_cm      DECIMAL(5,1)   NULL,
    circ_brazo_cm       DECIMAL(5,1)   NULL,
    circ_muslo_cm       DECIMAL(5,1)   NULL,
    circ_pecho_cm       DECIMAL(5,1)   NULL,
    circ_pantorrilla_cm DECIMAL(5,1)   NULL,
    observaciones       NVARCHAR(4000) NULL,
    created_at DATETIME2(0) NOT NULL, created_by NVARCHAR(40) NULL, updated_at DATETIME2(0) NULL, updated_by NVARCHAR(40) NULL,
    version INT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uq_medicion_fecha ON medicion_antropometrica (paciente_id, profesional_id, fecha);

CREATE TABLE evaluacion_fisica (
    id             BIGINT IDENTITY(1,1) PRIMARY KEY,
    paciente_id    BIGINT        NOT NULL REFERENCES paciente(id),
    profesional_id BIGINT        NOT NULL REFERENCES profesional(id),
    fecha          DATE          NOT NULL,
    prueba         NVARCHAR(80)  NOT NULL,
    ejercicio_id   BIGINT        NULL REFERENCES ejercicio(id),
    valor          DECIMAL(8,2)  NOT NULL,
    unidad         NVARCHAR(15)  NOT NULL,
    observaciones  NVARCHAR(300) NULL,
    created_at DATETIME2(0) NOT NULL, created_by NVARCHAR(40) NULL, updated_at DATETIME2(0) NULL, updated_by NVARCHAR(40) NULL,
    version INT NOT NULL DEFAULT 0
);
CREATE INDEX ix_evaluacion_paciente ON evaluacion_fisica (paciente_id, fecha);
