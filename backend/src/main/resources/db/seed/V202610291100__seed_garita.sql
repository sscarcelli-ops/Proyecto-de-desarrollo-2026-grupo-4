-- Seed para US-37..40 (Garita). Solo se aplica en perfil seed (dev,seed).
-- Agrega:
--   * Habilitación en barrio y documentos para el mono.suspendido existente (DNI 32555661)
--     para probar el motivo "Monotributista suspendido" en Garita.
--   * Trabajador DNI 30444555 "Luis Habilitado" para la Empresa con docs vigentes y sin ingreso activo.
--     Caso feliz para probar ingreso.

-- Habilitar el mono.suspendido en Barrio Los Álamos (para que la verificación llegue al chequeo de estado de perfil)
INSERT INTO habilitacion_barrio (perfil_id, barrio_id)
SELECT p.id, b.id
FROM perfil p, barrio b
WHERE p.cuit_cuil = '20-32555661-8' AND b.nombre = 'Barrio Los Álamos'
ON CONFLICT DO NOTHING;

-- Documentación vigente del trabajador del mono suspendido (para que no falle por documentación)
INSERT INTO documento (trabajador_id, tipo, archivo_ruta, fecha_vencimiento)
SELECT t.id, v.tipo,
       'trabajadores/seed/32555661/' || lower(v.tipo) || '.pdf',
       CURRENT_DATE + 180
FROM trabajador t
JOIN perfil p ON p.id = t.perfil_id AND p.cuit_cuil = '20-32555661-8'
JOIN (VALUES ('ART'), ('SEGURO_VIDA')) AS v(tipo) ON true
WHERE t.dni = '32555661'
ON CONFLICT DO NOTHING;

-- ============================================================
-- Luis Habilitado (DNI 30444555) — caso feliz de ingreso para la Empresa
-- ============================================================
INSERT INTO trabajador (perfil_id, nombre, apellido, dni, foto_ruta)
SELECT p.id, 'Luis', 'Habilitado', '30444555',
       'trabajadores/seed/30444555/foto.jpg'
FROM perfil p WHERE p.cuit_cuil = '30-71234567-1'
ON CONFLICT DO NOTHING;

INSERT INTO documento (trabajador_id, tipo, archivo_ruta, fecha_vencimiento)
SELECT t.id, v.tipo,
       'trabajadores/seed/30444555/' || lower(v.tipo) || '.pdf',
       CURRENT_DATE + 365
FROM trabajador t
JOIN perfil p ON p.id = t.perfil_id AND p.cuit_cuil = '30-71234567-1'
JOIN (VALUES ('ART'), ('SEGURO_VIDA')) AS v(tipo) ON true
WHERE t.dni = '30444555'
ON CONFLICT DO NOTHING;
