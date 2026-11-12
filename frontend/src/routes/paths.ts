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
  cambiarPassword: '/cambiar-contrasena', // US-07
  estadoSolicitud: '/estado-solicitud', // US-08
  garita: '/garita', // US-37 a US-40 (Guardia)
  barrios: '/barrios', // US-10
  barrioNuevo: '/barrios/nuevo', // US-10
  barrioEditar: '/barrios/:id/editar', // US-11
  empresas: '/empresas', // US-18 (Admin: Empresas y Monotributistas)
  operadorNuevo: '/operadores/nuevo', // US-13
  empresasBarrio: '/barrio/empresas', // US-33, US-34 (Operador: Empresas y Monotributistas del barrio)
  trabajadores: '/trabajadores', // US-26
  trabajadorNuevo: '/trabajadores/nuevo', // US-25
  trabajadorEditar: '/trabajadores/:id/editar', // US-26
} as const;
