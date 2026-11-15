-- Spec 11: outbox de notificaciones y alertas internas
CREATE TABLE notificacion (
    id                 BIGINT IDENTITY(1,1) PRIMARY KEY,
    tipo               NVARCHAR(30)   NOT NULL,
    usuario_id         BIGINT         NULL REFERENCES usuario(id),
    destinatario       NVARCHAR(120)  NULL,
    telefono           NVARCHAR(1024) NULL,
    plantilla          NVARCHAR(60)   NOT NULL,
    parametros         NVARCHAR(MAX)  NULL,
    referencia_tipo    NVARCHAR(40)   NULL,
    referencia_id      BIGINT         NULL,
    estado             NVARCHAR(12)   NOT NULL CONSTRAINT ck_notif_estado CHECK (estado IN ('PENDIENTE','ENVIADA','FALLIDA','OMITIDA')),
    intentos           INT            NOT NULL DEFAULT 0,
    proximo_intento_at DATETIME2(0)   NULL,
    error              NVARCHAR(500)  NULL,
    wa_message_id      NVARCHAR(120)  NULL,
    created_at         DATETIME2(0)   NOT NULL,
    enviada_at         DATETIME2(0)   NULL
);
CREATE INDEX ix_notif_pendientes ON notificacion (estado, proximo_intento_at);
CREATE INDEX ix_notif_usuario ON notificacion (usuario_id, tipo, created_at);

CREATE TABLE alerta (
    id                      BIGINT IDENTITY(1,1) PRIMARY KEY,
    destinatario_usuario_id BIGINT        NULL REFERENCES usuario(id),
    tipo                    NVARCHAR(30)  NOT NULL,
    mensaje                 NVARCHAR(500) NOT NULL,
    prioritaria             BIT           NOT NULL DEFAULT 0,
    referencia_tipo         NVARCHAR(40)  NULL,
    referencia_id           BIGINT        NULL,
    created_at              DATETIME2(0)  NOT NULL,
    leida_at                DATETIME2(0)  NULL
);
CREATE INDEX ix_alerta_dest ON alerta (destinatario_usuario_id, leida_at);
