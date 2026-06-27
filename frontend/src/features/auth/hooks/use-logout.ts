import { useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { authApi } from '@/lib/api/auth.api'
import { useAuthStore } from '@/stores/auth.store'
import { queryClient } from '@/lib/query-client'
import { ROUTES } from '@/constants/routes'

export function useLogout() {
  const { clearAuth } = useAuthStore()
  const navigate = useNavigate()

  return useCallback(async () => {
    const refreshToken = localStorage.getItem('__refresh_token__')
    try {
      if (refreshToken) {
        await authApi.logout({ refreshToken })
      }
    } catch {
      // Ignore server errors on logout — always clean up client state
    } finally {
      clearAuth()
      queryClient.clear()
      navigate(ROUTES.LOGIN, { replace: true })
    }
  }, [clearAuth, navigate])
}
