import { useEffect, useRef, useState } from 'react';
import {
  Alert,
  App,
  Button,
  Checkbox,
  Dropdown,
  Flex,
  Input,
  Modal,
  Pagination,
  Select,
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
import type { ColumnsType } from 'antd/es/table';
import { TablaResponsiva } from '../../../shared/components/TablaResponsiva';
import { EncabezadoPagina } from '../../../shared/components/EncabezadoPagina';
import { useParametrosUrl, usePaginaValida } from '../../../shared/hooks/useParametrosUrl';

/**
 * US-26: listado paginado de trabajadores.
 * US-27: dar de baja y reincorporar trabajador.
 * US-29: filtrar y buscar la nómina de trabajadores (RF-30).
 *
 * Muestra los trabajadores de la empresa logueada con su estado documental calculado al vuelo
 * (Vigente, Vencido, Pendiente de carga) y permite buscar, filtrar y dar de baja / reincorporar.
 */
export function TrabajadoresPage() {
  const { message } = App.useApp();
  const navigate = useNavigate();

  // Parámetros sincronizados en la URL (GUIA-DESARROLLO.md §5.2)
  const url = useParametrosUrl();
  const pagina = url.numero('pagina', 1);
  const tamano = 10;
  const busqueda = url.texto('q').slice(0, 100);
  const estadoRaw = url.texto('estado');
  const estado = ['VIGENTE', 'VENCIDO', 'PENDIENTE_CARGA'].includes(estadoRaw) ? estadoRaw : undefined;
  const incluirBajas = url.booleano('bajas', false);

  // Buscador con debounce de 300 ms (GUIA-DESARROLLO.md §5.2)
  const [textoBusqueda, setTextoBusqueda] = useState(busqueda);
  const [prevBusqueda, setPrevBusqueda] = useState(busqueda);
  const esperaBusqueda = useRef<number | undefined>(undefined);

  if (busqueda !== prevBusqueda) {
    setPrevBusqueda(busqueda);
    setTextoBusqueda(busqueda);
  }

  useEffect(() => () => window.clearTimeout(esperaBusqueda.current), []);

  const { data, isLoading, isError, error } = useTrabajadores({
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
    setTextoBusqueda('');
    window.clearTimeout(esperaBusqueda.current);
    url.cambiar({ q: null, estado: null, bajas: null, pagina: null });
  };

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
      render: (est: string, t) =>
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
          <EstadoTag estado={est as Estado} />
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
            items: t.dadoDeBaja
              ? [
                  {
                    key: 'detalle',
                    label: 'Ver detalle',
                    icon: <EyeOutlined />,
                    onClick: () => navigate(generatePath(PATHS.trabajadorEditar, { id: String(t.id) })),
                  },
                  {
                    key: 'reincorporar',
                    label: 'Reincorporar',
                    icon: <UserAddOutlined />,
                    onClick: () => setTrabajadorParaReincorporar(t),
                  },
                ]
              : [
                  {
                    key: 'editar',
                    label: 'Editar',
                    icon: <EditOutlined />,
                    onClick: () => navigate(generatePath(PATHS.trabajadorEditar, { id: String(t.id) })),
                  },
                  {
                    key: 'baja',
                    label: 'Dar de baja',
                    icon: <UserDeleteOutlined />,
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
    <div style={{ width: '100%', flex: 1, display: 'flex', flexDirection: 'column' }}>
      <EncabezadoPagina
        titulo="Trabajadores"
        accion={
          <Button
            type="primary"
            icon={<PlusCircleOutlined />}
            onClick={() => navigate(PATHS.trabajadorNuevo)}
            style={{ borderRadius: 6, fontWeight: 600 }}
          >
            Registrar trabajador
          </Button>
        }
        filtros={
          <Flex vertical gap={10} style={{ width: '100%' }}>
            <Flex gap={12} align="center" wrap style={{ width: '100%' }}>
              <Input
                placeholder="Buscar trabajador..."
                maxLength={100}
                prefix={<SearchOutlined style={{ color: palette.textSecondary }} />}
                value={textoBusqueda}
                allowClear
                style={{ flex: '1 1 220px', maxWidth: 360 }}
                onChange={(e) => {
                  const valor = e.target.value;
                  setTextoBusqueda(valor);
                  window.clearTimeout(esperaBusqueda.current);
                  esperaBusqueda.current = window.setTimeout(() => {
                    url.cambiar({ q: valor.trim() || null, pagina: null });
                  }, 300);
                }}
                onPressEnter={() => {
                  window.clearTimeout(esperaBusqueda.current);
                  if (textoBusqueda.trim() !== busqueda.trim()) {
                    url.cambiar({ q: textoBusqueda.trim() || null, pagina: null });
                  }
                }}
              />
              <Select<string>
                placeholder="Filtrar por estado"
                value={estado ?? 'TODOS'}
                onChange={(val) => url.cambiar({ estado: val === 'TODOS' ? null : val, pagina: null })}
                style={{ minWidth: 170 }}
                options={[
                  { label: 'Todos los estados', value: 'TODOS' },
                  { label: 'Vigente', value: 'VIGENTE' },
                  { label: 'Vencido', value: 'VENCIDO' },
                  { label: 'Pendiente de carga', value: 'PENDIENTE_CARGA' },
                ]}
              />
              <Checkbox
                checked={incluirBajas}
                onChange={(e) => url.cambiar({ bajas: e.target.checked ? true : null, pagina: null })}
              >
                Mostrar dados de baja
              </Checkbox>
            </Flex>

            {hayFiltrosActivos && (
              <Flex align="center" gap={8} wrap>
                <Typography.Text type="secondary" style={{ fontSize: 13 }}>
                  {data?.totalElements ?? 0}{' '}
                  {(data?.totalElements ?? 0) === 1 ? 'resultado encontrado' : 'resultados encontrados'}
                </Typography.Text>
                <span style={{ color: palette.borderSoft }}>•</span>
                <Button
                  type="link"
                  size="small"
                  onClick={limpiarFiltros}
                  style={{ padding: 0, fontSize: 13 }}
                >
                  Limpiar filtros
                </Button>
              </Flex>
            )}
          </Flex>
        }
      />

      {/* Aviso de error si la petición al backend falla */}
      {isError && (
        <Alert
          type="error"
          showIcon
          title="No se pudieron cargar los trabajadores"
          description={getErrorMessage(error)}
          style={{ marginBottom: 16, borderRadius: 8 }}
        />
      )}

      {/* Tabla con bordes redondeados y paginación desacoplada (T-07) */}
      <div
        style={{
          backgroundColor: palette.surface,
          boxShadow: '0 1px 2px 0 rgba(0, 0, 0, 0.03)',
          borderRadius: 8,
        }}
      >
        <TablaResponsiva<TrabajadorResponse>
          columnaTitulo="nombre"
          columnaAcciones="acciones"
          bordered
          className="tabla-redondeada"
          rowKey="id"
          columns={columnas}
          dataSource={data?.content ?? []}
          loading={isLoading}
          locale={{
            emptyText: isError
              ? 'No se pudo cargar la nómina de trabajadores debido a un error del servidor.'
              : hayFiltrosActivos
                ? 'No se encontraron trabajadores que coincidan con los criterios de búsqueda'
                : 'No hay trabajadores registrados. Usá el botón "Registrar trabajador" para agregar uno.',
          }}
          pagination={false}
          scroll={{ x: 750 }}
        />
      </div>

      {/* Paginación anclada al fondo gracias al flex del contenedor */}
      <Flex justify="center" style={{ marginTop: 'auto', paddingTop: 20, paddingBottom: 8 }}>
        <Pagination
          current={pagina}
          pageSize={tamano}
          total={data?.totalElements ?? 0}
          showSizeChanger={false}
          onChange={(page) => url.cambiar({ pagina: page === 1 ? null : page })}
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
