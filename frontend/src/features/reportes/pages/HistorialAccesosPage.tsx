import { useState } from 'react';
import { Button, Card, DatePicker, Flex, Input, Modal, Pagination, Select, Typography } from 'antd';
import type { TableColumnsType } from 'antd';
import { PlusOutlined, SearchOutlined, ClearOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import type { Dayjs } from 'dayjs';
import { ANCHO_COLUMNA_ESTADO, EstadoTag } from '../../../shared/components/EstadoTag';
import { getErrorMessage } from '../../../shared/lib/apiError';
import { useMiBarrio } from '../../barrios/api/barrios';
import { useEntidadesFiltro, useHistorialAccesos, type FiltrosHistorial, type MovimientoHistorial } from '../api/movimientos';
import { palette } from '../../../app/theme';
import { TablaResponsiva } from '../../../shared/components/TablaResponsiva';
import { EncabezadoPagina } from '../../../shared/components/EncabezadoPagina';
import { useParametrosUrl, usePaginaValida } from '../../../shared/hooks/useParametrosUrl';

const { RangePicker } = DatePicker;

// Fecha AAAA-MM-DD de la dirección; cualquier otra cosa se toma como "sin filtro"
const fechaValida = (valor: string) =>
  /^\d{4}-\d{2}-\d{2}$/.test(valor) && dayjs(valor).format('YYYY-MM-DD') === valor ? valor : undefined;

/**
 * US-36: Consultar el historial de accesos del barrio (Operador).
 * Filtros por rango de fechas, DNI y Empresa/Monotributista.
 * Paginado y orden cronológico descendente (del más reciente al más antiguo).
 * Adaptado a responsive (T-07) con EncabezadoPagina, TablaResponsiva y filtros en la URL.
 */
export function HistorialAccesosPage() {
  const url = useParametrosUrl();
  const pagina = url.numero('pagina', 1);
  const tamano = 10;
  const { data: barrio } = useMiBarrio();

  // Filtros desde la URL para persistencia al recargar o volver. Un valor escrito a mano que no es válido
  // (fecha mal formada, DNI con letras, entidad que no es un número) se ignora, como en los demás listados.
  const desdeUrl = fechaValida(url.texto('desde'));
  const hastaUrl = fechaValida(url.texto('hasta'));
  const dniUrl = /^\d{1,10}$/.test(url.texto('dni')) ? url.texto('dni') : undefined;
  const perfilIdNumero = url.numero('perfilId', 0);
  const perfilIdUrl = perfilIdNumero > 0 ? perfilIdNumero : undefined;

  // Estados locales para los inputs antes de hacer clic en "Filtrar"
  const [rangoFechas, setRangoFechas] = useState<[Dayjs | null, Dayjs | null] | null>(
    desdeUrl && hastaUrl ? [dayjs(desdeUrl), dayjs(hastaUrl)] : null
  );
  const [dniInput, setDniInput] = useState(dniUrl ?? '');
  const [perfilIdInput, setPerfilIdInput] = useState<number | undefined>(perfilIdUrl);

  const filtrosAplicados: FiltrosHistorial = {
    desde: desdeUrl,
    hasta: hastaUrl,
    dni: dniUrl,
    perfilId: perfilIdUrl,
  };

  // Modal para ver motivo de egreso automático (+)
  const [motivoModal, setMotivoModal] = useState<string | null>(null);

  const { data: entidades } = useEntidadesFiltro();
  const { data, isLoading, error } = useHistorialAccesos(pagina, tamano, filtrosAplicados);

  usePaginaValida(url, pagina, data?.totalPages);

  const hayFiltrosActivos = Boolean(desdeUrl || hastaUrl || dniUrl || perfilIdUrl);

  const onFiltrar = () => {
    url.cambiar({
      desde: rangoFechas?.[0] ? rangoFechas[0].format('YYYY-MM-DD') : null,
      hasta: rangoFechas?.[1] ? rangoFechas[1].format('YYYY-MM-DD') : null,
      dni: dniInput.trim() ? dniInput.trim() : null,
      perfilId: perfilIdInput ?? null,
      pagina: null, // vuelve a la página 1
    });
  };

  const onLimpiar = () => {
    setRangoFechas(null);
    setDniInput('');
    setPerfilIdInput(undefined);
    url.cambiar({
      desde: null,
      hasta: null,
      dni: null,
      perfilId: null,
      pagina: null,
    });
  };

  const columnas: TableColumnsType<MovimientoHistorial> = [
    {
      title: 'Fecha y Hora',
      dataIndex: 'fechaHora',
      width: 170,
      align: 'center',
      // En una sola línea: fecha y hora se leen juntas
      render: (fecha: string) => <span style={{ whiteSpace: 'nowrap' }}>{dayjs(fecha).format('DD/MM/YYYY HH:mm')}</span>,
    },
    {
      title: 'Tipo',
      dataIndex: 'tipo',
      // El mismo tag que el resto de los listados (mismo tamaño en todas las tablas)
      width: ANCHO_COLUMNA_ESTADO,
      align: 'center',
      render: (tipo: string) => <EstadoTag estado={tipo as 'INGRESO' | 'EGRESO'} />,
    },
    {
      title: 'Trabajador',
      key: 'trabajador',
      render: (_, m) => `${m.trabajadorNombre} ${m.trabajadorApellido}`,
    },
    {
      title: 'DNI',
      dataIndex: 'trabajadorDni',
      width: 110,
      align: 'center',
    },
    {
      // Nombre de la columna como en la US
      title: 'Empresa/Monotributista',
      dataIndex: 'entidadNombre',
    },
    {
      title: 'Motivo',
      key: 'motivo',
      width: 80,
      align: 'center',
      render: (_, m) => {
        // Solo los egresos automáticos tienen motivo; el resto queda vacío (en el celular no muestra nada suelto)
        if (!m.motivo) return null;
        return (
          <Button
            size="small"
            shape="circle"
            icon={<PlusOutlined />}
            onClick={() => setMotivoModal(m.motivo || '')}
            title="Ver motivo del egreso automático"
            aria-label="Ver motivo del egreso automático"
          />
        );
      },
    },
  ];

  return (
    // Mismo ancho que los demás listados (sin un máximo propio)
    <div style={{ width: '100%', flex: 1, display: 'flex', flexDirection: 'column' }}>
      <EncabezadoPagina
        titulo={barrio?.nombre ? `Historial de accesos - ${barrio.nombre}` : 'Historial de accesos'}
        descripcion="Registro cronológico de ingresos y egresos de trabajadores en el barrio"
        filtros={
          <Card
            size="small"
            style={{ width: '100%', borderRadius: 8, backgroundColor: palette.surface, borderColor: palette.borderSoft }}
          >
            <Flex wrap="wrap" gap={16} align="flex-end">
              <Flex vertical gap={4}>
                <Typography.Text style={{ fontSize: 13, color: palette.textSecondary, fontWeight: 500 }}>
                  Rango de fechas
                </Typography.Text>
                <RangePicker
                  value={rangoFechas}
                  onChange={(fechas) => setRangoFechas(fechas as [Dayjs | null, Dayjs | null])}
                  format="DD/MM/YYYY"
                  placeholder={['Desde', 'Hasta']}
                  style={{ width: 260 }}
                />
              </Flex>

              <Flex vertical gap={4}>
                <Typography.Text style={{ fontSize: 13, color: palette.textSecondary, fontWeight: 500 }}>
                  DNI
                </Typography.Text>
                <Input
                  placeholder="Buscar por DNI..."
                  value={dniInput}
                  onChange={(e) => setDniInput(e.target.value.replace(/\D/g, ''))}
                  onPressEnter={onFiltrar}
                  maxLength={10}
                  style={{ width: 160 }}
                />
              </Flex>

              <Flex vertical gap={4}>
                <Typography.Text style={{ fontSize: 13, color: palette.textSecondary, fontWeight: 500 }}>
                  Empresa / Monotributista
                </Typography.Text>
                {/* "Todos" es una opción más, como en los otros filtros (no un valor vacío) */}
                <Select<number | 'TODOS'>
                  value={perfilIdInput ?? 'TODOS'}
                  onChange={(val) => setPerfilIdInput(val === 'TODOS' ? undefined : val)}
                  style={{ minWidth: 220 }}
                  options={[
                    { value: 'TODOS', label: 'Todos' },
                    ...(entidades?.map((e) => ({
                      value: e.id,
                      label: `${e.nombre} (${e.tipo === 'EMPRESA' ? 'Empresa' : 'Monotributista'})`,
                    })) ?? []),
                  ]}
                />
              </Flex>

              <Flex gap={8} style={{ marginLeft: 'auto' }}>
                {hayFiltrosActivos && (
                  <Button icon={<ClearOutlined />} onClick={onLimpiar}>
                    Limpiar
                  </Button>
                )}
                <Button type="primary" icon={<SearchOutlined />} onClick={onFiltrar}>
                  Filtrar
                </Button>
              </Flex>
            </Flex>
          </Card>
        }
      />

      {/* Tabla de Movimientos Responsiva */}
      <div style={{ backgroundColor: palette.surface, boxShadow: '0 1px 2px 0 rgba(0, 0, 0, 0.03)', borderRadius: 8 }}>
        <TablaResponsiva<MovimientoHistorial>
          columnaTitulo="trabajador"
          columnaAcciones="motivo"
          bordered
          className="tabla-redondeada"
          rowKey="id"
          columns={columnas}
          dataSource={data?.content ?? []}
          loading={isLoading}
          scroll={{ x: 800 }}
          locale={{
            emptyText: (
              <div style={{ padding: '40px 16px', color: palette.textSecondary, fontSize: 15 }}>
                {error
                  ? getErrorMessage(error)
                  : hayFiltrosActivos
                    ? 'No se encontraron resultados con los filtros aplicados' // US-36 CA-3, igual que los otros listados
                    : 'Actualmente no existen Ingresos ni Egresos registrados al Barrio Privado'}
              </div>
            ),
          }}
          pagination={false}
        />
      </div>

      <Flex justify="center" style={{ marginTop: 'auto', paddingTop: 20, paddingBottom: 8 }}>
        <Pagination
          current={pagina}
          pageSize={tamano}
          total={data?.totalElements ?? 0}
          showSizeChanger={false}
          hideOnSinglePage
          onChange={(p) => url.cambiar({ pagina: p })}
        />
      </Flex>

      {/* Modal para ver el motivo de egreso automático */}
      <Modal
        title="Motivo del egreso automático"
        open={Boolean(motivoModal)}
        onCancel={() => setMotivoModal(null)}
        footer={[
          <Button key="cerrar" type="primary" onClick={() => setMotivoModal(null)}>
            Entendido
          </Button>,
        ]}
      >
        <Typography.Paragraph style={{ fontSize: 15, marginTop: 16 }}>
          {motivoModal}
        </Typography.Paragraph>
      </Modal>
    </div>
  );
}
