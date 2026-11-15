-- Spec 13: tokens de recupero de contraseña (se guarda el hash SHA-256)
CREATE TABLE token_recupero (
    id         BIGINT IDENTITY(1,1) PRIMARY KEY,
    usuario_id BIGINT       NOT NULL REFERENCES usuario(id),
    token_hash NVARCHAR(64) NOT NULL CONSTRAINT uq_token_recupero UNIQUE,
    expira_at  DATETIME2(0) NOT NULL,
    usado_at   DATETIME2(0) NULL,
    canal      NVARCHAR(20) NOT NULL,
    created_at DATETIME2(0) NOT NULL
);
CREATE INDEX ix_token_recupero_usuario ON token_recupero (usuario_id, created_at);
