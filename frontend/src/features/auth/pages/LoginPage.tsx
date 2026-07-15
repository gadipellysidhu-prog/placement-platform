import { Link, useLocation } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { AlertCircle, CheckCircle2 } from 'lucide-react'
import { Button } from '@/shared/ui/button'
import { Input } from '@/shared/ui/input'
import { PasswordInput } from '@/shared/ui/password-input'
import { FormField } from '@/shared/forms/form-field'
import { Spinner } from '@/shared/ui/spinner'
import { Alert } from '@/shared/ui/alert'
import { ROUTES } from '@/constants/routes'
import { useLogin } from '../hooks/use-login'
import { loginSchema, type LoginFormValues } from '../schemas/auth.schemas'
import { normalizeApiError } from '@/lib/api/error'

export default function LoginPage() {
  const { mutate: login, isPending, error } = useLogin()

  const form = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: '', password: '' },
  })

  const errorMessage = error ? normalizeApiError(error).message : null

  // A notice passed via navigation state (e.g. after successful registration,
  // which does not auto-authenticate and redirects here to sign in).
  const location = useLocation()
  const notice = (location.state as { notice?: string } | null)?.notice ?? null

  return (
    <div className="space-y-6">
      <div className="space-y-1">
        <h2 className="text-xl font-semibold text-foreground">Sign in</h2>
        <p className="text-sm text-muted-foreground">
          Enter your credentials to access your account
        </p>
      </div>

      {notice && !errorMessage && (
        <Alert className="flex items-start gap-2">
          <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0" />
          <span className="text-sm">{notice}</span>
        </Alert>
      )}

      {errorMessage && (
        <Alert variant="destructive" className="flex items-start gap-2">
          <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
          <span className="text-sm">{errorMessage}</span>
        </Alert>
      )}

      <form
        onSubmit={form.handleSubmit((values) => login(values))}
        className="space-y-4"
        noValidate
      >
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

        <FormField label="Password" error={form.formState.errors.password?.message} required>
          <PasswordInput
            autoComplete="current-password"
            placeholder="Enter your password"
            aria-required="true"
            disabled={isPending}
            {...form.register('password')}
          />
        </FormField>

        <div className="flex items-center justify-end">
          <Link
            to={ROUTES.FORGOT_PASSWORD}
            className="text-sm text-primary underline-offset-4 hover:underline"
          >
            Forgot password?
          </Link>
        </div>

        <Button type="submit" className="w-full" disabled={isPending}>
          {isPending ? (
            <>
              <Spinner size="sm" className="mr-2" />
              Signing in…
            </>
          ) : (
            'Sign in'
          )}
        </Button>
      </form>

      <p className="text-center text-sm text-muted-foreground">
        Don&apos;t have an account?{' '}
        <Link
          to={ROUTES.REGISTER}
          className="font-medium text-primary underline-offset-4 hover:underline"
        >
          Create one
        </Link>
      </p>
    </div>
  )
}
