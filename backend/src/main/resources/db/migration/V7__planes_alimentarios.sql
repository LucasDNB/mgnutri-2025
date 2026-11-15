-- Spec 04: planes alimentarios
CREATE TABLE plan_alimentario (
    id                BIGINT IDENTITY(1,1) PRIMARY KEY,
    paciente_id       BIGINT         NOT NULL REFERENCES paciente(id),
    nutricionista_id  BIGINT         NOT NULL REFERENCES profesional(id),
    nombre            NVARCHAR(120)  NOT NULL,
    fecha_inicio      DATE           NOT NULL,
    duracion_semanas  INT            NOT NULL,
    fecha_fin         DATE           NOT NULL,
    modo              NVARCHAR(12)   NOT NULL CONSTRAINT ck_plan_modo CHECK (modo IN ('MANUAL','AUTOMATICO')),
    kcal_objetivo     INT            NULL,
    pct_hc            DECIMAL(5,2)   NULL,
    pct_proteinas     DECIMAL(5,2)   NULL,
    pct_grasas        DECIMAL(5,2)   NULL,
    parametros_generacion NVARCHAR(2000) NULL,
    estado            NVARCHAR(12)   NOT NULL CONSTRAINT ck_plan_estado CHECK (estado IN ('BORRADOR','ACTIVO','FINALIZADO','ELIMINADO')),
    observaciones     NVARCHAR(1000) NULL,
    created_at DATETIME2(0) NOT NULL, created_by NVARCHAR(40) NULL, updated_at DATETIME2(0) NULL, updated_by NVARCHAR(40) NULL,
    version INT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uq_plan_activo ON plan_alimentario (paciente_id) WHERE estado = 'ACTIVO';
CREATE INDEX ix_plan_paciente ON plan_alimentario (paciente_id, estado);

CREATE TABLE comida_plan (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    plan_id         BIGINT        NOT NULL REFERENCES plan_alimentario(id),
    dia_semana      INT           NOT NULL CONSTRAINT ck_comida_dia CHECK (dia_semana BETWEEN 1 AND 7),
    tipo_comida     NVARCHAR(20)  NOT NULL CONSTRAINT ck_comida_tipo CHECK (tipo_comida IN ('DESAYUNO','COLACION_MANANA','ALMUERZO','MERIENDA','COLACION_TARDE','CENA')),
    orden           INT           NOT NULL DEFAULT 0,
    descripcion     NVARCHAR(500) NULL,
    kcal_estimadas  DECIMAL(7,1)  NULL,
    activo          BIT           NOT NULL DEFAULT 1
);
CREATE UNIQUE INDEX uq_comida_plan ON comida_plan (plan_id, dia_semana, tipo_comida) WHERE activo = 1;

CREATE TABLE item_comida (
    id               BIGINT IDENTITY(1,1) PRIMARY KEY,
    comida_plan_id   BIGINT       NOT NULL REFERENCES comida_plan(id),
    alimento_id      BIGINT       NOT NULL REFERENCES alimento(id),
    gramos           DECIMAL(7,1) NOT NULL CONSTRAINT ck_item_gramos CHECK (gramos BETWEEN 1 AND 2000),
    cantidad_casera  NVARCHAR(60) NULL,
    orden            INT          NOT NULL DEFAULT 0,
    activo           BIT          NOT NULL DEFAULT 1
);
