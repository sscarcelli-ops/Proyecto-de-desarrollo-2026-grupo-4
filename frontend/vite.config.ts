import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
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
})
