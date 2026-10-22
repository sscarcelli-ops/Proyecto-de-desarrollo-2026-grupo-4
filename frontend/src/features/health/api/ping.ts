import { useQuery } from '@tanstack/react-query';
import { httpClient } from '../../../shared/lib/httpClient';

export interface PingResponse {
  status: string;
  message: string;
  timestamp: string;
}

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
