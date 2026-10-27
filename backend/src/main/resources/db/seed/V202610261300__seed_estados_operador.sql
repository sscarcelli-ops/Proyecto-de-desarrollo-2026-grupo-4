-- Seed de US-04 (CA-3, RN-15): Operador de un barrio Inactivo, que no puede iniciar sesión.
-- El Operador sin barrio ya existe en el seed base: operador.libre@seed.test ("Disponible").
-- Contraseña: Prueba123.

INSERT INTO usuario (email, password_hash) VALUES
    ('operador.inactivo@seed.test', '$2a$10$p5r72fnKScsMFwcSBC5ILOwGeEo2byCvzm0fMI73oT6VEugMlDHWq');

INSERT INTO usuario_rol (usuario_id, rol_id)
SELECT u.id, r.id FROM usuario u, rol r
WHERE u.email = 'operador.inactivo@seed.test' AND r.codigo = 'OPERADOR';

INSERT INTO operador (usuario_id, nombre, apellido, telefono)
SELECT id, 'Ignacio', 'Inactivo', '1145550003' FROM usuario WHERE email = 'operador.inactivo@seed.test';

-- Barrio desactivado que conserva su Operador (RN-13): "Tu barrio se encuentra inactivo"
INSERT INTO barrio (nombre, localidad_provincia, direccion, estado, operador_id)
SELECT 'Barrio Las Lomas', 'Tigre, Buenos Aires', 'Av. Liniers 1200', 'INACTIVO', id
FROM usuario WHERE email = 'operador.inactivo@seed.test';
