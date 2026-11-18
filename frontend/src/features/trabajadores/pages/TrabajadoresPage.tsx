import { useState } from 'react';
import {
  App,
  Button,
  Checkbox,
  Dropdown,
  Flex,
  Input,
  Modal,
  Pagination,
  Select,
  Table,
  Tag,
  Typography,
} from 'antd';
import {
  EllipsisOutlined,
  EditOutlined,
  EyeOutlined,
  PlusCircleOutlined,
  SearchOutlined,
  UserAddOutlined,
  UserDeleteOutlined,
} from '@ant-design/icons';
import { generatePath, useNavigate } from 'react-router';
import {
  useTrabajadores,
  useDarDeBajaTrabajador,
  useReincorporarTrabajador,
  type TrabajadorResponse,
} from '../api/trabajadores';
import { EstadoTag, type Estado } from '../../../shared/components/EstadoTag';
import { PATHS } from '../../../routes/paths';
import { palette, estadoTonos } from '../../../app/theme';
import { getErrorMessage } from '../../../shared/lib/apiError';
import { useParametrosUrl } from '../../../shared/hooks/useParametrosUrl';
import { usePaginaValida } from '../../../shared/hooks/usePaginaValida';
import type { ColumnsType } from 'antd/es/table';

/**
 * US-26: listado paginado de trabajadores.
 * US-27: dar de baja trabajador.
 * US-29: filtrar y buscar nómina por estado documental, texto y bajas (RF-30).
 *
 * Muestra los trabajadores de la empresa logueada con su estado documental calculado al vuelo
 * (Vigente, Vencido, Pendiente de carga), permite buscar por nombre/apellido/DNI, filtrar por estado,
 * incluir u ocultar dados de baja, y sincroniza los parámetros en la URL (GUIA-DESARROLLO.md §5.2).
 */
