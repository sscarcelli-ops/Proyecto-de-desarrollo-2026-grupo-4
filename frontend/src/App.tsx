import { useState } from 'react';
import { Button, Card, Typography, Alert, Space, Tag } from 'antd';
import { CheckCircleOutlined, ApiOutlined, ReloadOutlined } from '@ant-design/icons';
import { httpClient } from './shared/lib/httpClient';

const { Title, Text } = Typography;

interface PingResponse {
  status: string;
  message: string;
  timestamp: string;
}

function App() {
  const [data, setData] = useState<PingResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const testConnection = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await httpClient.get<PingResponse>('/ping');
      setData(response.data);
    } catch (err: any) {
      setError(err.message || 'Error al conectar con el backend');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh', background: '#f0f2f5' }}>
      <Card style={{ width: 450, boxShadow: '0 4px 12px rgba(0,0,0,0.1)' }}>
        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          <div style={{ textAlign: 'center' }}>
            <Title level={3} style={{ margin: 0 }}>Grupo 4 - Control de Accesos</Title>
            <Text type="secondary">Tarea T-02: Configuración Base</Text>
          </div>

          <Alert
            message="Frontend: React + Vite + Ant Design"
            description="Listo para validar comunicación con Spring Boot"
            type="info"
            showIcon
          />

          <Button
            type="primary"
            icon={<ApiOutlined />}
            loading={loading}
            onClick={testConnection}
            block
            size="large"
          >
            Probar conexión con Backend (/api/ping)
          </Button>

          {data && (
            <Alert
              message="¡Conexión Exitosa con el Backend!"
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
              message="Error de conexión"
              description={`${error}. Asegurate de que el backend esté corriendo en el puerto 8080.`}
              type="error"
              showIcon
            />
          )}
        </Space>
      </Card>
    </div>
  );
}

export default App;