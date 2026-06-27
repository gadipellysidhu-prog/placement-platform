import { useEffect, useState } from 'react'
import { authApi } from '@/lib/api/auth.api'
import { useAuthStore } from '@/stores/auth.store'

type SessionStatus = 'restoring' | 'ready'

/**
 * On mount, detects when the Zustand store shows isAuthenticated (rehydrated from
 * localStorage) but no access token is in sessionStorage (cleared on tab/browser close).
 * Silently calls /auth/refresh to obtain a new access token before rendering children.
 */
export function useSessionRestore(): SessionStatus {
  const { isAuthenticated, setAuth, setAccessToken, clearAuth, user } = useAuthStore()
  const [status, setStatus] = useState<SessionStatus>('restoring')

  useEffect(() => {
    const accessToken = sessionStorage.getItem('__access_token__')
    const refreshToken = localStorage.getItem('__refresh_token__')

    const alreadyHasToken = isAuthenticated && !!accessToken
    const canRestore = isAuthenticated && !accessToken && !!refreshToken

    if (alreadyHasToken || !isAuthenticated) {
      setStatus('ready')
      return
    }

    if (!canRestore) {
      clearAuth()
      setStatus('ready')
      return
    }

    let cancelled = false

    authApi
      .refresh({ refreshToken: refreshToken! })
      .then(async (tokens) => {
        if (cancelled) return
        if (user) {
          setAuth(tokens.accessToken, tokens.refreshToken, user)
        } else {
          setAccessToken(tokens.accessToken)
          localStorage.setItem('__refresh_token__', tokens.refreshToken)
          try {
            const freshUser = await authApi.me()
            if (!cancelled) {
              setAuth(tokens.accessToken, tokens.refreshToken, freshUser)
            }
          } catch {
            if (!cancelled) clearAuth()
          }
        }
      })
      .catch(() => {
        if (!cancelled) clearAuth()
      })
      .finally(() => {
        if (!cancelled) setStatus('ready')
      })

    return () => {
      cancelled = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return status
}
