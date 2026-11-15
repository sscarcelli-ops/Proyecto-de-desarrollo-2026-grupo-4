import { useState } from 'react';
import { App, Button, Flex, Input, Modal, Pagination, Popconfirm, Select, Space, Typography } from 'antd';
import {
  EyeOutlined,
  FilePdfOutlined,
  SearchOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { estadoTonos, palette } from '../../../app/theme';
import { EstadoTag, type Estado } from '../../../shared/components/EstadoTag';
import {
  usePerfiles,
  useAprobarPerfil,
  useRechazarPerfil,
  verConstanciaPdf,
  type PerfilResponse,
} from '../api/perfiles';
import { getErrorMessage } from '../../../shared/lib/apiError';
import { useParametrosUrl, usePaginaValida } from '../../../shared/hooks/useParametrosUrl';
import { TablaResponsiva } from '../../../shared/components/TablaResponsiva';
import { EncabezadoPagina } from '../../../shared/components/EncabezadoPagina';

const ESTADOS: PerfilResponse['estado'][] = ['PENDIENTE', 'APROBADO', 'RECHAZADO', 'SUSPENDIDO'];

export function EmpresasPage() {
  const { message } = App.useApp();
  // Página, búsqueda y estado van en la dirección: al recargar, el listado sigue donde estaba
  const url = useParametrosUrl();
  const pagina = url.numero('pagina', 1);
  // Mismo límite que el buscador y el backend (100): un texto más largo pegado en la dirección se corta
  const busqueda = url.texto('q').slice(0, 100);
  const estadoUrl = url.texto('estado');
  const filtroEstado = ESTADOS.includes(estadoUrl as PerfilResponse['estado'])
    ? (estadoUrl as PerfilResponse['estado'])
    : undefined;
  const tamano = 10;

  // Perfil seleccionado para el modal de detalle
  const [perfilDetalle, setPerfilDetalle] = useState<PerfilResponse | null>(null);

  // Estado para el modal de rechazo (US-20)
  const [modalRechazoVisible, setModalRechazoVisible] = useState(false);
  const [motivoRechazo, setMotivoRechazo] = useState('');
  const [errorMotivo, setErrorMotivo] = useState<string | null>(null);
  const [cargandoPdf, setCargandoPdf] = useState(false);

  const { data, isLoading } = usePerfiles({
    page: pagina,
    size: tamano,
    q: busqueda,
    estado: filtroEstado,
  });

  usePaginaValida(url, pagina, data?.totalPages);
  const aprobarMutation = useAprobarPerfil();
  const rechazarMutation = useRechazarPerfil();

  const handleVerPdf = async (id: number) => {
    try {
      setCargandoPdf(true);
      await verConstanciaPdf(id);
    } catch (err) {
      message.error(getErrorMessage(err));
    } finally {
      setCargandoPdf(false);
    }
  };

  const handleAprobar = async (id: number) => {
    try {
      const actualizado = await aprobarMutation.mutateAsync(id);
      message.success('Solicitud aprobada correctamente');
      setPerfilDetalle(actualizado);
    } catch (err) {
      message.error(getErrorMessage(err));
    }
  };

  const handleConfirmarRechazo = async () => {
    if (!motivoRechazo.trim()) {
      setErrorMotivo('El motivo de rechazo es obligatorio');
      return;
    }
    if (!perfilDetalle) return;

    try {
      const actualizado = await rechazarMutation.mutateAsync({
        id: perfilDetalle.id,
        motivo: motivoRechazo.trim(),
      });
      message.success('Solicitud rechazada');
      setPerfilDetalle(actualizado);
      setModalRechazoVisible(false);
      setMotivoRechazo('');
      setErrorMotivo(null);
    } catch (err) {
      message.error(getErrorMessage(err));
    }
  };

  const columnas: ColumnsType<PerfilResponse> = [
    {
      title: <span style={{ fontWeight: 600, color: palette.text }}>Empresa/Mono.</span>,
      key: 'nombre',
      render: (_, p) => (
        <span style={{ fontWeight: 500 }}>
          {p.tipo === 'EMPRESA' ? p.razonSocial : `${p.nombre ?? ''} ${p.apellido ?? ''}`.trim()}
        </span>
      ),
    },
    {
      title: <span style={{ fontWeight: 600, color: palette.text }}>CUIT/CUIL</span>,
      dataIndex: 'cuitCuil',
      width: 170,
      align: 'center',
    },
    {
      title: <span style={{ fontWeight: 600, color: palette.text }}>Estado</span>,
      dataIndex: 'estado',
      width: 220,
      align: 'center',
      render: (estado: PerfilResponse['estado']) => <EstadoTag estado={estado as Estado} />,
    },
    {
      title: <span style={{ fontWeight: 600, color: palette.text }}>Detalle</span>,
      key: 'detalle',
      width: 100,
      align: 'center',
      render: (_, p) => (
        <Button
          shape="circle"
          icon={<EyeOutlined />}
          onClick={() => setPerfilDetalle(p)}
          aria-label="Ver detalle"
        />
      ),
    },
  ];

  const nombreDetalle = perfilDetalle
    ? perfilDetalle.tipo === 'EMPRESA'
      ? perfilDetalle.razonSocial
      : `${perfilDetalle.nombre ?? ''} ${perfilDetalle.apellido ?? ''}`.trim()
    : '';

  const hayFiltrosActivos = Boolean(busqueda.trim() || filtroEstado);
  const textoVacio = hayFiltrosActivos
    ? 'No se encontraron resultados para los filtros aplicados.'
    : 'No hay Empresas ni Monotributistas registradas aún.';

  return (
    <div style={{ width: '100%', flex: 1, display: 'flex', flexDirection: 'column' }}>
      <EncabezadoPagina
        titulo="Empresas y Monotributistas"
        filtros={
          <>
        <Input
          placeholder="Buscar Empresa o Monotributista..."
          maxLength={100}
          prefix={<SearchOutlined style={{ color: palette.textSecondary }} />}
          value={busqueda}
          onChange={(e) => url.cambiar({ q: e.target.value, pagina: null })}
          allowClear
          style={{ flex: '1 1 220px', maxWidth: 360, height: 38, borderRadius: 6 }}
        />
        <Select<string>
          placeholder="Filtrar por estado"
          value={filtroEstado ?? 'TODOS'}
          onChange={(val) => url.cambiar({ estado: val === 'TODOS' ? null : val, pagina: null })}
          style={{ width: 200, height: 38 }}
          options={[
            { label: 'Todos los estados', value: 'TODOS' },
            { label: 'Pendiente de aprobación', value: 'PENDIENTE' },
            { label: 'Aprobado', value: 'APROBADO' },
            { label: 'Rechazado', value: 'RECHAZADO' },
            { label: 'Suspendido', value: 'SUSPENDIDO' },
          ]}
        />
          </>
        }
      />

      {/* Tabla con bordes redondeados y empty state exacto del Figma */}
      <div
        style={{
          backgroundColor: palette.surface,
          boxShadow: '0 1px 2px 0 rgba(0, 0, 0, 0.03)',
          borderRadius: 8,
        }}
      >
        <TablaResponsiva<PerfilResponse>
          columnaTitulo="nombre"
          columnaAcciones="detalle"
          bordered
          className="tabla-redondeada"
          rowKey="id"
          columns={columnas}
          dataSource={data?.content ?? []}
          loading={isLoading}
          locale={{
            emptyText: (
              <div style={{ padding: '40px 16px', color: palette.textSecondary, fontSize: 15 }}>
                {textoVacio}
              </div>
            ),
          }}
          pagination={false}
        />
      </div>

      {/* Paginación */}
      <Flex justify="center" style={{ marginTop: 'auto', paddingTop: 20, paddingBottom: 8 }}>
        <Pagination
          current={pagina}
          pageSize={tamano}
          total={data?.totalElements ?? 0}
          showSizeChanger={false}
          onChange={(page) => url.cambiar({ pagina: page })}
        />
      </Flex>

      {/* Modal / Panel de Detalle (US-18 y US-20) */}
      <Modal
        open={perfilDetalle !== null}
        onCancel={() => setPerfilDetalle(null)}
        footer={null}
        width={460}
        destroyOnHidden
        styles={{ body: { padding: '24px 20px' } }}
      >
        {perfilDetalle && (
          <div>
            <Flex justify="space-between" align="flex-start" style={{ marginBottom: 12 }}>
              <div>
                <Typography.Title level={4} style={{ margin: 0, fontWeight: 700, color: palette.text }}>
                  {nombreDetalle}
                </Typography.Title>
                <Typography.Text type="secondary" style={{ fontSize: 13 }}>
                  Detalle {perfilDetalle.tipo === 'EMPRESA' ? 'Empresa' : 'Monotributista'}
                </Typography.Text>
              </div>
              <EstadoTag estado={perfilDetalle.estado as Estado} />
            </Flex>

            <div style={{ borderTop: `1px solid ${palette.border}`, margin: '14px 0' }} />

            {/* Datos fiscales y de contacto */}
            <Space orientation="vertical" size={10} style={{ width: '100%', marginBottom: 16 }}>
              {perfilDetalle.tipo === 'EMPRESA' ? (
                <>
                  <div>
                    <Typography.Text type="secondary" style={{ fontSize: 12, display: 'block' }}>
                      Razón Social
                    </Typography.Text>
                    <Typography.Text strong style={{ fontSize: 14 }}>
                      {perfilDetalle.razonSocial}
                    </Typography.Text>
                  </div>
                  <div>
                    <Typography.Text type="secondary" style={{ fontSize: 12, display: 'block' }}>
                      CUIT
                    </Typography.Text>
                    <Typography.Text style={{ fontSize: 14 }}>{perfilDetalle.cuitCuil}</Typography.Text>
                  </div>
                </>
              ) : (
                <>
                  <div>
                    <Typography.Text type="secondary" style={{ fontSize: 12, display: 'block' }}>
                      Nombre y Apellido
                    </Typography.Text>
                    <Typography.Text strong style={{ fontSize: 14 }}>
                      {`${perfilDetalle.nombre ?? ''} ${perfilDetalle.apellido ?? ''}`.trim()}
                    </Typography.Text>
                  </div>
                  <div>
                    <Typography.Text type="secondary" style={{ fontSize: 12, display: 'block' }}>
                      CUIL
                    </Typography.Text>
                    <Typography.Text style={{ fontSize: 14 }}>{perfilDetalle.cuitCuil}</Typography.Text>
                  </div>
                  <div>
                    <Typography.Text type="secondary" style={{ fontSize: 12, display: 'block' }}>
                      DNI
                    </Typography.Text>
                    <Typography.Text style={{ fontSize: 14 }}>{perfilDetalle.dniTitular ?? '-'}</Typography.Text>
                  </div>
                </>
              )}

              <div>
                <Typography.Text type="secondary" style={{ fontSize: 12, display: 'block' }}>
                  Correo electrónico
                </Typography.Text>
                <Typography.Text style={{ fontSize: 14 }}>{perfilDetalle.email ?? '-'}</Typography.Text>
              </div>

              <div>
                <Typography.Text type="secondary" style={{ fontSize: 12, display: 'block' }}>
                  Teléfono
                </Typography.Text>
                <Typography.Text style={{ fontSize: 14 }}>{perfilDetalle.telefono ?? '-'}</Typography.Text>
              </div>

              {/* Constancia de inscripción PDF */}
              <div style={{ marginTop: 6 }}>
                <Button
                  type="link"
                  icon={<FilePdfOutlined />}
                  loading={cargandoPdf}
                  onClick={() => handleVerPdf(perfilDetalle.id)}
                  style={{ padding: 0, fontWeight: 500 }}
                >
                  Constancia de inscripción
                </Button>
              </div>
            </Space>

            {/* Motivo de rechazo si aplica */}
            {perfilDetalle.estado === 'RECHAZADO' && perfilDetalle.motivoRechazo && (
              <div
                style={{
                  backgroundColor: estadoTonos.error.background,
                  border: `1px solid ${estadoTonos.error.borderColor}`,
                  borderRadius: 6,
                  padding: '12px 14px',
                  marginBottom: 16,
                }}
              >
                <Typography.Text type="danger" strong style={{ display: 'block', marginBottom: 4, fontSize: 13 }}>
                  Motivo de rechazo:
                </Typography.Text>
                <Typography.Text style={{ fontSize: 13, color: estadoTonos.error.color }}>
                  {perfilDetalle.motivoRechazo}
                </Typography.Text>
              </div>
            )}

            {/* Acciones de aprobación / rechazo (US-20) */}
            {perfilDetalle.estado === 'PENDIENTE' && (
              <Flex gap={10} style={{ marginTop: 20 }}>
                <Popconfirm
                  title="Aprobar solicitud"
                  description="¿Confirmás la aprobación de esta cuenta para operar en la plataforma?"
                  onConfirm={() => handleAprobar(perfilDetalle.id)}
                  okText="Aprobar"
                  cancelText="Cancelar"
                  okButtonProps={{ loading: aprobarMutation.isPending }}
                >
                  <Button
                    type="primary"
                    style={{
                      flex: 1,
                      backgroundColor: palette.success,
                      borderColor: palette.success,
                      fontWeight: 600,
                    }}
                  >
                    Aprobar
                  </Button>
                </Popconfirm>

                <Button
                  danger
                  style={{ flex: 1, fontWeight: 600 }}
                  onClick={() => {
                    setErrorMotivo(null);
                    setMotivoRechazo('');
                    setModalRechazoVisible(true);
                  }}
                >
                  Rechazar
                </Button>
              </Flex>
            )}
          </div>
        )}
      </Modal>

      {/* Modal para ingresar Motivo de Rechazo (US-20) */}
      <Modal
        title={`Rechazar ${perfilDetalle?.tipo === 'EMPRESA' ? 'Empresa' : 'Monotributista'}`}
        open={modalRechazoVisible}
        onCancel={() => {
          setModalRechazoVisible(false);
          setMotivoRechazo('');
          setErrorMotivo(null);
        }}
        footer={[
          <Button
            key="cancel"
            onClick={() => {
              setModalRechazoVisible(false);
              setMotivoRechazo('');
              setErrorMotivo(null);
            }}
          >
            Cancelar
          </Button>,
          <Button
            key="submit"
            type="primary"
            danger
            loading={rechazarMutation.isPending}
            onClick={handleConfirmarRechazo}
          >
            Confirmar
          </Button>,
        ]}
      >
        <Typography.Paragraph type="secondary" style={{ fontSize: 13, marginBottom: 12 }}>
          La Empresa o Monotributista no podrá operar en la plataforma.
        </Typography.Paragraph>

        <div>
          <label
            htmlFor="motivo-rechazo"
            style={{ display: 'block', fontWeight: 600, marginBottom: 6, fontSize: 13 }}
          >
            Motivo de rechazo (obligatorio)
          </label>
          <Input.TextArea
            id="motivo-rechazo"
            rows={4}
            value={motivoRechazo}
            onChange={(e) => {
              setMotivoRechazo(e.target.value);
              if (errorMotivo) setErrorMotivo(null);
            }}
            placeholder="Explicá el motivo por el cual se rechaza la solicitud..."
            maxLength={500}
            showCount
            status={errorMotivo ? 'error' : undefined}
          />
          {errorMotivo && (
            <Typography.Text type="danger" style={{ fontSize: 12, marginTop: 4, display: 'block' }}>
              {errorMotivo}
            </Typography.Text>
          )}
        </div>
      </Modal>
    </div>
  );
}
