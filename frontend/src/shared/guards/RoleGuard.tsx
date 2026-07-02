import type { ReactNode } from 'react'
import { usePermission } from '@/shared/hooks/use-permission'
import type { Role } from '@/types/auth'

interface RoleGuardProps {
  /** Render children only if the user has at least this role. */
  minimumRole: Role
  /** Optional fallback rendered when the condition is not met. */
  fallback?: ReactNode
  children: ReactNode
}

/**
 * Conditionally renders children based on the user's role.
 * Does NOT replace route-level guards — use for UI-only conditional rendering.
 */
export function RoleGuard({ minimumRole, fallback = null, children }: RoleGuardProps) {
  const { hasRole } = usePermission()
  return hasRole(minimumRole) ? <>{children}</> : <>{fallback}</>
}
