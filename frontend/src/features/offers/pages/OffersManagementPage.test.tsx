import { beforeEach, describe, expect, it } from 'vitest'
import { http, HttpResponse } from 'msw'
import { renderWithProviders, screen, waitFor } from '@/test'
import { server } from '@/test/msw/server'
import { API_BASE_URL, mockOffer, pageOf } from '@/test'
import OffersManagementPage from './OffersManagementPage'

beforeEach(() => {
  sessionStorage.clear()
})

describe('OffersManagementPage', () => {
  it('lists offers and expires a pending offer after confirmation', async () => {
    let expireCalled = false
    server.use(
      http.post(`${API_BASE_URL}/api/offers/:id/expire`, () => {
        expireCalled = true
        return HttpResponse.json({ ...mockOffer, status: 'EXPIRED' })
      }),
    )

    const { user } = renderWithProviders(<OffersManagementPage />)

    expect(await screen.findByText('Acme Corp')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /^expire$/i }))
    expect(await screen.findByRole('heading', { name: /expire this offer\?/i })).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /expire offer/i }))

    await waitFor(() => expect(expireCalled).toBe(true))
  })

  it('offers no expire action for a terminal offer', async () => {
    server.use(
      http.get(`${API_BASE_URL}/api/offers`, () =>
        HttpResponse.json(pageOf([{ ...mockOffer, status: 'ACCEPTED' }])),
      ),
    )

    renderWithProviders(<OffersManagementPage />)

    expect(await screen.findByText('Acme Corp')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /^expire$/i })).not.toBeInTheDocument()
  })
})
