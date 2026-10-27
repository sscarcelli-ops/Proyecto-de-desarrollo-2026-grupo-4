import { useState } from 'react';
import { Button, Dropdown, Flex, Input, Table, Typography } from 'antd';
import { EllipsisOutlined, EditOutlined, PlusOutlined, SearchOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router';
import { useTrabajadores, type TrabajadorResponse } from '../api/trabajadores';
import { EstadoTag, type Estado } from '../../../shared/components/EstadoTag';
import { PATHS } from '../../../routes/paths';
import type { ColumnsType } from 'antd/es/table';

/**
 * US-26: listado paginado de trabajadores.
 *
 * Lo que muestra:
 * - Nombre completo (nombre + apellido concatenados, como en el wireframe)
 * - DNI
 * - Estado documental (badge de color con EstadoTag)
 * - Botón de acciones (Editar; Borrar se suma en S3 con US-27)
 *
 * La búsqueda filtra localmente sobre la página actual por nombre o DNI.
 * En un futuro se puede agregar búsqueda server-side con un query param.
 */
export function TrabajadoresPage() {
  const navigate = useNavigate();

  // Paginación: Ant Design cuenta desde 1, el hook convierte a 0-based internamente
  const [pagina, setPagina] = useState(1);
  const [tamano, setTamano] = useState(10);
  const { data, isLoading } = useTrabajadores(pagina, tamano);

  // Búsqueda local: filtra sobre los trabajadores de la página actual
  const [busqueda, setBusqueda] = useState('');

  const trabajadoresFiltrados = (data?.content ?? []).filter((t) => {
    if (!busqueda.trim()) return true;
    const texto = busqueda.toLowerCase();
    const nombreCompleto = `${t.nombre} ${t.apellido}`.toLowerCase();
    return nombreCompleto.includes(texto) || t.dni.includes(texto);
  });

  // Columnas de la tabla, siguiendo el wireframe: Nombre | DNI | Estado doc. | Detalle
  const columnas: ColumnsType<TrabajadorResponse> = [
    {
      title: 'Nombre',
      key: 'nombre',
      // Concatenamos nombre y apellido como muestra el wireframe ("Carlos Pablo Mendoza")
      render: (_, t) => `${t.nombre} ${t.apellido}`,
    },
    {
      title: 'DNI',
      dataIndex: 'dni',
      width: 140,
    },
    {
      title: 'Estado doc.',
      dataIndex: 'estadoDocumental',
      width: 180,
      render: (estado: string) => <EstadoTag estado={estado as Estado} />,
    },
    {
      title: 'Detalle',
      key: 'acciones',
      width: 80,
      align: 'center' as const,
      render: (_, t) => (
        <Dropdown
          trigger={['click']}
          menu={{
            items: [
              {
                key: 'editar',
                icon: <EditOutlined />,
                label: 'Editar',
                onClick: () => navigate(PATHS.trabajadorEditar(t.id)),
              },
              // Borrar se agrega en S3 (US-27). Ejemplo de cómo quedará:
              // { key: 'borrar', icon: <DeleteOutlined />, label: 'Borrar', danger: true,
              //   onClick: () => abrirModalConfirmacion(t) }
            ],
          }}
        >
          <Button type="text" icon={<EllipsisOutlined />} aria-label="Acciones" />
        </Dropdown>
      ),
    },
  ];

  return (
    <>
      <Typography.Title level={2} style={{ marginTop: 0 }}>
        Trabajadores
      </Typography.Title>

      {/* Barra superior: botón de registrar + campo de búsqueda (como en el wireframe) */}
      <Flex justify="space-between" align="center" wrap="wrap" gap={12} style={{ marginBottom: 16 }}>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={() => navigate(PATHS.trabajadorNuevo)}
        >
          Registrar trabajador
        </Button>

        <Input
          placeholder="Buscar trabajador..."
          prefix={<SearchOutlined />}
          allowClear
          value={busqueda}
          onChange={(e) => setBusqueda(e.target.value)}
          style={{ maxWidth: 300 }}
        />
      </Flex>

      {/* Tabla paginada. La paginación se conecta con el backend a través del hook useTrabajadores */}
      <Table<TrabajadorResponse>
        rowKey="id"
        columns={columnas}
        dataSource={trabajadoresFiltrados}
        loading={isLoading}
        locale={{
          emptyText: 'No hay trabajadores registrados. Usá el botón "Registrar trabajador" para agregar uno.',
        }}
        pagination={{
          current: pagina,
          pageSize: tamano,
          total: data?.totalElements ?? 0,
          showSizeChanger: true,
          pageSizeOptions: ['5', '10', '20'],
          onChange: (page, size) => {
            setPagina(page);
            setTamano(size);
            setBusqueda(''); // limpiar filtro al cambiar de página
          },
        }}
      />
    </>
  );
}
