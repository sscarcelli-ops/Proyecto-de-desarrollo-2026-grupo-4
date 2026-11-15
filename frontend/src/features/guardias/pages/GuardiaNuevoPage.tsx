import { Card, Flex, Form, Typography, App as AntdApp } from 'antd';
import { useNavigate } from 'react-router';
import { marcarErroresDeGuardia, useCrearGuardia } from '../api/guardias';
import { GuardiaForm } from '../components/GuardiaForm';
import { getErrorMessage } from '../../../shared/lib/apiError';
import { PATHS } from '../../../routes/paths';

const { Title } = Typography;

// US-32: alta de Guardia (Operador). Queda habilitado y asociado al barrio del Operador, que sale del token.
export function GuardiaNuevoPage() {
  const navigate = useNavigate();
  const { message } = AntdApp.useApp();
  const [form] = Form.useForm();
  const crear = useCrearGuardia();

  const volver = () => navigate(PATHS.guardias);

  return (
    <Flex vertical gap={16}>
      <Title level={3} style={{ margin: 0 }}>Nuevo Guardia</Title>
      <Card style={{ maxWidth: 560 }}>
        <GuardiaForm
          form={form}
          loading={crear.isPending}
          textoBoton="Crear Guardia"
          onCancel={volver}
          onFinish={({ nombre, apellido, email, password }) => {
            // Enter dos veces seguidas dispara el envío dos veces: mientras se guarda, no se acepta otro
            if (crear.isPending) return;
            crear.mutate(
              { nombre, apellido, email, password: password ?? '' },
              {
                onSuccess: () => {
                  message.success('Guardia creado.');
                  volver();
                },
                onError: (err) => {
                  if (!marcarErroresDeGuardia(form, err)) {
                    message.error(getErrorMessage(err));
                  }
                },
              },
            );
          }}
        />
      </Card>
    </Flex>
  );
}
