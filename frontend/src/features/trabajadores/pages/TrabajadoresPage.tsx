import { useState } from 'react';
import { Button, ConfigProvider, Dropdown, Flex, Input, Pagination, Table, Typography } from 'antd';
import {
  EllipsisOutlined,
  EditOutlined,
  PlusCircleOutlined,
  SearchOutlined,
  FilterFilled,
} from '@ant-design/icons';
import { useNavigate } from 'react-router';
import { useTrabajadores, type TrabajadorResponse } from '../api/trabajadores';
import { EstadoTag, type Estado } from '../../../shared/components/EstadoTag';
import { PATHS } from '../../../routes/paths';
import type { ColumnsType } from 'antd/es/table';

/**
 * US-26: listado paginado de trabajadores.
 *
 * Diseño alineado al wireframe de Figma:
 * - Título con presencia e impacto visual.
 * - Disparador limpio "(+) Registrar trabajador" con ícono verde.
 * - Campo buscador amplio con ícono de lupa y filtro.
 * - Tabla con bordes nítidos de 1px, esquinas redondeadas y filas con altura cómoda.
 * - Paginación anclada armónicamente en pantalla normal y pantalla completa (F11).
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

  // Columnas de la tabla, siguiendo el wireframe: Nombre | DNI | Estado documental | Detalle
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
    <div
      style={{
        maxWidth: 1080,
        margin: '0 auto',
        width: '100%',
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
      }}
    >
      {/* Título adaptable */}
      <Typography.Title
        level={1}
        style={{
          fontSize: 'clamp(26px, 2.2vw, 32px)',
          fontWeight: 700,
          color: '#1a2233',
          marginTop: 0,
          marginBottom: 16,
        }}
      >
        Trabajadores
      </Typography.Title>

      {/* Barra superior separada de la tabla con buen respiro */}
      <Flex justify="space-between" align="center" wrap="wrap" gap={16} style={{ marginBottom: 30 }}>
        <button
          type="button"
          onClick={() => navigate(PATHS.trabajadorNuevo)}
          style={{
            background: 'none',
            border: 'none',
            padding: 0,
            cursor: 'pointer',
            display: 'inline-flex',
            alignItems: 'center',
            gap: 12,
            transition: 'opacity 0.2s ease',
          }}
          onMouseEnter={(e) => (e.currentTarget.style.opacity = '0.8')}
          onMouseLeave={(e) => (e.currentTarget.style.opacity = '1')}
        >
          <PlusCircleOutlined style={{ fontSize: 32, color: '#10b981' }} />
          <span style={{ fontSize: 19, fontWeight: 600, color: '#1a2233' }}>
            Registrar trabajador
          </span>
        </button>

        <Input
          size="middle"
          placeholder="Buscar trabajador..."
          prefix={<SearchOutlined style={{ color: '#94a3b8', fontSize: 15, marginRight: 6 }} />}
          suffix={<FilterFilled style={{ color: '#475569', fontSize: 15, cursor: 'pointer' }} />}
          allowClear
          value={busqueda}
          onChange={(e) => setBusqueda(e.target.value)}
          style={{
            width: 320,
            borderRadius: 8,
            borderColor: '#cbd5e1',
            height: 38,
          }}
        />
      </Flex>

      {/* Tabla con bordes homogéneos, celdas proporcionadas y esquinas redondeadas */}
      <ConfigProvider
        theme={{
          components: {
            Table: {
              borderColor: '#cbd5e1',
              borderRadius: 8,
              cellPaddingBlock: 12,
            },
          },
        }}
      >
        <div
          style={{
            backgroundColor: '#ffffff',
            boxShadow: '0 1px 2px 0 rgba(0, 0, 0, 0.03)',
            borderRadius: 8,
          }}
        >
          <Table<TrabajadorResponse>
            bordered
            className="tabla-redondeada"
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

      {/* Paginación anclada al fondo gracias al flex del contenedor */}
      <Flex justify="center" style={{ marginTop: 'auto', paddingTop: 20, paddingBottom: 8 }}>
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
