import axios from 'axios';

// Formato estándar de error del backend (GlobalExceptionHandler, application/problem+json)
export interface ApiFieldError {
  field: string;
  message: string;
}

export interface ApiProblem {
  status: number;
  code: string;
  detail: string;
  title?: string;
  instance?: string;
  timestamp?: string;
  errors?: ApiFieldError[];
}

const NETWORK_ERROR_MESSAGE = 'No se pudo conectar con el servidor. Revisá tu conexión e intentá nuevamente.';
const UNKNOWN_ERROR_MESSAGE = 'Ocurrió un error inesperado. Intentá nuevamente más tarde.';

function isApiProblem(data: unknown): data is ApiProblem {
  return typeof data === 'object' && data !== null && 'code' in data && 'detail' in data;
}

/** Devuelve el error del backend en formato estándar, o null si no hubo respuesta (red caída, CORS, etc.). */
export function getApiProblem(error: unknown): ApiProblem | null {
  if (axios.isAxiosError(error) && isApiProblem(error.response?.data)) {
    return error.response.data;
  }
  return null;
}

/** Mensaje listo para mostrar al usuario (ej. message.error(getErrorMessage(error))). */
export function getErrorMessage(error: unknown): string {
  const problem = getApiProblem(error);
  if (problem) {
    return problem.detail;
  }
  if (axios.isAxiosError(error) && !error.response) {
    return NETWORK_ERROR_MESSAGE;
  }
  return UNKNOWN_ERROR_MESSAGE;
}

/**
 * Errores por campo en el formato de Form.setFields de Ant Design:
 *   onError: (error) => form.setFields(toFormFieldErrors(error))
 * Los nombres anidados del backend ("domicilio.calle") se convierten en rutas (['domicilio', 'calle']).
 */
export function toFormFieldErrors(error: unknown): { name: string[]; errors: string[] }[] {
  const byField = new Map<string, string[]>();
  for (const { field, message } of getApiProblem(error)?.errors ?? []) {
    byField.set(field, [...(byField.get(field) ?? []), message]);
  }
  return [...byField].map(([field, errors]) => ({ name: field.split('.'), errors }));
}
