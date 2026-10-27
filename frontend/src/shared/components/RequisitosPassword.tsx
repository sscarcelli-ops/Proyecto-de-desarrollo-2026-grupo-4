import { Flex, theme } from 'antd';
import { CheckCircleFilled, CheckOutlined } from '@ant-design/icons';
import { REQUISITOS_PASSWORD } from '../lib/politicaPassword';

/**
 * US-44: los requisitos de la política debajo del campo, cada uno en verde apenas se cumple (wireframe:
 * "✓ Mínimo 8 caracteres ✓ Un número ✓ Una Mayúscula ✓ Una Minúscula"). Se usa a través de CampoPasswordNueva.
 */
export function RequisitosPassword({ password = '' }: { password?: string }) {
  const { token } = theme.useToken();

  return (
    // marginBottom: con el error visible, Ant Design no deja espacio debajo y quedaba pegado al campo siguiente
    <Flex wrap gap="4px 16px" style={{ marginTop: 6, marginBottom: 8, fontSize: 12 }} aria-live="polite">
      {REQUISITOS_PASSWORD.map(({ texto, cumple }) => {
        const ok = cumple(password);
        return (
          <span key={texto} style={{ color: ok ? token.colorSuccess : token.colorTextTertiary }}>
            {ok ? <CheckCircleFilled /> : <CheckOutlined />} {texto}
            <span style={{ position: 'absolute', width: 1, height: 1, overflow: 'hidden', clipPath: 'inset(50%)' }}>
              {ok ? ' (cumplido)' : ' (pendiente)'}
            </span>
          </span>
        );
      })}
    </Flex>
  );
}
