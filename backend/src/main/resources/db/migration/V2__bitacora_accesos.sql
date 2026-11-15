-- Spec 14 / 13: bitácora y registro de accesos (append-only)
CREATE TABLE bitacora_evento (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    fecha_hora  DATETIME2(0)  NOT NULL,
    usuario_id  BIGINT        NULL,
    username    NVARCHAR(40)  NULL,
    roles       NVARCHAR(100) NULL,
    modulo      NVARCHAR(20)  NOT NULL,
    tipo_evento NVARCHAR(20)  NOT NULL,
    entidad     NVARCHAR(40)  NULL,
    entidad_id  NVARCHAR(40)  NULL,
    descripcion NVARCHAR(500) NULL,
    resultado   NVARCHAR(10)  NOT NULL CONSTRAINT ck_bitacora_resultado CHECK (resultado IN ('OK','ERROR')),
    ip          NVARCHAR(60)  NULL,
    detalle     NVARCHAR(MAX) NULL
);
CREATE INDEX ix_bitacora_fecha ON bitacora_evento (fecha_hora);
CREATE INDEX ix_bitacora_usuario ON bitacora_evento (usuario_id, fecha_hora);
CREATE INDEX ix_bitacora_modulo ON bitacora_evento (modulo, tipo_evento, fecha_hora);

CREATE TABLE registro_acceso (
    id                 BIGINT IDENTITY(1,1) PRIMARY KEY,
    fecha_hora         DATETIME2(0)  NOT NULL,
    usuario_id         BIGINT        NULL,
    username_intentado NVARCHAR(40)  NULL,
    roles              NVARCHAR(100) NULL,
    ip                 NVARCHAR(60)  NULL,
    user_agent         NVARCHAR(300) NULL,
    accion             NVARCHAR(30)  NOT NULL,
    detalle            NVARCHAR(300) NULL
);
CREATE INDEX ix_acceso_fecha ON registro_acceso (fecha_hora);
GO

CREATE TRIGGER trg_bitacora_inmutable ON bitacora_evento
INSTEAD OF UPDATE, DELETE AS
BEGIN
    THROW 51000, 'La bitacora es de solo lectura', 1;
END;
GO

CREATE TRIGGER trg_acceso_inmutable ON registro_acceso
INSTEAD OF UPDATE, DELETE AS
BEGIN
    THROW 51001, 'El registro de accesos es de solo lectura', 1;
END;
GO

IF DATABASE_PRINCIPAL_ID('mgnutri_app') IS NULL
    CREATE ROLE mgnutri_app;
GO
GRANT SELECT, INSERT, UPDATE, DELETE, EXECUTE ON SCHEMA::dbo TO mgnutri_app;
DENY UPDATE, DELETE ON bitacora_evento TO mgnutri_app;
DENY UPDATE, DELETE ON registro_acceso TO mgnutri_app;
GO
