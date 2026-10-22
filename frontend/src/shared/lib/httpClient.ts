import axios from 'axios';
import { authToken } from './authToken';

// URL base del backend tomada de variables de entorno o por defecto localhost:8080
const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

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

// 401 = token ausente, vencido (8 h) o inválido: se descarta para no reenviarlo.
// La redirección al login se agrega con la pantalla de inicio de sesión (US-04).
httpClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      authToken.clear();
    }
    return Promise.reject(error);
  },
);
