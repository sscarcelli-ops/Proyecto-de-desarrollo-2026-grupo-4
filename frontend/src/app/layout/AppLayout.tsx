import { useState } from 'react';
import { Button, Drawer, Flex, Layout, Menu, Typography } from 'antd';
import { MenuOutlined } from '@ant-design/icons';
import { Outlet, useLocation, useNavigate } from 'react-router';
import { getMenuItemsFor } from './menuItems';
import { useCurrentUser } from '../../features/auth/api/useCurrentUser';
import { BotonCambiarPassword } from '../../features/auth/components/BotonCambiarPassword';
import { BotonCerrarSesion } from '../../features/auth/components/BotonCerrarSesion';
import { Logo } from '../../shared/components/Logo';
import { ROL_LABEL } from '../../shared/types/rol';
import { palette } from '../theme';
import { usePantalla } from '../../shared/hooks/usePantalla';

const { Header, Sider, Content } = Layout;

/**
 * Layout de las pantallas autenticadas (wireframes): logo, menú lateral según los roles del usuario,
 * cerrar sesión abajo y el rol arriba a la derecha. Es el mismo para todos; solo cambian las opciones.
 *
 * T-07 (RNF-05), tres modos según el ancho de la pantalla (puntos de corte de Ant Design):
 * - Escritorio (lg, 992 px o más): menú lateral completo, con nombres.
 * - Tablet (md, de 768 a 991 px): menú lateral angosto, solo íconos; el nombre aparece al pasar el mouse.
 * - Celular (menos de 768 px): barra superior fija con el botón de menú, el logo y el rol; el menú se abre como
 *   panel desde la izquierda.
 */
export function AppLayout() {
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const user = useCurrentUser();
  const { esCelular, esEscritorio } = usePantalla();
  const [menuAbierto, setMenuAbierto] = useState(false);
  const rol = user?.roles.map((r) => ROL_LABEL[r]).join(' / ');

  const items = getMenuItemsFor(user?.roles ?? []).map((item) => ({
    key: item.path,
    icon: item.icon,
    label: item.label,
  }));

  // angosto: solo íconos (tablet). El contenido es el mismo en los tres modos.
  const menuLateral = (angosto: boolean) => (
    <Flex vertical style={{ height: '100%' }}>
      <Flex align="center" justify={angosto ? 'center' : 'flex-start'} gap={10} style={{ padding: angosto ? '24px 0' : '24px 16px' }}>
        <Logo size={36} sobreOscuro />
        {!angosto && (
          <Typography.Text strong style={{ fontSize: 16, color: palette.sidebarTextActive }}>Control de Accesos</Typography.Text>
        )}
      </Flex>
      <Menu
        theme="dark"
        mode="inline"
        inlineCollapsed={angosto}
        selectedKeys={[pathname]}
        items={items}
        onClick={({ key }) => {
          setMenuAbierto(false);
          navigate(key);
        }}
        style={{ borderInlineEnd: 'none', flex: 1 }}
      />
      <Flex vertical={angosto} align="center" justify="space-between" gap={angosto ? 8 : 0} style={{ padding: 16 }}>
        <BotonCambiarPassword forma="puntos" />
        <BotonCerrarSesion style={{ color: palette.sidebarText }} />
      </Flex>
    </Flex>
  );

  return (
    <Layout style={{ minHeight: '100vh' }}>
      {!esCelular && (
        <Sider
          theme="dark"
          width={265}
          collapsed={!esEscritorio}
          collapsedWidth={80}
          trigger={null}
          style={{ position: 'sticky', top: 0, height: '100vh' }}
        >
          {menuLateral(!esEscritorio)}
        </Sider>
      )}
      {esCelular && (
        <Drawer
          placement="left"
          size={265}
          open={menuAbierto}
          onClose={() => setMenuAbierto(false)}
          closable={false}
          styles={{ body: { padding: 0, background: palette.sidebar } }}
        >
          {menuLateral(false)}
        </Drawer>
      )}
      <Layout>
        {esCelular ? (
          // Barra superior del celular: el botón de menú junto al logo y el nombre, como en cualquier app
          <Header
            style={{
              position: 'sticky',
              top: 0,
              zIndex: 10,
              display: 'flex',
              alignItems: 'center',
              gap: 8,
              height: 56,
              paddingInline: 8,
              background: palette.surface,
              borderBottom: `1px solid ${palette.border}`,
              lineHeight: 'normal',
            }}
          >
            <Button type="text" icon={<MenuOutlined />} onClick={() => setMenuAbierto(true)} aria-label="Abrir menú" />
            <Logo size={28} />
            <Typography.Text strong ellipsis style={{ flex: 1, fontSize: 15, color: palette.text }}>
              Control de Accesos
            </Typography.Text>
            {rol && (
              <Typography.Text ellipsis style={{ maxWidth: '40%', color: palette.textSecondary, fontSize: 13 }}>
                {rol}
              </Typography.Text>
            )}
          </Header>
        ) : (
          <Header
            style={{
              display: 'flex',
              justifyContent: 'flex-end',
              alignItems: 'center',
              paddingInline: esEscritorio ? 32 : 24,
              background: 'transparent',
              height: 'auto',
              paddingTop: 24,
              paddingBottom: 8,
            }}
          >
            {rol && <Typography.Text style={{ fontWeight: 600, color: palette.text, fontSize: 16 }}>{rol}</Typography.Text>}
          </Header>
        )}
        <Content
          style={{
            // Márgenes: 16 px en el celular, 24 en tablet y 32 en escritorio
            padding: esCelular ? '16px 16px 20px' : esEscritorio ? '0 32px 24px' : '0 24px 24px',
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
