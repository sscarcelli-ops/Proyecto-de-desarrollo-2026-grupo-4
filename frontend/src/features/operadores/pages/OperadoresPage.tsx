import { useEffect, useRef, useState } from 'react';
import { Button, Flex, Input, Pagination, Select } from 'antd';
import type { TableColumnsType } from 'antd';
import { SearchOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router';
import { palette } from '../../../app/theme';
import { ANCHO_COLUMNA_ESTADO, EstadoTag } from '../../../shared/components/EstadoTag';
import { EncabezadoPagina } from '../../../shared/components/EncabezadoPagina';
import { TablaResponsiva } from '../../../shared/components/TablaResponsiva';
import { usePaginaValida, useParametrosUrl } from '../../../shared/hooks/useParametrosUrl';
import { getErrorMessage } from '../../../shared/lib/apiError';
import { PATHS } from '../../../routes/paths';
import { useOperadores, type EstadoOperador, type OperadorListado } from '../api/operadores';

const TAMANO_PAGINA = 10;

const ESTADOS: EstadoOperador[] = ['DISPONIBLE', 'OCUPADO'];

const OPCIONES_ESTADO: { value: EstadoOperador | 'TODOS'; label: string }[] = [
  { value: 'TODOS', label: 'Todos los estados' },
  { value: 'DISPONIBLE', label: 'Disponibles' },
  { value: 'OCUPADO', label: 'Ocupados' },
];

// US-15: Operadores del Administrador, con el barrio a cargo. Paginado y en orden alfabético (RNF-08).
// Página, búsqueda y estado van en la dirección (?pagina=2&q=olga&estado=OCUPADO): al recargar el listado sigue igual.
// El buscador busca solo, 300 ms después de la última tecla (o al instante con Enter), igual que los demás listados;
// el filtro por estado cambia al instante.
// Las acciones por fila (editar datos y cambiar contraseña, US-16 y US-17) se suman al menú "⋯" cuando existan.
export function OperadoresPage() {
  const navigate = useNavigate();
  const url = useParametrosUrl();
  const pagina = url.numero('pagina', 1);
  // Mismo límite que el buscador y el backend (100): un texto más largo pegado en la dirección se corta
  const busqueda = url.texto('q').slice(0, 100);
  const estadoUrl = url.texto('estado');
  // Solo valores conocidos: un ?estado=cualquiera se ignora
  const estado = ESTADOS.includes(estadoUrl as EstadoOperador) ? (estadoUrl as EstadoOperador) : undefined;

  const [texto, setTexto] = useState(busqueda); // lo que se escribe
  const espera = useRef<number | undefined>(undefined);
  // Al salir de la pantalla se cancela la búsqueda pendiente
  useEffect(() => () => window.clearTimeout(espera.current), []);

  const { data, error, isLoading } = useOperadores({ page: pagina, size: TAMANO_PAGINA, q: busqueda, estado });
  // Una página inexistente (?pagina=99) va a la última con datos
  usePaginaValida(url, pagina, data?.totalPages);

  const hayFiltros = busqueda !== '' || estado !== undefined;

  const columnas: TableColumnsType<OperadorListado> = [
    {
      title: 'Nombre y Apellido',
      key: 'nombre',
      ellipsis: true,
      render: (_, o) => `${o.nombre} ${o.apellido}`,
    },
    { title: 'Email', dataIndex: 'email', ellipsis: true },
    { title: 'Teléfono', dataIndex: 'telefono', width: 150 },
    {
      title: 'Barrio',
      dataIndex: 'barrioNombre',
      ellipsis: true,
      render: (barrio: string | null) => barrio ?? 'Sin barrio asignado',
    },
    {
      title: 'Estado',
      dataIndex: 'estado',
      width: ANCHO_COLUMNA_ESTADO,
      align: 'center',
      render: (valor: EstadoOperador) => <EstadoTag estado={valor} />,
    },
  ];

  return (
    <Flex vertical gap={16}>
      <EncabezadoPagina
        titulo="Operadores"
        style={{ marginBottom: 0 }}
        accion={<Button type="primary" onClick={() => navigate(PATHS.operadorNuevo)}>Crear Operador</Button>}
        filtros={
          <Flex gap={12} wrap>
            <Input
              value={texto}
              maxLength={100}
              allowClear
              placeholder="Buscar Operador..."
              aria-label="Buscar Operador por nombre o email"
              prefix={<SearchOutlined style={{ color: palette.textSecondary }} />}
              onChange={(e) => {
                const valor = e.target.value;
                setTexto(valor);
                window.clearTimeout(espera.current); // cada tecla reinicia la espera
                espera.current = window.setTimeout(() => url.cambiar({ q: valor.trim(), pagina: null }), 300);
              }}
              onPressEnter={() => {
                window.clearTimeout(espera.current);
                url.cambiar({ q: texto.trim(), pagina: null });
              }}
              style={{ flex: '1 1 220px', maxWidth: 360 }}
            />
            <Select<EstadoOperador | 'TODOS'>
              aria-label="Filtrar por estado"
              value={estado ?? 'TODOS'}
              options={OPCIONES_ESTADO}
              onChange={(valor) => url.cambiar({ estado: valor === 'TODOS' ? null : valor, pagina: null })}
              style={{ width: 200 }}
            />
          </Flex>
        }
      />

      <div style={{ backgroundColor: palette.surface, borderRadius: 8 }}>
        <TablaResponsiva<OperadorListado>
          columnaTitulo="nombre"
          bordered
          className="tabla-redondeada"
          rowKey="id"
          columns={columnas}
          dataSource={data?.content ?? []}
          loading={isLoading}
          scroll={{ x: 820 }}
          pagination={false}
          locale={{
            // US-15 CA-5
            emptyText: (
              <div style={{ padding: '40px 16px', color: palette.textSecondary, fontSize: 15 }}>
                {error
                  ? getErrorMessage(error)
                  : hayFiltros
                    ? 'No se encontraron resultados con los filtros aplicados'
                    : 'Todavía no hay Operadores creados'}
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
