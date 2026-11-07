-- T-11: el barrio guarda su localidad del catálogo (y con ella la provincia) en lugar de un texto libre.
-- Se muestra como "Provincia, Localidad" (ej. "Buenos Aires, Baradero").
ALTER TABLE barrio ADD COLUMN localidad_id VARCHAR(10) REFERENCES localidad(id);
CREATE INDEX ix_barrio_localidad ON barrio (localidad_id);

-- Barrios existentes: se pasan desde el texto "Localidad, Provincia" (el formato del seed y del alta anterior),
-- comparando sin mayúsculas ni espacios en los bordes. Si el nombre se repite en la provincia, se toma la
-- localidad de código más chico. Un texto que no coincide con el catálogo deja el barrio sin localidad: el
-- listado lo muestra "Sin cargar" y se completa desde Editar barrio.
UPDATE barrio b
SET localidad_id = (
    SELECT l.id
    FROM localidad l
    JOIN provincia p ON p.id = l.provincia_id
    WHERE lower(l.nombre) = lower(trim(regexp_replace(b.localidad_provincia, ',[^,]*$', '')))
      AND lower(p.nombre) = lower(trim(substring(b.localidad_provincia FROM ',([^,]*)$')))
    ORDER BY length(l.id), l.id
    LIMIT 1
);

-- El texto anterior ya no se usa ni se muestra. Se conserva como dato histórico, sin obligatoriedad, para no
-- perder lo que no coincidió con el catálogo.
ALTER TABLE barrio ALTER COLUMN localidad_provincia DROP NOT NULL;
COMMENT ON COLUMN barrio.localidad_provincia IS 'Texto libre anterior a T-11. Sin uso: la ubicación es localidad_id.';
