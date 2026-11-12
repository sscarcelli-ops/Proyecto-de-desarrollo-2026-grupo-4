import { Flex, Typography } from 'antd';
import { ClockCircleOutlined, ExclamationCircleOutlined } from '@ant-design/icons';
import { BotonCambiarPassword } from '../../auth/components/BotonCambiarPassword';
import { BotonCerrarSesion } from '../../auth/components/BotonCerrarSesion';
import { useMiSolicitud } from '../api/perfiles';
import { Logo } from '../../../shared/components/Logo';
import { palette } from '../../../app/theme';

/**
 * US-08 (RF-22): estado de la solicitud de una cuenta sin rol aprobado, según el wireframe.
 * Pendiente: aviso de espera. Rechazada: alerta con el motivo registrado por el Administrador (RF-33).
 * Corregir y reenviar la solicitud es US-09: hasta que se implemente, solo se muestra el motivo.
 */
export function EstadoSolicitudPage() {
  const { data: perfil } = useMiSolicitud();
  const rechazada = perfil?.estado === 'RECHAZADO';

  return (
    // Márgenes negativos: el encabezado y el pie ocupan todo el ancho de la tarjeta del PublicLayout
    <Flex vertical style={{ margin: '-48px -24px' }}>
      <Flex
        align="center"
        gap={12}
        style={{
          padding: '16px 24px',
          background: palette.tableHeader,
          borderBottom: `1px solid ${palette.borderSoft}`,
          borderRadius: '8px 8px 0 0',
        }}
      >
        <Logo size={28} />
        <Flex vertical style={{ flex: 1, minWidth: 0 }}>
          <Typography.Text strong style={{ fontSize: 16 }}>
            Portal de gestión de contratistas
          </Typography.Text>
          <Typography.Text type="secondary" style={{ fontSize: 13 }}>
            Estado de cuenta y solicitud
          </Typography.Text>
        </Flex>
      </Flex>

      <Flex
        vertical
        align="center"
        style={{
          margin: '32px 24px',
          padding: '40px 24px',
          textAlign: 'center',
          background: palette.background,
          border: `1px solid ${palette.border}`,
          borderRadius: 8,
        }}
      >
        {rechazada ? (
          <>
            <ExclamationCircleOutlined style={{ fontSize: 56, color: palette.error, marginBottom: 20 }} />
            <Typography.Title level={3} style={{ marginBottom: 12 }}>
              Su solicitud ha sido rechazada
            </Typography.Title>
            {perfil?.motivoRechazo && (
              <Typography.Paragraph style={{ fontSize: 15, maxWidth: 520, marginBottom: 0 }}>
                <Typography.Text strong>Motivo del rechazo:</Typography.Text> {perfil.motivoRechazo}
              </Typography.Paragraph>
            )}
          </>
        ) : (
          <>
            <ClockCircleOutlined style={{ fontSize: 56, color: palette.primary, marginBottom: 20 }} />
            <Typography.Title level={3} style={{ marginBottom: 12 }}>
              Tu solicitud está pendiente de aprobación
            </Typography.Title>
            <Typography.Paragraph type="secondary" style={{ fontSize: 15, maxWidth: 480, marginBottom: 0 }}>
              Podrás utilizar las funcionalidades de tu perfil cuando el Administrador apruebe tu solicitud.
            </Typography.Paragraph>
          </>
        )}
      </Flex>

      {/* "Cambiar contraseña" y "Cerrar sesión" juntos, como en el registro (acuerdo del grupo del 6/11).
          Cambiar la contraseña también sin rol aprobado (RF-22, US-07) */}
      <Flex
        gap={8}
        wrap
        justify="center"
        style={{ padding: '8px 16px', borderTop: `1px solid ${palette.borderSoft}` }}
      >
        <BotonCambiarPassword forma="link" />
        <BotonCerrarSesion conTexto />
      </Flex>
    </Flex>
  );
}
