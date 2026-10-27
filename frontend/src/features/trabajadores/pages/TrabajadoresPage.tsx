import { useState } from 'react';
import { Button, ConfigProvider, Dropdown, Flex, Input, Pagination, Table, Typography } from 'antd';
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
      title: <span style={{ fontWeight: 600, color: '#1a2233' }}>Nombre</span>,
      key: 'nombre',
      align: 'center' as const,
      render: (_, t) => `${t.nombre} ${t.apellido}`,
    },
    {
      title: <span style={{ fontWeight: 600, color: '#1a2233' }}>DNI</span>,
      dataIndex: 'dni',
      width: 170,
      align: 'center' as const,
    },
    {
      title: <span style={{ fontWeight: 600, color: '#1a2233' }}>Estado documental</span>,
      dataIndex: 'estadoDocumental',
      width: 220,
      align: 'center' as const,
      render: (estado: string) => <EstadoTag estado={estado as Estado} />,
    },
    {
      title: <span style={{ fontWeight: 600, color: '#1a2233' }}>Detalle</span>,
      key: 'acciones',
      width: 110,
      align: 'center' as const,
      render: (_, t) => (
        <Dropdown
          placement="topRight"
          trigger={['click']}
          menu={{
            items: [
              {
                key: 'editar',
                icon: <EditOutlined />,
                label: 'Editar',
                onClick: () => navigate(PATHS.trabajadorEditar(t.id)),
              },
            ],
          }}
        >
          <Button
            shape="circle"
            icon={<EllipsisOutlined style={{ fontSize: 16, color: '#4a5568', fontWeight: 'bold' }} />}
            aria-label="Acciones"
            style={{
              borderColor: '#4a5568',
              borderWidth: 1.5,
              width: 32,
              height: 32,
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          />
        </Dropdown>
      ),
    },
  ];

  return (
    <div style={{ maxWidth: 1080, margin: '0 auto', width: '100%' }}>
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

      {/* Tabla con bordes exteriores e interiores homogéneos de 1px y esquinas redondeadas */}
      <ConfigProvider
        theme={{
          components: {
            Table: {
              borderColor: '#cbd5e1',
              borderRadius: 8,
            },
          },
        }}
      >
        <div
          style={{
            borderRadius: 8,
            overflow: 'hidden',
            backgroundColor: '#ffffff',
            marginBottom: 24,
          }}
        >
          <Table<TrabajadorResponse>
            bordered
            rowKey="id"
            columns={columnas}
            dataSource={trabajadoresFiltrados}
            loading={isLoading}
            locale={{
              emptyText: 'No hay trabajadores registrados. Usá el botón "Registrar trabajador" para agregar uno.',
            }}
            pagination={false}
          />
        </div>
      </ConfigProvider>

      {/* Paginación centrada debajo del bloque redondeado de la tabla */}
      <Flex justify="center" style={{ marginTop: 16 }}>
        <Pagination
          current={pagina}
          pageSize={tamano}
          total={data?.totalElements ?? 0}
          showSizeChanger={false}
          onChange={(page, size) => {
            setPagina(page);
            setTamano(size);
            setBusqueda('');
          }}
        />
      </Flex>
    </div>
  );
}
