import { Card, Flex, Form, Typography, App as AntdApp } from 'antd';
import { useNavigate } from 'react-router';
import { useCrearBarrio, type BarrioRequest } from '../api/barrios';
import { BarrioForm } from '../components/BarrioForm';
import { getErrorMessage, toFormFieldErrors } from '../../../shared/lib/apiError';
import { PATHS } from '../../../routes/paths';

const { Title } = Typography;

// US-10: alta de barrio (Administrador). Obligatorios: nombre, localidad/provincia y dirección.
export function BarrioNuevoPage() {
  const navigate = useNavigate();
  const { message } = AntdApp.useApp();
  const [form] = Form.useForm();
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
        <BarrioForm form={form} loading={crear.isPending} onFinish={onFinish} onCancel={() => navigate(PATHS.barrios)} />
      </Card>
    </Flex>
  );
}
