import { useState, useEffect } from 'react';
import {
  Button,
  Card,
  DatePicker,
  Flex,
  Input,
  Modal,
  Pagination,
  Select,
  Table,
  Typography,
  notification,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { SearchOutlined, ClearOutlined, PlusOutlined } from '@ant-design/icons';
import dayjs, { type Dayjs } from 'dayjs';
import {
  getMovimientosPropios,
  getBarriosHabilitados,
  type MovimientoPropio,
  type BarrioFiltro,
} from '../api/movimientosPropios';
import { EstadoTag } from '../../../shared/components/EstadoTag';
import { palette } from '../../../app/theme';
import { useCurrentUser } from '../../auth/api/useCurrentUser';

const { RangePicker } = DatePicker;

interface FiltrosEstado {
  rangoFechas: [Dayjs, Dayjs] | null;
  dni: string;
  barrioId: number | null;
}

/**
 * US-31 (Empresa) y US-46 (Monotributista):
 * Consultar el historial de accesos propio.
 * 
 * - Empresa: ve los accesos de todos sus trabajadores, con filtros por DNI, Barrio y Fecha.
 * - Monotributista: es su propio trabajador único, por lo que no requiere filtro ni columna de DNI/Trabajador.
 */
export function HistorialPropioPage() {
  const currentUser = useCurrentUser();
  const esMonotributista = currentUser?.roles.includes('MONOTRIBUTISTA') && !currentUser?.roles.includes('EMPRESA');

  const [loading, setLoading] = useState<boolean>(false);
  const [data, setData] = useState<MovimientoPropio[]>([]);
  const [totalElements, setTotalElements] = useState<number>(0);
  const [page, setPage] = useState<number>(1);
  const [pageSize, setPageSize] = useState<number>(10);

  // Filtros en pantalla
  const [filtros, setFiltros] = useState<FiltrosEstado>({
    rangoFechas: null,
    dni: '',
    barrioId: null,
  });

  // Filtros aplicados a la consulta
  const [filtrosAplicados, setFiltrosAplicados] = useState<FiltrosEstado>({
    rangoFechas: null,
    dni: '',
    barrioId: null,
  });

  // Barrios para el dropdown
  const [barrios, setBarrios] = useState<BarrioFiltro[]>([]);

  // Modal para egreso automático
  const [modalMotivoVisible, setModalMotivoVisible] = useState(false);
  const [motivoSeleccionado, setMotivoSeleccionado] = useState<string>('');

  useEffect(() => {
    getBarriosHabilitados()
      .then((res) => setBarrios(res))
      .catch(() => {
        notification.error({
          message: 'Error al cargar barrios habilitados',
          description: 'No se pudieron recuperar los barrios donde está habilitado.',
        });
      });
  }, []);

  const cargarMovimientos = async (
    paginaActual: number,
    tamanioPagina: number,
    filtrosActuales: FiltrosEstado
  ) => {
    setLoading(true);
    try {
      const desdeStr = filtrosActuales.rangoFechas?.[0]
        ? filtrosActuales.rangoFechas[0].format('YYYY-MM-DD')
        : undefined;
      const hastaStr = filtrosActuales.rangoFechas?.[1]
        ? filtrosActuales.rangoFechas[1].format('YYYY-MM-DD')
        : undefined;

      const res = await getMovimientosPropios({
        desde: desdeStr,
        hasta: hastaStr,
        dni: esMonotributista ? undefined : (filtrosActuales.dni || undefined),
        barrioId: filtrosActuales.barrioId ?? undefined,
        page: paginaActual - 1,
        size: tamanioPagina,
      });

      setData(res.content);
      setTotalElements(res.totalElements);
    } catch {
      notification.error({
        message: 'Error al cargar historial',
        description: 'No se pudieron obtener los movimientos.',
      });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    cargarMovimientos(page, pageSize, filtrosAplicados);
  }, [page, pageSize, filtrosAplicados]);

  const handleFiltrar = () => {
    setPage(1);
    setFiltrosAplicados({ ...filtros });
  };

  const handleLimpiar = () => {
    const filtrosLimpios: FiltrosEstado = {
      rangoFechas: null,
      dni: '',
      barrioId: null,
    };
    setFiltros(filtrosLimpios);
    setFiltrosAplicados(filtrosLimpios);
    setPage(1);
  };

  const hayFiltrosActivos =
    filtrosAplicados.rangoFechas !== null ||
    (!esMonotributista && filtrosAplicados.dni.trim() !== '') ||
    filtrosAplicados.barrioId !== null;

  // Columnas base
  const columns: ColumnsType<MovimientoPropio> = [
    {
      title: <span style={{ fontWeight: 600, color: palette.text }}>Fecha y Hora</span>,
      dataIndex: 'fechaHora',
      key: 'fechaHora',
      width: 170,
      align: 'center',
      render: (val: string) => (val ? dayjs(val).format('DD/MM/YYYY HH:mm') : '—'),
    },
    {
      title: <span style={{ fontWeight: 600, color: palette.text }}>Tipo</span>,
      dataIndex: 'tipo',
      key: 'tipo',
      width: 120,
      align: 'center',
      render: (val: 'INGRESO' | 'EGRESO') => (
        <EstadoTag
          estado={val}
          style={{ minWidth: 80, height: 24, lineHeight: '22px', fontSize: 12, paddingInline: 12 }}
        />
      ),
    },
  ];

  // Si es Empresa, mostramos Trabajador y DNI
  if (!esMonotributista) {
    columns.push(
      {
        title: <span style={{ fontWeight: 600, color: palette.text }}>Trabajador</span>,
        key: 'trabajador',
        align: 'center',
        render: (_, record) => `${record.trabajadorNombre} ${record.trabajadorApellido}`,
      },
      {
        title: <span style={{ fontWeight: 600, color: palette.text }}>DNI</span>,
        dataIndex: 'trabajadorDni',
        key: 'trabajadorDni',
        width: 140,
        align: 'center',
      }
    );
  }

  // Columnas finales
  columns.push(
    {
      title: <span style={{ fontWeight: 600, color: palette.text }}>Barrio Privado</span>,
      dataIndex: 'barrioNombre',
      key: 'barrioNombre',
      align: 'center',
      ellipsis: true,
    },
    {
      title: <span style={{ fontWeight: 600, color: palette.text }}>Motivo</span>,
      key: 'motivo',
      width: 90,
      align: 'center',
      render: (_, record) => {
        if (!record.motivo) {
          return <span style={{ color: palette.textSecondary }}>—</span>;
        }
        return (
          <Button
            type="default"
            shape="circle"
            size="small"
            icon={<PlusOutlined />}
            onClick={() => {
              setMotivoSeleccionado(record.motivo || '');
              setModalMotivoVisible(true);
            }}
            title="Ver motivo del egreso automático"
          />
        );
      },
    }
  );

  const mensajeVacioInicial = esMonotributista
    ? 'Actualmente no posee Ingresos ni Egresos registrados'
    : 'Actualmente no existen Ingresos ni Egresos registrados para su Empresa';

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
      <Typography.Title
        level={1}
        style={{
          fontSize: 'clamp(26px, 2.2vw, 32px)',
          fontWeight: 700,
          color: palette.text,
          marginTop: 0,
          marginBottom: 40,
        }}
      >
        Historial de accesos
      </Typography.Title>

      {/* Barra de Filtros con estructura uniforme */}
      <Card
        size="small"
        style={{
          marginBottom: 40,
          borderRadius: 8,
          backgroundColor: palette.surface,
          borderColor: palette.borderSoft,
        }}
      >
        <Flex wrap="wrap" gap={16} align="flex-end">
          <Flex vertical gap={4}>
            <Typography.Text style={{ fontSize: 13, color: palette.textSecondary, fontWeight: 500 }}>
              Rango de fechas
            </Typography.Text>
            <RangePicker
              className="range-picker-centrado"
              placeholder={['Desde', 'Hasta']}
              format="DD/MM/YYYY"
              value={filtros.rangoFechas}
              onChange={(dates) =>
                setFiltros((prev) => ({
                  ...prev,
                  rangoFechas: dates ? [dates[0]!, dates[1]!] : null,
                }))
              }
              style={{ width: 260 }}
            />
          </Flex>

          {!esMonotributista && (
            <Flex vertical gap={4}>
              <Typography.Text style={{ fontSize: 13, color: palette.textSecondary, fontWeight: 500 }}>
                DNI
              </Typography.Text>
              <Input
                placeholder="Buscar por DNI..."
                style={{ width: 160 }}
                value={filtros.dni}
                maxLength={10}
                onChange={(e) => {
                  const val = e.target.value.replace(/\D/g, '');
                  setFiltros((prev) => ({ ...prev, dni: val }));
                }}
                onPressEnter={handleFiltrar}
              />
            </Flex>
          )}

          <Flex vertical gap={4}>
            <Typography.Text style={{ fontSize: 13, color: palette.textSecondary, fontWeight: 500 }}>
              Barrio privado
            </Typography.Text>
            <Select
              placeholder="Todos"
              style={{ width: 220 }}
              allowClear
              value={filtros.barrioId}
              onChange={(val) => setFiltros((prev) => ({ ...prev, barrioId: val ?? null }))}
              options={[
                { label: 'Todos', value: null },
                ...barrios.map((b) => ({ label: b.nombre, value: b.id })),
              ]}
            />
          </Flex>

          <Flex gap={8} style={{ marginLeft: 'auto' }}>
            {hayFiltrosActivos && (
              <Button icon={<ClearOutlined />} onClick={handleLimpiar}>
                Limpiar
              </Button>
            )}
            <Button type="primary" icon={<SearchOutlined />} onClick={handleFiltrar}>
              Filtrar
            </Button>
          </Flex>
        </Flex>
      </Card>

      {/* Contenedor de la tabla con esquinas redondeadas y borde de diseño */}
      <div
        style={{
          backgroundColor: palette.surface,
          boxShadow: '0 1px 2px 0 rgba(0, 0, 0, 0.03)',
          borderRadius: 8,
        }}
      >
        <Table<MovimientoPropio>
          bordered
          className="tabla-redondeada"
          rowKey="id"
          columns={columns}
          dataSource={data}
          loading={loading}
          scroll={{ x: esMonotributista ? 600 : 800 }}
          locale={{
            emptyText: (
              <div style={{ padding: '40px 16px', color: palette.textSecondary, fontSize: 15 }}>
                {hayFiltrosActivos
                  ? 'No se encontraron accesos que coincidan con los filtros aplicados'
                  : mensajeVacioInicial}
              </div>
            ),
          }}
          pagination={false}
        />
      </div>

      {/* Paginación desacoplada y anclada al pie */}
      <Flex justify="center" style={{ marginTop: 'auto', paddingTop: 20, paddingBottom: 8 }}>
        <Pagination
          current={page}
          pageSize={pageSize}
          total={totalElements}
          showSizeChanger={false}
          onChange={(nuevaPagina, nuevoTamanio) => {
            setPage(nuevaPagina);
            setPageSize(nuevoTamanio);
          }}
        />
      </Flex>

      <Modal
        title="Motivo del egreso automático"
        open={modalMotivoVisible}
        onCancel={() => setModalMotivoVisible(false)}
        footer={[
          <Button key="ok" type="primary" onClick={() => setModalMotivoVisible(false)}>
            Entendido
          </Button>,
        ]}
      >
        <Typography.Paragraph style={{ fontSize: 15, marginTop: 16 }}>
          {motivoSeleccionado}
        </Typography.Paragraph>
      </Modal>
    </div>
  );
}
