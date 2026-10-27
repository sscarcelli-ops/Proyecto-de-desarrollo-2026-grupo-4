import { useEffect } from 'react';
import { Alert, Button, Card, Flex, Form, Input, Spin, Typography } from 'antd';
import { useNavigate, useParams } from 'react-router';
import { useTrabajador, useEditarTrabajador, type TrabajadorEditarRequest } from '../api/trabajadores';
import { getErrorMessage, toFormFieldErrors } from '../../../shared/lib/apiError';
import { PATHS } from '../../../routes/paths';

/**
 * US-26: Modificar trabajador.
 *
 * Muestra el nombre completo del trabajador como título y permite editar:
 * - Nombre
 * - Apellido
 *
 * Por requerimiento (RF y RN-11), el DNI no puede ser modificado una vez registrado.
 * En S2 (US-28) se suman la visualización, reemplazo y borrado de Foto y Documentación.
 */
export function TrabajadorEditarPage() {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const trabajadorId = Number(id);

  const { data: trabajador, isLoading, isError, error } = useTrabajador(trabajadorId);
  const editar = useEditarTrabajador(trabajadorId);
  const [form] = Form.useForm<TrabajadorEditarRequest>();

  // Cuando los datos del trabajador cargan, se inicializa el formulario
  useEffect(() => {
    if (trabajador) {
      form.setFieldsValue({
        nombre: trabajador.nombre,
        apellido: trabajador.apellido,
      });
    }
  }, [trabajador, form]);

  if (isLoading) {
    return (
      <Flex justify="center" align="center" style={{ minHeight: 300 }}>
        <Spin size="large" />
      </Flex>
    );
  }

  if (isError || !trabajador) {
    return (
      <Flex vertical align="center" style={{ width: '100%', padding: '24px 0' }}>
        <Card style={{ width: '100%', maxWidth: 520 }}>
          <Alert
            type="error"
            showIcon
            title="No se pudo cargar el trabajador"
            description={getErrorMessage(error)}
            style={{ marginBottom: 16 }}
          />
          <Button type="primary" onClick={() => navigate(PATHS.trabajadores)}>
            Volver al listado
          </Button>
        </Card>
      </Flex>
    );
  }

  const nombreCompleto = `${trabajador.nombre} ${trabajador.apellido}`;

  const onFinish = (values: TrabajadorEditarRequest) => {
    editar.mutate(values, {
      onSuccess: () => {
        navigate(PATHS.trabajadores);
      },
      onError: (err) => {
        form.setFields(toFormFieldErrors(err) as never);
      },
    });
  };

  return (
    <Flex vertical align="center" style={{ width: '100%', padding: '24px 0' }}>
      <Card style={{ width: '100%', maxWidth: 520, borderRadius: 8 }}>
        <Typography.Title level={2} style={{ textAlign: 'center', marginTop: 0, marginBottom: 32 }}>
          {nombreCompleto}
        </Typography.Title>

        {editar.isError && (
          <Alert
            type="error"
            showIcon
            title={getErrorMessage(editar.error)}
            style={{ marginBottom: 24 }}
          />
        )}

        <Form
          form={form}
          layout="vertical"
          size="large"
          onFinish={onFinish}
          requiredMark="optional"
        >
          <Form.Item
            label="NOMBRE"
            name="nombre"
            rules={[
              { required: true, message: 'El nombre es obligatorio' },
              { max: 100, message: 'Máximo 100 caracteres' },
            ]}
          >
            <Input placeholder="Ingresá el nombre" />
          </Form.Item>

          <Form.Item
            label="APELLIDO"
            name="apellido"
            rules={[
              { required: true, message: 'El apellido es obligatorio' },
              { max: 100, message: 'Máximo 100 caracteres' },
            ]}
          >
            <Input placeholder="Ingresá el apellido" />
          </Form.Item>

          {/* S2 (US-28): Aquí se sumarán los componentes para actualizar/eliminar Foto, ART y Seguro */}

          <Flex gap={12} style={{ marginTop: 32 }}>
            <Button
              type="default"
              block
              onClick={() => navigate(PATHS.trabajadores)}
              disabled={editar.isPending}
            >
              Cancelar
            </Button>
            <Button
              type="primary"
              htmlType="submit"
              block
              loading={editar.isPending}
            >
              Aceptar
            </Button>
          </Flex>
        </Form>
      </Card>
    </Flex>
  );
}
