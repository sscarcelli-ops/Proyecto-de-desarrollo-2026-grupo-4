import { useSyncExternalStore } from 'react';

// JWT guardado en localStorage: lo comparten todas las pestañas del mismo navegador (cómodo en la garita),
// pero no otro navegador ni el modo incógnito (RF-21). Persiste al cerrar el navegador hasta que vence (8 h)
// o el usuario cierra sesión, por eso el logout (RF-25) siempre debe llamar a clear().
const TOKEN_KEY = 'accesos.token';

const listeners = new Set<() => void>();
const avisar = () => listeners.forEach((listener) => listener());

// El backend rechazó el token (vencido o alterado): el login lo avisa una vez. Se olvida al iniciar o cerrar sesión.
let sesionVencida = false;

// Otra pestaña inició o cerró sesión: esta también se entera
window.addEventListener('storage', (event) => {
  if (event.key === TOKEN_KEY || event.key === null) avisar();
});

function borrar(vencida: boolean): void {
  try {
    localStorage.removeItem(TOKEN_KEY);
  } catch {
    // nada que limpiar
  }
  sesionVencida = vencida;
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
    sesionVencida = false;
    avisar();
  },
  clear(): void {
    borrar(false);
  },
  /** El backend respondió 401 a un pedido con token: se borra y el login muestra "Tu sesión expiró". */
  expirar(): void {
    borrar(true);
  },
  sesionVencida(): boolean {
    return sesionVencida;
  },
  olvidarSesionVencida(): void {
    sesionVencida = false;
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
