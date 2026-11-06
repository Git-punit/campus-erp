import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  build: {
    // Spring Boot serves whatever is in static/, so the jar carries the UI too
    outDir: '../src/main/resources/static',
    emptyOutDir: true,
  },
})
