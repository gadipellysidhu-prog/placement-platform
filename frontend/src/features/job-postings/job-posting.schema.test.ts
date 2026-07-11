import { describe, expect, it } from 'vitest'
import {
  jobPostingFormSchema,
  toCreatePayload,
  toUpdatePayload,
  type JobPostingFormValues,
} from './job-posting.schema'

function validValues(overrides: Partial<JobPostingFormValues> = {}): JobPostingFormValues {
  return {
    companyId: '22222222-2222-2222-2222-222222222222',
    title: 'Backend Engineer',
    description: 'Build things',
    ctcMin: '8',
    ctcMax: '12',
    applicationDeadline: '2026-08-01',
    offerLimit: '5',
    ...overrides,
  }
}

describe('jobPostingFormSchema', () => {
  it('accepts a fully valid posting', () => {
    expect(jobPostingFormSchema.safeParse(validValues()).success).toBe(true)
  })

  it('requires a company (mirrors @NotNull companyId)', () => {
    const result = jobPostingFormSchema.safeParse(validValues({ companyId: '' }))
    expect(result.success).toBe(false)
    if (!result.success) {
      expect(result.error.issues[0].message).toMatch(/company is required/i)
    }
  })

  it('requires a non-blank title (mirrors @NotBlank)', () => {
    expect(jobPostingFormSchema.safeParse(validValues({ title: '   ' })).success).toBe(false)
  })

  it('rejects a title longer than 255 chars (mirrors @Size max=255)', () => {
    expect(jobPostingFormSchema.safeParse(validValues({ title: 'x'.repeat(256) })).success).toBe(
      false,
    )
    expect(jobPostingFormSchema.safeParse(validValues({ title: 'x'.repeat(255) })).success).toBe(
      true,
    )
  })

  it('rejects negative CTC (mirrors @DecimalMin 0.0) but allows blank', () => {
    expect(jobPostingFormSchema.safeParse(validValues({ ctcMin: '-1' })).success).toBe(false)
    expect(jobPostingFormSchema.safeParse(validValues({ ctcMin: '', ctcMax: '' })).success).toBe(
      true,
    )
    expect(jobPostingFormSchema.safeParse(validValues({ ctcMin: '0' })).success).toBe(true)
  })

  it('requires offerLimit ≥ 1 as a whole number (mirrors @Min(1) int)', () => {
    expect(jobPostingFormSchema.safeParse(validValues({ offerLimit: '' })).success).toBe(false)
    expect(jobPostingFormSchema.safeParse(validValues({ offerLimit: '0' })).success).toBe(false)
    expect(jobPostingFormSchema.safeParse(validValues({ offerLimit: '2.5' })).success).toBe(false)
    expect(jobPostingFormSchema.safeParse(validValues({ offerLimit: '1' })).success).toBe(true)
  })
})

describe('toCreatePayload', () => {
  it('coerces strings to the create request shape', () => {
    expect(toCreatePayload(validValues())).toEqual({
      companyId: '22222222-2222-2222-2222-222222222222',
      title: 'Backend Engineer',
      description: 'Build things',
      ctcMin: 8,
      ctcMax: 12,
      applicationDeadline: '2026-08-01',
      offerLimit: 5,
    })
  })

  it('maps blank optional fields to undefined', () => {
    const payload = toCreatePayload(
      validValues({ description: '', ctcMin: '', ctcMax: '', applicationDeadline: '' }),
    )
    expect(payload.description).toBeUndefined()
    expect(payload.ctcMin).toBeUndefined()
    expect(payload.ctcMax).toBeUndefined()
    expect(payload.applicationDeadline).toBeUndefined()
    expect(payload.offerLimit).toBe(5)
  })
})

describe('toUpdatePayload', () => {
  it('never includes companyId (fixed on update)', () => {
    const payload = toUpdatePayload(validValues())
    expect(payload).not.toHaveProperty('companyId')
    expect(payload.title).toBe('Backend Engineer')
    expect(payload.offerLimit).toBe(5)
  })
})
