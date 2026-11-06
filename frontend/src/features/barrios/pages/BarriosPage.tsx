import { useState } from 'react';
import { Alert, Button, Dropdown, Flex, Select, Table, Typography } from 'antd';
import type { MenuProps, TableColumnsType } from 'antd';
import { EditOutlined, EllipsisOutlined, PlusOutlined, UserAddOutlined } from '@ant-design/icons';
import { generatePath, useNavigate } from 'react-router';
import { useBarrios, type Barrio } from '../api/barrios';
import { AsignarOperadorModal } from '../components/AsignarOperadorModal';
import { EstadoTag, type Estado } from '../../../shared/components/EstadoTag';
import { getErrorMessage } from '../../../shared/lib/apiError';
import { PATHS } from '../../../routes/paths';

const { Title, Text } = Typography;

// Selector de estado del listado (RF-34)
type FiltroBarrios = 'TODOS' | 'ACTIVO' | 'INACTIVO' | 'SIN_OPERADOR';

const OPCIONES_FILTRO: { value: FiltroBarrios; label: string }[] = [
  { value: 'TODOS', label: 'Todos' },
  { value: 'ACTIVO', label: 'Activos' },
  { value: 'INACTIVO', label: 'Inactivos' },
  { value: 'SIN_OPERADOR', label: 'Sin Operador' },
];

function cumpleFiltro(barrio: Barrio, filtro: FiltroBarrios): boolean {
  switch (filtro) {
    case 'ACTIVO': return barrio.estado === 'ACTIVO';
    case 'INACTIVO': return barrio.estado === 'INACTIVO';
    case 'SIN_OPERADOR': return barrio.operadorId === null;
    default: return true;
  }
}

// US-10: listado de barrios privados (Administrador). Sin paginación, orden alfabético desde el backend.
// US-45: filtro "Sin Operador" y asignación del Operador desde cada barrio sin Operador.
export function BarriosPage() {
  const navigate = useNavigate();
  const { data, error, isLoading } = useBarrios();
  const [filtro, setFiltro] = useState<FiltroBarrios>('TODOS');
  const [barrioAAsignar, setBarrioAAsignar] = useState<Barrio | null>(null);

  const barrios = data?.filter((barrio) => cumpleFiltro(barrio, filtro));

  // Acciones disponibles según el caso de cada barrio. US-12 (activar/desactivar) y US-14 (reemplazar Operador)
  // se suman acá, cada una con su condición.
  const accionesDe = (barrio: Barrio): MenuProps['items'] => {
    const acciones: NonNullable<MenuProps['items']> = [
      {
        // US-11: editar los datos del barrio
        key: 'editar',
        icon: <EditOutlined />,
        label: 'Editar',
        onClick: () => navigate(generatePath(PATHS.barrioEditar, { id: String(barrio.id) })),
      },
    ];
    if (barrio.operadorId === null) {
      // US-45: solo los barrios sin Operador (con Operador se usa el reemplazo, US-14)
      acciones.push({
        key: 'asignar-operador',
        icon: <UserAddOutlined />,
        label: 'Asignar Operador',
        onClick: () => setBarrioAAsignar(barrio),
      });
    }
    return acciones;
  };

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
    {
      title: 'Acciones',
      key: 'acciones',
      width: 110,
      align: 'center',
      render: (_, barrio) => (
        <Dropdown placement="bottomRight" trigger={['click']} menu={{ items: accionesDe(barrio) }}>
          <Button shape="circle" icon={<EllipsisOutlined style={{ fontSize: 16 }} />} aria-label="Acciones" />
        </Dropdown>
      ),
    },
  ];

  return (
    <Flex vertical gap={16}>
      <Flex justify="space-between" align="center" wrap gap={12}>
        <Title level={3} style={{ margin: 0 }}>Barrios privados</Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate(PATHS.barrioNuevo)}>
          Nuevo barrio
        </Button>
      </Flex>

      <Flex align="center" gap={8} wrap>
        <Text>Estado:</Text>
        <Select<FiltroBarrios> value={filtro} onChange={setFiltro} options={OPCIONES_FILTRO} style={{ minWidth: 160 }} />
      </Flex>

      {error && <Alert type="error" showIcon title="No se pudieron cargar los barrios" description={getErrorMessage(error)} />}

      <Table<Barrio>
        rowKey="id"
        columns={columns}
        dataSource={barrios}
        loading={isLoading}
        pagination={false}
        scroll={{ x: 'max-content' }}
        locale={{ emptyText: filtro === 'TODOS' ? 'Todavía no hay barrios cargados' : 'No hay barrios con ese estado' }}
      />

      {barrioAAsignar && (
        <AsignarOperadorModal key={barrioAAsignar.id} barrio={barrioAAsignar} onClose={() => setBarrioAAsignar(null)} />
      )}
    </Flex>
  );
}
