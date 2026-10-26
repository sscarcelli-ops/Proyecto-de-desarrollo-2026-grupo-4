import { Alert, Button, Flex, Table, Typography } from 'antd';
import type { TableColumnsType } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router';
import { useBarrios, type Barrio } from '../api/barrios';
import { EstadoTag, type Estado } from '../../../shared/components/EstadoTag';
import { getErrorMessage } from '../../../shared/lib/apiError';
import { PATHS } from '../../../routes/paths';

const { Title, Text } = Typography;

const columns: TableColumnsType<Barrio> = [
  { title: 'Nombre', dataIndex: 'nombre', key: 'nombre' },
  { title: 'Localidad / Provincia', dataIndex: 'localidadProvincia', key: 'localidadProvincia' },
  { title: 'Dirección', dataIndex: 'direccion', key: 'direccion' },
  {
    // US-10, criterio 3: un barrio recién creado figura "Inactivo – sin Operador"
    title: 'Estado',
    key: 'estado',
    render: (_, barrio) => (
      <>
        <EstadoTag estado={barrio.estado as Estado} />
        {barrio.operadorId === null && <Text type="secondary"> – sin Operador</Text>}
      </>
    ),
  },
];

// US-10: listado de barrios privados (Administrador). Sin paginación, orden alfabético desde el backend.
export function BarriosPage() {
  const navigate = useNavigate();
  const { data, error, isLoading } = useBarrios();

  return (
    <Flex vertical gap={16}>
      <Flex justify="space-between" align="center" wrap gap={12}>
        <Title level={3} style={{ margin: 0 }}>Barrios privados</Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate(PATHS.barrioNuevo)}>
          Nuevo barrio
        </Button>
      </Flex>

      {error && <Alert type="error" showIcon title="No se pudieron cargar los barrios" description={getErrorMessage(error)} />}

      <Table<Barrio>
        rowKey="id"
        columns={columns}
        dataSource={data}
        loading={isLoading}
        pagination={false}
        scroll={{ x: 'max-content' }}
        locale={{ emptyText: 'Todavía no hay barrios cargados' }}
      />
    </Flex>
  );
}
