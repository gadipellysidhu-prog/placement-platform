import { useMutation } from '@tanstack/react-query'
import { useNavigate, useLocation } from 'react-router-dom'
import { authApi } from '@/lib/api/auth.api'
import { useAuthStore } from '@/stores/auth.store'
import { ROUTES } from '@/constants/routes'
import type { LoginFormValues } from '../schemas/auth.schemas'

export function useLogin() {
  const { setAuth } = useAuthStore()
  const navigate = useNavigate()
  const location = useLocation()

  const from =
    (location.state as { from?: { pathname: string } } | null)?.from?.pathname ?? ROUTES.DASHBOARD

  return useMutation({
    mutationFn: async (values: LoginFormValues) => {
      const tokens = await authApi.login(values)
      // Store access token in sessionStorage BEFORE calling /me so the
      // Axios request interceptor can attach the Bearer header.
      sessionStorage.setItem('__access_token__', tokens.accessToken)
      localStorage.setItem('__refresh_token__', tokens.refreshToken)
      const user = await authApi.me()
      return { tokens, user }
    },
    onSuccess: ({ tokens, user }) => {
      setAuth(tokens.accessToken, tokens.refreshToken, user)
      navigate(from, { replace: true })
    },
    onError: () => {
      // Clean up tokens written during mutationFn if anything fails
      sessionStorage.removeItem('__access_token__')
      localStorage.removeItem('__refresh_token__')
    },
  })
}
