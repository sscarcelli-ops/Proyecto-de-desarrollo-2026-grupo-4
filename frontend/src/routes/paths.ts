// Rutas de la aplicación en un único lugar, para no repetir strings en links y navegaciones
export const PATHS = {
  // Sin sesión
  login: '/',
  activar: '/activar', // US-01 (dirección del manual de instalación: no hay link desde el login)
  registro: '/registro', // US-02
  solicitarPerfil: '/solicitar-perfil', // US-03
  recuperarPassword: '/recuperar-contrasena', // US-06
  ping: '/ping', // prueba de conexión con el backend (T-02)
  // Con sesión
  inicio: '/inicio',
  estadoSolicitud: '/estado-solicitud', // US-08
  garita: '/garita', // US-37 a US-40 (Guardia)
  barrios: '/barrios', // US-10
  barrioNuevo: '/barrios/nuevo', // US-10
  barrioEditar: '/barrios/:id/editar', // US-11
  trabajadores: '/trabajadores', // US-26
  trabajadorNuevo: '/trabajadores/nuevo', // US-25
  trabajadorEditar: '/trabajadores/:id/editar', // US-26
} as const;
