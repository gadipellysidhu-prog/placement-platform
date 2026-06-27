import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { AlertCircle, CheckCircle2 } from 'lucide-react'
import { Button } from '@/shared/ui/button'
import { Input } from '@/shared/ui/input'
import { FormField } from '@/shared/forms/form-field'
import { Spinner } from '@/shared/ui/spinner'
import { Alert } from '@/shared/ui/alert'
import { ROUTES } from '@/constants/routes'
import { authApi } from '@/lib/api/auth.api'
import { normalizeApiError } from '@/lib/api/error'
import { forgotPasswordSchema, type ForgotPasswordFormValues } from '../schemas/auth.schemas'

export default function ForgotPasswordPage() {
  const [submitted, setSubmitted] = useState(false)
  const [isPending, setIsPending] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  const form = useForm<ForgotPasswordFormValues>({
    resolver: zodResolver(forgotPasswordSchema),
    defaultValues: { email: '' },
  })

  async function handleSubmit(values: ForgotPasswordFormValues) {
    setIsPending(true)
    setErrorMessage(null)
    try {
      await authApi.forgotPassword(values.email)
      setSubmitted(true)
    } catch (err) {
      setErrorMessage(normalizeApiError(err).message)
    } finally {
      setIsPending(false)
    }
  }

  if (submitted) {
    return (
      <div className="space-y-6">
        <div className="flex flex-col items-center gap-3 text-center">
          <CheckCircle2 className="h-10 w-10 text-green-500" aria-hidden="true" />
          <div className="space-y-1">
            <h2 className="text-xl font-semibold text-foreground">Check your inbox</h2>
            <p className="text-sm text-muted-foreground">
              If your email is registered, you will receive a password reset link shortly.
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

  return (
    <div className="space-y-6">
      <div className="space-y-1">
        <h2 className="text-xl font-semibold text-foreground">Reset password</h2>
        <p className="text-sm text-muted-foreground">
          Enter your email and we&apos;ll send you a reset link
        </p>
      </div>

      {errorMessage && (
        <Alert variant="destructive" className="flex items-start gap-2">
          <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
          <span className="text-sm">{errorMessage}</span>
        </Alert>
      )}

      <form onSubmit={form.handleSubmit(handleSubmit)} className="space-y-4" noValidate>
        <FormField label="Email address" error={form.formState.errors.email?.message} required>
          <Input
            type="email"
            autoComplete="email"
            placeholder="you@university.edu"
            aria-required="true"
            disabled={isPending}
            {...form.register('email')}
          />
        </FormField>

        <Button type="submit" className="w-full" disabled={isPending}>
          {isPending ? (
            <>
              <Spinner size="sm" className="mr-2" />
              Sending…
            </>
          ) : (
            'Send reset link'
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
