import axios from 'axios';
import { authToken } from './authToken';

// URL base del backend: siempre desde la configuración, sin valor por defecto en el código
const API_URL = import.meta.env.VITE_API_URL;
if (!API_URL) {
  throw new Error('Falta VITE_API_URL: copiar frontend/.env.example a frontend/.env');
}

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

// 401 a un pedido que llevaba token = vencido (8 h) o alterado: se descarta y RequireAuth lleva al login con el
// aviso "Tu sesión expiró". Sin token (ej. contraseña incorrecta en el login) el 401 lo maneja la pantalla.
httpClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (axios.isAxiosError(error) && error.response?.status === 401 && error.config?.headers?.Authorization) {
      authToken.expirar();
    }
    return Promise.reject(error);
  },
);
