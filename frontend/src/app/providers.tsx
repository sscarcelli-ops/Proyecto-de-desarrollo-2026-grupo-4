import type { ReactNode } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { App as AntdApp, ConfigProvider } from 'antd';
import esES from 'antd/locale/es_ES';
import { theme } from './theme';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
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
