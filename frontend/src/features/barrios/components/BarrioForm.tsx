import { Button, Flex, Form, Input } from 'antd';
import type { FormInstance } from 'antd';
import type { BarrioRequest } from '../api/barrios';

interface BarrioFormProps {
  // La crea la página (Form.useForm(), sin tipo: setFields recibe nombres de campo como texto) para marcar los errores que devuelve el backend en cada campo
  form: FormInstance;
  initialValues?: BarrioRequest;
  loading: boolean;
  onFinish: (values: BarrioRequest) => void;
  onCancel: () => void;
}

// Formulario compartido por el alta (US-10) y la edición (US-11).
// Mismos mensajes que el backend: si el navegador deja pasar algo, el servidor lo rechaza igual.
export function BarrioForm({ form, initialValues, loading, onFinish, onCancel }: BarrioFormProps) {
  return (
    <Form form={form} layout="vertical" initialValues={initialValues} onFinish={onFinish}>
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
        <Button onClick={onCancel}>Cancelar</Button>
        <Button type="primary" htmlType="submit" loading={loading}>Guardar</Button>
      </Flex>
    </Form>
  );
}
