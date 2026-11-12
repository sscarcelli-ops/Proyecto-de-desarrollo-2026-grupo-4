import { useState } from 'react';
import { App, Button, Divider, Flex, Pagination, Table, Typography } from 'antd';
import type { TableColumnsType } from 'antd';
import { EstadoTag } from '../../../shared/components/EstadoTag';
import { getErrorMessage } from '../../../shared/lib/apiError';
import { useMiBarrio } from '../../barrios/api/barrios';
import { useCambiarHabilitacion, usePerfilesDelBarrio, type PerfilBarrio } from '../api/habilitaciones';
import { palette } from '../../../app/theme';

const TIPO_LABEL: Record<PerfilBarrio['tipo'], string> = {
  EMPRESA: 'Empresa',
  MONOTRIBUTISTA: 'Monotributista',
};

/**
 * US-33: Empresas y Monotributistas aprobados por el Administrador y su estado en el barrio del Operador.
 * US-34: habilitar o deshabilitar cada uno en el barrio desde la columna "Acciones".
 * Los filtros de Estado y Tipo de Entidad del wireframe son US-35 y no forman parte de esta etapa.
 */
export function EmpresasBarrioPage() {
  const { message } = App.useApp();
  const [pagina, setPagina] = useState(1);
  const tamano = 10;
  const { data: barrio } = useMiBarrio();
  const { data, isLoading, error } = usePerfilesDelBarrio(pagina, tamano);

  // Si la página actual quedó vacía porque se fueron perfiles (por ejemplo, uno suspendido por el Administrador),
  // vuelve a la última página con datos en lugar de mostrar el mensaje de "no existen"
  const ultimaPagina = Math.max(1, data?.totalPages ?? 1);
  if (data && pagina > ultimaPagina) {
    setPagina(ultimaPagina);
  }
  const cambiarHabilitacion = useCambiarHabilitacion();
  // Fila que se está actualizando, para mostrar la carga solo en su botón
  const [enCurso, setEnCurso] = useState<number | null>(null);

  const alternar = async (perfil: PerfilBarrio) => {
    const habilitar = !perfil.habilitado;
    setEnCurso(perfil.id);
    try {
      await cambiarHabilitacion.mutateAsync({ id: perfil.id, habilitar });
      message.success(`Se ${habilitar ? 'habilitó' : 'deshabilitó'} a ${perfil.nombre} en el barrio.`);
    } catch (err) {
      message.error(getErrorMessage(err));
    } finally {
      setEnCurso(null);
    }
  };

  const columnas: TableColumnsType<PerfilBarrio> = [
    { title: 'Tipo de Entidad', dataIndex: 'tipo', width: 170, render: (tipo: PerfilBarrio['tipo']) => TIPO_LABEL[tipo] },
    { title: 'Nombre', dataIndex: 'nombre', ellipsis: true },
    { title: 'CUIT/CUIL', dataIndex: 'cuitCuil', width: 160, align: 'center' },
    {
      title: 'Estado en el Barrio',
      dataIndex: 'habilitado',
      width: 190,
      align: 'center',
      render: (habilitado: boolean) => <EstadoTag estado={habilitado ? 'HABILITADO' : 'DESHABILITADO'} />,
    },
    {
      // Sin baja ni suspensión: son exclusivas del Administrador (RF-16, US-33 CA-3)
      title: 'Acciones',
      key: 'acciones',
      width: 160,
      align: 'center',
      render: (_, perfil) => (
        <Button
          onClick={() => alternar(perfil)}
          loading={enCurso === perfil.id}
          disabled={enCurso !== null && enCurso !== perfil.id}
          style={{ minWidth: 120 }}
        >
          {perfil.habilitado ? 'Deshabilitar' : 'Habilitar'}
        </Button>
      ),
    },
  ];

  return (
    <div style={{ maxWidth: 1080, margin: '0 auto', width: '100%', flex: 1, display: 'flex', flexDirection: 'column' }}>
      <Typography.Title level={4} style={{ margin: 0 }}>
        {barrio?.nombre ?? ' '}
      </Typography.Title>
      <Divider style={{ marginTop: 12, marginBottom: 20 }} />

      <Typography.Title
        level={1}
        style={{ fontSize: 'clamp(26px, 2.2vw, 32px)', fontWeight: 700, color: palette.text, marginTop: 0, marginBottom: 4 }}
      >
        Empresas y Monotributistas
      </Typography.Title>
      <Typography.Paragraph type="secondary" style={{ marginBottom: 20 }}>
        Empresas y monotributistas aprobados por el Administrador y su estado en tu barrio
      </Typography.Paragraph>

      <div style={{ backgroundColor: palette.surface, boxShadow: '0 1px 2px 0 rgba(0, 0, 0, 0.03)', borderRadius: 8 }}>
        <Table<PerfilBarrio>
          bordered
          className="tabla-redondeada"
          rowKey="id"
          columns={columnas}
          dataSource={data?.content ?? []}
          loading={isLoading}
          scroll={{ x: 800 }}
          locale={{
            // US-33 CA-4
            emptyText: (
              <div style={{ padding: '40px 16px', color: palette.textSecondary, fontSize: 15 }}>
                {error ? getErrorMessage(error) : 'Actualmente no existen Empresas ni Monotributistas aprobados para mostrar'}
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
          onChange={setPagina}
        />
      </Flex>
    </div>
  );
}
