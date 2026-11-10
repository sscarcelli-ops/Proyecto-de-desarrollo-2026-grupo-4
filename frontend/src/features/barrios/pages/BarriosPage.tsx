import { useState } from 'react';
import { Alert, Button, Flex, Select, Switch, Table, Tooltip, Typography, App as AntdApp } from 'antd';
import type { TableColumnsType } from 'antd';
import { EditOutlined, PlusOutlined, UserSwitchOutlined } from '@ant-design/icons';
import { generatePath, useNavigate } from 'react-router';
import { useBarrios, useCambiarEstadoBarrio, type Barrio } from '../api/barrios';
import { AsignarOperadorSelect } from '../components/AsignarOperadorSelect';
import { getErrorMessage } from '../../../shared/lib/apiError';
import { textoUbicacion } from '../../ubicaciones/api/ubicaciones';
import { PATHS } from '../../../routes/paths';
import { palette } from '../../../app/theme';

const { Title, Text } = Typography;

// Ancho mínimo de la tabla: por debajo se desplaza de costado en lugar de apretar las columnas. Las columnas de
// contenido corto (Op. Asignado, Estado, Acción) tienen ancho fijo; las de texto se reparten el resto, así en una
// pantalla ancha el espacio va a los textos y no queda aire entre columnas cortas.
const ANCHO_MINIMO_TABLA = 950;

// Selector de estado del listado (RF-34)
type FiltroBarrios = 'TODOS' | 'ACTIVO' | 'INACTIVO' | 'SIN_OPERADOR';

const OPCIONES_FILTRO: { value: FiltroBarrios; label: string }[] = [
  { value: 'TODOS', label: 'Todos' },
  { value: 'ACTIVO', label: 'Activos' },
  { value: 'INACTIVO', label: 'Inactivos' },
  { value: 'SIN_OPERADOR', label: 'Sin Operador' },
];

function cumpleFiltro(barrio: Barrio, filtro: FiltroBarrios): boolean {
  switch (filtro) {
    case 'ACTIVO': return barrio.estado === 'ACTIVO';
    case 'INACTIVO': return barrio.estado === 'INACTIVO';
    case 'SIN_OPERADOR': return barrio.operadorId === null;
    default: return true;
  }
}

