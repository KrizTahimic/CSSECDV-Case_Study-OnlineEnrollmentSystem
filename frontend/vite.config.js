import { defineConfig } from 'vite';
import reactSwc from '@vitejs/plugin-react-swc';

export default defineConfig({
  plugins: [reactSwc()],
  server: {
    port: 3000,
    open: true,
    proxy: {
      '/api/auth': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        secure: false
      },
      '/api/courses': {
        target: 'http://localhost:8082',
        changeOrigin: true,
        secure: false
      },
      '/api/enrollments': {
        target: 'http://localhost:8083',
        changeOrigin: true,
        secure: false
      },
      '/api/grades': {
        target: 'http://localhost:8084',
        changeOrigin: true,
        secure: false
      }
    }
  },
  build: {
    outDir: 'build',
    sourcemap: true
  },
  esbuild: {
    loader: 'jsx',
    include: /src\/.*\.jsx?$/,
    exclude: []
  },
  optimizeDeps: {
    esbuildOptions: {
      loader: {
        '.js': 'jsx'
      }
    }
  }
}); 