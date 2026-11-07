import { useState } from 'react';
import { Alert, Button, Dropdown, Flex, Select, Table, Typography } from 'antd';
import type { MenuProps, TableColumnsType } from 'antd';
import { EditOutlined, EllipsisOutlined, PlusOutlined } from '@ant-design/icons';
import { generatePath, useNavigate } from 'react-router';
import { useBarrios, type Barrio } from '../api/barrios';
import { AsignarOperadorSelect } from '../components/AsignarOperadorSelect';
import { EstadoTag, type Estado } from '../../../shared/components/EstadoTag';
import { getErrorMessage } from '../../../shared/lib/apiError';
import { PATHS } from '../../../routes/paths';

const { Title, Text } = Typography;

// Suma de los anchos de las columnas de la tabla (Barrio 220 + Localidad 200 + Dirección 240 + Op. Asignado 230 + Estado 190 + Acción 90)
const ANCHO_TABLA = 1170;

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
// US-45: filtro "Sin Operador" y asignación del Operador desde la columna "Op. Asignado".
export function BarriosPage() {
  const navigate = useNavigate();
  const { data, error, isLoading } = useBarrios();
  const [filtro, setFiltro] = useState<FiltroBarrios>('TODOS');

  const barrios = data?.filter((barrio) => cumpleFiltro(barrio, filtro));

  // Acciones disponibles según el caso de cada barrio. US-14 (reemplazar Operador) y US-12 (activar/desactivar)
  // se suman acá, cada una con su condición. La asignación del primer Operador va en la columna "Op. Asignado".
  const accionesDe = (barrio: Barrio): NonNullable<MenuProps['items']> => [
    {
      // US-11: editar los datos del barrio
      key: 'editar',
      icon: <EditOutlined />,
      label: 'Editar',
      onClick: () => navigate(generatePath(PATHS.barrioEditar, { id: String(barrio.id) })),
    },
  ];

  // Los textos largos se cortan con "…" y se ven completos al pasar el mouse
  const columns: TableColumnsType<Barrio> = [
    { title: 'Barrio', dataIndex: 'nombre', key: 'nombre', width: 220, ellipsis: true },
    { title: 'Localidad', dataIndex: 'localidadProvincia', key: 'localidadProvincia', width: 200, ellipsis: true },
    { title: 'Dirección', dataIndex: 'direccion', key: 'direccion', width: 240, ellipsis: true },
    {
      // Con Operador: su nombre. Sin Operador: el desplegable para asignarlo (US-45)
      title: 'Op. Asignado',
      key: 'operador',
      width: 230,
      ellipsis: true,
      render: (_, barrio) =>
        barrio.operadorId === null
          ? <AsignarOperadorSelect barrio={barrio} />
          : `${barrio.operadorNombre} ${barrio.operadorApellido}`,
    },
    {
      // US-10, criterio 3: un barrio recién creado figura "Inactivo – sin Operador"
      title: 'Estado',
      key: 'estado',
      width: 190,
      render: (_, barrio) => (
        <>
          <EstadoTag estado={barrio.estado as Estado} />
          {barrio.operadorId === null && <Text type="secondary"> – sin Operador</Text>}
        </>
      ),
    },
    {
      title: 'Acción',
      key: 'accion',
      width: 90,
      align: 'center',
      render: (_, barrio) => (
        <Dropdown placement="bottomRight" trigger={['click']} menu={{ items: accionesDe(barrio) }}>
          <Button shape="circle" icon={<EllipsisOutlined style={{ fontSize: 16 }} />} aria-label="Acción" />
        </Dropdown>
      ),
    },
  ];

  return (
    <Flex vertical gap={16}>
      <Title level={3} style={{ margin: 0 }}>Barrios privados</Title>

      <Flex justify="space-between" align="center" wrap gap={12}>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate(PATHS.barrioNuevo)}>
          Nuevo barrio
        </Button>
        <Flex align="center" gap={8}>
          <Text>Estado:</Text>
          <Select<FiltroBarrios> value={filtro} onChange={setFiltro} options={OPCIONES_FILTRO} style={{ minWidth: 160 }} />
        </Flex>
      </Flex>

      {error && <Alert type="error" showIcon title="No se pudieron cargar los barrios" description={getErrorMessage(error)} />}

      <Table<Barrio>
        rowKey="id"
        columns={columns}
        dataSource={barrios}
        loading={isLoading}
        pagination={false}
        // Ancho fijo por columna: con 'max-content' la tabla crece con el texto y los "…" no llegan a aparecer.
        // En pantallas angostas la tabla se desplaza horizontalmente.
        tableLayout="fixed"
        scroll={{ x: ANCHO_TABLA }}
        locale={{ emptyText: filtro === 'TODOS' ? 'Todavía no hay barrios cargados' : 'No hay barrios con ese estado' }}
      />
    </Flex>
  );
}
