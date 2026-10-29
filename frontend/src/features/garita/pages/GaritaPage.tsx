import { useState } from 'react';
import {
  Alert,
  Avatar,
  Button,
  Card,
  ConfigProvider,
  Flex,
  Form,
  Input,
  Radio,
  Spin,
  Tag,
  Tooltip,
  Typography,
} from 'antd';
import {
  ExclamationCircleOutlined,
  KeyOutlined,
  LogoutOutlined,
  SearchOutlined,
  UserOutlined,
  CheckCircleOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router';
import { garitaTheme, palette, estadoTonos } from '../../../app/theme';
import {
  useBuscarPorDni,
  useVerificarAcceso,
  useRegistrarEgreso,
  type BusquedaDniResponse,
  type VinculoResultado,
} from '../api/garitaApi';
import { getErrorMessage } from '../../../shared/lib/apiError';
import { useCurrentUser } from '../../auth/api/useCurrentUser';
import { useLogout } from '../../auth/api/session';
import { PATHS } from '../../../routes/paths';

const { Title, Text, Paragraph } = Typography;

type PasoGarita = 'BUSQUEDA' | 'SELECCION_EMPRESA' | 'HABILITADO' | 'DENEGADO';

/* ========================================================================= */
/* COMPONENTES REUTILIZABLES DE GARITA                                       */
/* ========================================================================= */

/** Barra superior fija del guardia (misma paleta azul institucional del menú general) */
function GaritaHeader({
  nombreGuardia,
  onLogout,
}: {
  nombreGuardia: string;
  onLogout: () => void;
}) {
  return (
    <header
      style={{
        width: '100%',
        backgroundColor: palette.sidebar,
        borderBottom: '1px solid rgba(255, 255, 255, 0.1)',
        padding: '12px 16px',
        display: 'flex',
        justifyContent: 'center',
        boxShadow: '0 2px 4px rgba(0,0,0,0.12)',
        position: 'sticky',
        top: 0,
        zIndex: 100,
      }}
    >
      <div
        style={{
          width: '100%',
          maxWidth: 420,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
        }}
      >
        <Avatar
          size={36}
          style={{
            backgroundColor: palette.sidebarHover,
            border: '1px solid rgba(255, 255, 255, 0.2)',
            flexShrink: 0,
          }}
        />

        <div
          style={{
            width: 1,
            height: 24,
            backgroundColor: 'rgba(255, 255, 255, 0.25)',
            margin: '0 12px',
          }}
        />

        <Text
          strong
          style={{
            fontSize: 15,
            color: palette.sidebarTextActive,
            flex: 1,
            textAlign: 'center',
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            whiteSpace: 'nowrap',
          }}
        >
          Guardia: {nombreGuardia}
        </Text>

        <div
          style={{
            width: 1,
            height: 24,
            backgroundColor: 'rgba(255, 255, 255, 0.25)',
            margin: '0 12px',
          }}
        />

        <Tooltip title="Cerrar sesión">
          <Button
            type="text"
            icon={<LogoutOutlined style={{ fontSize: 20, color: palette.sidebarTextActive }} />}
            onClick={onLogout}
            aria-label="Cerrar sesión"
            style={{ padding: 4 }}
          />
        </Tooltip>
      </div>
    </header>
  );
}

/** Título estándar en dos líneas de las tarjetas de Garita */
function GaritaTitulo({ marginBottom = 28 }: { marginBottom?: number }) {
  return (
    <>
      <Title
        level={2}
        style={{
          textAlign: 'center',
          margin: 0,
          fontWeight: 700,
          fontSize: 24,
          color: palette.text,
        }}
      >
        Control de acceso
      </Title>
      <Title
        level={3}
        style={{
          textAlign: 'center',
          margin: `6px 0 ${marginBottom}px`,
          fontWeight: 700,
          fontSize: 20,
          color: palette.text,
        }}
      >
        Ingreso / Egreso
      </Title>
    </>
  );
}

/** Ficha con Avatar, Nombre, DNI, Empresa y aviso si no tiene foto */
function FichaTrabajador({
  trabajador,
  nombreVinculo,
  marginBottom = 24,
}: {
  trabajador: BusquedaDniResponse;
  nombreVinculo?: string;
  marginBottom?: number;
}) {
  return (
    <Flex gap={16} align="center" style={{ marginBottom }}>
      <Avatar
        size={60}
        icon={<UserOutlined />}
        style={{ backgroundColor: palette.primary, flexShrink: 0 }}
      />
      <Flex vertical>
        <Title level={4} style={{ margin: 0, fontWeight: 700, fontSize: 17 }}>
          {trabajador.nombre} {trabajador.apellido}
        </Title>
        <Text style={{ fontSize: 15, color: palette.textSecondary }}>
          DNI: {trabajador.dni}
        </Text>
        {nombreVinculo && (
          <Text strong style={{ fontSize: 14, color: palette.textSecondary, marginTop: 2 }}>
            {nombreVinculo}
          </Text>
        )}
        {!trabajador.tieneFoto && (
          <Tag
            color="warning"
            icon={<ExclamationCircleOutlined />}
            style={{ marginTop: 4, width: 'fit-content', fontSize: 12 }}
          >
            Sin foto: solicitar DNI físico
          </Tag>
        )}
      </Flex>
    </Flex>
  );
}

/** Tarjeta blanca contenedora principal */
function GaritaCard({
  children,
  padding = '36px 24px',
}: {
  children: React.ReactNode;
  padding?: string;
}) {
  return (
    <Card
      bordered
      style={{
        borderRadius: 8,
        borderColor: palette.border,
        backgroundColor: palette.surface,
      }}
      styles={{ body: { padding } }}
    >
      {children}
    </Card>
  );
}

/** Botón inferior separado para cambiar contraseña */
function BotonCambiarPassword({ onClick }: { onClick: () => void }) {
  return (
    <div
      role="button"
      tabIndex={0}
      onClick={onClick}
      onKeyDown={(e) => e.key === 'Enter' && onClick()}
      style={{
        backgroundColor: palette.surface,
        border: `1px solid ${palette.border}`,
        borderRadius: 8,
        padding: '14px 20px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 12,
        cursor: 'pointer',
        transition: 'background-color 0.2s',
      }}
    >
      <KeyOutlined style={{ fontSize: 20, color: palette.text }} />
      <Text strong style={{ fontSize: 15, color: palette.text }}>
        Cambiar Contraseña
      </Text>
    </div>
  );
}

/* ========================================================================= */
/* PÁGINA PRINCIPAL DE GARITA (US-37 y US-38)                                 */
/* ========================================================================= */
export function GaritaPage() {
  const navigate = useNavigate();
  const user = useCurrentUser();
  const logout = useLogout();

  // Estados del flujo
  const [paso, setPaso] = useState<PasoGarita>('BUSQUEDA');
  const [trabajador, setTrabajador] = useState<BusquedaDniResponse | null>(null);
  const [vinculoSeleccionado, setVinculoSeleccionado] = useState<VinculoResultado | null>(null);
  const [errorBusqueda, setErrorBusqueda] = useState<string | null>(null);
  const [motivoDenegado, setMotivoDenegado] = useState<string>('');

  const buscarMutation = useBuscarPorDni();
  const verificarMutation = useVerificarAcceso();
  const egresoMutation = useRegistrarEgreso();

  // Detecta si el motivo de denegación es un ingreso activo (para ofrecer registrar egreso)
  const esIngresoActivo = motivoDenegado.includes('ya tiene un ingreso activo');

  const handleLogout = () => {
    logout();
    navigate(PATHS.login, { replace: true });
  };

  // ── US-37: Búsqueda por DNI ────────────────────────────────────────────────
  const onBuscar = (values: { dni: string }) => {
    const limpio = values.dni?.trim();
    if (!limpio) return;

    setErrorBusqueda(null);
    setMotivoDenegado('');

    buscarMutation.mutate(limpio, {
      onSuccess: (data) => {
        setTrabajador(data);

        // Si tiene más de una empresa/vínculo -> Pantalla de selección
        if (data.vinculosBusqueda.length > 1) {
          setVinculoSeleccionado(data.vinculosBusqueda[0]);
          setPaso('SELECCION_EMPRESA');
        } else if (data.vinculosBusqueda.length === 1) {
          // Si tiene una sola empresa -> Verificar directo (US-38)
          const unico = data.vinculosBusqueda[0];
          setVinculoSeleccionado(unico);
          ejecutarVerificacion(unico);
        } else {
          setErrorBusqueda('No se encontró un trabajador con este DNI');
        }
      },
      onError: (err) => {
        setTrabajador(null);
        setErrorBusqueda(getErrorMessage(err));
      },
    });
  };

  // ── US-38: Verificación de Acceso ──────────────────────────────────────────
  const ejecutarVerificacion = (vinculo: VinculoResultado) => {
    if (!vinculo.trabajadorId) return;

    verificarMutation.mutate(vinculo.trabajadorId, {
      onSuccess: (res) => {
        if (res.habilitado) {
          setPaso('HABILITADO');
        } else {
          setMotivoDenegado(res.motivo || 'No habilitado para ingresar');
          setPaso('DENEGADO');
        }
      },
      onError: (err) => {
        setMotivoDenegado(getErrorMessage(err));
        setPaso('DENEGADO');
      },
    });
  };

  const volverABusqueda = () => {
    setPaso('BUSQUEDA');
    setTrabajador(null);
    setVinculoSeleccionado(null);
    setErrorBusqueda(null);
    setMotivoDenegado('');
  };

  const volverASeleccionOABusqueda = () => {
    if (trabajador && trabajador.vinculosBusqueda.length > 1) {
      setPaso('SELECCION_EMPRESA');
    } else {
      volverABusqueda();
    }
  };

  return (
    <ConfigProvider theme={garitaTheme} componentSize="large">
      <div
        style={{
          minHeight: '100vh',
          backgroundColor: palette.background,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
        }}
      >
        {/* Barra superior fija */}
        <GaritaHeader
          nombreGuardia={user?.nombre ?? user?.email ?? 'Guardia'}
          onLogout={handleLogout}
        />

        {/* Contenedor central (ancho móvil 420px centrado) */}
        <div
          style={{
            width: '100%',
            maxWidth: 420,
            padding: '60px 16px 40px',
          }}
        >
          {/* ========================================================================= */}
          {/* PANTALLA 1: BÚSQUEDA POR DNI (Wireframe Screen 2)                         */}
          {/* ========================================================================= */}
          {paso === 'BUSQUEDA' && (
            <Flex vertical gap={16}>
              <GaritaCard padding="40px 24px">
                <GaritaTitulo marginBottom={36} />

                {errorBusqueda && (
                  <Alert
                    type="error"
                    showIcon
                    message="No se encontró un trabajador con este DNI"
                    style={{
                      marginBottom: 28,
                      borderRadius: 6,
                    }}
                  />
                )}

                <Form
                  layout="vertical"
                  onFinish={onBuscar}
                  requiredMark={false}
                  onValuesChange={() => setErrorBusqueda(null)}
                >
                  <Form.Item
                    name="dni"
                    rules={[
                      { required: true, message: 'Ingrese el número de DNI' },
                      { pattern: /^[0-9]+$/, message: 'Solo números, sin puntos' },
                    ]}
                    style={{ marginBottom: 60 }}
                  >
                    <Input
                      placeholder="Ingrese número de DNI..."
                      allowClear
                      autoFocus
                      inputMode="numeric"
                      style={{
                        height: 50,
                        fontSize: 16,
                        borderColor: palette.border,
                        borderRadius: 6,
                        marginTop: 10,
                        marginBottom: 10,
                      }}
                    />
                  </Form.Item>

                  <Flex justify="center" style={{ marginTop: 8 }}>
                    <Button
                      type="primary"
                      htmlType="submit"
                      loading={buscarMutation.isPending}
                      icon={<SearchOutlined />}
                      style={{
                        height: 48,
                        paddingInline: 40,
                        fontSize: 16,
                        fontWeight: 600,
                        borderRadius: 6,
                      }}
                    >
                      Buscar Trabajador
                    </Button>
                  </Flex>
                </Form>
              </GaritaCard>

              <BotonCambiarPassword onClick={() => navigate(PATHS.recuperarPassword)} />
            </Flex>
          )}

          {/* Spinner durante la verificación */}
          {verificarMutation.isPending && (
            <Flex justify="center" align="center" style={{ padding: 48 }}>
              <Spin size="large" tip="Verificando habilitación..." />
            </Flex>
          )}

          {/* ========================================================================= */}
          {/* PANTALLA 2: ELEGIR EMPRESA (Wireframe Screen 3)                           */}
          {/* ========================================================================= */}
          {!verificarMutation.isPending && paso === 'SELECCION_EMPRESA' && trabajador && (
            <GaritaCard>
              <GaritaTitulo />

              <FichaTrabajador trabajador={trabajador} marginBottom={20} />

              <Paragraph style={{ fontSize: 14, color: palette.textSecondary, marginBottom: 18, marginTop: 25 }}>
                Pertenece a más de una empresa habilitada.
                <br />
                <Text strong style={{ color: palette.text, fontSize: 15 }}>
                  ¿Bajo qué empresa ingresa hoy?
                </Text>
              </Paragraph>

              <Radio.Group
                value={vinculoSeleccionado?.trabajadorId}
                onChange={(e) => {
                  const sel = trabajador.vinculosBusqueda.find(
                    (v) => v.trabajadorId === e.target.value,
                  );
                  if (sel) setVinculoSeleccionado(sel);
                }}
                style={{ width: '100%', marginBottom: 28 }}
              >
                <Flex vertical gap={10}>
                  {trabajador.vinculosBusqueda.map((v) => (
                    <Card
                      key={v.trabajadorId}
                      size="small"
                      hoverable
                      onClick={() => setVinculoSeleccionado(v)}
                      style={{
                        cursor: 'pointer',
                        borderRadius: 6,
                        borderColor:
                          vinculoSeleccionado?.trabajadorId === v.trabajadorId
                            ? palette.primary
                            : palette.borderSoft,
                      }}
                      styles={{ body: { padding: '10px 14px' } }}
                    >
                      <Radio value={v.trabajadorId} style={{ width: '100%' }}>
                        <Text strong style={{ fontSize: 15, color: palette.text }}>
                          {v.nombreVinculo}
                        </Text>
                      </Radio>
                    </Card>
                  ))}
                </Flex>
              </Radio.Group>

              <Flex gap={12}>
                <Button
                  type="primary"
                  onClick={() => vinculoSeleccionado && ejecutarVerificacion(vinculoSeleccionado)}
                  style={{ flex: 2, height: 46, fontSize: 15, fontWeight: 600 }}
                >
                  Continuar con el ingreso
                </Button>
                <Button
                  onClick={volverABusqueda}
                  style={{ flex: 1, height: 46, fontSize: 15 }}
                >
                  Cancelar
                </Button>
              </Flex>
            </GaritaCard>
          )}

          {/* ========================================================================= */}
          {/* PANTALLA 3: ACCESO HABILITADO (Wireframe Screen 7)                         */}
          {/* ========================================================================= */}
          {!verificarMutation.isPending && paso === 'HABILITADO' && trabajador && (
            <GaritaCard>
              <GaritaTitulo />

              <FichaTrabajador
                trabajador={trabajador}
                nombreVinculo={vinculoSeleccionado?.nombreVinculo}
                marginBottom={28}
              />

              <div
                style={{
                  backgroundColor: palette.success,
                  color: palette.surface,
                  textAlign: 'center',
                  padding: '16px 12px',
                  borderRadius: 6,
                  fontWeight: 700,
                  fontSize: 18,
                  letterSpacing: '0.5px',
                  marginBottom: 60,
                  marginTop: 40,
                  boxShadow: '0 2px 4px rgba(30,123,74,0.2)',
                }}
              >
                ACCESO HABILITADO
              </div>

              <Flex justify="center">
                <Button
                  type="primary"
                  onClick={volverABusqueda}
                  style={{ height: 46, paddingInline: 36, fontSize: 15, fontWeight: 600 }}
                >
                  Nueva búsqueda
                </Button>
              </Flex>
            </GaritaCard>
          )}

          {/* ========================================================================= */}
          {/* PANTALLA 4: ACCESO DENEGADO (Wireframe Screen 5)                          */}
          {/* ========================================================================= */}
          {!verificarMutation.isPending && paso === 'DENEGADO' && trabajador && (
            <GaritaCard>
              <GaritaTitulo />

              <FichaTrabajador
                trabajador={trabajador}
                nombreVinculo={vinculoSeleccionado?.nombreVinculo}
                marginBottom={24}
              />

              <div
                style={{
                  backgroundColor: palette.error,
                  color: palette.surface,
                  textAlign: 'center',
                  padding: '16px 12px',
                  borderRadius: 6,
                  fontWeight: 700,
                  fontSize: 18,
                  letterSpacing: '0.5px',
                  marginBottom: 16,
                  boxShadow: '0 2px 4px rgba(179,38,30,0.2)',
                }}
              >
                ACCESO DENEGADO
              </div>

              {!esIngresoActivo && (
                <div style={{ textAlign: 'center', marginBottom: 28 }}>
                  <Text strong style={{ fontSize: 15, color: palette.text }}>
                    Motivo: {motivoDenegado}
                  </Text>
                </div>
              )}

              {esIngresoActivo && trabajador?.ingresoActivo && (
                <div
                  style={{
                    backgroundColor: estadoTonos.aviso.background,
                    border: `1px solid ${estadoTonos.aviso.borderColor}`,
                    borderRadius: 6,
                    padding: '12px 16px',
                    marginBottom: 24,
                    textAlign: 'center',
                  }}
                >
                  <Text style={{ fontSize: 14, color: estadoTonos.aviso.color }}>
                    Ingresó bajo <Text strong>{trabajador.ingresoActivo.nombreVinculo}</Text>.
                    Registre su egreso para habilitarlo.
                  </Text>
                </div>
              )}

              <Flex gap={12}>
                <Button
                  type="primary"
                  onClick={volverABusqueda}
                  style={{ flex: 1, height: 46, fontSize: 15, fontWeight: 600 }}
                >
                  Nueva búsqueda
                </Button>

                {esIngresoActivo && trabajador?.ingresoActivo ? (
                  <Button
                    loading={egresoMutation.isPending}
                    icon={<CheckCircleOutlined />}
                    onClick={() => {
                      if (!trabajador?.ingresoActivo?.trabajadorId) return;
                      egresoMutation.mutate(trabajador.ingresoActivo!.trabajadorId, {
                        onSuccess: () => volverABusqueda(),
                      });
                    }}
                    style={{
                      flex: 1,
                      height: 46,
                      fontSize: 15,
                      fontWeight: 600,
                      backgroundColor: palette.warning,
                      borderColor: palette.warning,
                      color: palette.surface,
                    }}
                  >
                    Registrar Egreso
                  </Button>
                ) : (
                  <Button
                    onClick={volverASeleccionOABusqueda}
                    style={{ flex: 1, height: 46, fontSize: 15 }}
                  >
                    Volver
                  </Button>
                )}
              </Flex>
            </GaritaCard>
          )}
        </div>
      </div>
    </ConfigProvider>
  );
}
