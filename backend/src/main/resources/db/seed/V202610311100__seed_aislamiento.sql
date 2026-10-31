-- Seed de aislamiento (T-03, US-41, US-42): un segundo barrio activo con su Operador y su Guardia, y una segunda
-- Empresa aprobada con su propio trabajador. Sirve para probar que cada barrio y cada Empresa solo ven lo suyo.
--
-- Todo es nuevo (usuarios, CUIT, DNI y barrio): no cambia lo que ven los usuarios de los seeds anteriores.
-- Contraseña Prueba123, como el resto.

INSERT INTO usuario (email, password_hash) VALUES
    ('operador.robles@seed.test', '$2a$10$p5r72fnKScsMFwcSBC5ILOwGeEo2byCvzm0fMI73oT6VEugMlDHWq'),
    ('guardia.robles@seed.test',  '$2a$10$p5r72fnKScsMFwcSBC5ILOwGeEo2byCvzm0fMI73oT6VEugMlDHWq'),
    ('empresa.sur@seed.test',     '$2a$10$p5r72fnKScsMFwcSBC5ILOwGeEo2byCvzm0fMI73oT6VEugMlDHWq');

INSERT INTO usuario_rol (usuario_id, rol_id)
SELECT u.id, r.id
FROM (VALUES
        ('operador.robles@seed.test', 'OPERADOR'),
        ('guardia.robles@seed.test',  'GUARDIA'),
        ('empresa.sur@seed.test',     'EMPRESA')
     ) AS v(email, rol)
JOIN usuario u ON u.email = v.email
JOIN rol r     ON r.codigo = v.rol;

-- Segundo barrio activo, con su Operador y su Guardia
INSERT INTO operador (usuario_id, nombre, apellido, telefono)
SELECT id, 'Romina', 'Robles', '1145550004' FROM usuario WHERE email = 'operador.robles@seed.test';

INSERT INTO barrio (nombre, localidad_provincia, direccion, estado, operador_id)
SELECT 'Barrio Los Robles', 'Luján, Buenos Aires', 'Av. Constitución 500', 'ACTIVO', id
FROM usuario WHERE email = 'operador.robles@seed.test';

INSERT INTO guardia (usuario_id, barrio_id, nombre, apellido, habilitado)
SELECT u.id, b.id, 'Rodrigo', 'Robles', TRUE
FROM usuario u, barrio b
WHERE u.email = 'guardia.robles@seed.test' AND b.nombre = 'Barrio Los Robles';

-- Segunda Empresa, habilitada solo en Los Robles, con un trabajador con la documentación al día
INSERT INTO perfil (usuario_id, tipo, estado, cuit_cuil, razon_social, telefono, constancia_ruta, resuelto_en)
SELECT id, 'EMPRESA', 'APROBADO', '30-71888999-2', 'Jardines del Sur S.R.L.', '1145553000',
       'perfiles/seed/empresa-sur/constancia.pdf', now()
FROM usuario WHERE email = 'empresa.sur@seed.test';

INSERT INTO habilitacion_barrio (perfil_id, barrio_id)
SELECT p.id, b.id
FROM perfil p, barrio b
WHERE p.cuit_cuil = '30-71888999-2' AND b.nombre = 'Barrio Los Robles';

INSERT INTO trabajador (perfil_id, nombre, apellido, dni)
SELECT id, 'Sofía', 'Sur', '31222333' FROM perfil WHERE cuit_cuil = '30-71888999-2';

INSERT INTO documento (trabajador_id, tipo, archivo_ruta, fecha_vencimiento)
SELECT t.id, v.tipo, 'trabajadores/seed/' || t.dni || '/' || lower(v.tipo) || '.pdf', CURRENT_DATE + v.dias
FROM trabajador t
JOIN perfil p ON p.id = t.perfil_id AND p.cuit_cuil = '30-71888999-2'
JOIN (VALUES ('ART', 365), ('SEGURO_VIDA', 365)) AS v(tipo, dias) ON TRUE
WHERE t.dni = '31222333';
