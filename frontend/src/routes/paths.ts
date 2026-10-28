// Rutas de la aplicación en un único lugar, para no repetir strings en links y navegaciones
export const PATHS = {
  // Sin sesión
  login: '/',
  activar: '/activar', // US-01 (dirección del manual de instalación: no hay link desde el login)
  registro: '/registro', // US-02
  recuperarPassword: '/recuperar-contrasena', // US-06
  ping: '/ping', // prueba de conexión con el backend (T-02)
  // Con sesión
  inicio: '/inicio',
} as const;
