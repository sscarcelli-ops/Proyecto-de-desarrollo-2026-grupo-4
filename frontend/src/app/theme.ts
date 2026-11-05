import type { ThemeConfig } from 'antd';

export const palette = {
  primary: '#1f4e9c',
  sidebar: '#0f2747',
  sidebarHover: '#183659',
  sidebarText: '#c9d4e5',
  sidebarTextActive: '#ffffff',
  background: '#f5f7fa',
  surface: '#ffffff',
  tableHeader: '#f8fafc',
  border: '#d5dbe4',
  borderSoft: '#e3e8ef',
  text: '#1a2233',
  textSecondary: '#5b6577',
  textPlaceholder: '#98a2b3',
  success: '#1e7b4a',
  error: '#b3261e',
  warning: '#b76e00',
  botonAprobar: '#10B981',
  rechazoFondo: '#FEF2F2',
  rechazoBorde: '#FCA5A5',
  rechazoTexto: '#991B1B',
};

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
    Form: {
      labelColor: palette.textSecondary,
      labelFontSize: 12,
      verticalLabelPadding: '0 0 4px',
    },
  },
};

export const garitaTheme: ThemeConfig = {
  token: {
    fontSize: 16,
    controlHeightLG: 48,
  },
};