// US-10: listado de barrios privados (Administrador). Sin paginación, orden alfabético desde el backend.
// US-45: filtro "Sin Operador" y asignación del Operador desde la columna "Op. Asignado".
//
// Cómo quedan las acciones (propuesta del grupo, el wireframe está desactualizado en esta pantalla):
// - "Op. Asignado": sin Operador, el desplegable para asignarlo (US-45); con Operador, su nombre y el ícono
//   "Reemplazar Operador" (US-14).
// - "Estado": interruptor con el texto adentro, "Activo" (verde) / "Inactivo" (gris) (US-12). Que el barrio no tiene
//   Operador ya lo muestra "Op. Asignado", así que no se repite acá.
// - "Acción": el lápiz "Editar barrio" (US-11). Si se suman más acciones por barrio, se vuelve a un menú "⋯".
export function BarriosPage() {
  const navigate = useNavigate();
  const { message, modal } = AntdApp.useApp();
  const { data, error, isLoading } = useBarrios();
  const [filtro, setFiltro] = useState<FiltroBarrios>('TODOS');
  const cambiarEstadoBarrio = useCambiarEstadoBarrio();
  // Barrio al que se le está cambiando el estado: su interruptor muestra que está cargando
  const [cambiandoId, setCambiandoId] = useState<number | null>(null);

  const barrios = data?.filter((barrio) => cumpleFiltro(barrio, filtro));

  // US-12: el backend valida (activar sin Operador, por ejemplo) y devuelve el mensaje que se muestra
  const cambiarEstado = async (barrio: Barrio, activo: boolean) => {
    setCambiandoId(barrio.id);
    try {
      await cambiarEstadoBarrio.mutateAsync({ id: barrio.id, activo });
      message.success(`${barrio.nombre} quedó ${activo ? 'Activo' : 'Inactivo'}.`);
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setCambiandoId(null);
    }
  };

  // Activar es directo; desactivar pide confirmar, porque cierra los ingresos abiertos de sus trabajadores
  const alTocarElInterruptor = (barrio: Barrio, activar: boolean) => {
    if (activar) {
      void cambiarEstado(barrio, true);
      return;
    }
    modal.confirm({
      title: '¿Desea desactivar el barrio?',
      content: 'Podrá reactivarlo más adelante.',
      okText: 'Desactivar barrio',
      cancelText: 'Cancelar',
      okButtonProps: { danger: true },
      onOk: () => cambiarEstado(barrio, false),
    });
  };

  // Los textos largos se cortan con "…" y se ven completos al pasar el mouse
  const columns: TableColumnsType<Barrio> = [
    { title: 'Barrio', dataIndex: 'nombre', key: 'nombre', ellipsis: true },
    {
      // T-11: título del wireframe ("Localidad") y la ubicación completa, "Provincia, Localidad". Un barrio anterior que no coincidió con el catálogo se completa en Editar.
      title: 'Localidad',
      key: 'ubicacion',
      ellipsis: true,
      render: (_, barrio) =>
        textoUbicacion(barrio.provincia, barrio.localidad) ?? <Text type="secondary">Sin cargar</Text>,
    },
    { title: 'Dirección', dataIndex: 'direccion', key: 'direccion', ellipsis: true },
    {
      // Con Operador: su nombre. Sin Operador: el desplegable para asignarlo (US-45)
      title: 'Op. Asignado',
      key: 'operador',
      // El desplegable mide 190 + el margen de la celda
      width: 230,
      // Sin ellipsis en la columna: cortaría también el desplegable y le agregaría "…". Solo se corta el nombre.
      render: (_, barrio) => {
        if (barrio.operadorId === null) {
          return <AsignarOperadorSelect barrio={barrio} />;
        }
        const nombre = `${barrio.operadorNombre} ${barrio.operadorApellido}`;
        return (
          // El nombre a la izquierda y el botón a la derecha, con borde: se lee como acción y no como adorno del nombre
          <Flex align="center" justify="space-between" gap={8}>
            <Text ellipsis={{ tooltip: nombre }}>{nombre}</Text>
            {/* TODO US-14: abrir el mismo SelectorOperador de US-45 (con el Operador actual arriba) y, al elegir,
                confirmar "¿Reemplazar a [actual] por [nuevo] como Operador de [barrio]? [actual] quedará disponible y
                se cerrará su sesión." Por ahora solo avisa que no está disponible. */}
            <Tooltip title="Reemplazar Operador">
              <Button
                size="small"
                icon={<UserSwitchOutlined />}
                aria-label="Reemplazar Operador"
                onClick={() => message.info('Reemplazar el Operador todavía no está disponible (US-14).')}
              />
            </Tooltip>
          </Flex>
        );
      },
    },
    {
      // US-10, criterio 3: un barrio recién creado figura Inactivo; que no tiene Operador lo dice "Op. Asignado"
      // con el desplegable "Asignar un Operador" (no se repite "– sin Operador" acá)
      title: 'Estado',
      key: 'estado',
      width: 130,
      // Centrado, igual que Acción: los controles de largo fijo se leen mejor al medio de su columna
      align: 'center',
      render: (_, barrio) => {
        const activo = barrio.estado === 'ACTIVO';
        return (
          <>
            <Switch
              checked={activo}
              checkedChildren="Activo"
              unCheckedChildren="Inactivo"
              aria-label={activo ? 'Desactivar barrio' : 'Activar barrio'}
              // Verde activo, gris inactivo (no rojo: un barrio inactivo no tiene un error). El gris es el del texto
              // secundario para que "Inactivo" en blanco se lea bien
              style={{ backgroundColor: activo ? palette.success : palette.textSecondary }}
              loading={cambiandoId === barrio.id}
              onChange={(checked) => alTocarElInterruptor(barrio, checked)}
            />
          </>
        );
      },
    },
    {
      // US-11: editar los datos del barrio. Una sola acción por barrio: el lápiz directo, sin menú "⋯"
      title: 'Acción',
      key: 'accion',
      width: 80,
      align: 'center',
      render: (_, barrio) => (
        <Tooltip title="Editar barrio">
          <Button
            size="small"
            icon={<EditOutlined />}
            aria-label="Editar barrio"
            onClick={() => navigate(generatePath(PATHS.barrioEditar, { id: String(barrio.id) }))}
          />
        </Tooltip>
      ),
    },
  ];

  return (
    <Flex vertical gap={16}>
      <Title level={3} style={{ margin: 0 }}>Barrios privados</Title>

      <Flex justify="space-between" align="center" wrap gap={12}>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate(PATHS.barrioNuevo)}>
          Nuevo barrio
        </Button>
        <Flex align="center" gap={8}>
          <Text>Estado:</Text>
          <Select<FiltroBarrios> value={filtro} onChange={setFiltro} options={OPCIONES_FILTRO} style={{ minWidth: 160 }} />
        </Flex>
      </Flex>

      {error && <Alert type="error" showIcon title="No se pudieron cargar los barrios" description={getErrorMessage(error)} />}

      <Table<Barrio>
        rowKey="id"
        columns={columns}
        dataSource={barrios}
        loading={isLoading}
        pagination={false}
        // Distribución fija: con 'max-content' la tabla crece con el texto y los "…" no llegan a aparecer.
        // En pantallas angostas (menos de ANCHO_MINIMO_TABLA) la tabla se desplaza horizontalmente.
        tableLayout="fixed"
        scroll={{ x: ANCHO_MINIMO_TABLA }}
        locale={{ emptyText: filtro === 'TODOS' ? 'Todavía no hay barrios cargados' : 'No hay barrios con ese estado' }}
      />
    </Flex>
  );
}
