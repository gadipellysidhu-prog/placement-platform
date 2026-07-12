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
import { resetPasswordSchema, type ResetPasswordFormValues } from '../schemas/auth.schemas'

/**
 * Completes a password reset using the single-use token embedded in the backend's
 * "Reset your password" link ({FRONTEND_BASE_URL}/reset-password?token=...).
 * On success the backend revokes every existing session, so the user is sent back
 * to the login page to sign in with the new password.
 */
export default function ResetPasswordPage() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token') ?? ''
  const navigate = useNavigate()
  const toast = useToast()

  const reset = useMutation({
    mutationFn: (newPassword: string) => authApi.resetPassword({ token, newPassword }),
    onSuccess: () => {
      toast.success('Password reset', 'Please sign in with your new password.')
      navigate(ROUTES.LOGIN, { replace: true })
    },
  })

  const form = useForm<ResetPasswordFormValues>({
    resolver: zodResolver(resetPasswordSchema),
    defaultValues: { password: '', confirmPassword: '' },
  })

  // ── Missing token — cannot proceed ───────────────────────────────────────
  if (!token) {
    return (
      <div className="space-y-6">
        <div className="flex flex-col items-center gap-3 text-center">
          <AlertCircle className="h-10 w-10 text-destructive" aria-hidden="true" />
          <div className="space-y-1">
            <h2 className="text-xl font-semibold text-foreground">Invalid reset link</h2>
            <p className="text-sm text-muted-foreground">
              This password reset link is missing its token. Request a new one to continue.
            </p>
          </div>
        </div>
        <Button asChild className="w-full">
          <Link to={ROUTES.FORGOT_PASSWORD}>Request a new reset link</Link>
        </Button>
      </div>
    )
  }

  const errorMessage = reset.isError ? normalizeApiError(reset.error).message : null

  return (
    <div className="space-y-6">
      <div className="space-y-1">
        <h2 className="text-xl font-semibold text-foreground">Set a new password</h2>
        <p className="text-sm text-muted-foreground">Choose a new password for your account.</p>
      </div>

      {errorMessage && (
        <Alert variant="destructive" className="flex flex-col gap-2">
          <div className="flex items-start gap-2">
            <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
            <span className="text-sm">{errorMessage}</span>
          </div>
          <Link
            to={ROUTES.FORGOT_PASSWORD}
            className="text-sm font-medium underline underline-offset-4"
          >
            Request a new reset link
          </Link>
        </Alert>
      )}

      <form
        onSubmit={form.handleSubmit((values) => reset.mutate(values.password))}
        className="space-y-4"
        noValidate
      >
        <FormField
          label="New password"
          error={form.formState.errors.password?.message}
          required
          hint="At least 8 characters"
        >
          <PasswordInput
            autoComplete="new-password"
            placeholder="Create a strong password"
            aria-required="true"
            disabled={reset.isPending}
            {...form.register('password')}
          />
        </FormField>

        <FormField
          label="Confirm new password"
          error={form.formState.errors.confirmPassword?.message}
          required
        >
          <PasswordInput
            autoComplete="new-password"
            placeholder="Re-enter your new password"
            aria-required="true"
            disabled={reset.isPending}
            {...form.register('confirmPassword')}
          />
        </FormField>

        <Button type="submit" className="w-full" disabled={reset.isPending}>
          {reset.isPending ? (
            <>
              <Spinner size="sm" className="mr-2" />
              Resetting…
            </>
          ) : (
            'Reset password'
          )}
        </Button>
      </form>

      <p className="text-center text-sm text-muted-foreground">
        Remember your password?{' '}
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
