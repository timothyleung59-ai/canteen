import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 开发环境：把后端接口代理到本地后端，规避跨域。
// 生产环境：前端静态文件由 Nginx 托管，/bc 和 /admin 同源反代到后端，无需此代理。
const BACKEND = process.env.VITE_BACKEND || 'http://localhost:8088'

export default defineConfig({
  plugins: [vue()],
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: {
      '/bc': { target: BACKEND, changeOrigin: true },
      '/admin': { target: BACKEND, changeOrigin: true }
    }
  },
  build: {
    outDir: 'dist',
    chunkSizeWarningLimit: 1500
  }
})
