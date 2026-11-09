import { Alert, Button, Card, Flex, Form, Skeleton, Typography, App as AntdApp } from 'antd';
import { useNavigate, useSearchParams } from 'react-router';
import { marcarErroresDeOperador, useCrearOperador, type OperadorRequest } from '../api/operadores';
import { OperadorForm } from '../components/OperadorForm';
import { nombreConUbicacion, useAsignarOperador, useBarrio } from '../../barrios/api/barrios';
import { getErrorMessage } from '../../../shared/lib/apiError';
import { PATHS } from '../../../routes/paths';

const { Title, Text } = Typography;

// US-13: alta de Operador (Administrador). Queda "Disponible" hasta que se lo asigne a un barrio.
// US-45: con ?barrio=<id> (desde "Crear un nuevo Operador" del listado de barrios) el Operador se crea y se asigna
// a ese barrio en la misma operación, y el barrio queda Activo.
// Cuando exista el listado de Operadores (US-15), "volver" y el destino al guardar sin barrio pasan a ser ese listado.
export function OperadorNuevoPage() {
  const navigate = useNavigate();
  const { message } = AntdApp.useApp();
  const [form] = Form.useForm();
  const [params] = useSearchParams();
  const barrioParam = params.get('barrio');
  const tieneBarrio = barrioParam !== null;
  const barrioId = barrioParam !== null && /^\d+$/.test(barrioParam) ? Number(barrioParam) : null;

  const { data: barrio, error: errorBarrio, isLoading: cargandoBarrio } = useBarrio(
    barrioId === null ? undefined : String(barrioId),
  );
  const crear = useCrearOperador();
  const crearYAsignar = useAsignarOperador(barrioId ?? 0);

  const volver = () => navigate(PATHS.barrios);

  const onFinish = async (values: OperadorRequest) => {
    // Enter dos veces seguidas dispara el envío dos veces: mientras se guarda, no se acepta otro
    if (crear.isPending || crearYAsignar.isPending) return;
    try {
      if (barrio) {
        await crearYAsignar.mutateAsync({ nuevoOperador: values });
        message.success(`Operador creado y asignado. ${barrio.nombre} quedó Activo.`);
      } else {
        await crear.mutateAsync(values);
        message.success('Operador creado. Queda disponible hasta que se le asigne un barrio.');
      }
      volver();
    } catch (error) {
      if (!marcarErroresDeOperador(form, error)) {
        message.error(getErrorMessage(error));
      }
    }
  };

  const problemaConElBarrio = !tieneBarrio
    ? null
    : barrioId === null
      ? 'El barrio indicado no es válido.'
      : errorBarrio
        ? getErrorMessage(errorBarrio)
        : barrio && barrio.operadorId !== null
          ? `${nombreConUbicacion(barrio)} ya tiene un Operador asignado.`
          : null;

  return (
    <Flex vertical gap={16}>
      <Title level={3} style={{ margin: 0 }}>Nuevo Operador</Title>

      {/* Mismo formato que "No se pudo cargar el barrio" en Editar barrio: título, motivo y salida */}
      {problemaConElBarrio && (
        <Flex vertical gap={12} align="flex-start" style={{ maxWidth: 560 }}>
          <Alert
            type="error"
            showIcon
            title="No se puede crear el Operador para este barrio"
            description={problemaConElBarrio}
            style={{ width: '100%' }}
          />
          <Button onClick={volver}>Volver a Barrios</Button>
        </Flex>
      )}

      {tieneBarrio && barrioId !== null && cargandoBarrio && <Skeleton active style={{ maxWidth: 560 }} />}

      {!problemaConElBarrio && !(tieneBarrio && cargandoBarrio) && (
        <Card style={{ maxWidth: 560 }}>
          <Flex vertical gap={16}>
            {barrio && (
              <Text type="secondary">
                Se asignará a <strong>{nombreConUbicacion(barrio)}</strong>, que quedará Activo.
              </Text>
            )}
            <OperadorForm
              form={form}
              loading={crear.isPending || crearYAsignar.isPending}
              onFinish={(values) => void onFinish(values)}
              onCancel={volver}
            />
          </Flex>
        </Card>
      )}
    </Flex>
  );
}
