import { useSyncExternalStore } from 'react';

// JWT guardado en localStorage: lo comparten todas las pestañas del mismo navegador (cómodo en la garita),
// pero no otro navegador ni el modo incógnito (RF-21). Persiste al cerrar el navegador hasta que vence (8 h)
// o el usuario cierra sesión, por eso el logout (RF-25) siempre debe llamar a clear().
const TOKEN_KEY = 'accesos.token';

export const AVISO_SESION_VENCIDA = 'Tu sesión expiró. Iniciá sesión de nuevo.';

const listeners = new Set<() => void>();
const avisar = () => listeners.forEach((listener) => listener());

// Por qué el backend cortó la sesión (vencida, cerrada en otro dispositivo, cuenta suspendida...): el login lo
// muestra una vez. Se olvida al iniciar o cerrar sesión.
let avisoLogin: string | null = null;

// Otra pestaña inició o cerró sesión: esta también se entera
window.addEventListener('storage', (event) => {
  if (event.key === TOKEN_KEY || event.key === null) avisar();
});

function borrar(aviso: string | null): void {
  try {
    localStorage.removeItem(TOKEN_KEY);
  } catch {
    // nada que limpiar
  }
  avisoLogin = aviso;
  avisar();
}

// localStorage puede lanzar excepción (almacenamiento bloqueado): se trata como "sin sesión"
export const authToken = {
  get(): string | null {
    try {
      return localStorage.getItem(TOKEN_KEY);
    } catch {
      return null;
    }
  },
  set(token: string): void {
    try {
      localStorage.setItem(TOKEN_KEY, token);
    } catch {
      // sin almacenamiento disponible el usuario tendrá que volver a iniciar sesión
    }
    avisoLogin = null;
    avisar();
  },
  clear(): void {
    borrar(null);
  },
  /**
   * El backend rechazó la sesión: se borra el token y el login muestra el motivo. Por defecto "Tu sesión expiró"
   * (401: vencida, alterada o cerrada en otro dispositivo); con el mensaje del backend si la cuenta ya no puede
   * operar (403: suspendida, Guardia deshabilitado, barrio inactivo).
   */
  expirar(aviso: string = AVISO_SESION_VENCIDA): void {
    borrar(aviso);
  },
  avisoLogin(): string | null {
    return avisoLogin;
  },
  olvidarAvisoLogin(): void {
    avisoLogin = null;
  },
  subscribe(listener: () => void): () => void {
    listeners.add(listener);
    return () => listeners.delete(listener);
  },
};

/** Token actual, re-renderizando cuando cambia (logout, 401 o login/logout en otra pestaña). */
export function useAuthToken(): string | null {
  return useSyncExternalStore(authToken.subscribe, authToken.get);
}
