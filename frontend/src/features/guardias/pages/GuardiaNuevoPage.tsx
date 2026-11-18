import { Form, App as AntdApp } from 'antd';
import { useNavigate } from 'react-router';
import { marcarErroresDeGuardia, useCrearGuardia } from '../api/guardias';
import { GuardiaForm } from '../components/GuardiaForm';
import { getErrorMessage } from '../../../shared/lib/apiError';
import { PATHS } from '../../../routes/paths';
import { PaginaFormulario } from '../../../shared/components/PaginaFormulario';

// US-32: alta de Guardia (Operador). Queda habilitado y asociado al barrio del Operador, que sale del token.
export function GuardiaNuevoPage() {
  const navigate = useNavigate();
  const { message } = AntdApp.useApp();
  const [form] = Form.useForm();
  const crear = useCrearGuardia();

  const volver = () => navigate(PATHS.guardias);

  return (
    <PaginaFormulario titulo="Nuevo Guardia">
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
    </PaginaFormulario>
  );
}
