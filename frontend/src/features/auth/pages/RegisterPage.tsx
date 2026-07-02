import { Link } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { AlertCircle } from 'lucide-react'
import { Button } from '@/shared/ui/button'
import { Input } from '@/shared/ui/input'
import { PasswordInput } from '@/shared/ui/password-input'
import { FormField } from '@/shared/forms/form-field'
import { Spinner } from '@/shared/ui/spinner'
import { Alert } from '@/shared/ui/alert'
import { ROUTES } from '@/constants/routes'
import { useRegister } from '../hooks/use-register'
import { registerSchema, type RegisterFormValues } from '../schemas/auth.schemas'
import { normalizeApiError } from '@/lib/api/error'

export default function RegisterPage() {
  const { mutate: register, isPending, error } = useRegister()

  const form = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: { email: '', password: '' },
  })

  const errorMessage = error ? normalizeApiError(error).message : null

  return (
    <div className="space-y-6">
      <div className="space-y-1">
        <h2 className="text-xl font-semibold text-foreground">Create account</h2>
        <p className="text-sm text-muted-foreground">
          Register as a student to start your placement journey
        </p>
      </div>

      {errorMessage && (
        <Alert variant="destructive" className="flex items-start gap-2">
          <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
          <span className="text-sm">{errorMessage}</span>
        </Alert>
      )}

      <form
        onSubmit={form.handleSubmit((values) => register(values))}
        className="space-y-4"
        noValidate
      >
        <FormField
          label="Email address"
          error={form.formState.errors.email?.message}
          required
          hint="Use your university email address"
        >
          <Input
            type="email"
            autoComplete="email"
            placeholder="you@university.edu"
            aria-required="true"
            disabled={isPending}
            {...form.register('email')}
          />
        </FormField>

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
            disabled={isPending}
            {...form.register('password')}
          />
        </FormField>

        <Button type="submit" className="w-full" disabled={isPending}>
          {isPending ? (
            <>
              <Spinner size="sm" className="mr-2" />
              Creating account…
            </>
          ) : (
            'Create account'
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
