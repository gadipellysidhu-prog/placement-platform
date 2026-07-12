import { useEffect, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { AlertCircle, CheckCircle2, MailCheck } from 'lucide-react'
import { Button } from '@/shared/ui/button'
import { Input } from '@/shared/ui/input'
import { FormField } from '@/shared/forms/form-field'
import { Spinner } from '@/shared/ui/spinner'
import { Alert } from '@/shared/ui/alert'
import { ROUTES } from '@/constants/routes'
import { authApi } from '@/lib/api/auth.api'
import { normalizeApiError } from '@/lib/api/error'
import { forgotPasswordSchema, type ForgotPasswordFormValues } from '../schemas/auth.schemas'

type VerificationStatus = 'verifying' | 'success' | 'error'

/**
 * Consumes the single-use email-verification token embedded in the backend's
 * "Verify your email" link ({FRONTEND_BASE_URL}/verify-email?token=...).
 *
 * Verification fires automatically once on mount. Because the token is single-use,
 * a duplicate request (e.g. React StrictMode's double-invoke in development) would
 * consume the token and then fail replay protection — so a ref guard ensures the
 * confirm call is dispatched exactly once.
 *
 * The outcome is held in local component state (not a react-query mutation): under
 * StrictMode, react-query drops the fire-on-mount mutation's result when it
 * simulates an unmount/remount, which would leave the page stuck on "Verifying…".
 * Plain state survives that cycle, so the resolved status always renders.
 */
export default function VerifyEmailPage() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token') ?? ''
  const dispatched = useRef(false)
  const [status, setStatus] = useState<VerificationStatus>('verifying')
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  useEffect(() => {
    if (dispatched.current || !token) return
    dispatched.current = true
    authApi
      .confirmEmailVerification({ token })
      .then(() => setStatus('success'))
      .catch((err) => {
        setErrorMessage(normalizeApiError(err).message)
        setStatus('error')
      })
  }, [token])

  // ── No token in the URL ──────────────────────────────────────────────────
  if (!token) {
    return (
      <div className="space-y-6">
        <StatusHeader
          icon={<AlertCircle className="h-10 w-10 text-destructive" aria-hidden="true" />}
          title="Invalid verification link"
          description="This link is missing its verification token. Request a new one below."
        />
        <ResendVerificationForm />
        <BackToLogin />
      </div>
    )
  }

  // ── Success ──────────────────────────────────────────────────────────────
  if (status === 'success') {
    return (
      <div className="space-y-6">
        <StatusHeader
          icon={<CheckCircle2 className="h-10 w-10 text-green-500" aria-hidden="true" />}
          title="Email verified"
          description="Your email address has been verified. You can now sign in to your account."
        />
        <Button asChild className="w-full">
          <Link to={ROUTES.LOGIN}>Continue to sign in</Link>
        </Button>
      </div>
    )
  }

  // ── Failure — invalid or expired token ───────────────────────────────────
  if (status === 'error') {
    return (
      <div className="space-y-6">
        <StatusHeader
          icon={<AlertCircle className="h-10 w-10 text-destructive" aria-hidden="true" />}
          title="Verification failed"
          description={errorMessage || 'This verification link is invalid or has expired.'}
        />
        <ResendVerificationForm />
        <BackToLogin />
      </div>
    )
  }

  // ── Verifying (default) ───────────────────────────────────────────────────
  return (
    <div className="space-y-6">
      <StatusHeader
        icon={<Spinner size="lg" label="Verifying your email" />}
        title="Verifying your email…"
        description="Please wait while we confirm your email address."
      />
    </div>
  )
}

/** Shared centered icon + heading + description block. */
function StatusHeader({
  icon,
  title,
  description,
}: {
  icon: React.ReactNode
  title: string
  description: string
}) {
  return (
    <div className="flex flex-col items-center gap-3 text-center">
      {icon}
      <div className="space-y-1">
        <h2 className="text-xl font-semibold text-foreground">{title}</h2>
        <p className="text-sm text-muted-foreground">{description}</p>
      </div>
    </div>
  )
}

function BackToLogin() {
  return (
    <Link
      to={ROUTES.LOGIN}
      className="block text-center text-sm font-medium text-primary underline-offset-4 hover:underline"
    >
      Back to sign in
    </Link>
  )
}

/**
 * Requests a fresh verification email. The backend never reveals whether the
 * account exists or is already verified (always 202), so the confirmation copy
 * is deliberately generic.
 */
function ResendVerificationForm() {
  const resend = useMutation({
    mutationFn: (email: string) => authApi.resendVerification(email),
  })

  const form = useForm<ForgotPasswordFormValues>({
    resolver: zodResolver(forgotPasswordSchema),
    defaultValues: { email: '' },
  })

  if (resend.isSuccess) {
    return (
      <Alert className="flex items-start gap-2">
        <MailCheck className="mt-0.5 h-4 w-4 shrink-0 text-green-500" />
        <span className="text-sm">
          If your email is registered and not yet verified, a new verification link is on its way.
        </span>
      </Alert>
    )
  }

  const errorMessage = resend.isError ? normalizeApiError(resend.error).message : null

  return (
    <form
      onSubmit={form.handleSubmit((values) => resend.mutate(values.email))}
      className="space-y-4"
      noValidate
    >
      {errorMessage && (
        <Alert variant="destructive" className="flex items-start gap-2">
          <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
          <span className="text-sm">{errorMessage}</span>
        </Alert>
      )}

      <FormField label="Email address" error={form.formState.errors.email?.message} required>
        <Input
          type="email"
          autoComplete="email"
          placeholder="you@university.edu"
          aria-required="true"
          disabled={resend.isPending}
          {...form.register('email')}
        />
      </FormField>

      <Button type="submit" className="w-full" disabled={resend.isPending}>
        {resend.isPending ? (
          <>
            <Spinner size="sm" className="mr-2" />
            Sending…
          </>
        ) : (
          'Resend verification email'
        )}
      </Button>
    </form>
  )
}
