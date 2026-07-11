import { z } from 'zod'
import type { CreateJobPostingRequest, UpdateJobPostingRequest } from '@/lib/api'

/**
 * Client validation for job-posting create/edit. Mirrors the backend Bean Validation
 * annotations EXACTLY — no invented rules:
 *
 *  - companyId       @NotNull            → required (create only; fixed on update)
 *  - title           @NotBlank @Size 255 → non-empty, max 255 chars
 *  - description     (none)              → optional, unbounded
 *  - ctcMin/ctcMax   @DecimalMin("0.0")  → optional, ≥ 0 when present
 *  - applicationDeadline (none)          → optional ISO date (yyyy-MM-dd)
 *  - offerLimit      @Min(1) int         → required integer ≥ 1
 *
 * The backend does NOT constrain ctcMax ≥ ctcMin, so neither do we.
 *
 * Every field is bound to a string-valued control, so the schema validates strings and
 * leaves numeric/optional coercion to `toCreatePayload` / `toUpdatePayload`. Keeping the
 * schema transform-free means `z.input === z.output`, which the installed resolver
 * version types correctly (it has no separate transformed-output generic).
 */

/** Optional non-negative money string: '' passes; otherwise must be a finite number ≥ 0. */
const optionalCtc = z
  .string()
  .refine((v) => v.trim() === '' || (Number.isFinite(Number(v)) && Number(v) >= 0), {
    message: 'Must be a number of 0 or greater',
  })

export const jobPostingFormSchema = z.object({
  companyId: z.string().min(1, 'Company is required'),
  title: z
    .string()
    .trim()
    .min(1, 'Title is required')
    .max(255, 'Title must be at most 255 characters'),
  description: z.string().optional(),
  ctcMin: optionalCtc,
  ctcMax: optionalCtc,
  applicationDeadline: z.string().optional(),
  offerLimit: z
    .string()
    .trim()
    .min(1, 'Offer limit is required')
    .refine((v) => /^\d+$/.test(v), 'Offer limit must be a whole number')
    .refine((v) => Number(v) >= 1, 'Offer limit must be at least 1'),
})

export type JobPostingFormValues = z.infer<typeof jobPostingFormSchema>

/** '' / whitespace → undefined, otherwise a parsed number. */
function toNumber(value: string | undefined): number | undefined {
  const trimmed = (value ?? '').trim()
  return trimmed === '' ? undefined : Number(trimmed)
}

/** '' / whitespace → undefined, otherwise the trimmed string. */
function toText(value: string | undefined): string | undefined {
  const trimmed = (value ?? '').trim()
  return trimmed === '' ? undefined : trimmed
}

/** Validated form values → POST /api/job-postings payload. */
export function toCreatePayload(values: JobPostingFormValues): CreateJobPostingRequest {
  return {
    companyId: values.companyId,
    title: values.title.trim(),
    description: toText(values.description),
    ctcMin: toNumber(values.ctcMin),
    ctcMax: toNumber(values.ctcMax),
    applicationDeadline: toText(values.applicationDeadline),
    offerLimit: Number(values.offerLimit),
  }
}

/** Validated form values → PUT /api/job-postings/{id} payload (companyId is fixed on update). */
export function toUpdatePayload(values: JobPostingFormValues): UpdateJobPostingRequest {
  return {
    title: values.title.trim(),
    description: toText(values.description),
    ctcMin: toNumber(values.ctcMin),
    ctcMax: toNumber(values.ctcMax),
    applicationDeadline: toText(values.applicationDeadline),
    offerLimit: Number(values.offerLimit),
  }
}
