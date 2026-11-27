import { Button, Col, Flex, Form, Input, Row } from 'antd';
import type { FormInstance } from 'antd';
import { CampoPasswordNueva } from '../../../shared/components/CamposPassword';
import type { OperadorRequest } from '../api/operadores';

// Valores que se muestran al editar (US-16)
interface OperadorFormValues {
  nombre: string;
  apellido: string;
  email: string;
  telefono: string;
}

interface OperadorFormProps {
  // La crea la página (Form.useForm(), sin tipo) para poder marcar los errores que devuelve el backend
  form: FormInstance;
  loading: boolean;
  onFinish: (values: OperadorRequest) => void;
  // Vuelve a la pantalla anterior sin guardar
  onCancel: () => void;
  textoBoton?: string;
  // US-16: edición. El email se ve pero no se puede cambiar y no hay contraseña (se cambia con US-17).
  edicion?: boolean;
  initialValues?: Partial<OperadorFormValues>;
}

// Formulario de alta de Operador (US-13). Se reutiliza en US-45 (con el barrio indicado) y en US-14 (reemplazar Operador).
// Con `edicion` (US-16) es el de modificar: email de solo lectura y sin contraseña.
// Mismos mensajes que el backend: si el navegador deja pasar algo, el servidor lo rechaza igual.
export function OperadorForm({
  form,
  loading,
  onFinish,
  onCancel,
  textoBoton = 'Crear Operador',
  edicion = false,
  initialValues,
}: OperadorFormProps) {
  return (
    // Tamaño grande, como los demás formularios (Barrios, Trabajadores, registro)
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
      {edicion ? (
        // US-16 CA-2: el email es el usuario de acceso: se muestra sin posibilidad de edición
        <Form.Item name="email" label="Email">
          <Input disabled />
        </Form.Item>
      ) : (
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
      )}
      <Form.Item
        name="telefono"
        label="Teléfono"
        rules={[{ required: true, whitespace: true, message: 'El teléfono es obligatorio' }, { max: 30 }]}
      >
        <Input maxLength={30} />
      </Form.Item>
      {!edicion && <CampoPasswordNueva label="Contraseña inicial" />}

      <Flex justify="flex-end" gap={8} wrap>
        <Button onClick={onCancel}>Cancelar</Button>
        <Button type="primary" htmlType="submit" loading={loading}>{textoBoton}</Button>
      </Flex>
    </Form>
  );
}
