import { Button, Card, Flex, Form, Input, Typography, App as AntdApp } from 'antd';
import { useNavigate } from 'react-router';
import { useCrearBarrio, type BarrioRequest } from '../api/barrios';
import { getErrorMessage, toFormFieldErrors } from '../../../shared/lib/apiError';
import { PATHS } from '../../../routes/paths';

const { Title } = Typography;

// US-10: alta de barrio (Administrador). Obligatorios: nombre, localidad/provincia y dirección.
// Los mismos mensajes que el backend: si el navegador deja pasar algo, el servidor lo rechaza igual.
export function BarrioNuevoPage() {
  const navigate = useNavigate();
  const { message } = AntdApp.useApp();
  const [form] = Form.useForm<BarrioRequest>();
  const crear = useCrearBarrio();

  const onFinish = (values: BarrioRequest) => {
    crear.mutate(values, {
      onSuccess: () => {
        message.success('Barrio creado. Queda Inactivo hasta que tenga Operador.');
        navigate(PATHS.barrios);
      },
      onError: (error) => {
        const fieldErrors = toFormFieldErrors(error);
        if (fieldErrors.length > 0) {
          form.setFields(fieldErrors);
        } else {
          message.error(getErrorMessage(error));
        }
      },
    });
  };

  return (
    <Flex vertical gap={16}>
      <Title level={3} style={{ margin: 0 }}>Nuevo barrio</Title>

      <Card style={{ maxWidth: 560 }}>
        <Form form={form} layout="vertical" onFinish={onFinish} requiredMark="optional">
          <Form.Item
            name="nombre"
            label="Nombre"
            rules={[{ required: true, whitespace: true, message: 'El nombre es obligatorio' }, { max: 150 }]}
          >
            <Input maxLength={150} />
          </Form.Item>
          <Form.Item
            name="localidadProvincia"
            label="Localidad / Provincia"
            rules={[{ required: true, whitespace: true, message: 'La localidad/provincia es obligatoria' }, { max: 150 }]}
          >
            <Input maxLength={150} />
          </Form.Item>
          <Form.Item
            name="direccion"
            label="Dirección"
            rules={[{ required: true, whitespace: true, message: 'La dirección es obligatoria' }, { max: 255 }]}
          >
            <Input maxLength={255} />
          </Form.Item>

          <Flex justify="flex-end" gap={8} wrap>
            <Button onClick={() => navigate(PATHS.barrios)}>Cancelar</Button>
            <Button type="primary" htmlType="submit" loading={crear.isPending}>Guardar</Button>
          </Flex>
        </Form>
      </Card>
    </Flex>
  );
}
