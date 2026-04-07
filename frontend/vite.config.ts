import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      // API 请求代理到后端（包括 /api/v1/uploads）
      '/api': {
        target: 'http://localhost:8084',
        changeOrigin: true,
      },
    },
  },
})
