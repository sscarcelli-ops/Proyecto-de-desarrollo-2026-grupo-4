-- Seed de US-04 (CA-3): cuentas con la contraseña correcta que NO pueden iniciar sesión.
-- Contraseña de todas: Prueba123. Usa los barrios del seed base (Los Álamos activo, San Jorge inactivo).

INSERT INTO usuario (email, password_hash) VALUES
    ('mono.suspendido@seed.test',     '$2a$10$p5r72fnKScsMFwcSBC5ILOwGeEo2byCvzm0fMI73oT6VEugMlDHWq'),
    ('guardia.deshabilitado@seed.test', '$2a$10$p5r72fnKScsMFwcSBC5ILOwGeEo2byCvzm0fMI73oT6VEugMlDHWq'),
    ('guardia.inactivo@seed.test',    '$2a$10$p5r72fnKScsMFwcSBC5ILOwGeEo2byCvzm0fMI73oT6VEugMlDHWq');

-- Al suspender se conserva el rol aprobado: lo que impide operar es perfil.estado = 'SUSPENDIDO' (RF-06)
INSERT INTO usuario_rol (usuario_id, rol_id)
SELECT u.id, r.id
FROM (VALUES
        ('mono.suspendido@seed.test',       'MONOTRIBUTISTA'),
        ('guardia.deshabilitado@seed.test', 'GUARDIA'),
        ('guardia.inactivo@seed.test',      'GUARDIA')
     ) AS v(email, rol)
JOIN usuario u ON u.email = v.email
JOIN rol r     ON r.codigo = v.rol;

-- "Tu cuenta se encuentra suspendida"
INSERT INTO perfil (usuario_id, tipo, estado, cuit_cuil, nombre, apellido, dni_titular, constancia_ruta,
                    motivo_suspension, resuelto_en)
SELECT id, 'MONOTRIBUTISTA', 'SUSPENDIDO', '20-32555661-8', 'Sergio', 'Suspendido', '32555661',
       'perfiles/seed/mono-suspendido/constancia.pdf', 'Documentación fiscal adeudada.', now()
FROM usuario WHERE email = 'mono.suspendido@seed.test';

-- Como Monotributista aprobado antes de la suspensión, es su propio trabajador (RF-09)
INSERT INTO trabajador (perfil_id, nombre, apellido, dni)
SELECT id, nombre, apellido, dni_titular FROM perfil WHERE cuit_cuil = '20-32555661-8';

-- "Tu usuario se encuentra deshabilitado": Guardia del barrio activo, deshabilitado por su Operador (RN-09)
INSERT INTO guardia (usuario_id, barrio_id, nombre, apellido, habilitado)
SELECT u.id, b.id, 'Diego', 'Deshabilitado', FALSE
FROM usuario u, barrio b
WHERE u.email = 'guardia.deshabilitado@seed.test' AND b.nombre = 'Barrio Los Álamos';

-- "Tu barrio se encuentra inactivo": Guardia habilitado de un barrio Inactivo (RN-13 conserva sus Guardias)
INSERT INTO guardia (usuario_id, barrio_id, nombre, apellido, habilitado)
SELECT u.id, b.id, 'Irene', 'Inactivo', TRUE
FROM usuario u, barrio b
WHERE u.email = 'guardia.inactivo@seed.test' AND b.nombre = 'Barrio San Jorge';
