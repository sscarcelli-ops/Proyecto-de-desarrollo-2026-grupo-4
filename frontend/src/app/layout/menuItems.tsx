import type { ReactNode } from 'react';
import type { Rol } from '../../shared/types/rol';
import { HomeOutlined } from '@ant-design/icons';
import { PATHS } from '../../routes/paths';
export interface AppMenuItem {
  path: string;
  label: string;
  icon: ReactNode;
  // Roles que ven la opción. Vacío = todos los usuarios autenticados.
  roles: Rol[];
}

// Cada feature agrega acá su opción del menú lateral (una línea por pantalla), en el orden en que se muestra.
// Ej. según wireframes: { path: '/barrios', label: 'Barrios', icon: <HomeOutlined />, roles: ['ADMIN'] }
export const MENU_ITEMS: AppMenuItem[] = [
  { path: PATHS.barrios, label: 'Barrios privados', icon: <HomeOutlined />, roles: ['ADMIN'] },
];

/** Opciones visibles para los roles del usuario (RF-22). */
export function getMenuItemsFor(roles: Rol[]): AppMenuItem[] {
  return MENU_ITEMS.filter((item) => item.roles.length === 0 || item.roles.some((rol) => roles.includes(rol)));
}