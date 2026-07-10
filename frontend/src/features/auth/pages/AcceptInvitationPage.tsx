import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { AlertCircle } from 'lucide-react'
import { Button } from '@/shared/ui/button'
import { PasswordInput } from '@/shared/ui/password-input'
import { FormField } from '@/shared/forms/form-field'
import { Spinner } from '@/shared/ui/spinner'
import { Alert } from '@/shared/ui/alert'
import { ROUTES } from '@/constants/routes'
import { authApi } from '@/lib/api/auth.api'
import { normalizeApiError } from '@/lib/api/error'
import { useToast } from '@/shared/hooks/use-toast'
import { acceptInvitationSchema, type AcceptInvitationFormValues } from '../schemas/auth.schemas'

/**
 * Accepts a privileged-user invitation using the single-use token embedded in the
 * backend's invitation email ({FRONTEND_BASE_URL}/accept-invitation?token=...).
 * Setting the password activates the invited account (status INVITED → ACTIVE,
 * email verified), after which the user signs in normally.
 */
export default function AcceptInvitationPage() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token') ?? ''
  const navigate = useNavigate()
  const toast = useToast()

  const accept = useMutation({
    mutationFn: (newPassword: string) => authApi.acceptInvitation({ token, newPassword }),
    onSuccess: () => {
      toast.success('Invitation accepted', 'Your account is active — please sign in.')
      navigate(ROUTES.LOGIN, { replace: true })
    },
  })

  const form = useForm<AcceptInvitationFormValues>({
    resolver: zodResolver(acceptInvitationSchema),
    defaultValues: { password: '', confirmPassword: '' },
  })

  // ── Missing token — cannot proceed ───────────────────────────────────────
  if (!token) {
    return (
      <div className="space-y-6">
        <div className="flex flex-col items-center gap-3 text-center">
          <AlertCircle className="h-10 w-10 text-destructive" aria-hidden="true" />
          <div className="space-y-1">
            <h2 className="text-xl font-semibold text-foreground">Invalid invitation link</h2>
            <p className="text-sm text-muted-foreground">
              This invitation link is missing its token. Please use the link from your invitation
              email, or contact your administrator for a new invite.
            </p>
          </div>
        </div>
        <Link
          to={ROUTES.LOGIN}
          className="block text-center text-sm font-medium text-primary underline-offset-4 hover:underline"
        >
          Back to sign in
        </Link>
      </div>
    )
  }

  const errorMessage = accept.isError ? normalizeApiError(accept.error).message : null

  return (
    <div className="space-y-6">
      <div className="space-y-1">
        <h2 className="text-xl font-semibold text-foreground">Accept your invitation</h2>
        <p className="text-sm text-muted-foreground">
          Set a password to activate your account and get started.
        </p>
      </div>

      {errorMessage && (
        <Alert variant="destructive" className="flex flex-col gap-2">
          <div className="flex items-start gap-2">
            <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
            <span className="text-sm">{errorMessage}</span>
          </div>
          <span className="text-sm">
            This invitation may have expired or already been used. Contact your administrator for a
            new invite.
          </span>
        </Alert>
      )}

      <form
        onSubmit={form.handleSubmit((values) => accept.mutate(values.password))}
        className="space-y-4"
        noValidate
      >
        <FormField
          label="Password"
          error={form.formState.errors.password?.message}
          required
          hint="At least 8 characters"
        >
          <PasswordInput
            autoComplete="new-password"
            placeholder="Create a strong password"
            aria-required="true"
            disabled={accept.isPending}
            {...form.register('password')}
          />
        </FormField>

        <FormField
          label="Confirm password"
          error={form.formState.errors.confirmPassword?.message}
          required
        >
          <PasswordInput
            autoComplete="new-password"
            placeholder="Re-enter your password"
            aria-required="true"
            disabled={accept.isPending}
            {...form.register('confirmPassword')}
          />
        </FormField>

        <Button type="submit" className="w-full" disabled={accept.isPending}>
          {accept.isPending ? (
            <>
              <Spinner size="sm" className="mr-2" />
              Activating…
            </>
          ) : (
            'Activate account'
          )}
        </Button>
      </form>

      <p className="text-center text-sm text-muted-foreground">
        Already have an account?{' '}
        <Link
          to={ROUTES.LOGIN}
          className="font-medium text-primary underline-offset-4 hover:underline"
        >
          Sign in
        </Link>
      </p>
    </div>
  )
}
