import type { ReactNode } from 'react'
import { Spinner } from '@/shared/ui/spinner'
import { useSessionRestore } from './hooks/use-session-restore'

interface SessionProviderProps {
  children: ReactNode
}

/**
 * Wraps the app to silently restore the session on mount.
 * When `isAuthenticated` is true (persisted from localStorage) but no access token
 * exists in sessionStorage (cleared on browser close), it calls /auth/refresh before
 * rendering any protected content — preventing flashes of unauthenticated UI.
 */
export function SessionProvider({ children }: SessionProviderProps) {
  const status = useSessionRestore()

  if (status === 'restoring') {
    return (
      <div
        className="flex min-h-screen items-center justify-center bg-background"
        aria-live="polite"
        aria-label="Restoring session"
      >
        <Spinner size="lg" />
      </div>
    )
  }

  return <>{children}</>
}
