import { useNavigate } from 'react-router';
import { App as AntdApp } from 'antd';
import { SelectorOperador } from '../../operadores/components/SelectorOperador';
import { useAsignarOperador, type Barrio } from '../api/barrios';
import { getErrorMessage } from '../../../shared/lib/apiError';
import { PATHS } from '../../../routes/paths';

// US-45: columna "Op. Asignado" de un barrio sin Operador. Elegir un Operador disponible lo asigna y activa el barrio;
// "Crear un nuevo Operador" lleva a Nuevo Operador indicando el barrio, donde se crea y se asigna en un solo paso.
export function AsignarOperadorSelect({ barrio }: { barrio: Barrio }) {
  const navigate = useNavigate();
  const { message } = AntdApp.useApp();
  const asignar = useAsignarOperador(barrio.id);

  // mutateAsync (y no las opciones de mutate): al asignar, la fila deja de mostrar este componente
  const asignarOperador = async (operadorId: number) => {
    try {
      await asignar.mutateAsync({ operadorId });
      message.success(`Operador asignado. ${barrio.nombre} quedó Activo.`);
    } catch (error) {
      message.error(getErrorMessage(error));
    }
  };

  return (
    <SelectorOperador
      loading={asignar.isPending}
      disabled={asignar.isPending}
      onSeleccionar={(operador) => void asignarOperador(operador.id)}
      onCrearNuevo={() => navigate(`${PATHS.operadorNuevo}?barrio=${barrio.id}`)}
    />
  );
}
