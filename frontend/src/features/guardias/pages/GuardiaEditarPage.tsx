import { Alert, Button, Card, Flex, Form, Skeleton, Typography, App as AntdApp } from 'antd';
import { useNavigate, useParams } from 'react-router';
import { marcarErroresDeGuardia, useGuardia, useModificarGuardia } from '../api/guardias';
import { GuardiaForm } from '../components/GuardiaForm';
import { getErrorMessage } from '../../../shared/lib/apiError';
import { PATHS } from '../../../routes/paths';
import { NotFoundPage } from '../../../routes/NotFoundPage';
import { idDeRuta } from '../../../shared/lib/idDeRuta';

const { Title } = Typography;

// US-32: modificar un Guardia del barrio (Operador). La contraseña es opcional: en blanco no se cambia.
export function GuardiaEditarPage() {
  const { id: parametro } = useParams();
  // Un id que no es un número (/guardias/abc/editar) no llega al backend: "página no encontrada"
  const idValido = idDeRuta(parametro);
  const id = idValido === null ? undefined : String(idValido);
  const navigate = useNavigate();
  const { message } = AntdApp.useApp();
  const [form] = Form.useForm();
  const { data: guardia, error, isLoading } = useGuardia(id);
  const modificar = useModificarGuardia(id ?? '');

  const volver = () => navigate(PATHS.guardias);

  if (idValido === null) {
    return <NotFoundPage />;
  }

  return (
    <Flex vertical gap={16}>
      <Title level={3} style={{ margin: 0 }}>Editar Guardia</Title>

      {error && (
        <Flex vertical gap={12} align="flex-start">
          {/* Un Guardia de otro barrio responde 404, igual que uno inexistente (US-42) */}
          <Alert type="error" showIcon title="No se pudo cargar el Guardia" description={getErrorMessage(error)} />
          <Button onClick={volver}>Volver al listado</Button>
        </Flex>
      )}

      {isLoading && <Skeleton active style={{ maxWidth: 560 }} />}

      {guardia && (
        <Card style={{ maxWidth: 560 }}>
          <GuardiaForm
            form={form}
            edicion
            initialValues={{ nombre: guardia.nombre, apellido: guardia.apellido, email: guardia.email }}
            loading={modificar.isPending}
            textoBoton="Confirmar"
            onCancel={volver}
            onFinish={({ nombre, apellido, email, password }) => {
              if (modificar.isPending) return;
              modificar.mutate(
                { nombre, apellido, email, password: password || undefined },
                {
                  onSuccess: () => {
                    message.success('Guardia actualizado.');
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
      )}
    </Flex>
  );
}
