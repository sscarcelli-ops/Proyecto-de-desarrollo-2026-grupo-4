import type { ReactNode } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { App as AntdApp, ConfigProvider } from 'antd';
import esES from 'antd/locale/es_ES';
import { theme } from './theme';
import { authToken } from '../shared/lib/authToken';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

// Cambio de sesión (login, logout, 401 o login/logout en otra pestaña): se descartan los datos de la anterior,
// para que otro usuario en el mismo navegador no vea, ni por un instante, la caché del que salió (RF-23).
let tokenDeLaCache = authToken.get();
authToken.subscribe(() => {
  const token = authToken.get();
  if (token !== tokenDeLaCache) {
    tokenDeLaCache = token;
    queryClient.removeQueries();
  }
});

// Campos obligatorios: "NOMBRE *", con el asterisco después de la etiqueta (como en los wireframes)
const marcaObligatorio = (label: ReactNode, { required }: { required: boolean }) => (
  <>
    {label}
    {required && ' *'}
  </>
);

// Providers globales: caché de datos del servidor (TanStack Query) y tema/idioma de Ant Design
export function AppProviders({ children }: { children: ReactNode }) {
  return (
    <QueryClientProvider client={queryClient}>
      <ConfigProvider locale={esES} theme={theme} form={{ requiredMark: marcaObligatorio }}>
        <AntdApp>{children}</AntdApp>
      </ConfigProvider>
    </QueryClientProvider>
  );
}
