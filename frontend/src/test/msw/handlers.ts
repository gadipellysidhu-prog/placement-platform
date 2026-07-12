import { http, HttpResponse } from 'msw'
import { API_BASE_URL } from '../constants'
import type { AuthTokens, User } from '@/types'

/** Canonical fixtures reused across tests so assertions stay in sync with handlers. */
export const mockStudentUser: User = {
  email: 'student@university.edu',
  role: 'ROLE_STUDENT',
}

export const mockOfficerUser: User = {
  email: 'officer@university.edu',
  role: 'ROLE_PLACEMENT_OFFICER',
}

export const mockTokens: AuthTokens = {
  accessToken: 'mock-access-token',
  refreshToken: 'mock-refresh-token',
  accessTokenExpiresIn: 900,
  tokenType: 'Bearer',
}

/**
 * Default happy-path handlers. Individual tests override these with `server.use(...)`
 * to exercise failure, 401/refresh, and edge-case flows against the real axios client.
 */
export const handlers = [
  http.post(`${API_BASE_URL}/auth/login`, () => HttpResponse.json(mockTokens)),

  http.post(`${API_BASE_URL}/auth/refresh`, () => HttpResponse.json(mockTokens)),

  http.post(`${API_BASE_URL}/auth/logout`, () => new HttpResponse(null, { status: 204 })),

  // ── Auth journeys ────────────────────────────────────────────────────────
  // The backend returns a MessageResponse envelope; success statuses mirror
  // AuthController (200 for confirm/reset/accept, 202 for the "send email" pair).
  http.post(`${API_BASE_URL}/auth/verify-email/confirm`, () =>
    HttpResponse.json({ message: 'Email verified successfully.' }),
  ),

  http.post(`${API_BASE_URL}/auth/resend-verification`, () =>
    HttpResponse.json(
      { message: 'If an account exists for that email, a message has been sent.' },
      { status: 202 },
    ),
  ),

  http.post(`${API_BASE_URL}/auth/forgot-password`, () =>
    HttpResponse.json(
      { message: 'If an account exists for that email, a message has been sent.' },
      { status: 202 },
    ),
  ),

  http.post(`${API_BASE_URL}/auth/reset-password`, () =>
    HttpResponse.json({ message: 'Password has been reset. Please sign in again.' }),
  ),

  http.post(`${API_BASE_URL}/auth/accept-invitation`, () =>
    HttpResponse.json({
      message: 'Invitation accepted. Your account is now active — please sign in.',
    }),
  ),

  http.get(`${API_BASE_URL}/api/users/me`, () => HttpResponse.json(mockStudentUser)),
]
