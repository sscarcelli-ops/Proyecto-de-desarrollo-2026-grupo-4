import { Alert, Button, Space, Tag, Typography } from 'antd';
import { ApiOutlined, CheckCircleOutlined } from '@ant-design/icons';
import { usePing } from '../api/ping';
import { getErrorMessage } from '../../../shared/lib/apiError';

const { Title, Text } = Typography;

// Pantalla provisoria de la T-02: se reemplaza por el login (US-04) en la ruta "/"
export function PingPage() {
  const { data, error, isFetching, refetch } = usePing();

  return (
    <Space orientation="vertical" size="middle" style={{ width: '100%', maxWidth: 420, margin: '0 auto', display: 'flex' }}>
      <div style={{ textAlign: 'center' }}>
        <Title level={3} style={{ margin: 0 }}>Grupo 4 - Control de Accesos</Title>
        <Text type="secondary">Tarea T-02: Configuración Base</Text>
      </div>

      <Alert
        title="Frontend: React + Vite + Ant Design"
        description="Listo para validar comunicación con Spring Boot"
        type="info"
        showIcon
      />

      <Button
        type="primary"
        icon={<ApiOutlined />}
        loading={isFetching}
        onClick={() => refetch()}
        block
        size="large"
      >
        Probar conexión con Backend (/api/ping)
      </Button>

      {data && !error && (
        <Alert
          title="¡Conexión Exitosa con el Backend!"
          description={
            <div style={{ marginTop: 8 }}>
              <p><Text strong>Mensaje:</Text> <Tag color="green">{data.message}</Tag></p>
              <p><Text strong>Estado:</Text> <Tag color="blue">{data.status}</Tag></p>
              <p><Text strong>Timestamp:</Text> {data.timestamp}</p>
            </div>
          }
          type="success"
          showIcon
          icon={<CheckCircleOutlined />}
        />
      )}

      {error && (
        <Alert
          title="Error de conexión"
          description={getErrorMessage(error)}
          type="error"
          showIcon
        />
      )}
    </Space>
  );
}
