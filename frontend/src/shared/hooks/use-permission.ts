import { useAuthStore } from '@/stores/auth.store'
import { ROLES } from '@/constants/roles'
import type { Role } from '@/types/auth'

interface UsePermissionReturn {
  /** True if the current user has at least this role (respects hierarchy: ADMIN > OFFICER > STUDENT). */
  hasRole: (role: Role) => boolean
  isAdmin: boolean
  isOfficer: boolean
  isStudent: boolean
  role: Role | null
}

const ROLE_RANK: Record<Role, number> = {
  ROLE_STUDENT: 1,
  ROLE_PLACEMENT_OFFICER: 2,
  ROLE_ADMIN: 3,
}

/** Returns permission helpers derived from the authenticated user's role. */
export function usePermission(): UsePermissionReturn {
  const user = useAuthStore((s) => s.user)
  const role = user?.role ?? null

  function hasRole(required: Role): boolean {
    if (!role) return false
    return ROLE_RANK[role] >= ROLE_RANK[required]
  }

  return {
    hasRole,
    isAdmin: role === ROLES.ADMIN,
    isOfficer: hasRole(ROLES.PLACEMENT_OFFICER),
    isStudent: hasRole(ROLES.STUDENT),
    role,
  }
}
