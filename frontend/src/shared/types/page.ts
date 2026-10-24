// Respuesta estándar de los listados paginados del backend (common/paging/PageResponse.java).
// page empieza en 0; la Table de Ant Design empieza en 1: current = page + 1 y se pide ?page=current - 1.
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
