-- Specs 01 / 02: pacientes, profesionales, administradores, asignaciones
CREATE TABLE paciente (
    id                 BIGINT IDENTITY(1,1) PRIMARY KEY,
    usuario_id         BIGINT         NOT NULL CONSTRAINT uq_paciente_usuario UNIQUE REFERENCES usuario(id),
    nombre             NVARCHAR(80)   NOT NULL,
    apellido           NVARCHAR(80)   NOT NULL,
    tipo_documento     NVARCHAR(10)   NOT NULL,
    nro_documento      NVARCHAR(20)   NOT NULL CONSTRAINT uq_paciente_doc UNIQUE,
    fecha_nacimiento   DATE           NOT NULL,
    sexo               NVARCHAR(10)   NOT NULL CONSTRAINT ck_paciente_sexo CHECK (sexo IN ('F','M','X')),
    telefono_whatsapp  NVARCHAR(1024) NULL,
    email              NVARCHAR(1024) NULL,
    domicilio          NVARCHAR(1024) NULL,
    objetivo           NVARCHAR(300)  NULL,
    antecedentes_salud NVARCHAR(4000) NULL,
    observaciones      NVARCHAR(4000) NULL,
    estado             NVARCHAR(10)   NOT NULL CONSTRAINT ck_paciente_estado CHECK (estado IN ('ACTIVO','BAJA')),
    fecha_baja         DATE           NULL,
    motivo_baja        NVARCHAR(300)  NULL,
    created_at DATETIME2(0) NOT NULL, created_by NVARCHAR(40) NULL, updated_at DATETIME2(0) NULL, updated_by NVARCHAR(40) NULL,
    version INT NOT NULL DEFAULT 0
);
CREATE INDEX ix_paciente_apellido ON paciente (apellido, nombre);

CREATE TABLE profesional (
    id                 BIGINT IDENTITY(1,1) PRIMARY KEY,
    usuario_id         BIGINT         NOT NULL CONSTRAINT uq_profesional_usuario UNIQUE REFERENCES usuario(id),
    nombre             NVARCHAR(80)   NOT NULL,
    apellido           NVARCHAR(80)   NOT NULL,
    tipo_documento     NVARCHAR(10)   NOT NULL,
    nro_documento      NVARCHAR(20)   NOT NULL CONSTRAINT uq_profesional_doc UNIQUE,
    matricula          NVARCHAR(30)   NULL,
    tipo               NVARCHAR(15)   NOT NULL CONSTRAINT ck_profesional_tipo CHECK (tipo IN ('NUTRICIONISTA','ENTRENADOR','INTEGRAL')),
    especialidad       NVARCHAR(100)  NULL,
    telefono_whatsapp  NVARCHAR(1024) NULL,
    email              NVARCHAR(1024) NULL,
    duracion_turno_min INT            NOT NULL DEFAULT 30,
    estado             NVARCHAR(10)   NOT NULL CONSTRAINT ck_profesional_estado CHECK (estado IN ('ACTIVO','BAJA')),
    fecha_baja         DATE           NULL,
    created_at DATETIME2(0) NOT NULL, created_by NVARCHAR(40) NULL, updated_at DATETIME2(0) NULL, updated_by NVARCHAR(40) NULL,
    version INT NOT NULL DEFAULT 0
);

CREATE TABLE administrador (
    id         BIGINT IDENTITY(1,1) PRIMARY KEY,
    usuario_id BIGINT         NOT NULL CONSTRAINT uq_admin_usuario UNIQUE REFERENCES usuario(id),
    nombre     NVARCHAR(80)   NOT NULL,
    apellido   NVARCHAR(80)   NOT NULL,
    telefono   NVARCHAR(1024) NULL
);

CREATE TABLE paciente_profesional (
    id               BIGINT IDENTITY(1,1) PRIMARY KEY,
    paciente_id      BIGINT       NOT NULL REFERENCES paciente(id),
    profesional_id   BIGINT       NOT NULL REFERENCES profesional(id),
    area             NVARCHAR(15) NOT NULL CONSTRAINT ck_pp_area CHECK (area IN ('NUTRICION','ENTRENAMIENTO')),
    fecha_asignacion DATE         NOT NULL,
    activo           BIT          NOT NULL DEFAULT 1
);
CREATE UNIQUE INDEX uq_pp_activo ON paciente_profesional (paciente_id, profesional_id, area) WHERE activo = 1;
