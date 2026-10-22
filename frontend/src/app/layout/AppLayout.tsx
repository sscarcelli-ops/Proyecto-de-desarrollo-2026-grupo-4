import { Avatar, Button, Flex, Layout, Menu, Tooltip, Typography } from 'antd';
import { LogoutOutlined } from '@ant-design/icons';
import { Outlet, useLocation, useNavigate } from 'react-router';
import { getMenuItemsFor } from './menuItems';
import { useCurrentUser } from '../../features/auth/api/useCurrentUser';
import { authToken } from '../../shared/lib/authToken';
import { ROL_LABEL } from '../../shared/types/rol';
import { PATHS } from '../../routes/paths';

const { Header, Sider, Content } = Layout;

/**
 * Layout de las pantallas autenticadas (wireframes): logo, menú lateral según los roles del usuario,
 * cerrar sesión abajo y el rol arriba a la derecha. Es el mismo para todos; solo cambian las opciones.
 */
export function AppLayout() {
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const user = useCurrentUser();

  const items = getMenuItemsFor(user?.roles ?? []).map((item) => ({
    key: item.path,
    icon: item.icon,
    label: item.label,
  }));

  // RF-25. El token persiste en localStorage: cerrar sesión siempre lo borra.
  const logout = () => {
    authToken.clear();
    navigate(PATHS.home);
  };

  return (
    <Layout style={{ minHeight: '100vh' }}>
      {/* En pantallas chicas (garita en el celular, RNF-05) el menú se oculta y se abre con el botón */}
      <Sider theme="light" width={240} breakpoint="lg" collapsedWidth={0}>
        <Flex vertical style={{ height: '100%' }}>
          <Flex align="center" gap={12} style={{ padding: '24px 20px' }}>
            {/* Reemplazar por el logo definitivo */}
            <Avatar size={44} />
            <Typography.Text strong style={{ fontSize: 18 }}>Logo</Typography.Text>
          </Flex>
          <Menu
            mode="inline"
            selectedKeys={[pathname]}
            items={items}
            onClick={({ key }) => navigate(key)}
            style={{ borderInlineEnd: 'none', flex: 1 }}
          />
          <Flex justify="flex-end" style={{ padding: 16 }}>
            <Tooltip title="Cerrar sesión">
              <Button type="text" icon={<LogoutOutlined />} onClick={logout} aria-label="Cerrar sesión" />
            </Tooltip>
          </Flex>
        </Flex>
      </Sider>
      <Layout>
        <Header style={{ display: 'flex', justifyContent: 'flex-end', alignItems: 'center', paddingInline: 32 }}>
          {user && <Typography.Text strong>{user.roles.map((rol) => ROL_LABEL[rol]).join(' / ')}</Typography.Text>}
        </Header>
        <Content style={{ padding: '8px 32px 32px' }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
