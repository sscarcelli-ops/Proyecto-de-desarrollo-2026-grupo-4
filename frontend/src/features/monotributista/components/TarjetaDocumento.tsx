import { Button, Card, Flex, Typography } from 'antd';
import { DeleteOutlined, DownloadOutlined, FilePdfOutlined, SyncOutlined } from '@ant-design/icons';
import dayjs, { type Dayjs } from 'dayjs';
import { palette } from '../../../app/theme';
import { EstadoTag } from '../../../shared/components/EstadoTag';
import type { ApiSchema } from '../../../shared/api/types';

type DocumentoResponse = ApiSchema<'DocumentoResponse'>;

export interface TarjetaDocumentoProps {
  titulo: string;
  docOriginal?: DocumentoResponse;
  docNuevo?: { archivo: File; fecha: Dayjs } | null;
  eliminado: boolean;
  descargando?: boolean;
  onCargar: () => void;
  onBorrar: () => void;
  onDescargar: () => void;
}

/**
 * Tarjeta compartida para gestión documental (ART y Seguro de Vida) (US-24, US-28).
 * Muestra el estado visual de la póliza, datos de vencimiento, descarga, renovación y eliminación.
 */
export function TarjetaDocumento({
  titulo,
  docOriginal,
  docNuevo,
  eliminado,
  descargando = false,
  onCargar,
  onBorrar,
  onDescargar,
}: TarjetaDocumentoProps) {
  const tieneDoc = !eliminado && (Boolean(docNuevo) || Boolean(docOriginal));
  const nombreArchivo = docNuevo ? docNuevo.archivo.name : docOriginal?.archivoNombre;
  const fechaVenc = docNuevo ? docNuevo.fecha : (docOriginal ? dayjs(docOriginal.fechaVencimiento) : null);
  const vencido = fechaVenc ? fechaVenc.isBefore(dayjs().startOf('day')) : false;

  return (
    <Card size="small" style={{ marginBottom: 16, background: palette.tableHeader }}>
      <Flex vertical gap={8}>
        <Flex justify="space-between" align="center">
          <Typography.Text strong>{titulo}</Typography.Text>
          <EstadoTag estado={tieneDoc ? (vencido ? 'VENCIDO' : 'VIGENTE') : 'PENDIENTE_CARGA'} />
        </Flex>

        {tieneDoc ? (
          <>
            <Typography.Text type="secondary" style={{ fontSize: 13 }}>
              Archivo: {nombreArchivo}
            </Typography.Text>
            <Typography.Text type="secondary" style={{ fontSize: 13 }}>
              Vencimiento: {fechaVenc?.format('DD/MM/YYYY')}
            </Typography.Text>
            <Flex gap={8} style={{ width: '100%', marginTop: 6 }} wrap="wrap">
              <Button
                size="small"
                style={{ flex: 1, minWidth: 90 }}
                icon={<DownloadOutlined />}
                loading={descargando}
                onClick={onDescargar}
              >
                Descargar
              </Button>
              <Button
                size="small"
                style={{ flex: 1, minWidth: 90 }}
                icon={<SyncOutlined />}
                onClick={onCargar}
              >
                Renovar
              </Button>
              <Button
                size="small"
                danger
                style={{ flex: 1, minWidth: 90 }}
                icon={<DeleteOutlined />}
                onClick={onBorrar}
              >
                Borrar
              </Button>
            </Flex>
          </>
        ) : (
          <Button block icon={<FilePdfOutlined />} onClick={onCargar}>
            Subir archivo PDF
          </Button>
        )}
      </Flex>
    </Card>
  );
}
