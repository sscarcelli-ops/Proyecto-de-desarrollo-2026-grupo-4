// Política de contraseñas (US-44, RNF-09): las mismas reglas y mensajes que el backend
// (common/security/PoliticaPassword.java). Si cambia una, cambiar las dos.

export const MENSAJE_POLITICA =
  'La contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula y un número';
export const MENSAJE_LARGO = 'La contraseña no puede tener más de 72 caracteres';

const MAXIMO_BYTES = 72; // límite de bcrypt

export interface RequisitoPassword {
  texto: string;
  cumple: (password: string) => boolean;
}

// En el orden de los wireframes. \p{Lu}/\p{Ll}/\p{Nd}: como Character.isUpperCase/isLowerCase/isDigit (incluye Ñ y tildes)
export const REQUISITOS_PASSWORD: RequisitoPassword[] = [
  { texto: 'Mínimo 8 caracteres', cumple: (p) => p.length >= 8 },
  { texto: 'Un número', cumple: (p) => /\p{Nd}/u.test(p) },
  { texto: 'Una mayúscula', cumple: (p) => /\p{Lu}/u.test(p) },
  { texto: 'Una minúscula', cumple: (p) => /\p{Ll}/u.test(p) },
];

export function cumplePolitica(password: string): boolean {
  return REQUISITOS_PASSWORD.every((r) => r.cumple(password));
}

export function esDemasiadoLarga(password: string): boolean {
  return new TextEncoder().encode(password).length > MAXIMO_BYTES;
}
