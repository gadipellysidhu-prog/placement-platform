import { useMutation } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { authApi } from '@/lib/api/auth.api'
import { ROUTES } from '@/constants/routes'
import type { RegisterFormValues } from '../schemas/auth.schemas'

/**
 * Registers a new student account.
 *
 * The backend issues NO JWT on registration — the account starts unverified and
 * cannot sign in until its email is verified / an administrator approves it. So
 * this hook must NOT attempt to auto-authenticate: it submits the registration and
 * sends the user to the login page with the backend's confirmation message. The
 * previous implementation assumed registration returned tokens, stored `undefined`,
 * and then failed the follow-up `/me` call — which surfaced as a bogus "tokens not
 * verified" error and a forced redirect to /login.
 */
export function useRegister() {
  const navigate = useNavigate()

  return useMutation({
    mutationFn: (values: RegisterFormValues) =>
      authApi.register({ ...values, role: 'ROLE_STUDENT' }),
    onSuccess: (result) => {
      navigate(ROUTES.LOGIN, { replace: true, state: { notice: result.message } })
    },
  })
}
