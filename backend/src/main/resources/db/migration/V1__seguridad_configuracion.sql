-- Spec 13 / configuración: usuarios, roles, tokens revocados, configuración del sistema, T&C
CREATE TABLE usuario (
    id                        BIGINT IDENTITY(1,1) PRIMARY KEY,
    username                  NVARCHAR(20)  NOT NULL CONSTRAINT uq_usuario_username UNIQUE,
    password_hash             NVARCHAR(100) NOT NULL,
    estado                    NVARCHAR(20)  NOT NULL CONSTRAINT ck_usuario_estado CHECK (estado IN ('ACTIVO','BLOQUEADO','BAJA')),
    debe_cambiar_password     BIT           NOT NULL DEFAULT 1,
    intentos_fallidos         INT           NOT NULL DEFAULT 0,
    bloqueado_hasta           DATETIME2(0)  NULL,
    terminos_version_aceptada NVARCHAR(10)  NULL,
    terminos_aceptados_at     DATETIME2(0)  NULL,
    ultimo_acceso_at          DATETIME2(0)  NULL,
    tokens_validos_desde      DATETIME2(0)  NULL,
    created_at                DATETIME2(0)  NOT NULL,
    created_by                NVARCHAR(40)  NULL,
    updated_at                DATETIME2(0)  NULL,
    updated_by                NVARCHAR(40)  NULL,
    version                   INT           NOT NULL DEFAULT 0
);

CREATE TABLE usuario_rol (
    usuario_id BIGINT       NOT NULL REFERENCES usuario(id),
    rol        NVARCHAR(20) NOT NULL CONSTRAINT ck_usuario_rol CHECK (rol IN ('ADMIN','NUTRICIONISTA','ENTRENADOR','PACIENTE')),
    CONSTRAINT pk_usuario_rol PRIMARY KEY (usuario_id, rol)
);

CREATE TABLE token_revocado (
    jti       NVARCHAR(36) NOT NULL PRIMARY KEY,
    expira_at DATETIME2(0) NOT NULL
);

CREATE TABLE configuracion_sistema (
    clave       NVARCHAR(80)  NOT NULL PRIMARY KEY,
    valor       NVARCHAR(500) NULL,
    descripcion NVARCHAR(200) NULL,
    updated_at  DATETIME2(0)  NULL,
    updated_by  NVARCHAR(40)  NULL
);

CREATE TABLE terminos_condiciones (
    version        NVARCHAR(10)  NOT NULL PRIMARY KEY,
    texto          NVARCHAR(MAX) NOT NULL,
    vigente_desde  DATETIME2(0)  NOT NULL
);

INSERT INTO configuracion_sistema (clave, valor, descripcion) VALUES
 ('centro.nombre', N'MG Nutrición y Entrenamiento', N'Nombre del centro'),
 ('centro.telefono_whatsapp', N'+5491100000000', N'Teléfono de WhatsApp de la central (E.164)'),
 ('centro.direccion', N'Pilar, Buenos Aires', N'Dirección de la central'),
 ('centro.horario', N'Lunes a viernes de 8 a 20 h · Sábados de 9 a 13 h', N'Horario de atención'),
 ('centro.url_sistema', N'http://localhost:8080', N'URL pública del sistema (se usa en mensajes)'),
 ('turnos.recordatorio_hora', N'09:00', N'Hora del recordatorio diario de turnos (HH:mm)'),
 ('pagos.recordatorio_cron', N'0 0 10 * * SAT', N'Cron del recordatorio semanal de deuda'),
 ('pagos.link', N'', N'Enlace de pago (opcional)'),
 ('backup.habilitado', N'false', N'Backup automático habilitado'),
 ('backup.programacion_cron', N'0 0 3 * * SUN', N'Cron del backup automático'),
 ('backup.retencion_dias', N'30', N'Días que se conservan los archivos de backup'),
 ('mantenimiento.mensaje', N'', N'Banner de mantenimiento programado (vacío = oculto)'),
 ('terminos.version_vigente', N'1.0', N'Versión vigente de términos y condiciones');

INSERT INTO terminos_condiciones (version, texto, vigente_desde) VALUES ('1.0', N'TÉRMINOS Y CONDICIONES DE USO Y POLÍTICA DE PRIVACIDAD

1. Responsable. MG Nutrición y Entrenamiento es responsable de la base de datos en los términos de la Ley 25.326 de Protección de los Datos Personales.

2. Finalidad. Los datos personales y de salud que usted brinda se utilizan exclusivamente para la gestión de turnos, planes alimentarios, rutinas de entrenamiento, seguimiento de su evolución, cuenta corriente y comunicaciones del centro (incluidas notificaciones por WhatsApp).

3. Datos tratados. Datos identificatorios (nombre, documento, fecha de nacimiento), de contacto (teléfono, email, domicilio), antropométricos, de actividad y de salud informados por usted. Los datos sensibles se almacenan cifrados.

4. Derechos. Usted puede ejercer los derechos de acceso, rectificación, actualización y supresión de sus datos (arts. 14 a 16 de la Ley 25.326) solicitándolo en la central. La Agencia de Acceso a la Información Pública, órgano de control de la Ley 25.326, atiende denuncias y reclamos.

5. Alcance del servicio. El sistema no realiza diagnósticos ni reemplaza la consulta médica. Las propuestas automáticas son siempre revisadas por un profesional.

6. Seguridad. Su usuario y contraseña son personales e intransferibles. Los accesos quedan registrados.

Al marcar "Leí y acepto" usted presta su consentimiento libre, expreso e informado.', SYSUTCDATETIME());
