// Rutas de la aplicación en un único lugar, para no repetir strings en links y navegaciones
export const PATHS = {
  // Sin sesión
  login: '/',
  registro: '/registro', // US-02
  recuperarPassword: '/recuperar-contrasena', // US-06
  ping: '/ping', // prueba de conexión con el backend (T-02)
  // Con sesión
  inicio: '/inicio',
  barrios: '/barrios', // US-10
  barrioNuevo: '/barrios/nuevo', // US-10
  barrioEditar: '/barrios/:id/editar', // US-11
} as const;
