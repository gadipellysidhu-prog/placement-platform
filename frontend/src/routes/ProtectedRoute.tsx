import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuthStore } from '@/stores/auth.store'
import { ROUTES } from '@/constants/routes'
import type { Role } from '@/types/auth'

interface ProtectedRouteProps {
  /** If provided, user must have exactly this role or will be sent to /403. */
  requiredRole?: Role
}

/**
 * Guards authenticated routes. Unauthenticated → /login (with return URL).
 * Wrong role → /403.
 */
export function ProtectedRoute({ requiredRole }: ProtectedRouteProps) {
  const { isAuthenticated, user } = useAuthStore()
  const location = useLocation()

  if (!isAuthenticated) {
    return <Navigate to={ROUTES.LOGIN} state={{ from: location }} replace />
  }

  if (requiredRole && user?.role !== requiredRole) {
    return <Navigate to={ROUTES.FORBIDDEN} replace />
  }

  return <Outlet />
}
