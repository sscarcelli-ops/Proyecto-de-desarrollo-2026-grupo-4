import { useState } from 'react';
import { Button, Drawer, Flex, Grid, Layout, Menu, Typography } from 'antd';
import { MenuOutlined } from '@ant-design/icons';
import { Outlet, useLocation, useNavigate } from 'react-router';
import { getMenuItemsFor } from './menuItems';
import { useCurrentUser } from '../../features/auth/api/useCurrentUser';
import { BotonCambiarPassword } from '../../features/auth/components/BotonCambiarPassword';
import { BotonCerrarSesion } from '../../features/auth/components/BotonCerrarSesion';
import { Logo } from '../../shared/components/Logo';
import { ROL_LABEL } from '../../shared/types/rol';
import { palette } from '../theme';

const { Header, Sider, Content } = Layout;

/**
 * Layout de las pantallas autenticadas (wireframes): logo, menú lateral según los roles del usuario,
 * cerrar sesión abajo y el rol arriba a la derecha. Es el mismo para todos; solo cambian las opciones.
 * En pantallas chicas (celular, RNF-05) el menú se abre como panel desde el botón del encabezado.
 */
export function AppLayout() {
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const user = useCurrentUser();
  const esEscritorio = Grid.useBreakpoint().lg ?? true;
  const [menuAbierto, setMenuAbierto] = useState(false);

  const items = getMenuItemsFor(user?.roles ?? []).map((item) => ({
    key: item.path,
    icon: item.icon,
    label: item.label,
  }));

  const menuLateral = (
    <Flex vertical style={{ height: '100%' }}>
      <Flex align="center" gap={10} style={{ padding: '24px 16px' }}>
        <Logo size={36} sobreOscuro />
        <Typography.Text strong style={{ fontSize: 16, color: palette.sidebarTextActive }}>Control de Accesos</Typography.Text>
      </Flex>
      <Menu
        theme="dark"
        mode="inline"
        selectedKeys={[pathname]}
        items={items}
        onClick={({ key }) => {
          setMenuAbierto(false);
          navigate(key);
        }}
        style={{ borderInlineEnd: 'none', flex: 1 }}
      />
      <Flex justify="space-between" style={{ padding: 16 }}>
        <BotonCambiarPassword forma="puntos" />
        <BotonCerrarSesion style={{ color: palette.sidebarText }} />
      </Flex>
    </Flex>
  );

  return (
    <Layout style={{ minHeight: '100vh' }}>
      {esEscritorio ? (
        <Sider
          theme="dark"
          width={240}
          style={{
            position: 'sticky',
            top: 0,
            height: '100vh',
          }}
        >
          {menuLateral}
        </Sider>
      ) : (
        <Drawer
          placement="left"
          size={240}
          open={menuAbierto}
          onClose={() => setMenuAbierto(false)}
          closable={false}
          styles={{ body: { padding: 0, background: palette.sidebar } }}
        >
          {menuLateral}
        </Drawer>
      )}
      <Layout>
        <Header
          style={{
            display: 'flex',
            justifyContent: esEscritorio ? 'flex-end' : 'space-between',
            alignItems: 'center',
            paddingInline: esEscritorio ? 32 : 16,
            background: 'transparent',
            height: 'auto',
            paddingTop: 24,
            paddingBottom: 8,
          }}
        >
          {!esEscritorio && (
            <Button type="text" icon={<MenuOutlined />} onClick={() => setMenuAbierto(true)} aria-label="Abrir menú" />
          )}
          {user && (
            <Typography.Text style={{ fontWeight: 600, color: palette.text, fontSize: 16 }}>
              {user.roles.map((rol) => ROL_LABEL[rol]).join(' / ')}
            </Typography.Text>
          )}
        </Header>
        <Content
          style={{
            padding: esEscritorio ? '0 32px 24px' : '0 16px 20px',
            display: 'flex',
            flexDirection: 'column',
            flex: 1,
          }}
        >
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
