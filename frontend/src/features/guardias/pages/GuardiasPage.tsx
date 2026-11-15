import { useState } from 'react';
import { App as AntdApp, Button, Dropdown, Flex, Table, Typography } from 'antd';
import type { MenuProps, TableColumnsType } from 'antd';
import { MoreOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router';
import { EstadoTag } from '../../../shared/components/EstadoTag';
import { getErrorMessage } from '../../../shared/lib/apiError';
import { palette } from '../../../app/theme';
import { PATHS } from '../../../routes/paths';
import { useCambiarEstadoGuardia, useGuardias, type Guardia } from '../api/guardias';

const { Title } = Typography;

// US-32: Guardias del barrio del Operador. Sin paginar (bajo volumen por barrio, RNF-08).
// Las acciones por fila van en el menú "⋯" con solo las que corresponden al estado del Guardia.
export function GuardiasPage() {
  const navigate = useNavigate();
  const { message, modal } = AntdApp.useApp();
  const { data, error, isLoading } = useGuardias();
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
      width: 150,
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
      <Flex justify="space-between" align="center" wrap gap={8}>
        <Title level={3} style={{ margin: 0 }}>Guardias</Title>
        <Button type="primary" onClick={() => navigate(PATHS.guardiaNuevo)}>Nuevo Guardia</Button>
      </Flex>

      <div style={{ backgroundColor: palette.surface, borderRadius: 8 }}>
        <Table<Guardia>
          bordered
          className="tabla-redondeada"
          rowKey="id"
          columns={columnas}
          dataSource={data ?? []}
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
    </Flex>
  );
}
