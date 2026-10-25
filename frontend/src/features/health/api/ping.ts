import { useQuery } from '@tanstack/react-query';
import { httpClient } from '../../../shared/lib/httpClient';
import type { ApiSchema } from '../../../shared/api/types';

// Tipo generado desde el DTO PingResponse del backend (npm run api:types)
type PingResponse = ApiSchema<'PingResponse'>;

async function getPing(): Promise<PingResponse> {
  const { data } = await httpClient.get<PingResponse>('/ping');
  return data;
}

// enabled: false => la consulta solo se ejecuta al llamar refetch() (botón "Probar conexión")
export function usePing() {
  return useQuery({
    queryKey: ['health', 'ping'],
    queryFn: getPing,
    enabled: false,
    retry: false,
  });
}
