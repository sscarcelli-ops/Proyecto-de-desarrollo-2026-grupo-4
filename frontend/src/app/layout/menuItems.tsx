import type { ReactNode } from 'react';
import { ApartmentOutlined, HomeOutlined, TeamOutlined } from '@ant-design/icons';
import type { Rol } from '../../shared/types/rol';
import { PATHS } from '../../routes/paths';

export interface AppMenuItem {
  path: string;
  label: string;
  icon: ReactNode;
  // Roles que ven la opción. Vacío = todos los usuarios autenticados.
  roles: Rol[];
}

// Opciones del menú lateral según wireframes y rol del usuario
export const MENU_ITEMS: AppMenuItem[] = [
  // Inicio solo para roles que lo tienen como dashboard (ej. Admin / Operador)
  { path: PATHS.inicio, label: 'Inicio', icon: <HomeOutlined />, roles: ['ADMIN', 'OPERADOR'] },
  { path: PATHS.barrios, label: 'Barrios privados', icon: <ApartmentOutlined />, roles: ['ADMIN'] },
  // US-25, US-26: Trabajadores de la Empresa
  { path: PATHS.trabajadores, label: 'Trabajadores', icon: <TeamOutlined />, roles: ['EMPRESA'] },
];

/** Opciones visibles para los roles del usuario (RF-22). */
export function getMenuItemsFor(roles: Rol[]): AppMenuItem[] {
  return MENU_ITEMS.filter((item) => item.roles.length === 0 || item.roles.some((rol) => roles.includes(rol)));
}
