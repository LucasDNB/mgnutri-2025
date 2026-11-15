-- Specs 08 / 09: actividad del paciente (registros, reemplazos, comentarios, molestias)
CREATE TABLE reemplazo_alimento (
    id                    BIGINT IDENTITY(1,1) PRIMARY KEY,
    item_comida_id        BIGINT       NOT NULL REFERENCES item_comida(id),
    alimento_reemplazo_id BIGINT       NOT NULL REFERENCES alimento(id),
    gramos                DECIMAL(7,1) NOT NULL,
    cantidad_casera       NVARCHAR(60) NULL,
    paciente_id           BIGINT       NOT NULL REFERENCES paciente(id),
    fecha_desde           DATE         NOT NULL,
    activo                BIT          NOT NULL DEFAULT 1
);
CREATE UNIQUE INDEX uq_reemplazo_alimento_activo ON reemplazo_alimento (item_comida_id) WHERE activo = 1;

CREATE TABLE registro_comida (
    id             BIGINT IDENTITY(1,1) PRIMARY KEY,
    paciente_id    BIGINT       NOT NULL REFERENCES paciente(id),
    comida_plan_id BIGINT       NOT NULL REFERENCES comida_plan(id),
    fecha          DATE         NOT NULL,
    realizada      BIT          NOT NULL DEFAULT 0,
    no_le_gusto    BIT          NOT NULL DEFAULT 0,
    le_cayo_mal    BIT          NOT NULL DEFAULT 0,
    created_at     DATETIME2(0) NOT NULL,
    CONSTRAINT uq_registro_comida UNIQUE (paciente_id, comida_plan_id, fecha)
);
CREATE INDEX ix_registro_comida_fecha ON registro_comida (paciente_id, fecha);

CREATE TABLE observacion_diaria (
    id             BIGINT IDENTITY(1,1) PRIMARY KEY,
    paciente_id    BIGINT        NOT NULL REFERENCES paciente(id),
    fecha          DATE          NOT NULL,
    nivel_hambre   INT           NOT NULL CONSTRAINT ck_obs_hambre CHECK (nivel_hambre BETWEEN 1 AND 5),
    nivel_saciedad INT           NOT NULL CONSTRAINT ck_obs_saciedad CHECK (nivel_saciedad BETWEEN 1 AND 5),
    estado_animo   INT           NULL CONSTRAINT ck_obs_animo CHECK (estado_animo BETWEEN 1 AND 5),
    comentario     NVARCHAR(300) NULL,
    CONSTRAINT uq_observacion_diaria UNIQUE (paciente_id, fecha)
);

CREATE TABLE reemplazo_ejercicio (
    id                     BIGINT IDENTITY(1,1) PRIMARY KEY,
    ejercicio_rutina_id    BIGINT NOT NULL REFERENCES ejercicio_rutina(id),
    ejercicio_reemplazo_id BIGINT NOT NULL REFERENCES ejercicio(id),
    paciente_id            BIGINT NOT NULL REFERENCES paciente(id),
    fecha                  DATE   NOT NULL,
    activo                 BIT    NOT NULL DEFAULT 1
);
CREATE UNIQUE INDEX uq_reemplazo_ejercicio_activo ON reemplazo_ejercicio (ejercicio_rutina_id) WHERE activo = 1;

CREATE TABLE registro_entrenamiento (
    id                      BIGINT IDENTITY(1,1) PRIMARY KEY,
    paciente_id             BIGINT       NOT NULL REFERENCES paciente(id),
    ejercicio_rutina_id     BIGINT       NOT NULL REFERENCES ejercicio_rutina(id),
    ejercicio_id            BIGINT       NOT NULL REFERENCES ejercicio(id),
    fecha                   DATE         NOT NULL,
    completado              BIT          NOT NULL DEFAULT 0,
    series_realizadas       INT          NULL,
    repeticiones_realizadas INT          NULL,
    carga_kg                DECIMAL(6,2) NULL,
    created_at              DATETIME2(0) NOT NULL,
    CONSTRAINT uq_registro_entrenamiento UNIQUE (paciente_id, ejercicio_rutina_id, fecha)
);
CREATE INDEX ix_registro_entrenamiento_fecha ON registro_entrenamiento (paciente_id, fecha);

CREATE TABLE comentario (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    paciente_id         BIGINT        NOT NULL REFERENCES paciente(id),
    tipo                NVARCHAR(10)  NOT NULL CONSTRAINT ck_comentario_tipo CHECK (tipo IN ('PLAN','RUTINA')),
    plan_id             BIGINT        NULL REFERENCES plan_alimentario(id),
    comida_plan_id      BIGINT        NULL REFERENCES comida_plan(id),
    rutina_id           BIGINT        NULL REFERENCES rutina(id),
    ejercicio_rutina_id BIGINT        NULL REFERENCES ejercicio_rutina(id),
    fecha               DATE          NOT NULL,
    texto               NVARCHAR(300) NOT NULL,
    created_at          DATETIME2(0)  NOT NULL,
    leido_at            DATETIME2(0)  NULL,
    leido_por           NVARCHAR(40)  NULL
);
CREATE INDEX ix_comentario_paciente ON comentario (paciente_id, fecha);

CREATE TABLE molestia (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    paciente_id         BIGINT        NOT NULL REFERENCES paciente(id),
    fecha               DATE          NOT NULL,
    descripcion         NVARCHAR(300) NOT NULL,
    intensidad          INT           NOT NULL CONSTRAINT ck_molestia_int CHECK (intensidad BETWEEN 1 AND 10),
    ubicacion_corporal  NVARCHAR(60)  NULL,
    ejercicio_rutina_id BIGINT        NULL REFERENCES ejercicio_rutina(id),
    contexto            NVARCHAR(15)  NOT NULL CONSTRAINT ck_molestia_ctx CHECK (contexto IN ('ENTRENAMIENTO','ALIMENTACION','GENERAL')),
    created_at          DATETIME2(0)  NOT NULL,
    revisada_at         DATETIME2(0)  NULL,
    revisada_por        NVARCHAR(40)  NULL
);
CREATE INDEX ix_molestia_paciente ON molestia (paciente_id, fecha);
