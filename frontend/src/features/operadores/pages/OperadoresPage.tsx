import { useState } from 'react';
import { Button, Flex, Input, Pagination, Select, Table, Typography } from 'antd';
import type { TableColumnsType } from 'antd';
import { useNavigate } from 'react-router';
import { palette } from '../../../app/theme';
import { EstadoTag } from '../../../shared/components/EstadoTag';
import { getErrorMessage } from '../../../shared/lib/apiError';
import { PATHS } from '../../../routes/paths';
import { useOperadores, type EstadoOperador, type OperadorListado } from '../api/operadores';

const { Title } = Typography;

const TAMANO_PAGINA = 10;

const OPCIONES_ESTADO: { value: EstadoOperador | 'TODOS'; label: string }[] = [
  { value: 'TODOS', label: 'Todos' },
  { value: 'DISPONIBLE', label: 'Disponibles' },
  { value: 'OCUPADO', label: 'Ocupados' },
];

// US-15: Operadores del Administrador, con el barrio a cargo. Paginado y en orden alfabético (RNF-08).
// El buscador (nombre o email) se ejecuta con Enter o con el botón, no al escribir; el filtro por estado es directo.
// Las acciones por fila (editar datos y cambiar contraseña, US-16 y US-17) se suman al menú "⋯" cuando existan.
export function OperadoresPage() {
  const navigate = useNavigate();
  const [pagina, setPagina] = useState(1);
  const [texto, setTexto] = useState(''); // lo que se escribe
  const [busqueda, setBusqueda] = useState(''); // lo que se buscó
  const [estado, setEstado] = useState<EstadoOperador | undefined>(undefined);

  const { data, error, isLoading } = useOperadores({ page: pagina, size: TAMANO_PAGINA, q: busqueda, estado });

  const buscar = (valor: string) => {
    setBusqueda(valor.trim());
    setPagina(1);
  };

  const hayFiltros = busqueda !== '' || estado !== undefined;

  const columnas: TableColumnsType<OperadorListado> = [
    {
      title: 'Nombre y Apellido',
      key: 'nombre',
      ellipsis: true,
      render: (_, o) => `${o.nombre} ${o.apellido}`,
    },
    { title: 'Email', dataIndex: 'email', ellipsis: true },
    { title: 'Teléfono', dataIndex: 'telefono', width: 150 },
    {
      title: 'Barrio',
      dataIndex: 'barrioNombre',
      ellipsis: true,
      render: (barrio: string | null) => barrio ?? 'Sin barrio asignado',
    },
    {
      title: 'Estado',
      dataIndex: 'estado',
      width: 170,
      align: 'center',
      render: (valor: EstadoOperador) => <EstadoTag estado={valor} />,
    },
  ];

  return (
    <Flex vertical gap={16}>
      <Flex justify="space-between" align="center" wrap gap={8}>
        <Title level={3} style={{ margin: 0 }}>Operadores</Title>
        <Button type="primary" onClick={() => navigate(PATHS.operadorNuevo)}>Crear Operador</Button>
      </Flex>

      <Flex gap={12} wrap>
        <Input.Search
          placeholder="Buscar por nombre o email"
          maxLength={100}
          allowClear
          enterButton
          value={texto}
          onChange={(e) => {
            setTexto(e.target.value);
            // Al borrar el texto (con la cruz o a mano) se vuelve a mostrar todo, sin esperar otro Enter
            if (e.target.value === '' && busqueda !== '') buscar('');
          }}
          onSearch={buscar}
          style={{ maxWidth: 380 }}
        />
        <Select<EstadoOperador | 'TODOS'>
          aria-label="Filtrar por estado"
          value={estado ?? 'TODOS'}
          options={OPCIONES_ESTADO}
          onChange={(valor) => {
            setEstado(valor === 'TODOS' ? undefined : valor);
            setPagina(1);
          }}
          style={{ width: 170 }}
        />
      </Flex>

      <div style={{ backgroundColor: palette.surface, borderRadius: 8 }}>
        <Table<OperadorListado>
          bordered
          className="tabla-redondeada"
          rowKey="id"
          columns={columnas}
          dataSource={data?.content ?? []}
          loading={isLoading}
          scroll={{ x: 820 }}
          pagination={false}
          locale={{
            // US-15 CA-5
            emptyText: (
              <div style={{ padding: '40px 16px', color: palette.textSecondary, fontSize: 15 }}>
                {error
                  ? getErrorMessage(error)
                  : hayFiltros
                    ? 'No se encontraron resultados con los filtros aplicados'
                    : 'Todavía no hay Operadores creados'}
              </div>
            ),
          }}
        />
      </div>

      <Flex justify="center">
        <Pagination
          current={pagina}
          pageSize={TAMANO_PAGINA}
          total={data?.totalElements ?? 0}
          showSizeChanger={false}
          hideOnSinglePage
          onChange={setPagina}
        />
      </Flex>
    </Flex>
  );
}
