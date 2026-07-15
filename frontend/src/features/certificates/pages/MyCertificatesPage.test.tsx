import { beforeEach, describe, expect, it } from 'vitest'
import { http, HttpResponse } from 'msw'
import { renderWithProviders, screen, waitFor } from '@/test'
import { server } from '@/test/msw/server'
import { API_BASE_URL, mockCertificate } from '@/test'
import MyCertificatesPage from './MyCertificatesPage'

function docFileInput(): HTMLInputElement {
  const input = document.querySelector('input[type="file"]')
  if (!input) throw new Error('file input not found')
  return input as HTMLInputElement
}

function pdf() {
  return new File(['%PDF-1.4 dummy'], 'certificate.pdf', { type: 'application/pdf' })
}

beforeEach(() => {
  sessionStorage.clear()
})

describe('MyCertificatesPage', () => {
  it('lists the student’s certificates with their verification status', async () => {
    renderWithProviders(<MyCertificatesPage />)

    expect(await screen.findByText('AWS Certified Developer')).toBeInTheDocument()
    expect(screen.getByText('Pending')).toBeInTheDocument()
  })

  it('submits a certificate only after the document upload completes, sending the file id as fileKey', async () => {
    let submittedBody: Record<string, unknown> | null = null
    server.use(
      http.post(`${API_BASE_URL}/api/certificates`, async ({ request }) => {
        submittedBody = (await request.json()) as Record<string, unknown>
        return HttpResponse.json(mockCertificate, { status: 201 })
      }),
    )

    const { user } = renderWithProviders(<MyCertificatesPage />)

    // Wait for the profile-gated submit button, then open the dialog.
    await user.click(await screen.findByRole('button', { name: /submit certificate/i }))
    expect(
      await screen.findByRole('heading', { name: /submit a certificate/i }),
    ).toBeInTheDocument()

    // Submission is disabled until a file has been uploaded.
    const submit = screen.getByRole('button', { name: /^submit$/i })
    expect(submit).toBeDisabled()

    await user.type(screen.getByLabelText(/certificate name/i), 'React Professional')
    await user.upload(docFileInput(), pdf())

    // Upload completed → success state visible and submit enabled.
    expect(await screen.findByTestId('file-upload-success')).toBeInTheDocument()
    await waitFor(() => expect(submit).toBeEnabled())

    await user.click(submit)

    await waitFor(() => expect(submittedBody).not.toBeNull())
    expect(submittedBody).toMatchObject({
      studentId: 'student-1',
      name: 'React Professional',
      fileKey: 'file-1',
    })
  })
})
