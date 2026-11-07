import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv } from 'vite'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  // Configuración del servidor de desarrollo, desde frontend/.env o las variables del sistema (ver .env.example).
  // Sin el prefijo VITE_ a propósito: las usa solo Vite y nunca llegan al código que descarga el navegador.
  const env = loadEnv(mode, process.cwd(), '')
  // El frontend llama a /api en su misma dirección (shared/lib/httpClient.ts) y Vite lo reenvía al backend.
  // En Docker hace lo mismo nginx (nginx/default.conf.template).
  const backend = { target: env.BACKEND_URL || 'http://localhost:8080', changeOrigin: true }
  const puerto = Number(env.FRONTEND_PORT || 5173)

  return {
    plugins: [react()],
    server: { port: puerto, proxy: { '/api': backend } },
    preview: { port: puerto, proxy: { '/api': backend } },
    build: {
      rolldownOptions: {
        output: {
          // Librerías en archivos aparte del código de la app: el navegador las guarda en caché y no las vuelve
          // a descargar cada vez que cambia una pantalla
          codeSplitting: {
            groups: [
              { name: 'antd', test: /node_modules[\\/](antd|@ant-design|@rc-component|rc-[^\\/]+)[\\/]/ },
              { name: 'vendor', test: /node_modules[\\/]/ },
            ],
          },
        },
      },
      // El aviso por defecto es 500 kB. Ant Design ocupa ~1,1 MB minificado (~340 kB comprimido) aunque se importen
      // solo los componentes usados, así que su archivo siempre lo supera. El código de la app queda muy por debajo.
      chunkSizeWarningLimit: 1200,
    },
  }
})
