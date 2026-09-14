import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Dev server proxies /api to the local api-gateway so the app can always call
// relative /api/... paths - no CORS setup needed, and the same relative paths
// work unchanged against nginx's proxy_pass in the built Docker image (see
// nginx.conf).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8000',
        changeOrigin: true,
      },
    },
  },
});
