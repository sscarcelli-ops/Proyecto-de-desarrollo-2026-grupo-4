import { Alert, Button, Card, Flex, Form, Input, Typography } from 'antd';
import { useNavigate } from 'react-router';
import { useCrearTrabajador, type TrabajadorRequest } from '../api/trabajadores';
import { getErrorMessage, toFormFieldErrors } from '../../../shared/lib/apiError';
import { PATHS } from '../../../routes/paths';

/**
 * US-25: Registrar trabajador.
 *
 * En S1 se registran exclusivamente los datos básicos:
 * - Nombre
 * - Apellido
 * - DNI ("Sin puntos", validación numérica básica)
 *
 * En S2 (US-28) se incorporan:
 * - Carga de foto de perfil
 * - Carga de constancia de ART con selector modal de vencimiento
 * - Carga de Seguro de Vida con selector modal de vencimiento
 */
export function TrabajadorNuevoPage() {
  const navigate = useNavigate();
  const [form] = Form.useForm<TrabajadorRequest>();
  const crear = useCrearTrabajador();

  const onFinish = (values: TrabajadorRequest) => {
    crear.mutate(values, {
      onSuccess: () => {
        // Redirige al listado; la tabla ya tendrá el nuevo registro cacheado
        navigate(PATHS.trabajadores);
      },
      onError: (error) => {
        // Si el backend devuelve errores de campo (ej. DNI inválido), se asignan al formulario
        form.setFields(toFormFieldErrors(error) as never);
      },
    });
  };

  return (
    <Flex vertical align="center" style={{ width: '100%', padding: '24px 0' }}>
      <Card style={{ width: '100%', maxWidth: 520, borderRadius: 8 }}>
        <Typography.Title level={2} style={{ textAlign: 'center', marginTop: 0, marginBottom: 32 }}>
          Nuevo trabajador
        </Typography.Title>

        {crear.isError && (
          <Alert
            type="error"
            showIcon
            title={getErrorMessage(crear.error)}
            style={{ marginBottom: 24 }}
          />
        )}

        <Form
          form={form}
          layout="vertical"
          size="large"
          onFinish={onFinish}
          requiredMark="optional"
        >
          <Form.Item
            label="NOMBRE"
            name="nombre"
            rules={[
              { required: true, message: 'El nombre es obligatorio' },
              { max: 100, message: 'Máximo 100 caracteres' },
            ]}
          >
            <Input placeholder="Ingresá el nombre" />
          </Form.Item>

          <Form.Item
            label="APELLIDO"
            name="apellido"
            rules={[
              { required: true, message: 'El apellido es obligatorio' },
              { max: 100, message: 'Máximo 100 caracteres' },
            ]}
          >
            <Input placeholder="Ingresá el apellido" />
          </Form.Item>

          <Form.Item
            label="DNI"
            name="dni"
            rules={[
              { required: true, message: 'El DNI es obligatorio' },
              { pattern: /^[0-9]{7,8}$/, message: 'Ingresá un DNI válido de 7 u 8 dígitos (sin puntos)' },
            ]}
          >
            <Input placeholder="Sin puntos" maxLength={8} inputMode="numeric" />
          </Form.Item>

          {/* S2 (US-28): Aquí se sumarán los componentes de carga de Foto, ART y Seguro de Vida */}

          <Flex vertical gap={12} style={{ marginTop: 32 }}>
            <Button
              type="primary"
              htmlType="submit"
              block
              loading={crear.isPending}
            >
              Aceptar
            </Button>
            <Button
              type="text"
              block
              onClick={() => navigate(PATHS.trabajadores)}
              disabled={crear.isPending}
            >
              Volver
            </Button>
          </Flex>
        </Form>
      </Card>
    </Flex>
  );
}
