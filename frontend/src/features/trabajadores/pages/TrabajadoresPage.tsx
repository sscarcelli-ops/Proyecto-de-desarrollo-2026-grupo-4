import { useState } from 'react';
import { App, Button, Dropdown, Flex, Modal, Pagination, Table, Tag, Typography } from 'antd';
import {
  EllipsisOutlined,
  EditOutlined,
  PlusCircleOutlined,
  UserDeleteOutlined,
} from '@ant-design/icons';
import { generatePath, useNavigate } from 'react-router';
import { useTrabajadores, useDarDeBajaTrabajador, type TrabajadorResponse } from '../api/trabajadores';
import { EstadoTag, type Estado } from '../../../shared/components/EstadoTag';
import { PATHS } from '../../../routes/paths';
import { palette, estadoTonos } from '../../../app/theme';
import { getErrorMessage } from '../../../shared/lib/apiError';
import type { ColumnsType } from 'antd/es/table';

/**
 * US-26: listado paginado de trabajadores.
 * US-27: dar de baja trabajador.
 *
 * Muestra los trabajadores de la empresa logueada con su estado documental calculado al vuelo
 * (Vigente, Vencido, Pendiente de carga) y permite dar de baja a un trabajador.
 */
export function TrabajadoresPage() {
  const { message } = App.useApp();
  const navigate = useNavigate();

  // Paginación: Ant Design cuenta desde 1, el hook convierte a 0-based internamente
  const [pagina, setPagina] = useState(1);
  const [tamano, setTamano] = useState(10);
  const { data, isLoading } = useTrabajadores(pagina, tamano);
  const darDeBaja = useDarDeBajaTrabajador();

  // Modal para confirmar dar de baja (US-27)
  const [trabajadorParaBaja, setTrabajadorParaBaja] = useState<TrabajadorResponse | null>(null);

  const handleConfirmarBaja = () => {
    if (!trabajadorParaBaja) return;
    darDeBaja.mutate(trabajadorParaBaja.id, {
      onSuccess: () => {
        message.success('Trabajador dado de baja correctamente');
        setTrabajadorParaBaja(null);
      },
      onError: (err) => {
        message.error(getErrorMessage(err));
      },
    });
  };

  // Columnas de la tabla: Nombre | DNI | Estado documental | Detalle
  const columnas: ColumnsType<TrabajadorResponse> = [
    {
      title: <span style={{ fontWeight: 600, color: palette.text }}>Nombre</span>,
      key: 'nombre',
      align: 'center' as const,
      render: (_, t) => (
        <Flex align="center" justify="center" gap={8}>
          <span style={{ color: t.dadoDeBaja ? palette.textSecondary : palette.text }}>
            {t.nombre} {t.apellido}
          </span>
          {t.dadoDeBaja && (
            <Tag
              bordered
              style={{
                ...estadoTonos.neutro,
                marginInlineEnd: 0,
                fontSize: 11,
                height: 22,
                lineHeight: '20px',
                paddingInline: 6,
                borderRadius: 4,
              }}
            >
              Dado de baja
            </Tag>
          )}
        </Flex>
      ),
    },
    {
      title: <span style={{ fontWeight: 600, color: palette.text }}>DNI</span>,
      dataIndex: 'dni',
      width: 170,
      align: 'center' as const,
      render: (dni: string, t) => (
        <span style={{ color: t.dadoDeBaja ? palette.textSecondary : palette.text }}>
          {dni}
        </span>
      ),
    },
    {
      title: <span style={{ fontWeight: 600, color: palette.text }}>Estado documental</span>,
      dataIndex: 'estadoDocumental',
      width: 220,
      align: 'center' as const,
      render: (estado: string, t) => (
        <div style={{ opacity: t.dadoDeBaja ? 0.6 : 1 }}>
          <EstadoTag estado={estado as Estado} />
        </div>
      ),
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
                disabled: t.dadoDeBaja,
                onClick: () => navigate(generatePath(PATHS.trabajadorEditar, { id: String(t.id) })),
              },
              {
                key: 'baja',
                icon: <UserDeleteOutlined />,
                label: 'Dar de baja',
                danger: true,
                disabled: t.dadoDeBaja,
                onClick: () => setTrabajadorParaBaja(t),
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

      {/* Modal de confirmación para dar de baja (simétrico según wireframe US-27) */}
      <Modal
        open={trabajadorParaBaja !== null}
        centered
        onCancel={() => setTrabajadorParaBaja(null)}
        destroyOnHidden
        closable={false}
        width={440}
        footer={
          <Flex gap={12} style={{ width: '100%', marginTop: 20 }}>
            <Button
              block
              style={{ flex: 1 }}
              size="large"
              disabled={darDeBaja.isPending}
              onClick={() => setTrabajadorParaBaja(null)}
            >
              Cancelar
            </Button>
            <Button
              danger
              type="primary"
              block
              style={{ flex: 1 }}
              size="large"
              loading={darDeBaja.isPending}
              onClick={handleConfirmarBaja}
            >
              Dar de baja
            </Button>
          </Flex>
        }
      >
        <Typography.Title
          level={4}
          style={{ textAlign: 'center', margin: '16px 0 8px 0', fontSize: 18 }}
        >
          ¿Seguro que quiere dar de baja al trabajador {trabajadorParaBaja?.nombre} {trabajadorParaBaja?.apellido}?
        </Typography.Title>
        <Typography.Paragraph
          type="secondary"
          style={{ textAlign: 'center', margin: '0 0 16px 0', fontSize: 13 }}
        >
          Esta acción inhabilitará al trabajador para ingresar a cualquier barrio bajo esta empresa.
        </Typography.Paragraph>
      </Modal>
    </div>
  );
}
