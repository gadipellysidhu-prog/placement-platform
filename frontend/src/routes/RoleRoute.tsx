import { Navigate, Outlet } from 'react-router-dom'
import { usePermission } from '@/shared/hooks/use-permission'
import { ROUTES } from '@/constants/routes'
import type { Role } from '@/types/auth'

interface RoleRouteProps {
  /** User must have at least this role (hierarchy-aware). */
  minimumRole: Role
}

/**
 * Protects routes by minimum role level.
 * Respects the role hierarchy: ADMIN > PLACEMENT_OFFICER > STUDENT.
 */
export function RoleRoute({ minimumRole }: RoleRouteProps) {
  const { hasRole } = usePermission()

  if (!hasRole(minimumRole)) {
    return <Navigate to={ROUTES.FORBIDDEN} replace />
  }

  return <Outlet />
}
