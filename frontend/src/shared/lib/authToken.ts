// JWT guardado en localStorage: lo comparten todas las pestañas del mismo navegador (cómodo en la garita),
// pero no otro navegador ni el modo incógnito (RF-21). Persiste al cerrar el navegador hasta que vence (8 h)
// o el usuario cierra sesión, por eso el logout (RF-25) siempre debe llamar a clear().
const TOKEN_KEY = 'accesos.token';

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
  },
  clear(): void {
    try {
      localStorage.removeItem(TOKEN_KEY);
    } catch {
      // nada que limpiar
    }
  },
};
