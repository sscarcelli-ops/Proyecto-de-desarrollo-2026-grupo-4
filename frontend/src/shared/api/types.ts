import type { components } from './schema';

// Tipos de los DTO del backend, generados desde Swagger: no se escriben a mano.
// Después de crear o cambiar un DTO en el backend: levantar el backend y correr `npm run api:types`.
// Uso: const barrio: ApiSchema<'BarrioResponse'> = ...
export type ApiSchema<Name extends keyof components['schemas']> = components['schemas'][Name];
