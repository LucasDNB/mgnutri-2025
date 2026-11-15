-- Spec 10: catálogos de alimentos y ejercicios (estructura)
CREATE TABLE grupo_alimento (
    id               BIGINT IDENTITY(1,1) PRIMARY KEY,
    nombre           NVARCHAR(60)  NOT NULL CONSTRAINT uq_grupo_alimento UNIQUE,
    macro_referencia NVARCHAR(12)  NOT NULL CONSTRAINT ck_grupo_macro CHECK (macro_referencia IN ('HC','PROTEINAS','GRASAS','KCAL')),
    descripcion      NVARCHAR(200) NULL
);

CREATE TABLE alimento (
    id                   BIGINT IDENTITY(1,1) PRIMARY KEY,
    nombre               NVARCHAR(120) NOT NULL CONSTRAINT uq_alimento_nombre UNIQUE,
    nombre_normalizado   NVARCHAR(120) NOT NULL CONSTRAINT uq_alimento_norm UNIQUE,
    grupo_alimento_id    BIGINT        NOT NULL REFERENCES grupo_alimento(id),
    kcal_100g            DECIMAL(7,2)  NOT NULL,
    hc_100g              DECIMAL(7,2)  NOT NULL,
    proteinas_100g       DECIMAL(7,2)  NOT NULL,
    grasas_100g          DECIMAL(7,2)  NOT NULL,
    fibra_100g           DECIMAL(7,2)  NOT NULL DEFAULT 0,
    sodio_mg_100g        DECIMAL(8,2)  NULL,
    unidad_casera        NVARCHAR(60)  NULL,
    gramos_unidad_casera DECIMAL(7,1)  NULL,
    apto_desayuno        BIT NOT NULL DEFAULT 0,
    apto_colacion        BIT NOT NULL DEFAULT 0,
    apto_almuerzo        BIT NOT NULL DEFAULT 0,
    activo               BIT NOT NULL DEFAULT 1,
    created_at DATETIME2(0) NOT NULL, created_by NVARCHAR(40) NULL, updated_at DATETIME2(0) NULL, updated_by NVARCHAR(40) NULL,
    version INT NOT NULL DEFAULT 0,
    CONSTRAINT ck_alimento_valores CHECK (kcal_100g >= 0 AND kcal_100g <= 900 AND hc_100g >= 0 AND proteinas_100g >= 0 AND grasas_100g >= 0 AND fibra_100g >= 0
                                          AND hc_100g + proteinas_100g + grasas_100g + fibra_100g <= 100)
);

CREATE TABLE equivalencia_manual (
    id                      BIGINT IDENTITY(1,1) PRIMARY KEY,
    alimento_id             BIGINT        NOT NULL REFERENCES alimento(id),
    alimento_equivalente_id BIGINT        NOT NULL REFERENCES alimento(id),
    gramos_origen           DECIMAL(7,1)  NOT NULL,
    gramos_equivalente      DECIMAL(7,1)  NOT NULL,
    nota                    NVARCHAR(120) NULL
);

CREATE TABLE grupo_muscular (
    id     BIGINT IDENTITY(1,1) PRIMARY KEY,
    nombre NVARCHAR(60) NOT NULL CONSTRAINT uq_grupo_muscular UNIQUE
);

CREATE TABLE ejercicio (
    id                          BIGINT IDENTITY(1,1) PRIMARY KEY,
    nombre                      NVARCHAR(120) NOT NULL CONSTRAINT uq_ejercicio_nombre UNIQUE,
    nombre_normalizado          NVARCHAR(120) NOT NULL CONSTRAINT uq_ejercicio_norm UNIQUE,
    descripcion                 NVARCHAR(500) NULL,
    grupo_muscular_principal_id BIGINT        NOT NULL REFERENCES grupo_muscular(id),
    dificultad                  NVARCHAR(10)  NOT NULL CONSTRAINT ck_ejercicio_dif CHECK (dificultad IN ('BAJA','MEDIA','ALTA')),
    equipamiento                NVARCHAR(100) NULL,
    video_url                   NVARCHAR(300) NULL,
    activo                      BIT NOT NULL DEFAULT 1,
    created_at DATETIME2(0) NOT NULL, created_by NVARCHAR(40) NULL, updated_at DATETIME2(0) NULL, updated_by NVARCHAR(40) NULL,
    version INT NOT NULL DEFAULT 0
);

CREATE TABLE ejercicio_grupo_secundario (
    ejercicio_id      BIGINT NOT NULL REFERENCES ejercicio(id),
    grupo_muscular_id BIGINT NOT NULL REFERENCES grupo_muscular(id),
    CONSTRAINT pk_ejercicio_grupo_sec PRIMARY KEY (ejercicio_id, grupo_muscular_id)
);

INSERT INTO grupo_alimento (nombre, macro_referencia, descripcion) VALUES
 (N'Lácteos', 'PROTEINAS', N'Leches, yogures y quesos'),
 (N'Carnes y huevos', 'PROTEINAS', N'Carnes rojas, aves, pescados, huevos'),
 (N'Cereales, legumbres y derivados', 'HC', N'Cereales, panes, pastas, legumbres'),
 (N'Frutas', 'HC', N'Frutas frescas y secas'),
 (N'Verduras', 'HC', N'Hortalizas y verduras'),
 (N'Aceites y grasas', 'GRASAS', N'Aceites, manteca, palta'),
 (N'Frutos secos y semillas', 'GRASAS', N'Nueces, almendras, semillas'),
 (N'Azúcares y dulces', 'HC', N'Azúcar, dulces, mermeladas'),
 (N'Bebidas e infusiones', 'KCAL', N'Bebidas, jugos, infusiones');

INSERT INTO grupo_muscular (nombre) VALUES
 (N'Pecho'), (N'Espalda'), (N'Hombros'), (N'Bíceps'), (N'Tríceps'), (N'Antebrazos'), (N'Core/Abdominales'),
 (N'Glúteos'), (N'Cuádriceps'), (N'Isquiotibiales'), (N'Aductores'), (N'Pantorrillas'), (N'Cuerpo completo'), (N'Cardiovascular');
