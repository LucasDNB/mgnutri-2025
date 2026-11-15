-- Spec 07: aranceles, cargos, pagos y saldo
CREATE TABLE arancel (
    id            BIGINT IDENTITY(1,1) PRIMARY KEY,
    concepto      NVARCHAR(100) NOT NULL,
    monto         DECIMAL(12,2) NOT NULL CONSTRAINT ck_arancel_monto CHECK (monto > 0),
    vigente_desde DATE          NOT NULL,
    activo        BIT           NOT NULL DEFAULT 1,
    created_at DATETIME2(0) NOT NULL, created_by NVARCHAR(40) NULL, updated_at DATETIME2(0) NULL, updated_by NVARCHAR(40) NULL,
    version INT NOT NULL DEFAULT 0
);

CREATE TABLE cargo (
    id               BIGINT IDENTITY(1,1) PRIMARY KEY,
    paciente_id      BIGINT        NOT NULL REFERENCES paciente(id),
    concepto         NVARCHAR(100) NOT NULL,
    periodo          NVARCHAR(7)   NULL,
    monto            DECIMAL(12,2) NOT NULL CONSTRAINT ck_cargo_monto CHECK (monto > 0),
    fecha            DATE          NOT NULL,
    vencimiento      DATE          NOT NULL,
    arancel_id       BIGINT        NULL REFERENCES arancel(id),
    anulado          BIT           NOT NULL DEFAULT 0,
    motivo_anulacion NVARCHAR(200) NULL,
    created_at DATETIME2(0) NOT NULL, created_by NVARCHAR(40) NULL, updated_at DATETIME2(0) NULL, updated_by NVARCHAR(40) NULL,
    version INT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uq_cargo_duplicado ON cargo (paciente_id, concepto, periodo, monto) WHERE anulado = 0;

CREATE SEQUENCE seq_comprobante AS BIGINT START WITH 1 INCREMENT BY 1;

CREATE TABLE pago (
    id               BIGINT IDENTITY(1,1) PRIMARY KEY,
    paciente_id      BIGINT        NOT NULL REFERENCES paciente(id),
    fecha            DATE          NOT NULL,
    monto            DECIMAL(12,2) NOT NULL CONSTRAINT ck_pago_monto CHECK (monto > 0),
    medio            NVARCHAR(15)  NOT NULL CONSTRAINT ck_pago_medio CHECK (medio IN ('EFECTIVO','TRANSFERENCIA','DEBITO','CREDITO','MERCADO_PAGO','OTRO')),
    referencia       NVARCHAR(60)  NULL,
    observaciones    NVARCHAR(300) NULL,
    nro_comprobante  BIGINT        NOT NULL CONSTRAINT uq_pago_comprobante UNIQUE,
    anulado          BIT           NOT NULL DEFAULT 0,
    motivo_anulacion NVARCHAR(200) NULL,
    created_at DATETIME2(0) NOT NULL, created_by NVARCHAR(40) NULL, updated_at DATETIME2(0) NULL, updated_by NVARCHAR(40) NULL,
    version INT NOT NULL DEFAULT 0
);
CREATE INDEX ix_pago_fecha ON pago (fecha);
CREATE INDEX ix_pago_paciente ON pago (paciente_id, fecha);
GO

CREATE VIEW v_saldo_paciente AS
SELECT p.id AS paciente_id,
       ISNULL(c.total, 0) AS total_cargos,
       ISNULL(pg.total, 0) AS total_pagos,
       ISNULL(c.total, 0) - ISNULL(pg.total, 0) AS saldo
FROM paciente p
LEFT JOIN (SELECT paciente_id, SUM(monto) total FROM cargo WHERE anulado = 0 GROUP BY paciente_id) c ON c.paciente_id = p.id
LEFT JOIN (SELECT paciente_id, SUM(monto) total FROM pago WHERE anulado = 0 GROUP BY paciente_id) pg ON pg.paciente_id = p.id;
GO
