import { Button, Col, Flex, Form, Input, Row } from 'antd';
import type { FormInstance } from 'antd';
import { CampoPasswordNueva } from '../../../shared/components/CamposPassword';
import type { OperadorRequest } from '../api/operadores';

interface OperadorFormProps {
  // La crea la página (Form.useForm(), sin tipo) para poder marcar los errores que devuelve el backend
  form: FormInstance;
  loading: boolean;
  onFinish: (values: OperadorRequest) => void;
  textoBoton?: string;
}

// Formulario de alta de Operador (US-13). Se reutiliza en US-45 (con el barrio indicado) y en US-14 (reemplazar Operador).
// Mismos mensajes que el backend: si el navegador deja pasar algo, el servidor lo rechaza igual.
export function OperadorForm({ form, loading, onFinish, textoBoton = 'Crear Operador' }: OperadorFormProps) {
  return (
    <Form form={form} layout="vertical" onFinish={onFinish}>
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
      <Form.Item
        name="telefono"
        label="Teléfono"
        rules={[{ required: true, whitespace: true, message: 'El teléfono es obligatorio' }, { max: 30 }]}
      >
        <Input maxLength={30} />
      </Form.Item>
      <CampoPasswordNueva label="Contraseña inicial" />

      <Flex justify="flex-end">
        <Button type="primary" htmlType="submit" loading={loading}>{textoBoton}</Button>
      </Flex>
    </Form>
  );
}
