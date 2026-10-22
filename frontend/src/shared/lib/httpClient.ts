import axios from 'axios';

// URL base del backend tomada de variables de entorno o por defecto localhost:8080
const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

export const httpClient = axios.create({
  baseURL: API_URL,
  withCredentials: true, // Para cookies de sesión / JWT
  headers: {
    'Content-Type': 'application/json',
  },
});