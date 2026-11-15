import { Form, Input } from 'antd';
import type { NamePath } from 'antd/es/form/interface';
import { RequisitosPassword } from './RequisitosPassword';
import {
  MENSAJE_LARGO,
  MENSAJE_OBLIGATORIA,
  MENSAJE_POLITICA,
  cumplePolitica,
  esDemasiadoLarga,
} from '../lib/politicaPassword';

interface Props {
  name?: NamePath;
  label?: string;
  placeholder?: string;
  // Al modificar un usuario la contraseña se puede dejar en blanco (no se cambia): no es obligatoria ni lleva "*"
  opcional?: boolean;
}

/**
 * US-44: campo para DEFINIR una contraseña (registro, alta de Operador o Guardia, cambio, recupero, activación).
 * Trae el indicador de requisitos y la misma validación que el backend (@PasswordSegura). No va en el login.
 *
 *   <CampoPasswordNueva />                                    // name="password", label="Contraseña"
 *   <CampoPasswordNueva name="passwordInicial" label="Contraseña inicial" />
 */
export function CampoPasswordNueva({ name = 'password', label = 'Contraseña', placeholder, opcional = false }: Props) {
  const password: string | undefined = Form.useWatch(name, Form.useFormInstance());

  return (
    <Form.Item
      name={name}
      label={label}
      // Al salir del campo y al enviar: mientras escribe, el indicador ya muestra lo que falta
      validateTrigger="onBlur"
      rules={[
        { required: !opcional, message: MENSAJE_OBLIGATORIA },
        {
          validator: (_, value?: string) => {
            if (!value) return Promise.resolve();
            if (esDemasiadoLarga(value)) return Promise.reject(new Error(MENSAJE_LARGO));
            return cumplePolitica(value) ? Promise.resolve() : Promise.reject(new Error(MENSAJE_POLITICA));
          },
        },
      ]}
      extra={<RequisitosPassword password={password} />}
    >
      <Input.Password autoComplete="new-password" placeholder={placeholder} maxLength={72} />
    </Form.Item>
  );
}

/**
 * "Repetí la contraseña" de los wireframes: tiene que coincidir con el campo de CampoPasswordNueva.
 *
 *   <CampoConfirmarPassword />                                // compara con name="password"
 */
export function CampoConfirmarPassword({
  name = 'confirmarPassword',
  label = 'Repetí la contraseña',
  placeholder,
  campoPassword = 'password',
  opcional = false,
}: Props & { campoPassword?: NamePath }) {
  return (
    <Form.Item
      name={name}
      label={label}
      dependencies={[campoPassword]}
      validateTrigger="onBlur"
      rules={[
        // Opcional: solo se exige confirmar si se escribió una contraseña nueva
        ({ getFieldValue }) => ({
          required: !opcional || Boolean(getFieldValue(campoPassword)),
          message: 'La confirmación de la contraseña es obligatoria',
        }),
        ({ getFieldValue }) => ({
          validator: (_, value?: string) =>
            !value || value === getFieldValue(campoPassword)
              ? Promise.resolve()
              : Promise.reject(new Error('Las contraseñas no coinciden')),
        }),
      ]}
    >
      <Input.Password autoComplete="new-password" placeholder={placeholder} maxLength={72} />
    </Form.Item>
  );
}
