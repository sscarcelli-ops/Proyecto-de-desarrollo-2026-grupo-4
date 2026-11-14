import { Alert, Button, Flex, Form, Spin, Typography } from 'antd';
import { Link, useNavigate, useSearchParams } from 'react-router';
import { useRestablecerPassword, useValidarRecupero } from '../api/recupero';
import { getApiProblem, getErrorMessage, toFormFieldErrors } from '../../../shared/lib/apiError';
import { CampoConfirmarPassword, CampoPasswordNueva } from '../../../shared/components/CamposPassword';
import { PATHS } from '../../../routes/paths';

interface FormValues {
  password: string;
  confirmarPassword: string;
}

/** Al terminar vuelve al login con este aviso (CA-3). */
export interface PasswordRestablecidaState {
  passwordRestablecida: true;
}

const ENLACE_INVALIDO = 'El enlace de recuperación no es válido o ha vencido';

/**
 * US-06, paso 2: se abre desde el enlace del correo (/restablecer-contrasena?token=...). Si el enlace no sirve
 * (inexistente, usado o vencido) lo dice antes de pedir nada (CA-4); si sirve, pide la contraseña nueva con la política
 * de US-44 y vuelve al login (CA-3).
 */
export function RestablecerPasswordPage() {
  const navigate = useNavigate();
  const [form] = Form.useForm<FormValues>();
  const token = useSearchParams()[0].get('token');
  const validacion = useValidarRecupero(token);
  const restablecer = useRestablecerPassword();

  const onFinish = ({ password }: FormValues) =>
    restablecer.mutate(
      { token: token ?? '', password },
      {
        onSuccess: () => {
          const state: PasswordRestablecidaState = { passwordRestablecida: true };
          navigate(PATHS.login, { replace: true, state });
        },
        onError: (error) => form.setFields(toFormFieldErrors<FormValues>(error)),
      },
    );

  // Enlace que no sirve: al abrirlo, o al guardar si venció o se usó mientras completaba (ENLACE_INVALIDO del backend)
  const enlaceInvalido =
    !token ||
    (validacion.isError && getApiProblem(validacion.error)?.code === 'ENLACE_INVALIDO') ||
    (restablecer.isError && getApiProblem(restablecer.error)?.code === 'ENLACE_INVALIDO');

  // Otros errores (sin conexión, del servidor), arriba del formulario
  const errorGeneral =
    (validacion.isError && !enlaceInvalido && validacion.error) ||
    (restablecer.isError && !enlaceInvalido && toFormFieldErrors(restablecer.error).length === 0 && restablecer.error);

  return (
    <Flex vertical style={{ width: '100%', maxWidth: 400, margin: '0 auto' }}>
      <Typography.Title level={2} style={{ marginTop: 0, marginBottom: 28, textAlign: 'center' }}>
        Restablecer contraseña
      </Typography.Title>

      {enlaceInvalido ? (
        <>
          <Alert type="error" showIcon title={ENLACE_INVALIDO} style={{ marginBottom: 24 }} />
          <Flex vertical align="center" gap={8}>
            <Link to={PATHS.recuperarPassword}>Pedir un enlace nuevo</Link>
            <Link to={PATHS.login}>Volver a iniciar sesión</Link>
          </Flex>
        </>
      ) : validacion.isPending ? (
        <Flex justify="center" style={{ padding: 32 }}>
          <Spin />
        </Flex>
      ) : (
        <>
          {errorGeneral && (
            <Alert type="error" showIcon title={getErrorMessage(errorGeneral)} style={{ marginBottom: 20 }} />
          )}

          <Form
            form={form}
            layout="vertical"
            size="large"
            requiredMark={false}
            onFinish={onFinish}
            onValuesChange={() => restablecer.isError && restablecer.reset()}
          >
            <CampoPasswordNueva label="Nueva contraseña" placeholder="Contraseña" />
            <CampoConfirmarPassword label="Repita su nueva contraseña" placeholder="Repetir contraseña" />

            <Flex gap={12} style={{ marginTop: 24 }}>
              <Button block onClick={() => navigate(PATHS.login)} disabled={restablecer.isPending}>
                Cancelar
              </Button>
              <Button type="primary" htmlType="submit" block loading={restablecer.isPending}>
                Guardar
              </Button>
            </Flex>
          </Form>
        </>
      )}
    </Flex>
  );
}
