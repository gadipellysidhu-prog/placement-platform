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
