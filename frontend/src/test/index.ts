/**
 * Single import surface for tests. A test file only ever needs:
 *   import { renderWithProviders, screen, waitFor } from '@/test'
 */
export * from '@testing-library/react'
export { default as userEvent } from '@testing-library/user-event'
export {
  renderWithProviders,
  createTestQueryClient,
  type RenderWithProvidersResult,
} from './render'
export { server } from './msw/server'
export { handlers, mockStudentUser, mockOfficerUser, mockTokens } from './msw/handlers'
export { API_BASE_URL } from './constants'
