import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const devPort = Number(env.VITE_DEV_SERVER_PORT || 3000)
  const apiBasePath = env.VITE_API_BASE_PATH || '/api'
  const apiTarget = env.VITE_API_TARGET || 'http://localhost:8080'

  return {
    plugins: [vue()],
    resolve: {
      alias: {
        '@': path.resolve(__dirname, './src')
      }
    },
    server: {
      port: Number.isFinite(devPort) ? devPort : 3000,
      proxy: {
        [apiBasePath]: {
          target: apiTarget,
          changeOrigin: true
        }
      }
    }
  }
})
