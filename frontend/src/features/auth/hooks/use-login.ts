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
      const user = await authApi.me()
      return { tokens, user }
    },
    onSuccess: ({ tokens, user }) => {
      setAuth(tokens.accessToken, tokens.refreshToken, user)
      navigate(from, { replace: true })
    },
  })
}
