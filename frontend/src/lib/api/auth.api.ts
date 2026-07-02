import { apiClient } from '@/lib/axios'
import type { AuthTokens, User } from '@/types'

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  email: string
  password: string
  role: 'ROLE_STUDENT'
}

export interface RefreshRequest {
  refreshToken: string
}

export interface LogoutRequest {
  refreshToken: string
}

export const authApi = {
  login: (data: LoginRequest) =>
    apiClient.post<AuthTokens>('/auth/login', data).then((r) => r.data),

  register: (data: RegisterRequest) =>
    apiClient.post<AuthTokens>('/auth/register', data).then((r) => r.data),

  refresh: (data: RefreshRequest) =>
    apiClient.post<AuthTokens>('/auth/refresh', data).then((r) => r.data),

  logout: (data: LogoutRequest) => apiClient.post<void>('/auth/logout', data).then((r) => r.data),

  forgotPassword: (email: string) =>
    apiClient.post<void>('/auth/forgot-password', null, { params: { email } }).then((r) => r.data),

  verifyEmail: (email: string) =>
    apiClient.post<void>('/auth/verify-email', null, { params: { email } }).then((r) => r.data),

  me: () => apiClient.get<User>('/api/users/me').then((r) => r.data),
}
