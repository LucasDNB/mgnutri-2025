-- Spec 02 / 03: disponibilidad, bloqueos, turnos
CREATE TABLE disponibilidad (
    id             BIGINT IDENTITY(1,1) PRIMARY KEY,
    profesional_id BIGINT        NOT NULL REFERENCES profesional(id),
    dia_semana     INT           NOT NULL CONSTRAINT ck_disp_dia CHECK (dia_semana BETWEEN 1 AND 7),
    hora_desde     TIME(0)       NOT NULL,
    hora_hasta     TIME(0)       NOT NULL,
    ubicacion      NVARCHAR(120) NULL,
    activo         BIT           NOT NULL DEFAULT 1,
    CONSTRAINT ck_disp_horas CHECK (hora_desde < hora_hasta)
);

CREATE TABLE bloqueo_agenda (
    id             BIGINT IDENTITY(1,1) PRIMARY KEY,
    profesional_id BIGINT        NOT NULL REFERENCES profesional(id),
    desde          DATETIME2(0)  NOT NULL,
    hasta          DATETIME2(0)  NOT NULL,
    motivo         NVARCHAR(200) NULL,
    CONSTRAINT ck_bloqueo_rango CHECK (desde < hasta)
);

CREATE TABLE turno (
    id                       BIGINT IDENTITY(1,1) PRIMARY KEY,
    paciente_id              BIGINT        NOT NULL REFERENCES paciente(id),
    profesional_id           BIGINT        NOT NULL REFERENCES profesional(id),
    area                     NVARCHAR(15)  NOT NULL CONSTRAINT ck_turno_area CHECK (area IN ('NUTRICION','ENTRENAMIENTO')),
    inicio                   DATETIME2(0)  NOT NULL,
    fin                      DATETIME2(0)  NOT NULL,
    ubicacion                NVARCHAR(120) NULL,
    estado                   NVARCHAR(12)  NOT NULL CONSTRAINT ck_turno_estado CHECK (estado IN ('RESERVADO','CANCELADO','ATENDIDO','AUSENTE')),
    motivo_cancelacion       NVARCHAR(200) NULL,
    cancelado_por_usuario_id BIGINT        NULL REFERENCES usuario(id),
    cancelado_por_rol        NVARCHAR(20)  NULL,
    recordatorio_enviado_at  DATETIME2(0)  NULL,
    creado_por_usuario_id    BIGINT        NULL REFERENCES usuario(id),
    fuera_de_agenda          BIT           NOT NULL DEFAULT 0,
    created_at DATETIME2(0) NOT NULL, created_by NVARCHAR(40) NULL, updated_at DATETIME2(0) NULL, updated_by NVARCHAR(40) NULL,
    version INT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uq_turno_prof_inicio ON turno (profesional_id, inicio) WHERE estado = 'RESERVADO';
CREATE INDEX ix_turno_paciente ON turno (paciente_id, inicio);
CREATE INDEX ix_turno_profesional ON turno (profesional_id, inicio);

CREATE TABLE turno_historial (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    turno_id        BIGINT        NOT NULL REFERENCES turno(id),
    inicio_anterior DATETIME2(0)  NOT NULL,
    fin_anterior    DATETIME2(0)  NOT NULL,
    motivo          NVARCHAR(200) NULL,
    cambiado_por    NVARCHAR(40)  NULL,
    cambiado_at     DATETIME2(0)  NOT NULL
);
