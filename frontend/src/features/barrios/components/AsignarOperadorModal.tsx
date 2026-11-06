import { useState } from 'react';
import { Alert, Empty, Flex, Form, Modal, Radio, Select, Skeleton, Typography, App as AntdApp } from 'antd';
import { marcarErroresDeOperador, useOperadoresDisponibles, type OperadorRequest } from '../../operadores/api/operadores';
import { OperadorForm } from '../../operadores/components/OperadorForm';
import { useAsignarOperador, type AsignarOperadorRequest, type Barrio } from '../api/barrios';
import { getApiProblem, getErrorMessage } from '../../../shared/lib/apiError';

type Modo = 'existente' | 'nuevo';

interface Props {
  barrio: Barrio;
  onClose: () => void;
}

// US-45: asignar un Operador a un barrio sin Operador. Se elige uno disponible o se crea uno nuevo
// (con el formulario de US-13), y al confirmar el barrio queda Activo. Se monta al abrirlo: arranca siempre limpio.
export function AsignarOperadorModal({ barrio, onClose }: Props) {
  const { message } = AntdApp.useApp();
  const [modo, setModo] = useState<Modo>('existente');
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null);
  const [selectorForm] = Form.useForm<{ operadorId: number }>();
  const [operadorForm] = Form.useForm();
  const { data: disponibles, isLoading, error: errorDisponibles } = useOperadoresDisponibles();
  const asignar = useAsignarOperador(barrio.id);

  // Sin Operadores disponibles solo se puede crear uno nuevo
  const sinDisponibles = disponibles !== undefined && disponibles.length === 0;
  const modoActual: Modo = sinDisponibles ? 'nuevo' : modo;

  const confirmar = (request: AsignarOperadorRequest) => {
    setErrorGeneral(null);
    asignar.mutate(request, {
      onSuccess: () => {
        message.success(`Operador asignado. ${barrio.nombre} quedó Activo.`);
        onClose();
      },
      onError: (error) => {
        if (modoActual === 'nuevo' && marcarErroresDeOperador(operadorForm, error)) {
          return;
        }
        // Si otro Administrador lo asignó mientras tanto, se limpia la selección (la lista ya se actualiza sola)
        if (getApiProblem(error)?.code === 'OPERADOR_OCUPADO') {
          selectorForm.resetFields();
        }
        setErrorGeneral(getErrorMessage(error));
      },
    });
  };

  const onOk = () => (modoActual === 'nuevo' ? operadorForm.submit() : selectorForm.submit());

  return (
    <Modal
      open
      title={`Asignar Operador a ${barrio.nombre}`}
      okText="Asignar"
      cancelText="Cancelar"
      onOk={onOk}
      onCancel={onClose}
      confirmLoading={asignar.isPending}
    >
      <Flex vertical gap={16}>
        <Typography.Text type="secondary">
          Al asignar un Operador, el barrio pasa a Activo.
        </Typography.Text>

        {errorGeneral && <Alert type="error" showIcon title={errorGeneral} />}
        {errorDisponibles && (
          <Alert type="error" showIcon title="No se pudieron cargar los Operadores" description={getErrorMessage(errorDisponibles)} />
        )}

        <Radio.Group
          value={modoActual}
          onChange={(e) => {
            setModo(e.target.value as Modo);
            setErrorGeneral(null);
          }}
          optionType="button"
          options={[
            { label: 'Elegir un Operador disponible', value: 'existente', disabled: sinDisponibles },
            { label: 'Crear un Operador nuevo', value: 'nuevo' },
          ]}
        />

        {isLoading && <Skeleton active />}

        {sinDisponibles && modoActual === 'nuevo' && (
          <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="No hay Operadores disponibles. Creá uno nuevo." />
        )}

        {modoActual === 'existente' && disponibles && !sinDisponibles && (
          <Form form={selectorForm} layout="vertical" onFinish={({ operadorId }) => confirmar({ operadorId })}>
            <Form.Item
              name="operadorId"
              label="Operador disponible"
              rules={[{ required: true, message: 'Debe seleccionar un Operador' }]}
            >
              <Select
                showSearch={{ optionFilterProp: 'label' }}
                placeholder="Seleccioná un Operador"
                options={disponibles.map((o) => ({ value: o.id, label: `${o.apellido}, ${o.nombre} (${o.email})` }))}
              />
            </Form.Item>
          </Form>
        )}

        {modoActual === 'nuevo' && (
          <OperadorForm
            form={operadorForm}
            ocultarBotones
            onFinish={(nuevoOperador: OperadorRequest) => confirmar({ nuevoOperador })}
          />
        )}
      </Flex>
    </Modal>
  );
}
