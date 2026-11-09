import { Alert, App as AntdApp, Button, Flex, Form, Input, Typography } from 'antd';
import { useNavigate } from 'react-router';
import { useCambiarPassword } from '../api/session';
import { getErrorMessage, toFormFieldErrors } from '../../../shared/lib/apiError';
import { CampoConfirmarPassword, CampoPasswordNueva } from '../../../shared/components/CamposPassword';
import { PATHS } from '../../../routes/paths';

interface FormValues {
  passwordActual: string;
  passwordNueva: string;
  confirmarPassword: string;
}

/**
 * US-07: cambiar la contraseña propia. La usan todos los roles y también las cuentas sin rol aprobado.
 * Al salir vuelve a PATHS.login: con la sesión abierta, ese camino lleva a la pantalla inicial de cada usuario.
 */
export function CambiarPasswordPage() {
  const navigate = useNavigate();
  const { message } = AntdApp.useApp();
  const [form] = Form.useForm<FormValues>();
  const cambiar = useCambiarPassword();
  const volver = () => navigate(PATHS.login, { replace: true });

  const onFinish = ({ passwordActual, passwordNueva }: FormValues) => {
    cambiar.mutate(
      { passwordActual, passwordNueva },
      {
        onSuccess: () => {
          message.success('Contraseña actualizada.');
          volver();
        },
        onError: (error) => form.setFields(toFormFieldErrors<FormValues>(error)),
      },
    );
  };

  // Los errores de un campo se muestran en ese campo; el resto (contraseña actual incorrecta), arriba
  const errorGeneral = cambiar.isError && toFormFieldErrors(cambiar.error).length === 0;

  return (
    <Flex vertical style={{ width: '100%', maxWidth: 400, margin: '0 auto' }}>
      <Typography.Title level={2} style={{ marginTop: 0, marginBottom: 28, textAlign: 'center' }}>
        Cambiar contraseña
      </Typography.Title>

      {errorGeneral && (
        <Alert type="error" showIcon title={getErrorMessage(cambiar.error)} style={{ marginBottom: 20 }} />
      )}

      <Form
        form={form}
        layout="vertical"
        size="large"
        requiredMark={false}
        onFinish={onFinish}
        onValuesChange={() => cambiar.isError && cambiar.reset()}
      >
        <Form.Item
          name="passwordActual"
          label="Contraseña actual"
          rules={[{ required: true, message: 'La contraseña actual es obligatoria' }]}
        >
          <Input.Password autoComplete="current-password" placeholder="Contraseña" maxLength={72} />
        </Form.Item>

        <CampoPasswordNueva name="passwordNueva" label="Nueva contraseña" placeholder="Contraseña" />

        <CampoConfirmarPassword
          campoPassword="passwordNueva"
          label="Repita su nueva contraseña"
          placeholder="Repetir contraseña"
        />

        <Flex gap={12} style={{ marginTop: 24 }}>
          <Button block onClick={volver} disabled={cambiar.isPending}>
            Cancelar
          </Button>
          <Button type="primary" htmlType="submit" block loading={cambiar.isPending}>
            Guardar
          </Button>
        </Flex>
      </Form>
    </Flex>
  );
}
