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

/** Generic message envelope returned by the backend's non-entity auth responses. */
export interface MessageResponse {
  message: string
}

/** Body for confirming a single-use email-verification token. */
export interface ConfirmVerificationRequest {
  token: string
}

/** Body for completing a password reset with a token and the new password. */
export interface ResetPasswordRequest {
  token: string
  newPassword: string
}

/** Body for accepting an invitation and setting the account password. */
export interface AcceptInvitationRequest {
  token: string
  newPassword: string
}

export const authApi = {
  login: (data: LoginRequest) =>
    apiClient.post<AuthTokens>('/auth/login', data).then((r) => r.data),

  register: (data: RegisterRequest) =>
    apiClient.post<AuthTokens>('/auth/register', data).then((r) => r.data),

  refresh: (data: RefreshRequest) =>
    apiClient.post<AuthTokens>('/auth/refresh', data).then((r) => r.data),

  logout: (data: LogoutRequest) => apiClient.post<void>('/auth/logout', data).then((r) => r.data),

  /** Request a password-reset email. Backend expects a JSON body and always returns 202. */
  forgotPassword: (email: string) =>
    apiClient.post<MessageResponse>('/auth/forgot-password', { email }).then((r) => r.data),

  /** Complete a password reset with the token embedded in the email link. */
  resetPassword: (data: ResetPasswordRequest) =>
    apiClient.post<MessageResponse>('/auth/reset-password', data).then((r) => r.data),

  /** Consume a single-use email-verification token from the email link. */
  confirmEmailVerification: (data: ConfirmVerificationRequest) =>
    apiClient.post<MessageResponse>('/auth/verify-email/confirm', data).then((r) => r.data),

  /** Re-send an email-verification link. Backend expects a JSON body and always returns 202. */
  resendVerification: (email: string) =>
    apiClient.post<MessageResponse>('/auth/resend-verification', { email }).then((r) => r.data),

  /** Accept an invitation and set the account password from the invite link. */
  acceptInvitation: (data: AcceptInvitationRequest) =>
    apiClient.post<MessageResponse>('/auth/accept-invitation', data).then((r) => r.data),

  me: () => apiClient.get<User>('/api/users/me').then((r) => r.data),
}
