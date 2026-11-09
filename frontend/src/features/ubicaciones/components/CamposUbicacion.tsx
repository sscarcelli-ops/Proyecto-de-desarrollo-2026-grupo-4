import { Form, Select } from 'antd';
import { useMemo } from 'react';
import { useLocalidades, useProvincias, type Localidad } from '../api/ubicaciones';

// Búsqueda sin distinguir mayúsculas ni tildes: "lujan" encuentra "Luján"
const normalizar = (texto: string) => texto.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase();
const busqueda = {
  filterOption: (input: string, option?: { label?: unknown }) =>
    normalizar(String(option?.label ?? '')).includes(normalizar(input)),
};

// Una opción por localidad. Si el nombre se repite en la provincia (ej. "San José"), se agrega el partido o
// departamento entre paréntesis para distinguirlas.
function opcionesDeLocalidades(localidades: Localidad[] | undefined) {
  if (!localidades) return [];
  const repetidos = new Set<string>();
  const vistos = new Set<string>();
  for (const l of localidades) {
    if (vistos.has(l.nombre)) repetidos.add(l.nombre);
    vistos.add(l.nombre);
  }
  return localidades.map((l) => ({
    value: l.id,
    label: repetidos.has(l.nombre) ? `${l.nombre} (${l.departamento})` : l.nombre,
  }));
}

/**
 * T-11: provincia y localidad de una lista normalizada, en dos selectores con búsqueda. La provincia solo filtra
 * las localidades (no se envía: la localidad ya la trae). Va dentro de un Form con los campos provinciaId y
 * localidadId; al cambiar la provincia se borra la localidad elegida.
 */
export function CamposUbicacion() {
  const form = Form.useFormInstance();
  const provinciaId: string | undefined = Form.useWatch('provinciaId', form);
  const provincias = useProvincias();
  const localidades = useLocalidades(provinciaId);
  const opcionesLocalidad = useMemo(() => opcionesDeLocalidades(localidades.data), [localidades.data]);

  return (
    <>
      <Form.Item name="provinciaId" label="Provincia" rules={[{ required: true, message: 'La provincia es obligatoria' }]}>
        <Select
          showSearch={busqueda}
          placeholder="Elegí la provincia"
          loading={provincias.isLoading}
          options={provincias.data?.map((p) => ({ value: p.id, label: p.nombre }))}
          onChange={() => form.setFieldValue('localidadId', undefined)}
        />
      </Form.Item>
      <Form.Item name="localidadId" label="Localidad" rules={[{ required: true, message: 'La localidad es obligatoria' }]}>
        <Select
          showSearch={busqueda}
          placeholder={provinciaId ? 'Escribí para buscar la localidad' : 'Primero elegí la provincia'}
          disabled={!provinciaId}
          loading={localidades.isLoading}
          options={opcionesLocalidad}
        />
      </Form.Item>
    </>
  );
}
