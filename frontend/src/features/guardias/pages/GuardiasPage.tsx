import { useState } from 'react';
import { App as AntdApp, Button, Dropdown, Flex, Pagination } from 'antd';
import type { MenuProps, TableColumnsType } from 'antd';
import { MoreOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router';
import { ANCHO_COLUMNA_ESTADO, EstadoTag } from '../../../shared/components/EstadoTag';
import { getErrorMessage } from '../../../shared/lib/apiError';
import { palette } from '../../../app/theme';
import { PATHS } from '../../../routes/paths';
import { useCambiarEstadoGuardia, useGuardias, type Guardia } from '../api/guardias';
import { TablaResponsiva } from '../../../shared/components/TablaResponsiva';
import { EncabezadoPagina } from '../../../shared/components/EncabezadoPagina';
import { useParametrosUrl, usePaginaValida } from '../../../shared/hooks/useParametrosUrl';

const TAMANO_PAGINA = 10;


// US-32: Guardias del barrio del Operador, paginados y en orden alfabético (RNF-08). La página va en la dirección
// (?pagina=2): al recargar o al volver de Editar, el listado sigue donde estaba.
// Las acciones por fila van en el menú "⋯" con solo las que corresponden al estado del Guardia.
export function GuardiasPage() {
  const navigate = useNavigate();
  const { message, modal } = AntdApp.useApp();
  const url = useParametrosUrl();
  const pagina = url.numero('pagina', 1);
  const { data, error, isLoading } = useGuardias(pagina, TAMANO_PAGINA);
  // Si la página quedó vacía (se fueron Guardias) o se escribió a mano ?pagina=99, va a la última con datos
  usePaginaValida(url, pagina, data?.totalPages);
  const cambiarEstado = useCambiarEstadoGuardia();
  // Fila que se está actualizando, para mostrar la carga solo en su botón
  const [enCurso, setEnCurso] = useState<number | null>(null);

  const aplicarEstado = async (guardia: Guardia, habilitado: boolean) => {
    setEnCurso(guardia.id);
    try {
      await cambiarEstado.mutateAsync({ id: guardia.id, habilitado });
      message.success(`${guardia.nombre} ${guardia.apellido} quedó ${habilitado ? 'habilitado' : 'deshabilitado'}.`);
    } catch (err) {
      message.error(getErrorMessage(err));
    } finally {
      setEnCurso(null);
    }
  };

  // Habilitar es directo; deshabilitar pide confirmar: deja de poder iniciar sesión y operar, también con sesión abierta
  const alternar = (guardia: Guardia) => {
    if (!guardia.habilitado) {
      void aplicarEstado(guardia, true);
      return;
    }
    modal.confirm({
      title: '¿Desea deshabilitar al Guardia?',
      content: 'No podrá iniciar sesión ni operar. Podrás habilitarlo de nuevo más adelante.',
      okText: 'Deshabilitar Guardia',
      cancelText: 'Cancelar',
      okButtonProps: { danger: true },
      onOk: () => aplicarEstado(guardia, false),
    });
  };

  const menu = (guardia: Guardia): MenuProps => ({
    items: [
      { key: 'editar', label: 'Editar' },
      { key: 'estado', label: guardia.habilitado ? 'Deshabilitar' : 'Habilitar', danger: guardia.habilitado },
    ],
    onClick: ({ key }) => {
      if (key === 'editar') navigate(PATHS.guardiaEditar.replace(':id', String(guardia.id)));
      else alternar(guardia);
    },
  });

  const columnas: TableColumnsType<Guardia> = [
    {
      title: 'Nombre y Apellido',
      key: 'nombre',
      ellipsis: true,
      render: (_, g) => `${g.nombre} ${g.apellido}`,
    },
    { title: 'Email', dataIndex: 'email', ellipsis: true },
    {
      title: 'Estado',
      dataIndex: 'habilitado',
      width: ANCHO_COLUMNA_ESTADO,
      align: 'center',
      render: (habilitado: boolean) => <EstadoTag estado={habilitado ? 'HABILITADO' : 'DESHABILITADO'} />,
    },
    {
      title: 'Acciones',
      key: 'acciones',
      width: 110,
      align: 'center',
      render: (_, g) => (
        <Dropdown menu={menu(g)} trigger={['click']} disabled={enCurso !== null}>
          <Button type="text" icon={<MoreOutlined />} loading={enCurso === g.id} aria-label="Acciones" />
        </Dropdown>
      ),
    },
  ];

  return (
    <Flex vertical gap={16}>
      <EncabezadoPagina
        titulo="Guardias"
        style={{ marginBottom: 0 }}
        accion={<Button type="primary" onClick={() => navigate(PATHS.guardiaNuevo)}>Nuevo Guardia</Button>}
      />

      <div style={{ backgroundColor: palette.surface, borderRadius: 8 }}>
        <TablaResponsiva<Guardia>
          columnaTitulo="nombre"
          columnaAcciones="acciones"
          bordered
          className="tabla-redondeada"
          rowKey="id"
          columns={columnas}
          dataSource={data?.content ?? []}
          loading={isLoading}
          scroll={{ x: 640 }}
          pagination={false}
          locale={{
            // US-32 CA-9
            emptyText: (
              <div style={{ padding: '40px 16px', color: palette.textSecondary, fontSize: 15 }}>
                {error ? getErrorMessage(error) : 'Actualmente no existen Guardias creados para el barrio privado'}
              </div>
            ),
          }}
        />
      </div>

      <Flex justify="center">
        <Pagination
          current={pagina}
          pageSize={TAMANO_PAGINA}
          total={data?.totalElements ?? 0}
          showSizeChanger={false}
          hideOnSinglePage
          onChange={(page) => url.cambiar({ pagina: page })}
        />
      </Flex>
    </Flex>
  );
}
