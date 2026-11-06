-- US-28 / US-26 CA-3: nombre con el que la Empresa subió cada documento, para mostrarlo en la edición.
-- En disco el archivo se guarda con un UUID; los documentos cargados antes quedan en NULL y se muestran como "ART.pdf" o "Seguro de Vida.pdf".
ALTER TABLE documento ADD COLUMN archivo_nombre VARCHAR(255);
