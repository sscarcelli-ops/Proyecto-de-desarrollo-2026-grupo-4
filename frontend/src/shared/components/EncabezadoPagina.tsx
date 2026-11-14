import type { CSSProperties, ReactNode } from 'react';
import { Flex, Grid, Typography } from 'antd';
import { palette } from '../../app/theme';

/**
 * T-07: encabezado igual en todas las pantallas con listado.
 *
 *   Fila 1: título (y una descripción debajo, si hay) a la izquierda; la acción principal a la derecha.
 *   Fila 2: búsqueda y filtros, si hay.
 *
 * En el celular la acción baja debajo del título y ocupa todo el ancho (más fácil de tocar), y los filtros
 * bajan de línea si no entran.
 *
 *   <EncabezadoPagina
 *     titulo="Guardias"
 *     accion={<Button type="primary">Nuevo Guardia</Button>}
 *     filtros={<Select ... />}
 *   />
 */
export function EncabezadoPagina({
  titulo,
  descripcion,
  accion,
  filtros,
  style,
}: {
  titulo: ReactNode;
  descripcion?: ReactNode;
  accion?: ReactNode;
  filtros?: ReactNode;
  style?: CSSProperties;
}) {
  const esCelular = !(Grid.useBreakpoint().md ?? true);

  return (
    <Flex vertical gap={16} style={{ marginBottom: 20, ...style }}>
      <Flex justify="space-between" align={esCelular ? 'stretch' : 'center'} gap={12} vertical={esCelular}>
        <div style={{ minWidth: 0 }}>
          <Typography.Title
            level={1}
            style={{ fontSize: 'clamp(24px, 2.2vw, 30px)', fontWeight: 700, color: palette.text, margin: 0 }}
          >
            {titulo}
          </Typography.Title>
          {descripcion && (
            <Typography.Paragraph type="secondary" style={{ margin: '4px 0 0' }}>
              {descripcion}
            </Typography.Paragraph>
          )}
        </div>
        {/* En el celular el botón ocupa todo el ancho (clase en index.css) */}
        {accion && <div className={esCelular ? 'encabezado-accion-celular' : undefined}>{accion}</div>}
      </Flex>
      {filtros && (
        <Flex gap={12} align="center" wrap>
          {filtros}
        </Flex>
      )}
    </Flex>
  );
}
