import { Card, Flex, Form, Typography, App as AntdApp } from 'antd';
import { useNavigate } from 'react-router';
import { marcarErroresDeOperador, useCrearOperador, type OperadorRequest } from '../api/operadores';
import { OperadorForm } from '../components/OperadorForm';
import { getErrorMessage } from '../../../shared/lib/apiError';
import { PATHS } from '../../../routes/paths';

const { Title } = Typography;

// US-13: alta de Operador (Administrador). Queda "Disponible" hasta que se lo asigne a un barrio.
// Cuando exista el listado de Operadores (US-15), "volver" y el destino al guardar pasan a ser ese listado.
export function OperadorNuevoPage() {
  const navigate = useNavigate();
  const { message } = AntdApp.useApp();
  const [form] = Form.useForm();
  const crear = useCrearOperador();

  const volver = () => navigate(PATHS.barrios);

  const onFinish = (values: OperadorRequest) => {
    crear.mutate(values, {
      onSuccess: () => {
        message.success('Operador creado. Queda disponible hasta que se le asigne un barrio.');
        volver();
      },
      onError: (error) => {
        if (!marcarErroresDeOperador(form, error)) {
          message.error(getErrorMessage(error));
        }
      },
    });
  };

  return (
    <Flex vertical gap={16}>
      <Title level={3} style={{ margin: 0 }}>Nuevo Operador</Title>
      <Card style={{ maxWidth: 560 }}>
        <OperadorForm form={form} loading={crear.isPending} onFinish={onFinish} onCancel={volver} />
      </Card>
    </Flex>
  );
}
