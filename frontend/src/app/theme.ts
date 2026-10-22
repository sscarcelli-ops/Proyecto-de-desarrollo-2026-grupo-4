import type { ThemeConfig } from 'antd';

// Único lugar donde se definen colores, bordes y tipografía de toda la app (Design Tokens de Ant Design).
// Para cambiar la paleta se tocan solo estos valores: ningún componente debe usar colores sueltos.
// Valores iniciales tomados de los wireframes (gris neutro + celeste para la opción seleccionada).
export const palette = {
  primary: '#1677ff',
  // Fondo del menú lateral y de las pantallas públicas (login, activación, registro)
  sidebar: '#ececec',
  sidebarSelected: '#cfe5f7',
  text: '#1f1f1f',
  content: '#ffffff',
};

export const theme: ThemeConfig = {
  token: {
    colorPrimary: palette.primary,
    colorText: palette.text,
    borderRadius: 6,
  },
  components: {
    Layout: {
      lightSiderBg: palette.sidebar,
      headerBg: palette.content,
      bodyBg: palette.content,
      lightTriggerBg: palette.sidebar,
    },
    Menu: {
      itemBg: 'transparent',
      itemColor: palette.text,
      itemSelectedBg: palette.sidebarSelected,
      itemSelectedColor: palette.text,
      itemHeight: 48,
    },
  },
};
