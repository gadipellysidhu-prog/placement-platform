import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { adminUsersApi, queryKeys } from '@/lib/api'
import type {
  AdminUserResponse,
  AssignRoleRequest,
  InviteUserRequest,
  ListAdminUsersParams,
} from '@/lib/api'

export function useAdminUsers(params?: ListAdminUsersParams) {
  return useQuery({
    queryKey: queryKeys.adminUsers.list(params as Record<string, unknown> | undefined),
    queryFn: () => adminUsersApi.list(params),
    staleTime: 30_000,
  })
}

export function useAdminUser(id: string) {
  return useQuery({
    queryKey: queryKeys.adminUsers.detail(id),
    queryFn: () => adminUsersApi.getById(id),
    staleTime: 30_000,
    enabled: !!id,
  })
}

/**
 * Account-state transitions. Every endpoint answers with the updated user, so the
 * detail entry is seeded directly and only the list projections are refetched.
 *
 * Deliberately not optimistic: disable/lock/role-change can be refused by the
 * backend (409 — last active administrator), so the row must reflect server truth
 * rather than flipping and then reverting.
 */
function useUserStateMutation(mutationFn: (id: string) => Promise<AdminUserResponse>) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn,
    onSuccess: (updated: AdminUserResponse) => {
      qc.setQueryData(queryKeys.adminUsers.detail(updated.id), updated)
      void qc.invalidateQueries({ queryKey: queryKeys.adminUsers.all() })
    },
  })
}

export function useEnableUser() {
  return useUserStateMutation((id) => adminUsersApi.enable(id))
}

export function useDisableUser() {
  return useUserStateMutation((id) => adminUsersApi.disable(id))
}

export function useLockUser() {
  return useUserStateMutation((id) => adminUsersApi.lock(id))
}

export function useUnlockUser() {
  return useUserStateMutation((id) => adminUsersApi.unlock(id))
}

export function useAssignUserRole(id: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: AssignRoleRequest) => adminUsersApi.assignRole(id, data),
    onSuccess: (updated: AdminUserResponse) => {
      qc.setQueryData(queryKeys.adminUsers.detail(updated.id), updated)
      void qc.invalidateQueries({ queryKey: queryKeys.adminUsers.all() })
    },
  })
}

/**
 * Invites a new privileged user. The account is created in INVITED state, so the
 * listing changes — but the endpoint returns only a message envelope, not the user.
 */
export function useInviteUser() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: InviteUserRequest) => adminUsersApi.invite(data),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: queryKeys.adminUsers.all() })
    },
  })
}
