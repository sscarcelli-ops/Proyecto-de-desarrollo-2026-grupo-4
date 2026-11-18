import { useState } from 'react';
import { Alert, Button, DatePicker, Flex, Modal, Typography, Upload } from 'antd';
import { UploadOutlined } from '@ant-design/icons';
import type { Dayjs } from 'dayjs';

export interface ModalCargarDocumentoProps {
  open: boolean;
  tipo: 'ART' | 'SEGURO_VIDA' | null;
  onCancel: () => void;
  onConfirm: (archivo: File, fecha: Dayjs) => void;
}

/**
 * Modal compartido para cargar o renovar póliza de ART y Seguro de Vida (US-28, US-24).
 * Reutilizado tanto en Empresa (TrabajadorEditarPage) como en Monotributista (DatosPersonalesPage).
 */
export function ModalCargarDocumento({
  open,
  tipo,
  onCancel,
  onConfirm,
}: ModalCargarDocumentoProps) {
  const [archivo, setArchivo] = useState<File | null>(null);
  const [fecha, setFecha] = useState<Dayjs | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleCerrar = () => {
    setArchivo(null);
    setFecha(null);
    setError(null);
    onCancel();
  };

  const handleConfirmar = () => {
    if (!archivo || !fecha) return;
    onConfirm(archivo, fecha);
    handleCerrar();
  };

  const titulo = tipo === 'ART' ? 'Cargar / Renovar Póliza de ART' : 'Cargar / Renovar Seguro de Vida';

  return (
    <Modal
      title={titulo}
      open={open}
      centered
      onCancel={handleCerrar}
      destroyOnHidden
      footer={
        <Flex gap={12} style={{ width: '100%', marginTop: 20 }}>
          <Button block style={{ flex: 1 }} size="large" onClick={handleCerrar}>
            Cancelar
          </Button>
          <Button
            type="primary"
            block
            style={{ flex: 1 }}
            size="large"
            disabled={!archivo || !fecha}
            onClick={handleConfirmar}
          >
            Cargar documento
          </Button>
        </Flex>
      }
    >
      <Flex vertical gap={16} style={{ marginTop: 16 }}>
        {error && <Alert type="error" showIcon title={error} />}

        <div>
          <Typography.Text strong style={{ display: 'block', marginBottom: 8 }}>
            Archivo (PDF hasta 8 MB)
          </Typography.Text>
          <Upload
            accept=".pdf"
            maxCount={1}
            fileList={archivo ? [{ uid: '-1', name: archivo.name, status: 'done' }] : []}
            beforeUpload={(file) => {
              const isPdf = file.type === 'application/pdf' || file.name.toLowerCase().endsWith('.pdf');
              if (!isPdf || file.size > 8 * 1024 * 1024) {
                setError('El archivo debe ser un PDF de hasta 8 MB');
                return Upload.LIST_IGNORE;
              }
              setError(null);
              setArchivo(file);
              return false;
            }}
            onRemove={() => setArchivo(null)}
            className="upload-block"
          >
            <Button block className="btn-upload-input" icon={<UploadOutlined />}>
              {archivo ? 'Cambiar archivo' : 'Subir archivo pdf'}
            </Button>
          </Upload>
        </div>

        <div>
          <Typography.Text strong style={{ display: 'block', marginBottom: 8 }}>
            Indique fecha de vencimiento
          </Typography.Text>
          <DatePicker
            size="large"
            placeholder="DD/MM/AAAA"
            format="DD/MM/YYYY"
            value={fecha}
            onChange={(date) => setFecha(date)}
            style={{ width: '100%' }}
          />
        </div>
      </Flex>
    </Modal>
  );
}
