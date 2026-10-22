// Mismos códigos que la tabla rol del backend (V202610211000__modelo_inicial.sql)
export type Rol = 'ADMIN' | 'OPERADOR' | 'GUARDIA' | 'EMPRESA' | 'MONOTRIBUTISTA';

// Nombre visible del rol (encabezado del layout, listados)
export const ROL_LABEL: Record<Rol, string> = {
  ADMIN: 'Administrador',
  OPERADOR: 'Operador',
  GUARDIA: 'Guardia',
  EMPRESA: 'Empresa',
  MONOTRIBUTISTA: 'Monotributista',
};
