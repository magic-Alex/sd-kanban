import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

const backendUrl = process.env.E2E_BACKEND_URL ?? 'http://localhost:8101'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 8102,
    proxy: {
      '/api': {
        target: backendUrl,
        changeOrigin: true,
      },
    },
  },
  test: {
    environment: 'happy-dom',
    exclude: ['node_modules/**', 'dist/**', 'e2e/**'],
  },
})
