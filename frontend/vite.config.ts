import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// where the dev server sends /api calls; error messages in dev name this address and port
const BACKEND = 'http://localhost:8080'

export default defineConfig({
  plugins: [react()],
  define: {
    __BACKEND__: JSON.stringify(BACKEND),
  },
  server: {
    port: 5173,
    proxy: {
      '/api': BACKEND,
    },
  },
  build: {
    // Spring Boot serves whatever is in static/, so the jar carries the UI too
    outDir: '../src/main/resources/static',
    emptyOutDir: true,
  },
})
