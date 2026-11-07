-- Dos barrios no pueden tener el mismo nombre en la misma localidad/provincia (sin distinguir mayúsculas
-- ni espacios en los bordes). Si ya hay repetidos (por ejemplo, datos de prueba), se renombran con un número
-- para poder crear el índice sin perder datos.
UPDATE barrio b
SET nombre = LEFT(b.nombre, 140) || ' (' || d.n || ')'
FROM (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY lower(btrim(nombre)), lower(btrim(localidad_provincia)) ORDER BY id) AS n
    FROM barrio
) d
WHERE b.id = d.id AND d.n > 1;

CREATE UNIQUE INDEX uq_barrio_nombre_localidad ON barrio (lower(btrim(nombre)), lower(btrim(localidad_provincia)));
