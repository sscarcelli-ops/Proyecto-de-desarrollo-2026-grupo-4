import type { ReactNode } from 'react';
import { Card, Flex, Typography } from 'antd';

/**
 * T-07: marco igual para todas las pantallas de alta y edición (Nuevo/Editar Barrio, Operador, Guardia...):
 * una tarjeta centrada, con aire arriba y el título centrado adentro. Es el mismo formato que ya usaban
 * Registrar y Editar trabajador, así todos los formularios se ven iguales.
 *
 *   <PaginaFormulario titulo="Nuevo Guardia">
 *     <GuardiaForm ... />
 *   </PaginaFormulario>
 *
 * Los avisos de error o la carga (Skeleton) también van adentro: el usuario siempre ve el título de la pantalla.
 */
export function PaginaFormulario({ titulo, children, ancho = 540 }: { titulo: ReactNode; children: ReactNode; ancho?: number }) {
  return (
    <Flex vertical align="center" style={{ width: '100%', padding: '24px 0' }}>
      <Card style={{ width: '100%', maxWidth: ancho, borderRadius: 8 }}>
        <Typography.Title level={2} style={{ textAlign: 'center', marginTop: 0, marginBottom: 24 }}>
          {titulo}
        </Typography.Title>
        {children}
      </Card>
    </Flex>
  );
}
