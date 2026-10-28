import { Tag } from 'antd';
import { estadoTonos, type EstadoTono } from '../../app/theme';

// Mapa único de estados del sistema: texto visible + tono de color. Las claves son los mismos códigos
// que devuelve el backend, así se usa directo: <EstadoTag estado={perfil.estado} />.
// Un estado nuevo se agrega acá; nunca se elige un color de estado a mano en una pantalla.
const ESTADOS = {
  // Estado documental del trabajador (RF-10, RN-04)
  VIGENTE: { label: 'Vigente', tono: 'exito' },
  VENCIDO: { label: 'Vencido', tono: 'error' },
  PENDIENTE_CARGA: { label: 'Pendiente de carga', tono: 'aviso' },
  // Solicitud de Empresa / Monotributista (RF-04)
  PENDIENTE: { label: 'Pendiente de aprobación', tono: 'aviso' },
  APROBADO: { label: 'Aprobado', tono: 'exito' },
  RECHAZADO: { label: 'Rechazado', tono: 'error' },
  SUSPENDIDO: { label: 'Suspendido', tono: 'suspendido' },
  // Habilitación en un barrio (RF-15) y Guardias (RF-14)
  HABILITADO: { label: 'Habilitado', tono: 'exito' },
  DESHABILITADO: { label: 'Deshabilitado', tono: 'neutro' },
  // Barrio (RF-02)
  ACTIVO: { label: 'Activo', tono: 'exito' },
  INACTIVO: { label: 'Inactivo', tono: 'neutro' },
  // Tipo de movimiento (RN-06)
  INGRESO: { label: 'Ingreso', tono: 'marca' },
  EGRESO: { label: 'Egreso', tono: 'neutro' },
} satisfies Record<string, { label: string; tono: EstadoTono }>;

export type Estado = keyof typeof ESTADOS;

/** Etiqueta de estado con el color que le corresponde según el mapa de estados del theme. */
export function EstadoTag({ estado }: { estado: Estado }) {
  const { label, tono } = ESTADOS[estado];
  return (
    <Tag
      bordered
      style={{
        ...estadoTonos[tono],
        marginInlineEnd: 0,
        minWidth: 155,
        paddingInline: 10,
        height: 28,
        lineHeight: '26px',
        textAlign: 'center',
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        fontSize: 13,
        borderRadius: 4,
      }}
    >
      {label}
    </Tag>
  );
}
