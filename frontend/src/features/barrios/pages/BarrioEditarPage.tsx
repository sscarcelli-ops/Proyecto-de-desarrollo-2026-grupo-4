import { Alert, Button, Card, Flex, Form, Skeleton, Typography, App as AntdApp } from 'antd';
import { useNavigate, useParams } from 'react-router';
import { useBarrio, useModificarBarrio, type BarrioRequest } from '../api/barrios';
import { BarrioForm } from '../components/BarrioForm';
import { getErrorMessage, toFormFieldErrors } from '../../../shared/lib/apiError';
import { PATHS } from '../../../routes/paths';

const { Title } = Typography;

// US-11: modificar un barrio (Administrador). Edita nombre, localidad/provincia y dirección.
// El Operador se asigna por US-45 y se cambia por US-14; el estado, por US-12.
export function BarrioEditarPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { message } = AntdApp.useApp();
  const [form] = Form.useForm();
  const { data: barrio, error, isLoading } = useBarrio(id);
  const modificar = useModificarBarrio(id ?? '');

  const volver = () => navigate(PATHS.barrios);

  const onFinish = (values: BarrioRequest) => {
    modificar.mutate(values, {
      onSuccess: () => {
        message.success('Barrio actualizado.');
        volver();
      },
      onError: (err) => {
        const fieldErrors = toFormFieldErrors(err);
        if (fieldErrors.length > 0) {
          form.setFields(fieldErrors);
        } else {
          message.error(getErrorMessage(err));
        }
      },
    });
  };

  return (
    <Flex vertical gap={16}>
      <Title level={3} style={{ margin: 0 }}>Editar barrio</Title>

      {error && (
        <Flex vertical gap={12} align="flex-start">
          <Alert type="error" showIcon title="No se pudo cargar el barrio" description={getErrorMessage(error)} />
          <Button onClick={volver}>Volver al listado</Button>
        </Flex>
      )}

      {isLoading && <Skeleton active style={{ maxWidth: 560 }} />}

      {barrio && (
        <Card style={{ maxWidth: 560 }}>
          <BarrioForm
            form={form}
            initialValues={{
              nombre: barrio.nombre,
              localidadProvincia: barrio.localidadProvincia,
              direccion: barrio.direccion,
            }}
            loading={modificar.isPending}
            onFinish={onFinish}
            onCancel={volver}
          />
        </Card>
      )}
    </Flex>
  );
}
