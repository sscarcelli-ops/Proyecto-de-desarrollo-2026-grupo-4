-- Seed base del camino feliz (solo perfil seed: SPRING_PROFILES_ACTIVE=dev,seed). Nunca en prod.
-- Todos los usuarios tienen la contraseña Prueba123 (bcrypt, mismo formato que BCryptPasswordEncoder).
--
-- Reglas para quien agregue datos de prueba:
--   * Un archivo nuevo y fechado por US (V<fecha>__seed_<tema>.sql). Nunca editar uno ya aplicado.
--   * Los id son GENERATED ALWAYS: no se escriben a mano. Las relaciones se arman buscando por un dato
--     único (email, nombre del barrio, CUIT, DNI + perfil).
--   * Las fechas son relativas a CURRENT_DATE / now() para que "vigente" y "vencido" sigan valiendo.
--   * Las rutas de archivos todavía no tienen archivo real: se agregan con el FileStorage (T-05).

-- ============================================================================================
-- Usuarios (email único) y roles
-- ============================================================================================
INSERT INTO usuario (email, password_hash) VALUES
    ('admin@seed.test',             '$2a$10$p5r72fnKScsMFwcSBC5ILOwGeEo2byCvzm0fMI73oT6VEugMlDHWq'),
    ('operador@seed.test',          '$2a$10$p5r72fnKScsMFwcSBC5ILOwGeEo2byCvzm0fMI73oT6VEugMlDHWq'),
    ('operador.libre@seed.test',    '$2a$10$p5r72fnKScsMFwcSBC5ILOwGeEo2byCvzm0fMI73oT6VEugMlDHWq'),
    ('guardia@seed.test',           '$2a$10$p5r72fnKScsMFwcSBC5ILOwGeEo2byCvzm0fMI73oT6VEugMlDHWq'),
    ('empresa@seed.test',           '$2a$10$p5r72fnKScsMFwcSBC5ILOwGeEo2byCvzm0fMI73oT6VEugMlDHWq'),
    ('mono@seed.test',              '$2a$10$p5r72fnKScsMFwcSBC5ILOwGeEo2byCvzm0fMI73oT6VEugMlDHWq'),
    ('mono.pendiente@seed.test',    '$2a$10$p5r72fnKScsMFwcSBC5ILOwGeEo2byCvzm0fMI73oT6VEugMlDHWq'),
    ('mono.rechazado@seed.test',    '$2a$10$p5r72fnKScsMFwcSBC5ILOwGeEo2byCvzm0fMI73oT6VEugMlDHWq');

-- EMPRESA / MONOTRIBUTISTA solo para perfiles APROBADOS (los pendientes y rechazados no tienen rol, RN-12)
INSERT INTO usuario_rol (usuario_id, rol_id)
SELECT u.id, r.id
FROM (VALUES
        ('admin@seed.test',          'ADMIN'),
        ('operador@seed.test',       'OPERADOR'),
        ('operador.libre@seed.test', 'OPERADOR'),
        ('guardia@seed.test',        'GUARDIA'),
        ('empresa@seed.test',        'EMPRESA'),
        ('mono@seed.test',           'MONOTRIBUTISTA')
     ) AS v(email, rol)
JOIN usuario u ON u.email = v.email
JOIN rol r     ON r.codigo = v.rol;

-- ============================================================================================
-- Operadores y barrios: uno Activo con Operador, uno Inactivo sin Operador (RN-10)
-- operador.libre queda "Disponible" para probar la asignación (US-45)
-- ============================================================================================
INSERT INTO operador (usuario_id, nombre, apellido, telefono)
SELECT id, 'Olga', 'Operadora', '1145550001' FROM usuario WHERE email = 'operador@seed.test'
UNION ALL
SELECT id, 'Oscar', 'Libre', '1145550002' FROM usuario WHERE email = 'operador.libre@seed.test';

INSERT INTO barrio (nombre, localidad_provincia, direccion, estado, operador_id)
SELECT 'Barrio Los Álamos', 'Pilar, Buenos Aires', 'Ruta 8 km 50', 'ACTIVO', id
FROM usuario WHERE email = 'operador@seed.test';

INSERT INTO barrio (nombre, localidad_provincia, direccion, estado, operador_id)
VALUES ('Barrio San Jorge', 'Escobar, Buenos Aires', 'Calle 888', 'INACTIVO', NULL);

INSERT INTO guardia (usuario_id, barrio_id, nombre, apellido, habilitado)
SELECT u.id, b.id, 'Gustavo', 'Guardia', TRUE
FROM usuario u, barrio b
WHERE u.email = 'guardia@seed.test' AND b.nombre = 'Barrio Los Álamos';

-- ============================================================================================
-- Perfiles: Empresa aprobada, Monotributista aprobado, uno Pendiente y uno Rechazado con motivo
-- ============================================================================================
INSERT INTO perfil (usuario_id, tipo, estado, cuit_cuil, razon_social, telefono, constancia_ruta, resuelto_en)
SELECT id, 'EMPRESA', 'APROBADO', '30-71234567-1', 'Limpiezas del Norte S.A.', '1145551000',
       'perfiles/seed/empresa/constancia.pdf', now()
