import { Button, Flex, Form, Input } from 'antd';
import type { FormInstance } from 'antd';
import type { BarrioRequest } from '../api/barrios';
import { CamposUbicacion } from '../../ubicaciones/components/CamposUbicacion';

/** Valores del formulario: los del backend más la provincia, que solo sirve para filtrar las localidades (T-11). */
export type BarrioFormValues = BarrioRequest & { provinciaId?: string };

interface BarrioFormProps {
  // La crea la página (Form.useForm(), sin tipo: setFields recibe nombres de campo como texto) para marcar los errores que devuelve el backend en cada campo
  form: FormInstance;
  initialValues?: Partial<BarrioFormValues>;
  loading: boolean;
  // Texto del botón principal según el wireframe: "Crear Barrio" en el alta, "Confirmar" en la edición
  textoGuardar: string;
  onFinish: (values: BarrioRequest) => void;
  onCancel: () => void;
}

// Formulario compartido por el alta (US-10) y la edición (US-11).
// Mismos mensajes que el backend: si el navegador deja pasar algo, el servidor lo rechaza igual.
export function BarrioForm({ form, initialValues, loading, textoGuardar, onFinish, onCancel }: BarrioFormProps) {
  // La provincia no viaja al backend: la localidad ya la trae
  const enviar = ({ nombre, localidadId, direccion }: BarrioFormValues) => onFinish({ nombre, localidadId, direccion });

  return (
    // Tamaño grande, como los demás formularios (Trabajadores, registro) y el wireframe
    <Form form={form} layout="vertical" size="large" initialValues={initialValues} onFinish={enviar}>
      <Form.Item
        name="nombre"
        label="Nombre"
        rules={[{ required: true, whitespace: true, message: 'El nombre es obligatorio' }, { max: 150 }]}
      >
        <Input maxLength={150} />
      </Form.Item>
      <CamposUbicacion />
      <Form.Item
        name="direccion"
        label="Dirección"
        rules={[{ required: true, whitespace: true, message: 'La dirección es obligatoria' }, { max: 255 }]}
      >
        <Input maxLength={255} />
      </Form.Item>

      <Flex justify="flex-end" gap={8} wrap>
        <Button onClick={onCancel}>Cancelar</Button>
        <Button type="primary" htmlType="submit" loading={loading}>{textoGuardar}</Button>
      </Flex>
    </Form>
  );
}
