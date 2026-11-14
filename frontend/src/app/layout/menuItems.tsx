import type { ReactNode } from 'react';
import { ApartmentOutlined, HomeOutlined, SafetyOutlined, ShopOutlined, TeamOutlined } from '@ant-design/icons';
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
  // Inicio solo para el Admin: el Operador entra directo a Empresas y Monotributistas (wireframes de US-33 y US-34)
  { path: PATHS.inicio, label: 'Inicio', icon: <HomeOutlined />, roles: ['ADMIN'] },
  { path: PATHS.barrios, label: 'Barrios privados', icon: <ApartmentOutlined />, roles: ['ADMIN'] },
  // US-18, US-20: Empresas y Monotributistas (Admin)
  { path: PATHS.empresas, label: 'Empresas y Monotributistas', icon: <ShopOutlined />, roles: ['ADMIN'] },
  // US-33, US-34: Empresas y Monotributistas aprobados y su habilitación en el barrio (Operador)
  { path: PATHS.empresasBarrio, label: 'Empresas y Monotributistas', icon: <ShopOutlined />, roles: ['OPERADOR'] },
  // US-32: Guardias del barrio (Operador)
  { path: PATHS.guardias, label: 'Guardias', icon: <SafetyOutlined />, roles: ['OPERADOR'] },
  // US-25, US-26: Trabajadores de la Empresa
  { path: PATHS.trabajadores, label: 'Trabajadores', icon: <TeamOutlined />, roles: ['EMPRESA'] },
];

/** Opciones visibles para los roles del usuario (RF-22). */
export function getMenuItemsFor(roles: Rol[]): AppMenuItem[] {
  return MENU_ITEMS.filter((item) => item.roles.length === 0 || item.roles.some((rol) => roles.includes(rol)));
}