FROM usuario WHERE email = 'empresa@seed.test';

-- El DNI 30111222 del Monotributista aprobado también trabaja para la Empresa (mismo DNI en dos vínculos, RN-08)
INSERT INTO perfil (usuario_id, tipo, estado, cuit_cuil, nombre, apellido, dni_titular, telefono, constancia_ruta, resuelto_en)
SELECT id, 'MONOTRIBUTISTA', 'APROBADO', '20-30111222-0', 'Juan', 'Pérez', '30111222', '1145552000',
       'perfiles/seed/mono/constancia.pdf', now()
FROM usuario WHERE email = 'mono@seed.test';

INSERT INTO perfil (usuario_id, tipo, estado, cuit_cuil, nombre, apellido, dni_titular, constancia_ruta)
SELECT id, 'MONOTRIBUTISTA', 'PENDIENTE', '27-33222111-1', 'Paula', 'Pendiente', '33222111',
       'perfiles/seed/mono-pendiente/constancia.pdf'
FROM usuario WHERE email = 'mono.pendiente@seed.test';

INSERT INTO perfil (usuario_id, tipo, estado, cuit_cuil, nombre, apellido, dni_titular, constancia_ruta,
                    motivo_rechazo, resuelto_en)
SELECT id, 'MONOTRIBUTISTA', 'RECHAZADO', '20-31444555-5', 'Raúl', 'Rechazado', '31444555',
       'perfiles/seed/mono-rechazado/constancia.pdf',
       'La constancia de inscripción no corresponde al CUIL declarado.', now()
FROM usuario WHERE email = 'mono.rechazado@seed.test';

-- La Empresa está habilitada en el barrio activo. El Monotributista aprobado NO (motivo "no habilitado en el barrio").
INSERT INTO habilitacion_barrio (perfil_id, barrio_id)
SELECT p.id, b.id
FROM perfil p, barrio b
WHERE p.cuit_cuil = '30-71234567-1' AND b.nombre = 'Barrio Los Álamos';

-- ============================================================================================
-- Trabajadores de la Empresa, uno por estado documental, y el Monotributista como su propio trabajador (RF-09)
-- ============================================================================================
INSERT INTO trabajador (perfil_id, nombre, apellido, dni, fecha_baja)
SELECT p.id, v.nombre, v.apellido, v.dni, v.fecha_baja
FROM perfil p,
     (VALUES
        ('Juan',   'Pérez',     '30111222', NULL::timestamptz),  -- vigente, con ingreso abierto
        ('Ana',    'Vigente',   '30777888', NULL),               -- vigente, listo para ingresar (sin foto)
        ('Carlos', 'Vencido',   '30333444', NULL),               -- ART vencida
        ('Marta',  'Pendiente', '30555666', NULL),               -- sin documentación
        ('Bruno',  'Baja',      '30999000', now() - INTERVAL '5 days')  -- dado de baja en este vínculo
     ) AS v(nombre, apellido, dni, fecha_baja)
WHERE p.cuit_cuil = '30-71234567-1';

INSERT INTO trabajador (perfil_id, nombre, apellido, dni)
SELECT id, nombre, apellido, dni_titular FROM perfil WHERE cuit_cuil = '20-30111222-0';

-- ============================================================================================
-- Documentación (el estado se calcula con el vencimiento más próximo, RN-04)
-- ============================================================================================
INSERT INTO documento (trabajador_id, tipo, archivo_ruta, fecha_vencimiento)
SELECT t.id, v.tipo, 'trabajadores/seed/' || t.dni || '/' || lower(v.tipo) || '.pdf', CURRENT_DATE + v.dias
FROM trabajador t
JOIN perfil p ON p.id = t.perfil_id AND p.cuit_cuil = '30-71234567-1'
JOIN (VALUES
        ('30111222', 'ART',         365),
        ('30111222', 'SEGURO_VIDA', 365),
        ('30777888', 'ART',         180),
        ('30777888', 'SEGURO_VIDA', 365),
        ('30333444', 'ART',         -10),
        ('30333444', 'SEGURO_VIDA', 365),
        ('30999000', 'ART',         365),
        ('30999000', 'SEGURO_VIDA', 365)
     ) AS v(dni, tipo, dias) ON v.dni = t.dni;

-- ============================================================================================
-- Un ingreso abierto (Juan Pérez por la Empresa): para probar el egreso y el error de ingreso activo,
-- también al intentar ingresarlo por su vínculo de Monotributista (RN-08)
-- ============================================================================================
INSERT INTO movimiento (trabajador_id, barrio_id, guardia_id, tipo, fecha_hora)
SELECT t.id, b.id, g.usuario_id, 'INGRESO', now() - INTERVAL '2 hours'
FROM trabajador t
JOIN perfil p  ON p.id = t.perfil_id AND p.cuit_cuil = '30-71234567-1'
JOIN barrio b  ON b.nombre = 'Barrio Los Álamos'
JOIN guardia g ON g.barrio_id = b.id
WHERE t.dni = '30111222';
