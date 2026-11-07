import axios from 'axios';
import { authToken } from './authToken';

// El frontend no sabe dónde está el backend: llama a /api en su misma dirección y el servidor que lo sirve reenvía
// esos pedidos al backend. En desarrollo lo hace Vite (vite.config.ts) y en Docker, nginx
// (nginx/default.conf.template). La dirección del backend es configuración de ese servidor (BACKEND_URL) y no queda
// escrita en el código compilado: la misma compilación sirve para cualquier instalación.
const API_URL = '/api';

// 403 que significan "esta cuenta ya no puede operar" (mismos códigos que el login): se cierra la sesión
const CUENTA_SIN_ACCESO = new Set(['CUENTA_SUSPENDIDA', 'USUARIO_DESHABILITADO', 'BARRIO_INACTIVO', 'OPERADOR_SIN_BARRIO']);

export const httpClient = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Agrega el JWT a cada petición: Authorization: Bearer <token>
httpClient.interceptors.request.use((config) => {
  const token = authToken.get();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// El backend rechazó la sesión del pedido: se descarta el token y RequireAuth lleva al login con el motivo.
// - 401: vencida, alterada o cerrada en otro dispositivo ("Tu sesión expiró").
// - 403 con un código de CUENTA_SIN_ACCESO: la cuenta se suspendió o deshabilitó con la sesión abierta.
// Solo si el pedido llevaba el token actual: un pedido viejo (de antes de otro login, o el propio logout) no corta
// la sesión nueva. Sin token (ej. contraseña incorrecta en el login) el error lo maneja la pantalla.
httpClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (axios.isAxiosError(error) && error.response && esDeLaSesionActual(error.config?.headers?.Authorization)) {
      const { status, data } = error.response;
      if (status === 401) {
        authToken.expirar();
      } else if (status === 403 && CUENTA_SIN_ACCESO.has(data?.code)) {
        authToken.expirar(data.detail);
      }
    }
    return Promise.reject(error);
  },
);

function esDeLaSesionActual(authorization: unknown): boolean {
  const token = authToken.get();
  return token !== null && authorization === `Bearer ${token}`;
}
