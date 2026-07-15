import { beforeEach, describe, expect, it } from 'vitest'
import { http, HttpResponse } from 'msw'
import { renderWithProviders, screen, waitFor } from '@/test'
import { server } from '@/test/msw/server'
import { API_BASE_URL, mockCertificate, pageOf } from '@/test'
import CertificateVerificationPage from './CertificateVerificationPage'

beforeEach(() => {
  sessionStorage.clear()
})

describe('CertificateVerificationPage', () => {
  it('shows the queue and verifies a pending certificate after confirmation', async () => {
    let verifyCalled = false
    server.use(
      http.post(`${API_BASE_URL}/api/certificates/:id/verify`, () => {
        verifyCalled = true
        return HttpResponse.json({ ...mockCertificate, verificationStatus: 'VERIFIED' })
      }),
    )

    const { user } = renderWithProviders(<CertificateVerificationPage />)

    expect(await screen.findByText('AWS Certified Developer')).toBeInTheDocument()
    expect(screen.getByText('CS2021001')).toBeInTheDocument()

    // Open the verify confirmation and confirm it.
    await user.click(screen.getByRole('button', { name: /^verify$/i }))
    expect(
      await screen.findByRole('heading', { name: /verify this certificate\?/i }),
    ).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /verify certificate/i }))

    await waitFor(() => expect(verifyCalled).toBe(true))
  })

  it('renders no review actions for a certificate that is not pending', async () => {
    server.use(
      http.get(`${API_BASE_URL}/api/certificates`, () =>
        HttpResponse.json(pageOf([{ ...mockCertificate, verificationStatus: 'VERIFIED' }])),
      ),
    )

    renderWithProviders(<CertificateVerificationPage />)

    expect(await screen.findByText('AWS Certified Developer')).toBeInTheDocument()
    expect(screen.getByText('Verified')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /^verify$/i })).not.toBeInTheDocument()
  })
})
