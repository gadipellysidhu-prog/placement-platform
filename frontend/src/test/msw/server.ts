import { setupServer } from 'msw/node'
import { handlers } from './handlers'

/**
 * Node-side MSW server shared by every test. Lifecycle (listen/resetHandlers/close)
 * is wired once in src/test/setup.ts so individual test files never manage it.
 */
export const server = setupServer(...handlers)
