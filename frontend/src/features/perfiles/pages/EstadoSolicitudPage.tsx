import { Alert, Flex, Typography } from 'antd';
import { ClockCircleOutlined } from '@ant-design/icons';
import { BotonCerrarSesion } from '../../auth/components/BotonCerrarSesion';
import { useMiSolicitud } from '../api/perfiles';
import { palette } from '../../../app/theme';

/**
 * US-08: Vista de estado de la solicitud.
 * Pendiente: aviso de espera. Rechazada: alerta (el motivo y el reenvío son US-09).
 */
export function EstadoSolicitudPage() {
  const { data: perfil } = useMiSolicitud();
  const rechazada = perfil?.estado === 'RECHAZADO';

  return (
    <Flex vertical align="center" justify="center" style={{ minHeight: '60vh', textAlign: 'center' }}>
      {rechazada ? (
        <Alert type="error" showIcon title="Su solicitud ha sido rechazada" style={{ marginBottom: 32 }} />
      ) : (
        <>
          <ClockCircleOutlined style={{ fontSize: 64, color: palette.primary, marginBottom: 24 }} />

          <Typography.Title level={2} style={{ marginBottom: 12 }}>
            Tu solicitud está pendiente de aprobación
          </Typography.Title>

          <Typography.Paragraph type="secondary" style={{ fontSize: 16, maxWidth: 500, marginBottom: 32 }}>
            Podrás utilizar las funcionalidades de tu perfil cuando el Administrador apruebe tu solicitud.
          </Typography.Paragraph>
        </>
      )}

      <BotonCerrarSesion conTexto />
    </Flex>
  );
}
