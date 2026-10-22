-- T-01 Modelo de datos inicial (PostgreSQL / Flyway)
-- Estados como VARCHAR + CHECK (más simple con Hibernate ddl-auto=validate que los ENUM nativos).
-- Archivos: solo se guarda la ruta relativa. El estado documental NO se guarda: se calcula (RNF-04).

CREATE TABLE usuario (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email          VARCHAR(255) NOT NULL,
    password_hash  VARCHAR(100) NOT NULL,
    creado_en      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uq_usuario_email ON usuario (lower(email));

-- RBAC con roles fijos (sin tabla de permisos). Un usuario puede tener 0, 1 o más roles (RN-12, RF-08).
-- EMPRESA / MONOTRIBUTISTA se asignan al APROBAR el perfil (US-20); ADMIN / OPERADOR / GUARDIA al crear la cuenta.
CREATE TABLE rol (
    id      SMALLINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo  VARCHAR(20) NOT NULL UNIQUE,
    nombre  VARCHAR(50) NOT NULL
);
INSERT INTO rol (codigo, nombre) VALUES
    ('ADMIN',          'Administrador'),
    ('OPERADOR',       'Operador'),
    ('GUARDIA',        'Guardia'),
    ('EMPRESA',        'Empresa'),
    ('MONOTRIBUTISTA', 'Monotributista');

CREATE TABLE usuario_rol (
    usuario_id  BIGINT   NOT NULL REFERENCES usuario(id),
    rol_id      SMALLINT NOT NULL REFERENCES rol(id),
    asignado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (usuario_id, rol_id)
);
CREATE INDEX ix_usuario_rol_rol ON usuario_rol (rol_id);

-- Datos propios de cada rol (el estado/alcance de cada rol vive acá, no en usuario_rol).
-- Crear la fila de usuario_rol y la de operador/guardia/perfil en la MISMA transacción.
CREATE TABLE operador (
    usuario_id  BIGINT PRIMARY KEY REFERENCES usuario(id),
    nombre      VARCHAR(100) NOT NULL,
    apellido    VARCHAR(100) NOT NULL,
    telefono    VARCHAR(30)  NOT NULL
);

-- El vínculo operador-barrio vive solo en BARRIO.operador_id (UNIQUE) => 1 operador por barrio y 1 barrio por operador.
CREATE TABLE barrio (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre               VARCHAR(150) NOT NULL,
    localidad_provincia  VARCHAR(150) NOT NULL,
    direccion            VARCHAR(255) NOT NULL,
    estado               VARCHAR(10)  NOT NULL DEFAULT 'INACTIVO' CHECK (estado IN ('ACTIVO','INACTIVO')),
    operador_id          BIGINT UNIQUE REFERENCES operador(usuario_id),
    creado_en            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    -- RN-10: no se puede tener un barrio Activo sin Operador
    CONSTRAINT ck_barrio_activo_con_operador CHECK (estado = 'INACTIVO' OR operador_id IS NOT NULL)
);

CREATE TABLE guardia (
    usuario_id  BIGINT PRIMARY KEY REFERENCES usuario(id),
    barrio_id   BIGINT       NOT NULL REFERENCES barrio(id),
    nombre      VARCHAR(100) NOT NULL,
    apellido    VARCHAR(100) NOT NULL,
    habilitado  BOOLEAN      NOT NULL DEFAULT TRUE   -- baja lógica reversible (RN-09)
);
CREATE INDEX ix_guardia_barrio ON guardia (barrio_id);

-- Empresa o Monotributista. Sin fila = "sin perfil" (paso 3 del registro sin completar).
CREATE TABLE perfil (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id         BIGINT       NOT NULL UNIQUE REFERENCES usuario(id),  -- UNIQUE: 1 perfil por cuenta en el MVP
    tipo               VARCHAR(15)  NOT NULL CHECK (tipo IN ('EMPRESA','MONOTRIBUTISTA')),
    estado             VARCHAR(15)  NOT NULL DEFAULT 'PENDIENTE'
                       CHECK (estado IN ('PENDIENTE','APROBADO','RECHAZADO','SUSPENDIDO')),
    cuit_cuil          VARCHAR(13)  NOT NULL UNIQUE,
    razon_social       VARCHAR(200),            -- solo Empresa
    nombre             VARCHAR(100),            -- solo Monotributista
    apellido           VARCHAR(100),            -- solo Monotributista
    dni_titular        VARCHAR(10),             -- solo Monotributista
    telefono           VARCHAR(30),
    constancia_ruta    VARCHAR(300) NOT NULL,
    motivo_rechazo     VARCHAR(500),
    motivo_suspension  VARCHAR(500),
    creado_en          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    resuelto_en        TIMESTAMPTZ,
    CONSTRAINT ck_perfil_empresa CHECK (
        tipo <> 'EMPRESA' OR (razon_social IS NOT NULL AND nombre IS NULL AND apellido IS NULL AND dni_titular IS NULL)),
    CONSTRAINT ck_perfil_mono CHECK (
        tipo <> 'MONOTRIBUTISTA' OR (razon_social IS NULL AND nombre IS NOT NULL AND apellido IS NOT NULL AND dni_titular IS NOT NULL)),
    CONSTRAINT ck_perfil_rechazo CHECK (estado <> 'RECHAZADO' OR motivo_rechazo IS NOT NULL)
);
-- Un DNI solo puede estar asociado a un Monotributista (RF-08)
CREATE UNIQUE INDEX uq_perfil_mono_dni ON perfil (dni_titular) WHERE tipo = 'MONOTRIBUTISTA';
CREATE INDEX ix_perfil_estado ON perfil (estado);

-- Sin registro = deshabilitada (RN-02, US-20). Habilitar = INSERT, deshabilitar = DELETE.
CREATE TABLE habilitacion_barrio (
    perfil_id     BIGINT      NOT NULL REFERENCES perfil(id),
    barrio_id     BIGINT      NOT NULL REFERENCES barrio(id),
    habilitado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (perfil_id, barrio_id)
);
CREATE INDEX ix_habilitacion_barrio ON habilitacion_barrio (barrio_id);

-- Cada perfil carga sus propios trabajadores; no se comparten por DNI (RN-11). Cada fila ES el "vínculo".
CREATE TABLE trabajador (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    perfil_id   BIGINT       NOT NULL REFERENCES perfil(id),
    nombre      VARCHAR(100) NOT NULL,
    apellido    VARCHAR(100) NOT NULL,
    dni         VARCHAR(10)  NOT NULL,
    foto_ruta   VARCHAR(300),
    fecha_baja  TIMESTAMPTZ,                         -- NULL = activo en ese vínculo (RN-05)
    creado_en   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_trabajador_perfil_dni UNIQUE (perfil_id, dni)   -- DNI único dentro del perfil
);
CREATE INDEX ix_trabajador_dni ON trabajador (dni);               -- búsqueda de la garita

-- Estado documental = Vigente / Vencido / Pendiente de carga, calculado con MIN(fecha_vencimiento).
-- Renovar reemplaza el archivo anterior (sin historial), por eso UNIQUE (trabajador_id, tipo).
CREATE TABLE documento (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    trabajador_id      BIGINT       NOT NULL REFERENCES trabajador(id),
    tipo               VARCHAR(15)  NOT NULL CHECK (tipo IN ('ART','SEGURO_VIDA')),
    archivo_ruta       VARCHAR(300) NOT NULL,
    fecha_vencimiento  DATE         NOT NULL,
    actualizado_en     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_documento_trabajador_tipo UNIQUE (trabajador_id, tipo)
);

-- Solo INSERT, nunca UPDATE/DELETE (RN-06). Ingreso activo = último movimiento del trabajador en el barrio es INGRESO.
CREATE TABLE movimiento (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    trabajador_id  BIGINT      NOT NULL REFERENCES trabajador(id),
    barrio_id      BIGINT      NOT NULL REFERENCES barrio(id),
    guardia_id     BIGINT      REFERENCES guardia(usuario_id),   -- NULL en egresos automáticos
    tipo           VARCHAR(10) NOT NULL CHECK (tipo IN ('INGRESO','EGRESO')),
    fecha_hora     TIMESTAMPTZ NOT NULL DEFAULT now(),
    motivo         VARCHAR(200),                                  -- ej. 'Egreso automático por suspensión de Empresa o Monotributista'
    CONSTRAINT ck_mov_automatico CHECK (guardia_id IS NOT NULL OR (tipo = 'EGRESO' AND motivo IS NOT NULL))
);
CREATE INDEX ix_mov_barrio_fecha      ON movimiento (barrio_id, fecha_hora DESC);       -- RI-01
CREATE INDEX ix_mov_trabajador_fecha  ON movimiento (trabajador_id, barrio_id, fecha_hora DESC);  -- ingreso activo / RI-02

-- Activación del Administrador (usuario_id NULL) y recupero de contraseña. Se guarda el hash, no el token.
CREATE TABLE token (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tipo        VARCHAR(20)  NOT NULL CHECK (tipo IN ('ACTIVACION_ADMIN','RECUPERO')),
    usuario_id  BIGINT       REFERENCES usuario(id),
    token_hash  VARCHAR(100) NOT NULL UNIQUE,
    expira_en   TIMESTAMPTZ  NOT NULL,
    usado_en    TIMESTAMPTZ,
    CONSTRAINT ck_token_usuario CHECK (tipo = 'ACTIVACION_ADMIN' OR usuario_id IS NOT NULL)
);
