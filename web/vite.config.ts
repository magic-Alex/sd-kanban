import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

const backendUrl = process.env.API_PROXY_TARGET ?? process.env.E2E_BACKEND_URL ?? 'http://localhost:8101'
const frontendUrl = process.env.FRONTEND_URL ?? process.env.E2E_FRONTEND_URL ?? 'http://localhost:8102'
const frontendHost = process.env.FRONTEND_HOST ?? '0.0.0.0'
const frontendPort = Number(process.env.FRONTEND_PORT ?? (new URL(frontendUrl).port || 8102))

export default defineConfig({
  plugins: [vue()],
  server: {
    host: frontendHost,
    port: frontendPort,
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
