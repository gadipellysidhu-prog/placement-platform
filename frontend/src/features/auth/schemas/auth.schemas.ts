import { z } from 'zod'
import { VALIDATION_MESSAGES } from '@/shared/forms/validation-messages'

export const loginSchema = z.object({
  email: z.string().min(1, VALIDATION_MESSAGES.required('Email')).email(VALIDATION_MESSAGES.email),
  password: z.string().min(1, VALIDATION_MESSAGES.required('Password')),
})

export type LoginFormValues = z.infer<typeof loginSchema>

export const registerSchema = z.object({
  email: z.string().min(1, VALIDATION_MESSAGES.required('Email')).email(VALIDATION_MESSAGES.email),
  password: z
    .string()
    .min(8, VALIDATION_MESSAGES.minLength(8, 'Password'))
    .max(128, VALIDATION_MESSAGES.maxLength(128, 'Password')),
})

export type RegisterFormValues = z.infer<typeof registerSchema>

export const forgotPasswordSchema = z.object({
  email: z.string().min(1, VALIDATION_MESSAGES.required('Email')).email(VALIDATION_MESSAGES.email),
})

export type ForgotPasswordFormValues = z.infer<typeof forgotPasswordSchema>

/**
 * Password rule shared by every "set a new password" flow (reset, invitation).
 * Mirrors the backend contract (@Size(min = 8, max = 128) on ResetPasswordRequest
 * and AcceptInvitationRequest) and the registration rule above.
 */
const newPasswordField = z
  .string()
  .min(8, VALIDATION_MESSAGES.minLength(8, 'Password'))
  .max(128, VALIDATION_MESSAGES.maxLength(128, 'Password'))

/**
 * Builds a { password, confirmPassword } schema whose two fields must match.
 * Reused by the reset-password and accept-invitation forms so the confirmation
 * rule lives in exactly one place.
 */
function passwordWithConfirmation() {
  return z
    .object({
      password: newPasswordField,
      confirmPassword: z.string().min(1, VALIDATION_MESSAGES.required('Password confirmation')),
    })
    .refine((data) => data.password === data.confirmPassword, {
      message: VALIDATION_MESSAGES.passwordMatch,
      path: ['confirmPassword'],
    })
}

export const resetPasswordSchema = passwordWithConfirmation()
export type ResetPasswordFormValues = z.infer<typeof resetPasswordSchema>

export const acceptInvitationSchema = passwordWithConfirmation()
export type AcceptInvitationFormValues = z.infer<typeof acceptInvitationSchema>
