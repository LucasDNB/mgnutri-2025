-- Spec 05: rutinas de entrenamiento
CREATE TABLE rutina (
    id               BIGINT IDENTITY(1,1) PRIMARY KEY,
    paciente_id      BIGINT         NOT NULL REFERENCES paciente(id),
    entrenador_id    BIGINT         NOT NULL REFERENCES profesional(id),
    nombre           NVARCHAR(120)  NOT NULL,
    objetivo         NVARCHAR(300)  NULL,
    fecha_inicio     DATE           NOT NULL,
    duracion_semanas INT            NOT NULL,
    fecha_fin        DATE           NOT NULL,
    estado           NVARCHAR(12)   NOT NULL CONSTRAINT ck_rutina_estado CHECK (estado IN ('ACTIVA','FINALIZADA','ELIMINADA')),
    observaciones    NVARCHAR(1000) NULL,
    created_at DATETIME2(0) NOT NULL, created_by NVARCHAR(40) NULL, updated_at DATETIME2(0) NULL, updated_by NVARCHAR(40) NULL,
    version INT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uq_rutina_activa ON rutina (paciente_id) WHERE estado = 'ACTIVA';

CREATE TABLE dia_rutina (
    id         BIGINT IDENTITY(1,1) PRIMARY KEY,
    rutina_id  BIGINT       NOT NULL REFERENCES rutina(id),
    dia_semana INT          NOT NULL CONSTRAINT ck_dia_rutina CHECK (dia_semana BETWEEN 1 AND 7),
    nombre     NVARCHAR(80) NULL,
    orden      INT          NOT NULL DEFAULT 0,
    activo     BIT          NOT NULL DEFAULT 1
);

CREATE TABLE ejercicio_rutina (
    id             BIGINT IDENTITY(1,1) PRIMARY KEY,
    dia_rutina_id  BIGINT        NOT NULL REFERENCES dia_rutina(id),
    ejercicio_id   BIGINT        NOT NULL REFERENCES ejercicio(id),
    orden          INT           NOT NULL DEFAULT 0,
    series         INT           NOT NULL CONSTRAINT ck_er_series CHECK (series BETWEEN 1 AND 20),
    repeticiones   NVARCHAR(20)  NOT NULL,
    descanso_seg   INT           NULL,
    carga_sugerida NVARCHAR(30)  NULL,
    notas          NVARCHAR(300) NULL,
    activo         BIT           NOT NULL DEFAULT 1
);
