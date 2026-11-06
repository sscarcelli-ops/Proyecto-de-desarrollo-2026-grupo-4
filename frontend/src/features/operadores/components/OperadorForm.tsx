import { Button, Flex, Form, Input } from 'antd';
import type { FormInstance } from 'antd';
import { CampoPasswordNueva } from '../../../shared/components/CamposPassword';
import type { OperadorRequest } from '../api/operadores';

interface OperadorFormProps {
  // La crea la página (Form.useForm(), sin tipo) para poder marcar los errores que devuelve el backend
  form: FormInstance;
  onFinish: (values: OperadorRequest) => void;
  // Embebido en otra pantalla (US-45, US-14): los botones los pone quien lo contiene
  ocultarBotones?: boolean;
  loading?: boolean;
  onCancel?: () => void;
}

// Formulario de alta de Operador (US-13). Se reutiliza embebido en US-45 y en US-14 (reemplazar Operador).
// Mismos mensajes que el backend: si el navegador deja pasar algo, el servidor lo rechaza igual.
export function OperadorForm({ form, onFinish, ocultarBotones = false, loading = false, onCancel }: OperadorFormProps) {
  return (
    <Form form={form} layout="vertical" onFinish={onFinish}>
      <Form.Item
        name="nombre"
        label="Nombre"
        rules={[{ required: true, whitespace: true, message: 'El nombre es obligatorio' }, { max: 100 }]}
      >
        <Input maxLength={100} />
      </Form.Item>
      <Form.Item
        name="apellido"
        label="Apellido"
        rules={[{ required: true, whitespace: true, message: 'El apellido es obligatorio' }, { max: 100 }]}
      >
        <Input maxLength={100} />
      </Form.Item>
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

      {!ocultarBotones && (
        <Flex justify="flex-end" gap={8} wrap>
          <Button onClick={onCancel}>Cancelar</Button>
          <Button type="primary" htmlType="submit" loading={loading}>Guardar</Button>
        </Flex>
      )}
    </Form>
  );
}
