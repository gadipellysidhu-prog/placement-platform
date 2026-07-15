import { z } from 'zod'
import type { CreateOfferRequest } from '@/lib/api'

/**
 * Client validation for officer offer creation. Mirrors the backend
 * {@code OfferCreateRequest} Bean Validation EXACTLY — no invented rules:
 *
 *  - applicationId @NotNull           → supplied from context (not a form field)
 *  - ctc           @DecimalMin("0.0") → optional; when present must be a number ≥ 0
 *  - joiningDate   (none)             → optional ISO date (yyyy-MM-dd)
 *
 * Both money and date fields are bound to string controls, so the schema validates
 * strings and defers coercion to {@link toCreateOfferPayload}.
 */
const optionalCtc = z
  .string()
  .refine((v) => v.trim() === '' || (Number.isFinite(Number(v)) && Number(v) >= 0), {
    message: 'Must be a number of 0 or greater',
  })

export const offerFormSchema = z.object({
  ctc: optionalCtc,
  joiningDate: z.string().optional(),
})

export type OfferFormValues = z.infer<typeof offerFormSchema>

function toNumber(value: string | undefined): number | undefined {
  const trimmed = (value ?? '').trim()
  return trimmed === '' ? undefined : Number(trimmed)
}

function toText(value: string | undefined): string | undefined {
  const trimmed = (value ?? '').trim()
  return trimmed === '' ? undefined : trimmed
}

/** Validated form values + the target application → POST /api/offers payload. */
export function toCreateOfferPayload(
  applicationId: string,
  values: OfferFormValues,
): CreateOfferRequest {
  return {
    applicationId,
    ctc: toNumber(values.ctc),
    joiningDate: toText(values.joiningDate),
  }
}
