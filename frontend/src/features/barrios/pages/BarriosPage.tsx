import { useState } from 'react';
import { Alert, Button, Flex, Pagination, Select, Switch, Tooltip, Typography, App as AntdApp } from 'antd';
import type { TableColumnsType } from 'antd';
import { EditOutlined, PlusOutlined } from '@ant-design/icons';
import { generatePath, useNavigate } from 'react-router';
import { useBarrios, useCambiarEstadoBarrio, type Barrio, type FiltroBarrio } from '../api/barrios';
import { AsignarOperadorSelect } from '../components/AsignarOperadorSelect';
import { ReemplazarOperadorBoton } from '../components/ReemplazarOperadorBoton';
import { getErrorMessage } from '../../../shared/lib/apiError';
import { textoUbicacion } from '../../ubicaciones/api/ubicaciones';
import { PATHS } from '../../../routes/paths';
import { palette } from '../../../app/theme';
import { TablaResponsiva } from '../../../shared/components/TablaResponsiva';
import { usePaginaValida, useParametrosUrl } from '../../../shared/hooks/useParametrosUrl';
import { EncabezadoPagina } from '../../../shared/components/EncabezadoPagina';

const { Text } = Typography;

// Ancho mínimo de la tabla: por debajo se desplaza de costado en lugar de apretar las columnas. Las columnas de
// contenido corto (Op. Asignado, Estado, Acción) tienen ancho fijo; las de texto se reparten el resto, así en una
// pantalla ancha el espacio va a los textos y no queda aire entre columnas cortas.
const ANCHO_MINIMO_TABLA = 950;

// Selector de estado del listado (RF-34). Filtra el backend: con el paginado, el navegador solo tiene una página.
type FiltroBarrios = 'TODOS' | FiltroBarrio;

const OPCIONES_FILTRO: { value: FiltroBarrios; label: string }[] = [
  { value: 'TODOS', label: 'Todos' },
  { value: 'ACTIVO', label: 'Activos' },
  { value: 'INACTIVO', label: 'Inactivos' },
  { value: 'SIN_OPERADOR', label: 'Sin Operador' },
];

const TAMANO_PAGINA = 10;

// US-10: listado de barrios privados (Administrador), paginado (RNF-08) y en orden alfabético desde el backend.
// La página y el filtro van en la dirección (?pagina=2&filtro=ACTIVO): al recargar o al volver de Editar, siguen.
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
  const url = useParametrosUrl();
  const pagina = url.numero('pagina', 1);
  // Solo valores conocidos: un ?filtro=cualquiera se toma como "Todos"
  const filtroUrl = url.texto('filtro');
  const filtro: FiltroBarrios = OPCIONES_FILTRO.some((o) => o.value === filtroUrl) ? (filtroUrl as FiltroBarrios) : 'TODOS';
  const { data, error, isLoading } = useBarrios(pagina, TAMANO_PAGINA, filtro === 'TODOS' ? undefined : filtro);
  // Una página inexistente (?pagina=99), o que quedó vacía, va a la última con datos
  usePaginaValida(url, pagina, data?.totalPages);
  const cambiarEstadoBarrio = useCambiarEstadoBarrio();
  // Barrio al que se le está cambiando el estado: su interruptor muestra que está cargando
  const [cambiandoId, setCambiandoId] = useState<number | null>(null);

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
            <ReemplazarOperadorBoton barrio={barrio} />
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
      <EncabezadoPagina
        titulo="Barrios privados"
        style={{ marginBottom: 0 }}
        accion={
          <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate(PATHS.barrioNuevo)}>
            Nuevo barrio
          </Button>
        }
        filtros={
          <Flex align="center" gap={8}>
            <Text>Estado:</Text>
            {/* Cambiar el filtro vuelve a la página 1 */}
            <Select<FiltroBarrios>
              value={filtro}
              onChange={(valor) => url.cambiar({ filtro: valor === 'TODOS' ? null : valor, pagina: null })}
              options={OPCIONES_FILTRO}
              style={{ minWidth: 160 }}
            />
          </Flex>
        }
      />

      {error && <Alert type="error" showIcon title="No se pudieron cargar los barrios" description={getErrorMessage(error)} />}

      <TablaResponsiva<Barrio>
          columnaTitulo="nombre"
          columnaAcciones="accion"
        rowKey="id"
        columns={columns}
        dataSource={data?.content ?? []}
        loading={isLoading}
        pagination={false}
        // Distribución fija: con 'max-content' la tabla crece con el texto y los "…" no llegan a aparecer.
        // En pantallas angostas (menos de ANCHO_MINIMO_TABLA) la tabla se desplaza horizontalmente.
        tableLayout="fixed"
        scroll={{ x: ANCHO_MINIMO_TABLA }}
        locale={{ emptyText: filtro === 'TODOS' ? 'Todavía no hay barrios cargados' : 'No hay barrios con ese estado' }}
      />

      <Flex justify="center">
        <Pagination
          current={pagina}
          pageSize={TAMANO_PAGINA}
          total={data?.totalElements ?? 0}
          showSizeChanger={false}
          hideOnSinglePage
          onChange={(p) => url.cambiar({ pagina: p })}
        />
      </Flex>
    </Flex>
  );
}
