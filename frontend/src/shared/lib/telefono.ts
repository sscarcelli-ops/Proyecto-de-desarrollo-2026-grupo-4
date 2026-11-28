import type { FormRule } from 'antd';

// Formato de un teléfono: el mismo que valida el backend (FormatoTelefono.java). Al menos 6 números y, además de
// números, solo espacios, +, - y paréntesis (ej. "+54 9 11 4555-0000"). Vacío pasa: si es obligatorio lo marca la
// regla required del campo.
export const reglaTelefono: FormRule = {
  pattern: /^\s*$|^(?=(?:\D*\d){6})[0-9 +()-]+$/,
  message: 'Ingresá un teléfono válido: solo números, espacios, +, - y paréntesis',
};
