import { Alert, Button, Flex, Form, Input, Typography } from 'antd';
import { Link } from 'react-router';
import { useSolicitarRecupero } from '../api/recupero';
import { getErrorMessage, toFormFieldErrors } from '../../../shared/lib/apiError';
import { PATHS } from '../../../routes/paths';

interface FormValues {
  email: string;
}

/**
 * US-06, paso 1: "¿Olvidaste tu contraseña?" del login. Pide el email y manda el enlace.
 * El mensaje de confirmación es el mismo exista o no la cuenta (CA-2): no revela qué emails están registrados.
 */
export function RecuperarPasswordPage() {
  const [form] = Form.useForm<FormValues>();
  const solicitar = useSolicitarRecupero();

  const onFinish = (values: FormValues) =>
    solicitar.mutate(values, {
      onError: (error) => form.setFields(toFormFieldErrors<FormValues>(error)),
    });

  // Los errores de un campo se muestran en ese campo; el resto (sin conexión, error del servidor), arriba
  const errorGeneral = solicitar.isError && toFormFieldErrors(solicitar.error).length === 0;

  return (
    <Flex vertical style={{ width: '100%', maxWidth: 360, margin: '0 auto' }}>
      <Typography.Title level={2} style={{ marginTop: 0, marginBottom: 12 }}>
        Recuperar contraseña
      </Typography.Title>

      {solicitar.isSuccess ? (
        <>
          <Alert
            type="success"
            showIcon
            title="Revisá tu correo"
            description="Si el email está registrado, te enviamos un enlace para elegir una nueva contraseña. Sirve una sola vez y por tiempo limitado."
            style={{ marginBottom: 24 }}
          />
          <Button type="link" block onClick={() => solicitar.reset()}>
            Enviar de nuevo
          </Button>
        </>
      ) : (
        <>
          <Typography.Paragraph type="secondary" style={{ marginBottom: 24 }}>
            Ingresá el email de tu cuenta y te enviamos un enlace para elegir una nueva contraseña.
          </Typography.Paragraph>

          {errorGeneral && (
            <Alert type="error" showIcon title={getErrorMessage(solicitar.error)} style={{ marginBottom: 24 }} />
          )}

          <Form
            form={form}
            layout="vertical"
            size="large"
            requiredMark={false}
            onFinish={onFinish}
            onValuesChange={() => solicitar.isError && solicitar.reset()}
          >
            <Form.Item
              label="Email"
              name="email"
              rules={[
                { required: true, message: 'El email es obligatorio' },
                { type: 'email', message: 'Ingresá un email válido' },
              ]}
            >
              <Input placeholder="Ingresá tu email" autoComplete="username" inputMode="email" maxLength={255} />
            </Form.Item>

            <Button type="primary" htmlType="submit" block loading={solicitar.isPending}>
              Enviar enlace
            </Button>
          </Form>
        </>
      )}

      <Flex justify="center" style={{ marginTop: 16 }}>
        <Link to={PATHS.login}>Volver a iniciar sesión</Link>
      </Flex>
    </Flex>
  );
}
