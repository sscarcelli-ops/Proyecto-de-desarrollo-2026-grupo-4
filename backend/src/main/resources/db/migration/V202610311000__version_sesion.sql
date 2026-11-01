-- T-03 / US-05: versión de sesión por usuario. El token lleva la versión con la que se emitió (claim "ver") y cada
-- petición la compara con esta columna. Al cerrar sesión se incrementa: todos los tokens anteriores de ese usuario
-- dejan de valer, aunque no hayan vencido.
ALTER TABLE usuario ADD COLUMN version_sesion INTEGER NOT NULL DEFAULT 0;
