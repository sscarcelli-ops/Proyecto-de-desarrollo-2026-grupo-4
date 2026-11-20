import { useState } from 'react';
import { App as AntdApp, Button, Flex, Modal, Tooltip, Typography } from 'antd';
import { UserSwitchOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router';
import { SelectorOperador } from '../../operadores/components/SelectorOperador';
import { nombreConUbicacion, useReemplazarOperador, type Barrio } from '../api/barrios';
import type { Operador } from '../../operadores/api/operadores';
import { getErrorMessage } from '../../../shared/lib/apiError';
import { PATHS } from '../../../routes/paths';

const { Text } = Typography;

// US-14: "Reemplazar Operador" en la columna "Op. Asignado" de un barrio con Operador (Activo o Inactivo).
// Abre el mismo desplegable de US-45 (disponibles, con búsqueda y "Crear un nuevo Operador"); elegir uno pide
// confirmar y, al confirmar, el backend hace el cambio en una sola operación: el anterior queda disponible y sin
// sesión, y el barrio nunca queda sin Operador ni con dos. "Crear un nuevo Operador" lleva a Nuevo Operador con el
// barrio y reemplazo=1, donde se crea y se reemplaza en un solo paso.
export function ReemplazarOperadorBoton({ barrio }: { barrio: Barrio }) {
  const navigate = useNavigate();
  const { message, modal } = AntdApp.useApp();
  const [abierto, setAbierto] = useState(false);
  const reemplazar = useReemplazarOperador(barrio.id);
  const actual = `${barrio.operadorNombre} ${barrio.operadorApellido}`;

  const confirmarReemplazo = (nuevo: Operador) => {
    const nombreNuevo = `${nuevo.nombre} ${nuevo.apellido}`;
    modal.confirm({
      title: `¿Reemplazar a ${actual} por ${nombreNuevo} como Operador de ${nombreConUbicacion(barrio)}?`,
      content: `${actual} quedará disponible y se cerrará su sesión.`,
      okText: 'Reemplazar Operador',
      cancelText: 'Cancelar',
      // Devuelve la promesa: el botón queda cargando y el diálogo se cierra al terminar
      onOk: async () => {
        try {
          await reemplazar.mutateAsync({ operadorId: nuevo.id });
          setAbierto(false);
          message.success(`Operador reemplazado. ${nombreNuevo} es ahora el Operador de ${barrio.nombre}.`);
        } catch (error) {
          // Si falla, no se modificó nada: el Operador anterior sigue asignado
          message.error(getErrorMessage(error));
        }
      },
    });
  };

  return (
    <>
      <Tooltip title="Reemplazar Operador">
        <Button
          size="small"
          icon={<UserSwitchOutlined />}
          aria-label="Reemplazar Operador"
          onClick={() => setAbierto(true)}
        />
      </Tooltip>
      <Modal
        open={abierto}
        title="Reemplazar Operador"
        destroyOnHidden
        onCancel={() => setAbierto(false)}
        footer={<Button onClick={() => setAbierto(false)}>Cancelar</Button>}
      >
        <Flex vertical gap={12}>
          <Text>
            Operador actual de <strong>{nombreConUbicacion(barrio)}</strong>: <strong>{actual}</strong>
          </Text>
          <Text type="secondary">Elegí quién queda a cargo del barrio. {actual} quedará disponible.</Text>
          <SelectorOperador
            placeholder="Elegir el nuevo Operador"
            loading={reemplazar.isPending}
            disabled={reemplazar.isPending}
            onSeleccionar={confirmarReemplazo}
            onCrearNuevo={() => navigate(`${PATHS.operadorNuevo}?barrio=${barrio.id}&reemplazo=1`)}
          />
        </Flex>
      </Modal>
    </>
  );
}
