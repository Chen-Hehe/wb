import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      // API 请求代理到后端
      '/api': {
        target: 'http://localhost:8084',
        changeOrigin: true,
      },
      // 图片上传文件代理到后端
      '/uploads': {
        target: 'http://localhost:8084',
        changeOrigin: true,
      },
    },
  },
})
