import { Alert, Button, Flex, Form, Skeleton, App as AntdApp } from 'antd';
import { useNavigate, useParams } from 'react-router';
import { marcarErroresDeOperador, useModificarOperador, useOperador, type OperadorModificarRequest } from '../api/operadores';
import { OperadorForm } from '../components/OperadorForm';
import { getErrorMessage } from '../../../shared/lib/apiError';
import { PATHS } from '../../../routes/paths';
import { NotFoundPage } from '../../../routes/NotFoundPage';
import { idDeRuta } from '../../../shared/lib/idDeRuta';
import { PaginaFormulario } from '../../../shared/components/PaginaFormulario';

// US-16: modificar los datos de un Operador (Administrador): nombre, apellido y teléfono. El email se ve y no se edita.
export function OperadorEditarPage() {
  const { id: parametro } = useParams();
  // Un id que no es un número (/operadores/abc/editar) no llega al backend: "página no encontrada"
  const idValido = idDeRuta(parametro);
  const id = idValido === null ? undefined : String(idValido);
  const navigate = useNavigate();
  const { message } = AntdApp.useApp();
  const [form] = Form.useForm();
  const { data: operador, error, isLoading } = useOperador(id);
  const modificar = useModificarOperador(id ?? '');

  const volver = () => navigate(PATHS.operadores);

  const onFinish = ({ nombre, apellido, telefono }: OperadorModificarRequest) => {
    // Enter dos veces seguidas dispara el envío dos veces: mientras se guarda, no se acepta otro
    if (modificar.isPending) return;
    // El email no viaja: no se puede cambiar
    modificar.mutate(
      { nombre, apellido, telefono },
      {
        onSuccess: () => {
          message.success('Operador actualizado.');
          volver();
        },
        onError: (err) => {
          if (!marcarErroresDeOperador(form, err)) {
            message.error(getErrorMessage(err));
          }
        },
      },
    );
  };

  if (idValido === null) {
    return <NotFoundPage />;
  }

  return (
    <PaginaFormulario titulo="Editar Operador">
      {error && (
        <Flex vertical gap={16} align="center">
          <Alert type="error" showIcon title="No se pudo cargar el Operador" description={getErrorMessage(error)} style={{ width: '100%' }} />
          <Button onClick={volver}>Volver al listado</Button>
        </Flex>
      )}

      {isLoading && <Skeleton active />}

      {operador && (
        <OperadorForm
          form={form}
          edicion
          initialValues={{
            nombre: operador.nombre,
            apellido: operador.apellido,
            email: operador.email,
            telefono: operador.telefono,
          }}
          loading={modificar.isPending}
          textoBoton="Confirmar"
          onFinish={onFinish}
          onCancel={volver}
        />
      )}
    </PaginaFormulario>
  );
}
