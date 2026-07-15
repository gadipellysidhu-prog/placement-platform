import { z } from 'zod'

/**
 * Client validation for certificate submission. Mirrors the backend
 * {@code CertificateCreateRequest} Bean Validation EXACTLY — no invented rules:
 *
 *  - studentId           @NotNull            → supplied from the authenticated profile
 *  - name                @NotBlank @Size 255 → non-empty, max 255 chars
 *  - issuingOrganization @Size 255           → optional, max 255 chars
 *  - skillId             (optional)          → not collected in this form
 *  - fileKey             @Size 500           → the uploaded file id, handled outside the form
 *
 * The document upload is required by the UI (submission is disabled until the upload
 * completes) even though the backend allows a null fileKey — a certificate without its
 * document has no verification value.
 */
export const certificateFormSchema = z.object({
  name: z
    .string()
    .trim()
    .min(1, 'Certificate name is required')
    .max(255, 'Name must be at most 255 characters'),
  issuingOrganization: z
    .string()
    .trim()
    .max(255, 'Issuing organization must be at most 255 characters')
    .optional(),
})

export type CertificateFormValues = z.infer<typeof certificateFormSchema>
