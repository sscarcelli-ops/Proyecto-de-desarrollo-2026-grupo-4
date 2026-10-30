import { useState } from 'react';
import { Button, Dropdown, Flex, Pagination, Table, Typography } from 'antd';
import {
  EllipsisOutlined,
  EditOutlined,
  PlusCircleOutlined,
} from '@ant-design/icons';
import { generatePath, useNavigate } from 'react-router';
import { useTrabajadores, type TrabajadorResponse } from '../api/trabajadores';
import { EstadoTag, type Estado } from '../../../shared/components/EstadoTag';
import { PATHS } from '../../../routes/paths';
import { palette } from '../../../app/theme';
import type { ColumnsType } from 'antd/es/table';

/**
 * US-26: listado paginado de trabajadores (alcance Semana 1).
 *
 * Muestra los trabajadores de la empresa logueada con su estado documental calculado al vuelo
 * (Vigente, Vencido, Pendiente de carga).
 */
export function TrabajadoresPage() {
  const navigate = useNavigate();

  // Paginación: Ant Design cuenta desde 1, el hook convierte a 0-based internamente
  const [pagina, setPagina] = useState(1);
  const [tamano, setTamano] = useState(10);
  const { data, isLoading } = useTrabajadores(pagina, tamano);

  // Columnas de la tabla: Nombre | DNI | Estado documental | Detalle
  const columnas: ColumnsType<TrabajadorResponse> = [
    {
      title: <span style={{ fontWeight: 600, color: palette.text }}>Nombre</span>,
      key: 'nombre',
      align: 'center' as const,
      render: (_, t) => `${t.nombre} ${t.apellido}`,
    },
    {
      title: <span style={{ fontWeight: 600, color: palette.text }}>DNI</span>,
      dataIndex: 'dni',
      width: 170,
      align: 'center' as const,
    },
    {
      title: <span style={{ fontWeight: 600, color: palette.text }}>Estado documental</span>,
      dataIndex: 'estadoDocumental',
      width: 220,
      align: 'center' as const,
      render: (estado: string) => <EstadoTag estado={estado as Estado} />,
    },
    {
      title: <span style={{ fontWeight: 600, color: palette.text }}>Detalle</span>,
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
                onClick: () => navigate(generatePath(PATHS.trabajadorEditar, { id: String(t.id) })),
              },
            ],
          }}
        >
          <Button
            shape="circle"
            icon={<EllipsisOutlined style={{ fontSize: 16 }} />}
            aria-label="Acciones"
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
          color: palette.text,
          marginTop: 0,
          marginBottom: 16,
        }}
      >
        Trabajadores
      </Typography.Title>

      {/* Acción principal: botón de registrar trabajador */}
      <Flex justify="flex-start" align="center" style={{ marginBottom: 24 }}>
        <Button
          type="primary"
          size="large"
          icon={<PlusCircleOutlined />}
          onClick={() => navigate(PATHS.trabajadorNuevo)}
          style={{
            borderRadius: 6,
            fontWeight: 600,
          }}
        >
          Registrar trabajador
        </Button>
      </Flex>

      {/* Tabla con bordes redondeados y paginación desacoplada */}
      <div
        style={{
          backgroundColor: palette.surface,
          boxShadow: '0 1px 2px 0 rgba(0, 0, 0, 0.03)',
          borderRadius: 8,
        }}
      >
        <Table<TrabajadorResponse>
          bordered
          className="tabla-redondeada"
          rowKey="id"
          columns={columnas}
          dataSource={data?.content ?? []}
          loading={isLoading}
          locale={{
            emptyText: 'No hay trabajadores registrados. Usá el botón "Registrar trabajador" para agregar uno.',
          }}
          pagination={false}
        />
      </div>

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
          }}
        />
      </Flex>
    </div>
  );
}
