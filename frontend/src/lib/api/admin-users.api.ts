import { apiClient } from '@/lib/axios'
import type { Page, PageParams, Role } from '@/types'

/**
 * Administrative account lifecycle state (backend `AccountStatus`). Distinct from
 * the transient brute-force lock — this is set explicitly by an administrator or
 * by the invitation flow, and gates login.
 */
export type AccountStatus = 'ACTIVE' | 'DISABLED' | 'LOCKED' | 'INVITED'

export interface AdminUserResponse {
  id: string
  email: string
  role: Role
  status: AccountStatus
  emailVerified: boolean
  createdAt: string
  updatedAt: string
  /**
   * Most recent login/refresh, derived server-side from refresh-token issuance.
   * Null when no activity is on record — it is NOT a fallback to createdAt.
   */
  lastActivityAt: string | null
}

/** Admin listing filters. `query` is a case-insensitive partial match on email only. */
export interface ListAdminUsersParams extends PageParams {
  role?: Role
  status?: AccountStatus
  query?: string
}

export interface InviteUserRequest {
  email: string
  role: Role
}

export interface AssignRoleRequest {
  role: Role
}

/** Backend `MessageResponse` envelope. */
export interface MessageResponse {
  message: string
}

/**
 * `/api/admin/users/**` — ADMIN only (class-level `@PreAuthorize("hasRole('ADMIN')")`).
 *
 * The backend refuses to disable, lock or demote the last active administrator,
 * answering 409 with an explanatory detail; callers surface that message verbatim.
 */
export const adminUsersApi = {
  list: (params?: ListAdminUsersParams) =>
    apiClient.get<Page<AdminUserResponse>>('/api/admin/users', { params }).then((r) => r.data),

  getById: (id: string) =>
    apiClient.get<AdminUserResponse>(`/api/admin/users/${id}`).then((r) => r.data),

  /** Creates an INVITED account and emails an activation link → 202. */
  invite: (data: InviteUserRequest) =>
    apiClient.post<MessageResponse>('/api/admin/users/invite', data).then((r) => r.data),

  enable: (id: string) =>
    apiClient.post<AdminUserResponse>(`/api/admin/users/${id}/enable`).then((r) => r.data),

  disable: (id: string) =>
    apiClient.post<AdminUserResponse>(`/api/admin/users/${id}/disable`).then((r) => r.data),

  lock: (id: string) =>
    apiClient.post<AdminUserResponse>(`/api/admin/users/${id}/lock`).then((r) => r.data),

  unlock: (id: string) =>
    apiClient.post<AdminUserResponse>(`/api/admin/users/${id}/unlock`).then((r) => r.data),

  assignRole: (id: string, data: AssignRoleRequest) =>
    apiClient.put<AdminUserResponse>(`/api/admin/users/${id}/role`, data).then((r) => r.data),
}
