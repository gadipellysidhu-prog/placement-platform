import { useMutation } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { authApi } from '@/lib/api/auth.api'
import { useAuthStore } from '@/stores/auth.store'
import { ROUTES } from '@/constants/routes'
import type { RegisterFormValues } from '../schemas/auth.schemas'

export function useRegister() {
  const { setAuth } = useAuthStore()
  const navigate = useNavigate()

  return useMutation({
    mutationFn: async (values: RegisterFormValues) => {
      const tokens = await authApi.register({ ...values, role: 'ROLE_STUDENT' })
      const user = await authApi.me()
      return { tokens, user }
    },
    onSuccess: ({ tokens, user }) => {
      setAuth(tokens.accessToken, tokens.refreshToken, user)
      navigate(ROUTES.DASHBOARD, { replace: true })
    },
  })
}
