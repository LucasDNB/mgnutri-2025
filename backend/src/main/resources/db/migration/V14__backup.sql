-- Spec 15: registro de backups
CREATE TABLE backup_registro (
    id             BIGINT IDENTITY(1,1) PRIMARY KEY,
    tipo           NVARCHAR(12)  NOT NULL CONSTRAINT ck_backup_tipo CHECK (tipo IN ('MANUAL','AUTOMATICO')),
    iniciado_at    DATETIME2(0)  NOT NULL,
    finalizado_at  DATETIME2(0)  NULL,
    estado         NVARCHAR(10)  NOT NULL CONSTRAINT ck_backup_estado CHECK (estado IN ('EN_CURSO','OK','ERROR')),
    archivo        NVARCHAR(200) NULL,
    tamano_bytes   BIGINT        NULL,
    ubicacion      NVARCHAR(400) NULL,
    solicitado_por NVARCHAR(40)  NULL,
    error          NVARCHAR(1000) NULL,
    expirado       BIT           NOT NULL DEFAULT 0
);

INSERT INTO configuracion_sistema (clave, valor, descripcion) VALUES
 ('backup.frecuencia', N'SEMANAL', N'Frecuencia del backup automático (DIARIA, SEMANAL, MENSUAL)'),
 ('backup.dia_semana', N'7', N'Día de la semana (1=lunes … 7=domingo) para backups semanales'),
 ('backup.dia_mes', N'1', N'Día del mes (1-28) para backups mensuales'),
 ('backup.hora', N'03:00', N'Hora del backup automático (HH:mm)');
