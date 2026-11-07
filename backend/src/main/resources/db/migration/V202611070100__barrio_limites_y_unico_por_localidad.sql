-- US-10 / US-11 con T-11:
-- 1. Largos máximos del barrio iguales en la base, el backend y el formulario: nombre 60, dirección 120.
-- 2. El barrio repetido se controla por nombre + localidad del catálogo (T-11), ya no por el texto libre
--    localidad_provincia, que quedó sin uso.

-- Si algún barrio supera los largos nuevos, se acorta antes de achicar la columna. Al nombre se le agrega su id
-- para que no choque con otro al acortarlo.
UPDATE barrio SET nombre = LEFT(btrim(nombre), 50) || ' (' || id || ')' WHERE length(nombre) > 60;
UPDATE barrio SET direccion = LEFT(direccion, 120) WHERE length(direccion) > 120;

ALTER TABLE barrio ALTER COLUMN nombre TYPE VARCHAR(60);
ALTER TABLE barrio ALTER COLUMN direccion TYPE VARCHAR(120);

-- El índice anterior (V202611062000) comparaba el texto libre: se reemplaza por uno sobre la localidad.
DROP INDEX IF EXISTS uq_barrio_nombre_localidad;

-- Si ya hay repetidos con la localidad nueva (por ejemplo, datos de prueba), se renombran con un número para
-- poder crear el índice sin perder datos. Los barrios sin localidad no chocan entre sí.
UPDATE barrio b
SET nombre = LEFT(btrim(b.nombre), 55) || ' (' || d.n || ')'
FROM (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY lower(btrim(nombre)), localidad_id ORDER BY id) AS n
    FROM barrio
    WHERE localidad_id IS NOT NULL
) d
WHERE b.id = d.id AND d.n > 1;

CREATE UNIQUE INDEX uq_barrio_nombre_localidad ON barrio (lower(btrim(nombre)), localidad_id);
