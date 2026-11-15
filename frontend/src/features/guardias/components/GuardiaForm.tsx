import { Button, Col, Flex, Form, Input, Row } from 'antd';
import type { FormInstance } from 'antd';
import { CampoConfirmarPassword, CampoPasswordNueva } from '../../../shared/components/CamposPassword';

// Los valores del alta (GuardiaRequest) y de la edición (GuardiaModificarRequest) coinciden; se agrega la confirmación
interface GuardiaFormValues {
  nombre: string;
  apellido: string;
  email: string;
  password?: string;
  confirmarPassword?: string;
}

interface GuardiaFormProps {
  // La crea la página (Form.useForm(), sin tipo) para poder marcar los errores que devuelve el backend
  form: FormInstance;
  loading: boolean;
  // true: modificar. La contraseña es opcional ("Dejar la contraseña en blanco para no cambiarla").
  edicion?: boolean;
  initialValues?: Partial<GuardiaFormValues>;
  onFinish: (values: GuardiaFormValues) => void;
  onCancel: () => void;
  textoBoton: string;
}

// Formulario de alta y edición de Guardias (US-32). Mismos mensajes que el backend: si el navegador deja pasar
// algo, el servidor lo rechaza igual.
export function GuardiaForm({ form, loading, edicion = false, initialValues, onFinish, onCancel, textoBoton }: GuardiaFormProps) {
  return (
    <Form form={form} layout="vertical" size="large" initialValues={initialValues} onFinish={onFinish}>
      <Row gutter={16}>
        <Col xs={24} sm={12}>
          <Form.Item
            name="nombre"
            label="Nombre"
            rules={[{ required: true, whitespace: true, message: 'El nombre es obligatorio' }, { max: 100 }]}
          >
            <Input maxLength={100} />
          </Form.Item>
        </Col>
        <Col xs={24} sm={12}>
          <Form.Item
            name="apellido"
            label="Apellido"
            rules={[{ required: true, whitespace: true, message: 'El apellido es obligatorio' }, { max: 100 }]}
          >
            <Input maxLength={100} />
          </Form.Item>
        </Col>
      </Row>
      <Form.Item
        name="email"
        label="Email"
        rules={[
          { required: true, whitespace: true, message: 'El email es obligatorio' },
          { type: 'email', message: 'Formato de e-mail no válido' },
          { max: 255 },
        ]}
      >
        <Input maxLength={255} autoComplete="off" />
      </Form.Item>
      <CampoPasswordNueva
        label={edicion ? 'Contraseña nueva' : 'Contraseña inicial'}
        placeholder={edicion ? 'Dejar la contraseña en blanco para no cambiarla' : undefined}
        opcional={edicion}
      />
      <CampoConfirmarPassword opcional={edicion} />

      <Flex justify="flex-end" gap={8} wrap>
        <Button onClick={onCancel}>Cancelar</Button>
        <Button type="primary" htmlType="submit" loading={loading}>{textoBoton}</Button>
      </Flex>
    </Form>
  );
}
