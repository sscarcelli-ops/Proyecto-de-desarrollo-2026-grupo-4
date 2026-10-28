import { useState } from 'react';
import { Alert, Button, Flex, Form, Input, Typography } from 'antd';
import { useNavigate } from 'react-router';
import { useActivar, useValidarActivacion, type ActivacionRequest } from '../api/activacion';
import { getApiProblem, getErrorMessage, toFormFieldErrors } from '../../../shared/lib/apiError';
import { CampoConfirmarPassword, CampoPasswordNueva } from '../../../shared/components/CamposPassword';
import { PATHS } from '../../../routes/paths';

type DatosDeCuenta = Omit<ActivacionRequest, 'token'>;

/** Lo que el login recibe al terminar: muestra "Cuenta creada" y deja el email cargado. */
export interface CuentaCreadaState {
  cuentaCreada: string;
}

/**
 * US-01: activación del Administrador inicial (wireframes "Activación de cuenta" y "Registrate").
 * Paso 1: el código del manual de instalación se valida sin consumirse. Paso 2: email y contraseña; al confirmar
 * se crea el Administrador, se consume el código y va al login. El código queda solo en memoria (no en la URL).
 */
export function ActivacionPage() {
  const [codigo, setCodigo] = useState<string | null>(null);

  return codigo === null ? (
    <PasoCodigo onValido={setCodigo} />
  ) : (
    <PasoRegistrate codigo={codigo} />
  );
}

/** Error del backend, con el botón "Iniciar sesión" si el código ya se usó (CA-3, CA-6). */
function ErrorDeActivacion({ error }: { error: unknown }) {
  const navigate = useNavigate();
  const utilizado = getApiProblem(error)?.code === 'TOKEN_UTILIZADO';

  // El botón va debajo del texto (no al costado): en el celular el mensaje queda legible
  return (
    <Alert
      type="error"
      showIcon
      title={getErrorMessage(error)}
      description={
        utilizado && (
          <Button size="small" onClick={() => navigate(PATHS.login)} style={{ marginTop: 8 }}>
            Iniciar sesión
          </Button>
        )
      }
      style={{ marginBottom: 24 }}
    />
  );
}

function PasoCodigo({ onValido }: { onValido: (codigo: string) => void }) {
  const validar = useValidarActivacion();

  const onFinish = ({ token }: { token: string }) =>
    validar.mutate({ token }, { onSuccess: () => onValido(token) });

  return (
    <Flex vertical style={{ width: '100%', maxWidth: 360, margin: '0 auto' }}>
      <Typography.Title level={2} style={{ marginTop: 0, marginBottom: 32, textAlign: 'center' }}>
        Activación de cuenta
      </Typography.Title>

      {validar.isError && <ErrorDeActivacion error={validar.error} />}

      <Form
        layout="vertical"
        size="large"
        requiredMark={false}
        onFinish={onFinish}
        onValuesChange={() => validar.isError && validar.reset()}
      >
        <Form.Item
          label="Ingrese el Token del manual de instalación"
          name="token"
          rules={[{ required: true, whitespace: true, message: 'El código de activación es obligatorio' }]}
        >
          <Input placeholder="Token" autoComplete="off" spellCheck={false} />
        </Form.Item>

        <Button type="primary" htmlType="submit" block loading={validar.isPending} style={{ marginTop: 8 }}>
          Continuar
        </Button>
      </Form>
    </Flex>
  );
}

function PasoRegistrate({ codigo }: { codigo: string }) {
  const navigate = useNavigate();
  const [form] = Form.useForm<DatosDeCuenta>();
  const activar = useActivar();

  const onFinish = ({ email, password }: DatosDeCuenta) =>
    activar.mutate(
      { token: codigo, email, password },
      {
        onSuccess: () => {
          const state: CuentaCreadaState = { cuentaCreada: email };
          navigate(PATHS.login, { replace: true, state });
        },
        onError: (error) => {
          // Email ya registrado: se marca en su campo. El resto (código usado o vencido) va en el aviso de arriba
          if (getApiProblem(error)?.code === 'EMAIL_DUPLICADO') {
            form.setFields([{ name: 'email', errors: [getErrorMessage(error)] }]);
            activar.reset();
          } else {
            form.setFields(toFormFieldErrors(error));
          }
        },
      },
    );

  return (
    <Flex vertical style={{ width: '100%', maxWidth: 360, margin: '0 auto' }}>
      <Typography.Title level={2} style={{ marginTop: 0, marginBottom: 32 }}>
        Registrate
      </Typography.Title>

      {activar.isError && !getApiProblem(activar.error)?.errors && <ErrorDeActivacion error={activar.error} />}

      <Form
        form={form}
        layout="vertical"
        size="large"
        requiredMark={false}
        onFinish={onFinish}
        onValuesChange={() => activar.isError && activar.reset()}
      >
        <Form.Item
          label="Ingrese su Email"
          name="email"
          rules={[
            { required: true, message: 'El email es obligatorio' },
            { type: 'email', message: 'Formato de e-mail no válido' },
          ]}
        >
          <Input placeholder="Email" autoComplete="username" inputMode="email" />
        </Form.Item>

        <CampoPasswordNueva label="Genere su contraseña" placeholder="Contraseña" />
        <CampoConfirmarPassword label="Repita su contraseña" placeholder="Repetir contraseña" />

        <Button type="primary" htmlType="submit" block loading={activar.isPending} style={{ marginTop: 8 }}>
          Crear Cuenta
        </Button>
      </Form>
    </Flex>
  );
}
