import { useEffect, useRef, useState } from 'react';
import type { Key, ReactNode } from 'react';
import { Card, Flex, Grid, Skeleton, Table, Typography } from 'antd';
import type { TableProps } from 'antd';
import type { AnyObject } from 'antd/es/_util/type';
import { palette } from '../../app/theme';

const { Text } = Typography;

/**
 * T-07 (RNF-05): tabla cuando entra, lista de tarjetas cuando no:
 * - en el celular (menos de 768 px), siempre tarjetas;
 * - en cualquier tamaño, si la tabla declara un ancho mínimo (scroll={{ x: 950 }}) y el espacio disponible es menor:
 *   en lugar de una tabla con scroll horizontal, tarjetas.
 *
 * Recibe las MISMAS props que la Table de Ant Design (columns, dataSource, rowKey, loading, locale...), así que en
 * cada pantalla alcanza con cambiar <Table por <TablaResponsiva y decir qué columna es el título de la tarjeta:
 *
 *   <TablaResponsiva<Guardia> columnaTitulo="nombre" columnaAcciones="acciones" columns={columnas} ... />
 *
 * En la tarjeta, cada columna se dibuja con su propio `render` (el mismo de la tabla): un Tag, un Switch o un menú
 * se ven igual y hacen lo mismo. Las columnas se identifican por `key` o, si no tienen, por `dataIndex`.
 * La paginación de cada pantalla va aparte (todas usan pagination={false} y un <Pagination> debajo).
 */
interface TablaResponsivaProps<T extends AnyObject> extends TableProps<T> {
  /** Columna que encabeza la tarjeta (ej. el nombre). */
  columnaTitulo: string;
  /** Columna de acciones (menú "⋯", botón de detalle): va arriba a la derecha de la tarjeta, sin etiqueta. */
  columnaAcciones?: string;
}

// Solo lo que la tarjeta lee de cada columna. Tipado a mano y no con TableColumnType<T>: el tipo completo de
// Ant Design es tan grande que el chequeo de TypeScript se queda sin memoria al combinarlo con un genérico.
interface Columna<T> {
  key?: Key;
  dataIndex?: string | number | readonly (string | number)[];
  title?: unknown;
  render?: (valor: unknown, registro: T, indice: number) => unknown;
}

function idDeColumna<T>(columna: Columna<T>): string | undefined {
  if (columna.key !== undefined) return String(columna.key);
  const indice = columna.dataIndex;
  if (indice === undefined) return undefined;
  return Array.isArray(indice) ? indice.join('.') : String(indice);
}

function valorDe<T>(registro: T, columna: Columna<T>): unknown {
  const indice = columna.dataIndex;
  if (indice === undefined) return undefined;
  const camino: readonly (string | number)[] = Array.isArray(indice) ? indice : [indice as string | number];
  return camino.reduce<unknown>(
    (actual, parte) => (actual == null ? undefined : (actual as Record<string | number, unknown>)[parte]),
    registro,
  );
}

/** Lo que muestra la celda de esa columna en la tabla: su render si tiene, si no el valor. */
function contenido<T>(registro: T, columna: Columna<T>, indice: number): ReactNode {
  const valor = valorDe(registro, columna);
  if (columna.render) {
    return columna.render(valor, registro, indice) as ReactNode;
  }
  return valor == null || valor === '' ? '—' : String(valor);
}

function claveDe<T>(registro: T, indice: number, rowKey: TableProps<T>['rowKey']): Key {
  if (typeof rowKey === 'function') return rowKey(registro, indice);
  if (typeof rowKey === 'string') return (registro as Record<string, Key>)[rowKey];
  return indice;
}

export function TablaResponsiva<T extends AnyObject>({ columnaTitulo, columnaAcciones, ...props }: TablaResponsivaProps<T>) {
  // md = 768 px: en el celular, siempre tarjetas
  const esCelular = !(Grid.useBreakpoint().md ?? true);

  // Ancho disponible para la tabla, medido en vivo (cambia al girar la tablet o achicar la ventana)
  const contenedor = useRef<HTMLDivElement>(null);
  const [ancho, setAncho] = useState<number | null>(null);
  useEffect(() => {
    const elemento = contenedor.current;
    if (!elemento) return;
    const observador = new ResizeObserver(([entrada]) => setAncho(entrada.contentRect.width));
    observador.observe(elemento);
    return () => observador.disconnect();
  }, []);

  const anchoMinimo = typeof props.scroll?.x === 'number' ? props.scroll.x : null;
  const noEntra = anchoMinimo !== null && ancho !== null && ancho < anchoMinimo;

  return (
    <div ref={contenedor}>
      {esCelular || noEntra ? (
        <Tarjetas<T> columnaTitulo={columnaTitulo} columnaAcciones={columnaAcciones} {...props} />
      ) : (
        <Table<T> {...props} />
      )}
    </div>
  );
}

/** La lista de tarjetas: una por fila, con las mismas columnas y renders de la tabla. */
function Tarjetas<T extends AnyObject>({ columnaTitulo, columnaAcciones, ...props }: TablaResponsivaProps<T>) {

  const columnas = (props.columns ?? []) as unknown as Columna<T>[];
  const filas = props.dataSource ?? [];
  const titulo = columnas.find((c) => idDeColumna(c) === columnaTitulo);
  const acciones = columnaAcciones ? columnas.find((c) => idDeColumna(c) === columnaAcciones) : undefined;
  const resto = columnas.filter((c) => c !== titulo && c !== acciones);
  const cargando = typeof props.loading === 'object' ? props.loading.spinning : props.loading;

  if (cargando && filas.length === 0) {
    return <Skeleton active paragraph={{ rows: 4 }} style={{ padding: 16 }} />;
  }

  if (filas.length === 0) {
    const vacio = props.locale?.emptyText;
    return (
      <div style={{ padding: '32px 16px', textAlign: 'center', color: palette.textSecondary }}>
        {typeof vacio === 'function' ? vacio() : (vacio ?? 'No hay datos')}
      </div>
    );
  }

  return (
    <Flex vertical gap={12} style={{ padding: 12, opacity: cargando ? 0.6 : 1 }}>
      {filas.map((registro, indice) => (
        <Card
          key={claveDe(registro, indice, props.rowKey)}
          size="small"
          // Clase en index.css: los botones de la tarjeta miden al menos 40 px, para tocarlos con el dedo
          className="tarjeta-responsiva"
          styles={{ body: { padding: 16 } }}
        >
          <Flex justify="space-between" align="flex-start" gap={8} style={{ marginBottom: resto.length ? 12 : 0 }}>
            <Text strong style={{ fontSize: 15, minWidth: 0, overflowWrap: 'anywhere' }}>
              {titulo ? contenido(registro, titulo, indice) : null}
            </Text>
            {acciones && <div style={{ flexShrink: 0 }}>{contenido(registro, acciones, indice)}</div>}
          </Flex>
          <Flex vertical gap={8}>
            {resto.map((columna) => (
              <Flex key={idDeColumna(columna)} justify="space-between" align="center" gap={12} wrap>
                {/* Algunas pantallas le dan estilo propio al título de la columna (negrita, color): en la tarjeta es
                    una etiqueta y va en gris, igual para todas (clase en index.css) */}
                <Text type="secondary" className="tarjeta-etiqueta" style={{ fontSize: 13 }}>
                  {columna.title as ReactNode}
                </Text>
                <div style={{ textAlign: 'right', minWidth: 0, overflowWrap: 'anywhere' }}>
                  {contenido(registro, columna, indice)}
                </div>
              </Flex>
            ))}
          </Flex>
        </Card>
      ))}
    </Flex>
  );
}