export function TrabajadoresPage() {
  const { message } = App.useApp();
  const navigate = useNavigate();

  // Parámetros sincronizados en la URL
  const url = useParametrosUrl();
  const pagina = url.numero('pagina', 1);
  const tamano = 10;
  const busqueda = url.texto('q').slice(0, 100);
  const estadoRaw = url.texto('estado');
  const estado = ['VIGENTE', 'VENCIDO', 'PENDIENTE_CARGA'].includes(estadoRaw) ? estadoRaw : undefined;
  const incluirBajas = url.booleano('bajas', false);

  const { data, isLoading } = useTrabajadores({
    page: pagina,
    size: tamano,
    q: busqueda || undefined,
    estadoDocumental: estado,
    incluirBajas,
  });

  usePaginaValida(url, pagina, data?.totalPages);

  const darDeBaja = useDarDeBajaTrabajador();
  const reincorporar = useReincorporarTrabajador();

  // Modal para confirmar dar de baja (US-27)
  const [trabajadorParaBaja, setTrabajadorParaBaja] = useState<TrabajadorResponse | null>(null);

  // Modal para confirmar reincorporar (US-27 CA-5)
  const [trabajadorParaReincorporar, setTrabajadorParaReincorporar] = useState<TrabajadorResponse | null>(null);

  const hayFiltrosActivos = Boolean(busqueda.trim() || estado || incluirBajas);

  const limpiarFiltros = () => {
    url.cambiar({ q: null, estado: null, bajas: null, pagina: null });
  };

  const textoVacio = hayFiltrosActivos
    ? 'No se encontraron resultados con los filtros aplicados'
    : 'No hay trabajadores registrados. Usá el botón "Registrar trabajador" para agregar uno.';

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

  const handleConfirmarReincorporar = () => {
    if (!trabajadorParaReincorporar) return;
    reincorporar.mutate(trabajadorParaReincorporar.id, {
      onSuccess: () => {
        message.success('Trabajador reincorporado correctamente');
        setTrabajadorParaReincorporar(null);
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
        <span style={{ color: t.dadoDeBaja ? palette.textSecondary : palette.text }}>
          {t.nombre} {t.apellido}
        </span>
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
      render: (estado: string, t) =>
        t.dadoDeBaja ? (
          <Tag
            bordered
            style={{
              ...estadoTonos.neutro,
              marginInlineEnd: 0,
              minWidth: 135,
              paddingInline: 8,
              height: 28,
              lineHeight: '26px',
              textAlign: 'center',
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: 13,
              borderRadius: 4,
            }}
          >
            Dado de baja
          </Tag>
        ) : (
          <EstadoTag estado={estado as Estado} />
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
                icon: t.dadoDeBaja ? <EyeOutlined /> : <EditOutlined />,
                label: t.dadoDeBaja ? 'Ver detalle' : 'Editar',
                onClick: () => navigate(generatePath(PATHS.trabajadorEditar, { id: String(t.id) })),
              },
              t.dadoDeBaja
                ? {
                    key: 'reincorporar',
                    icon: <UserAddOutlined />,
                    label: 'Reincorporar',
                    onClick: () => setTrabajadorParaReincorporar(t),
                  }
                : {
                    key: 'baja',
                    icon: <UserDeleteOutlined />,
                    label: 'Dar de baja',
                    danger: true,
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

      {/* Barra superior de acciones y filtros (US-29, RF-30) */}
      <Flex justify="space-between" align="flex-start" wrap="wrap" gap={16} style={{ marginBottom: 20 }}>
        {/* Acción principal: registrar trabajador */}
        <Button
          type="primary"
          size="large"
          icon={<PlusCircleOutlined />}
          onClick={() => navigate(PATHS.trabajadorNuevo)}
          style={{
            borderRadius: 6,
            fontWeight: 600,
            height: 40,
          }}
        >
          Registrar trabajador
        </Button>

        {/* Bloque derecho: Buscador + Filtro de estado, y debajo el switch de bajas con el contador */}
        <Flex vertical align="flex-end" gap={8} style={{ flex: 1, minWidth: 320, maxWidth: 520 }}>
          <Flex gap={10} style={{ width: '100%' }}>
            <Input
              placeholder="Buscar trabajador..."
              maxLength={100}
              prefix={<SearchOutlined style={{ color: palette.textSecondary }} />}
              value={busqueda}
              onChange={(e) => url.cambiar({ q: e.target.value, pagina: null })}
              allowClear
              style={{ flex: 1, minWidth: 200, height: 40, borderRadius: 6 }}
            />
            <Select<string>
              placeholder="Filtrar por estado"
              value={estado ?? 'TODOS'}
              onChange={(val) => url.cambiar({ estado: val === 'TODOS' ? null : val, pagina: null })}
              style={{ width: 180, height: 40 }}
              options={[
                { label: 'Todos los estados', value: 'TODOS' },
                { label: 'Vigente', value: 'VIGENTE' },
                { label: 'Vencido', value: 'VENCIDO' },
                { label: 'Pendiente de carga', value: 'PENDIENTE_CARGA' },
              ]}
            />
          </Flex>

          <Flex
            align="center"
            gap={12}
            wrap="wrap"
            justify="flex-end"
            style={{ minHeight: 28, height: 28 }}
          >
            {hayFiltrosActivos && (
              <>
                <Typography.Text type="secondary" style={{ fontSize: 13, lineHeight: '20px' }}>
                  {data?.totalElements ?? 0}{' '}
                  {(data?.totalElements ?? 0) === 1 ? 'resultado encontrado' : 'resultados encontrados'}
                </Typography.Text>
                <Button
                  type="link"
                  size="small"
                  onClick={limpiarFiltros}
                  style={{ padding: 0, fontSize: 13, height: 'auto', lineHeight: '20px' }}
                >
                  Limpiar filtros
                </Button>
                <span style={{ color: palette.borderSoft, lineHeight: '20px' }}>|</span>
              </>
            )}
            <Checkbox
              checked={incluirBajas}
              onChange={(e) => url.cambiar({ bajas: e.target.checked ? true : null, pagina: null })}
              style={{ fontSize: 13, lineHeight: '20px' }}
            >
              Mostrar dados de baja
            </Checkbox>
          </Flex>
        </Flex>
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
            emptyText: (
              <div style={{ padding: '40px 16px', textAlign: 'center' }}>
                <Typography.Paragraph type="secondary" style={{ margin: 0, fontSize: 15 }}>
                  {textoVacio}
                </Typography.Paragraph>
                {hayFiltrosActivos && (
                  <Button type="link" onClick={limpiarFiltros} style={{ marginTop: 8 }}>
                    Limpiar filtros
                  </Button>
                )}
              </div>
            ),
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
          onChange={(page) => {
            url.cambiar({ pagina: page === 1 ? null : page });
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

      {/* Modal de confirmación para reincorporar (US-27 CA-5) */}
      <Modal
        open={trabajadorParaReincorporar !== null}
        centered
        onCancel={() => setTrabajadorParaReincorporar(null)}
        destroyOnHidden
        closable={false}
        width={440}
        footer={
          <Flex gap={12} style={{ width: '100%', marginTop: 20 }}>
            <Button
              block
              style={{ flex: 1 }}
              size="large"
              disabled={reincorporar.isPending}
              onClick={() => setTrabajadorParaReincorporar(null)}
            >
              Cancelar
            </Button>
            <Button
              type="primary"
              block
              style={{ flex: 1 }}
              size="large"
              loading={reincorporar.isPending}
              onClick={handleConfirmarReincorporar}
            >
              Reincorporar
            </Button>
          </Flex>
        }
      >
        <Typography.Title
          level={4}
          style={{ textAlign: 'center', margin: '16px 0 8px 0', fontSize: 18 }}
        >
          ¿Seguro que quiere reincorporar al trabajador {trabajadorParaReincorporar?.nombre} {trabajadorParaReincorporar?.apellido}?
        </Typography.Title>
        <Typography.Paragraph
          type="secondary"
          style={{ textAlign: 'center', margin: '0 0 16px 0', fontSize: 13 }}
        >
          Esta acción habilitará nuevamente al trabajador para ingresar a los barrios según su documentación legal.
        </Typography.Paragraph>
      </Modal>
    </div>
  );
}
