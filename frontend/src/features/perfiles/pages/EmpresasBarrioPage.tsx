import { useState } from 'react';
import { App, Button, Divider, Flex, Pagination, Radio, Select, Typography } from 'antd';
import type { TableColumnsType } from 'antd';
import { EstadoTag } from '../../../shared/components/EstadoTag';
import { getErrorMessage } from '../../../shared/lib/apiError';
import { useMiBarrio } from '../../barrios/api/barrios';
import {
  useCambiarHabilitacion,
  usePerfilesDelBarrio,
  type CondicionBarrio,
  type PerfilBarrio,
} from '../api/habilitaciones';
import { palette } from '../../../app/theme';
import { TablaResponsiva } from '../../../shared/components/TablaResponsiva';
import { EncabezadoPagina } from '../../../shared/components/EncabezadoPagina';
import { useParametrosUrl, usePaginaValida } from '../../../shared/hooks/useParametrosUrl';

const TIPO_LABEL: Record<PerfilBarrio['tipo'], string> = {
  EMPRESA: 'Empresa',
  MONOTRIBUTISTA: 'Monotributista',
};

const TIPOS: PerfilBarrio['tipo'][] = ['EMPRESA', 'MONOTRIBUTISTA'];
const CONDICIONES: CondicionBarrio[] = ['HABILITADO', 'DESHABILITADO'];

/**
 * US-33: Empresas y Monotributistas aprobados por el Administrador y su estado en el barrio del Operador.
 * US-34: habilitar o deshabilitar cada uno en el barrio desde la columna "Acciones".
 * US-35: filtros por condición en el barrio y por tipo de entidad, combinables (RF-29).
 */
export function EmpresasBarrioPage() {
  const { message } = App.useApp();
  // La página y los filtros van en la dirección (?pagina=2&condicion=HABILITADO): al recargar, siguen iguales
  const url = useParametrosUrl();
  const pagina = url.numero('pagina', 1);
  const tamano = 10;
  // Un valor escrito a mano que no está en la lista se toma como "Todos"
  const tipoUrl = url.texto('tipo');
  const tipo = TIPOS.includes(tipoUrl as PerfilBarrio['tipo']) ? (tipoUrl as PerfilBarrio['tipo']) : undefined;
  const condicionUrl = url.texto('condicion');
  const condicion = CONDICIONES.includes(condicionUrl as CondicionBarrio)
    ? (condicionUrl as CondicionBarrio)
    : undefined;
  const { data: barrio } = useMiBarrio();
  const { data, isLoading, error } = usePerfilesDelBarrio(pagina, tamano, { tipo, condicion });

  // Si la página actual quedó vacía porque se fueron perfiles (por ejemplo, uno suspendido por el Administrador),
  // (o porque se escribió a mano ?pagina=99), vuelve a la última página con datos en lugar de mostrar el mensaje
  // de "no existen"
  usePaginaValida(url, pagina, data?.totalPages);
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

  const hayFiltrosActivos = Boolean(tipo || condicion);
  const textoVacio = hayFiltrosActivos
    ? 'No se encontraron resultados con los filtros aplicados' // US-35 CA-2
    : 'Actualmente no existen Empresas ni Monotributistas aprobados para mostrar';

  return (
    <div style={{ width: '100%', flex: 1, display: 'flex', flexDirection: 'column' }}>
      <Typography.Title level={4} style={{ margin: 0 }}>
        {barrio?.nombre ?? ' '}
      </Typography.Title>
      <Divider style={{ marginTop: 12, marginBottom: 20 }} />

      <EncabezadoPagina
        titulo="Empresas y Monotributistas"
        descripcion="Empresas y monotributistas aprobados por el Administrador y su estado en tu barrio"
        filtros={
          <>
            <Select<string>
              aria-label="Filtrar por estado en el barrio"
              value={condicion ?? 'TODOS'}
              onChange={(val) => url.cambiar({ condicion: val === 'TODOS' ? null : val, pagina: null })}
              style={{ width: 200, height: 38 }}
              options={[
                { label: 'Todos los estados', value: 'TODOS' },
                { label: 'Habilitados', value: 'HABILITADO' },
                { label: 'Deshabilitados', value: 'DESHABILITADO' },
              ]}
            />
            {/* Tipo de entidad con opciones a la vista, como en el wireframe de US-35 */}
            <Flex vertical gap={2}>
              <Typography.Text id="filtro-tipo" type="secondary" style={{ fontSize: 12 }}>
                Tipo de Entidad
              </Typography.Text>
              <Radio.Group
                aria-labelledby="filtro-tipo"
                value={tipo ?? 'TODOS'}
                onChange={(e) => url.cambiar({ tipo: e.target.value === 'TODOS' ? null : e.target.value, pagina: null })}
                options={[
                  { label: 'Todos', value: 'TODOS' },
                  { label: 'Empresa', value: 'EMPRESA' },
                  { label: 'Monotributista', value: 'MONOTRIBUTISTA' },
                ]}
              />
            </Flex>
          </>
        }
      />

      <div style={{ backgroundColor: palette.surface, boxShadow: '0 1px 2px 0 rgba(0, 0, 0, 0.03)', borderRadius: 8 }}>
        <TablaResponsiva<PerfilBarrio>
          columnaTitulo="nombre"
          columnaAcciones="acciones"
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
                {error ? getErrorMessage(error) : textoVacio}
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
          onChange={(page) => url.cambiar({ pagina: page })}
        />
      </Flex>
    </div>
  );
}
