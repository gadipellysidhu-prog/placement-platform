import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import viteConfig from './vite.config'

// Reuse the app's path aliases (single source of truth in vite.config.ts) so tests
// resolve modules exactly like production. The Tailwind Vite plugin is intentionally
// omitted — tests never render real CSS, and loading it adds significant per-file cost.
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: viteConfig.resolve?.alias,
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    // Run all test files in a single worker. Spawning multiple workers can hang
    // on startup when the repo path contains spaces (e.g. "clode 222"); one worker
    // is deterministic and plenty fast for this suite. `fileParallelism: false`
    // forces maxWorkers to 1 (Vitest 4). Identical behaviour in CI.
    pool: 'threads',
    fileParallelism: false,
    css: false,
    restoreMocks: true,
    clearMocks: true,
    // Guarantee env validation (src/config/env.ts) passes regardless of local .env files.
    env: {
      VITE_API_BASE_URL: 'http://localhost:8081',
      VITE_APP_NAME: 'Placement Intelligence Platform',
      VITE_APP_VERSION: '1.0.0-test',
      VITE_ENABLE_DEVTOOLS: 'false',
    },
    include: ['src/**/*.{test,spec}.{ts,tsx}'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html', 'lcov'],
      reportsDirectory: './coverage',
      include: ['src/**/*.{ts,tsx}'],
      exclude: [
        'src/**/*.d.ts',
        'src/test/**',
        'src/**/index.ts',
        'src/main.tsx',
        'src/**/*.{test,spec}.{ts,tsx}',
      ],
    },
  },
})
