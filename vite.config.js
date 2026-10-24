import { defineConfig } from 'vite';
import preact from '@preact/preset-vite';

// Publicado em https://wiapps.com.br/glowstudio/
export default defineConfig({
  base: './',
  plugins: [preact()],
  build: { target: 'es2020', assetsInlineLimit: 0 },
  test: { environment: 'node' },
});
