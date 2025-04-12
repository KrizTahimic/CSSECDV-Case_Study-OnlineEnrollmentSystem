import { defineConfig } from 'vite';
import reactSwc from '@vitejs/plugin-react-swc';

export default defineConfig({
  plugins: [reactSwc()],
  server: {
    host: '0.0.0.0',
    port: 3000,
    open: true,
    cors: true
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