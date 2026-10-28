import axios from 'axios';
import type { FormInstance } from 'antd';

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

// Lo que recibe form.setFields de un formulario con esos valores (sirve también para Form.useForm<Tipo>())
type FieldErrors<Values> = Parameters<FormInstance<Values>['setFields']>[0];

/**
 * Errores por campo en el formato de Form.setFields de Ant Design, también con formularios tipados:
 *   onError: (error) => form.setFields(toFormFieldErrors(error))
 * Los nombres anidados del backend ("domicilio.calle") se convierten en rutas (['domicilio', 'calle']).
 */
export function toFormFieldErrors<Values = unknown>(error: unknown): FieldErrors<Values> {
  const byField = new Map<string, string[]>();
  for (const { field, message } of getApiProblem(error)?.errors ?? []) {
    byField.set(field, [...(byField.get(field) ?? []), message]);
  }
  // El nombre viene del backend como texto: no se puede comprobar contra los campos del formulario al compilar
  return [...byField].map(([field, errors]) => ({
    name: field.split('.') as FieldErrors<Values>[number]['name'],
    errors,
  }));
}
