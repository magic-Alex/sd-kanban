import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 8102,
    proxy: {
      '/api': 'http://localhost:8101',
    },
  },
  test: {
    environment: 'happy-dom',
  },
})
