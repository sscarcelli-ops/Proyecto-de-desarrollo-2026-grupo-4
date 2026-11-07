import { useState } from 'react';
import { Button, Divider, Select } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { useOperadoresDisponibles, type Operador } from '../api/operadores';

interface SelectorOperadorProps {
  placeholder?: string;
  disabled?: boolean;
  loading?: boolean;
  // Se llama con el Operador elegido de la lista de disponibles
  onSeleccionar: (operador: Operador) => void;
  // Se llama con la opción fija del final: "Crear un nuevo Operador"
  onCrearNuevo: () => void;
}

// Búsqueda sin distinguir mayúsculas ni tildes, por nombre, apellido o email: "jose" encuentra "José Pérez"
const normalizar = (texto: string) => texto.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase();

// Desplegable de Operadores disponibles con "Crear un nuevo Operador" al final.
// Se usa para asignar un Operador a un barrio sin Operador (US-45) y para reemplazarlo (US-14).
// Con muchos Operadores no se rompe: la lista tiene su propio desplazamiento y solo dibuja lo visible; además se
// puede escribir para filtrarla.
export function SelectorOperador({
  placeholder = 'Asignar un Operador',
  disabled = false,
  loading = false,
  onSeleccionar,
  onCrearNuevo,
}: SelectorOperadorProps) {
  const { data: disponibles = [], isLoading } = useOperadoresDisponibles();
  const [busqueda, setBusqueda] = useState('');

  return (
    <Select<number | null>
      // Siempre vuelve a mostrar el texto de ayuda: elegir un Operador dispara la acción, no queda seleccionado
      value={null}
      placeholder={placeholder}
      style={{ width: 190 }}
      loading={loading || isLoading}
      disabled={disabled}
      popupMatchSelectWidth={false}
      showSearch={{
        onSearch: setBusqueda,
        filterOption: (input, option) => normalizar(String(option?.label ?? '')).includes(normalizar(input)),
      }}
      notFoundContent={busqueda ? 'Ningún Operador disponible coincide' : 'No hay Operadores disponibles'}
      options={disponibles.map((o) => ({ value: o.id, label: `${o.nombre} ${o.apellido} (${o.email})` }))}
      onChange={(id) => {
        const elegido = disponibles.find((o) => o.id === id);
        if (elegido) onSeleccionar(elegido);
      }}
      onOpenChange={(abierto) => !abierto && setBusqueda('')}
      popupRender={(lista) => (
        <>
          {lista}
          <Divider style={{ margin: '4px 0' }} />
          <Button type="text" block icon={<PlusOutlined />} onClick={onCrearNuevo} style={{ textAlign: 'left' }}>
            Crear un nuevo Operador
          </Button>
        </>
      )}
    />
  );
}
