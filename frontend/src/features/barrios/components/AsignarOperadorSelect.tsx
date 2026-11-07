import { useNavigate } from 'react-router';
import { App as AntdApp } from 'antd';
import { SelectorOperador } from '../../operadores/components/SelectorOperador';
import { useAsignarOperador, type Barrio } from '../api/barrios';
import type { Operador } from '../../operadores/api/operadores';
import { getErrorMessage } from '../../../shared/lib/apiError';
import { PATHS } from '../../../routes/paths';

// US-45: columna "Op. Asignado" de un barrio sin Operador. Elegir un Operador disponible pide confirmar (CA-4) y,
// al confirmar, lo asigna y activa el barrio; después solo se cambia reemplazándolo (US-14).
// "Crear un nuevo Operador" lleva a Nuevo Operador indicando el barrio, donde se crea y se asigna en un solo paso.
export function AsignarOperadorSelect({ barrio }: { barrio: Barrio }) {
  const navigate = useNavigate();
  const { message, modal } = AntdApp.useApp();
  const asignar = useAsignarOperador(barrio.id);

  const confirmarAsignacion = (operador: Operador) => {
    modal.confirm({
      title: `¿Asignar a ${operador.nombre} ${operador.apellido} como Operador de ${barrio.nombre}?`,
      content: 'El barrio quedará Activo. Para cambiar el Operador después, hay que reemplazarlo.',
      okText: 'Confirmar',
      cancelText: 'Cancelar',
      // Devuelve la promesa: el botón queda cargando y el diálogo se cierra al terminar
      onOk: async () => {
        try {
          await asignar.mutateAsync({ operadorId: operador.id });
          message.success(`Operador asignado. ${barrio.nombre} quedó Activo.`);
        } catch (error) {
          message.error(getErrorMessage(error));
        }
      },
    });
  };

  return (
    <SelectorOperador
      loading={asignar.isPending}
      disabled={asignar.isPending}
      onSeleccionar={confirmarAsignacion}
      onCrearNuevo={() => navigate(`${PATHS.operadorNuevo}?barrio=${barrio.id}`)}
    />
  );
}
