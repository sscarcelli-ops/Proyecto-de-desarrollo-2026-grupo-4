// Rutas de la aplicación en un único lugar, para no repetir strings en links y navegaciones
export const PATHS = {
  // Sin sesión
  login: '/',
  registro: '/registro', // US-02
  recuperarPassword: '/recuperar-contrasena', // US-06
  ping: '/ping', // prueba de conexión con el backend (T-02)
  // Con sesión
  inicio: '/inicio',
  // Trabajadores (US-25, US-26)
  trabajadores: '/trabajadores',
  trabajadorNuevo: '/trabajadores/nuevo',
  trabajadorEditar: (id: number | string) => `/trabajadores/${id}/editar`,
} as const;
