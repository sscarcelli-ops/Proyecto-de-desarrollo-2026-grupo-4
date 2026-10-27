import type { ThemeConfig } from 'antd';

// Único lugar donde se definen colores, bordes y tipografía de toda la app (Design Tokens de Ant Design).
// Para cambiar la paleta se tocan solo estos valores: ningún componente debe usar colores sueltos.
// Paleta azul institucional + Inter. A partir de cada color base, Ant Design genera solo sus tonos
// (hover, presionado, fondos suaves).

export const palette = {
  // Marca y acciones: botones principales, links, opción seleccionada. Contraste con texto blanco 8:1.
  primary: '#1f4e9c',
  // Menú lateral (azul marino) y su texto
  sidebar: '#0f2747',
  sidebarHover: '#183659',
  sidebarText: '#c9d4e5',
  sidebarTextActive: '#ffffff',
  // Superficies
  background: '#f5f7fa',
  surface: '#ffffff',
  tableHeader: '#f8fafc',
  border: '#d5dbe4',
  borderSoft: '#e3e8ef',
  // Texto
  text: '#1a2233',
  textSecondary: '#5b6577',
  textPlaceholder: '#98a2b3',
  // Semánticos (solo para estados y validaciones, nunca para decorar)
  success: '#1e7b4a',
  error: '#b3261e',
  warning: '#b76e00',
};

// Mapa de estados → colores (lo usa shared/components/EstadoTag). Cada tono tiene un significado:
// exito = habilita · error = bloquea · aviso = espera una acción · suspendido = grave pero reversible ·
// neutro = inactivo sin ser un error · marca = categoría sin juicio (ej. Ingreso).
export const estadoTonos = {
  exito: { background: '#e3f3ea', color: '#1e7b4a', borderColor: '#b5dec6' },
  error: { background: '#fbe9e7', color: '#b3261e', borderColor: '#f3c3be' },
  aviso: { background: '#fdf1dc', color: '#8a5200', borderColor: '#f3d7a6' },
  suspendido: { background: '#fdebe1', color: '#a3410e', borderColor: '#f5c7ab' },
  neutro: { background: '#eef1f5', color: '#4a5568', borderColor: '#d5dbe4' },
  marca: { background: '#e6edf8', color: '#1f4e9c', borderColor: '#bccde9' },
} as const;

export type EstadoTono = keyof typeof estadoTonos;

export const theme: ThemeConfig = {
  token: {
    colorPrimary: palette.primary,
    colorInfo: palette.primary,
    colorLink: palette.primary,
    colorSuccess: palette.success,
    colorError: palette.error,
    colorWarning: palette.warning,
    // Fondos y bordes de Alert, Result, Message: los mismos tonos del mapa de estados (los que Ant Design
    // calcula solo a partir de colores base oscuros quedan grisáceos)
    colorSuccessBg: estadoTonos.exito.background,
    colorSuccessBorder: estadoTonos.exito.borderColor,
    colorErrorBg: estadoTonos.error.background,
    colorErrorBorder: estadoTonos.error.borderColor,
    colorWarningBg: estadoTonos.aviso.background,
    colorWarningBorder: estadoTonos.aviso.borderColor,
    colorInfoBg: estadoTonos.marca.background,
    colorInfoBorder: estadoTonos.marca.borderColor,

    colorText: palette.text,
    colorTextSecondary: palette.textSecondary,
    colorTextDescription: palette.textSecondary,
    colorTextPlaceholder: palette.textPlaceholder,

    colorBgLayout: palette.background,
    colorBgContainer: palette.surface,
    colorBorder: palette.border,
    colorBorderSecondary: palette.borderSoft,

    fontFamily: "'Inter Variable', system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif",
    fontSize: 14,
    borderRadius: 6,
  },
  components: {
    Layout: {
      siderBg: palette.sidebar,
      triggerBg: palette.sidebarHover,
      headerBg: palette.surface,
      bodyBg: palette.background,
    },
    Menu: {
      darkItemBg: palette.sidebar,
      darkItemColor: palette.sidebarText,
      darkItemHoverBg: palette.sidebarHover,
      darkItemHoverColor: palette.sidebarTextActive,
      darkItemSelectedBg: palette.primary,
      darkItemSelectedColor: palette.sidebarTextActive,
      itemHeight: 44,
      itemBorderRadius: 6,
    },
    Table: {
      headerBg: palette.tableHeader,
      headerColor: palette.textSecondary,
      rowHoverBg: palette.background,
      borderColor: palette.borderSoft,
    },
    // Etiquetas de los formularios como en los wireframes ("NOMBRE *"): chicas y en gris. Las mayúsculas
    // están en index.css y el "*" de obligatorio en providers.tsx (no son tokens de Ant Design).
    Form: {
      labelColor: palette.textSecondary,
      labelFontSize: 12,
      verticalLabelPadding: '0 0 4px',
    },
  },
};

// Garita (RNF-05): se usa en el celular, apurado y a veces al sol. Envolver sus pantallas con
// <ConfigProvider theme={garitaTheme} componentSize="large"> para agrandar texto y botones.
export const garitaTheme: ThemeConfig = {
  token: {
    fontSize: 16,
    controlHeightLG: 48,
  },
};
