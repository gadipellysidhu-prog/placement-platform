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

  http.get(`${API_BASE_URL}/api/users/me`, () => HttpResponse.json(mockStudentUser)),
]
