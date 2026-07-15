import { beforeEach, describe, expect, it } from 'vitest'
import { http, HttpResponse } from 'msw'
import { renderWithProviders, screen, waitFor } from '@/test'
import { server } from '@/test/msw/server'
import { API_BASE_URL, mockOffer } from '@/test'
import MyOffersPage from './MyOffersPage'

beforeEach(() => {
  sessionStorage.clear()
})

describe('MyOffersPage', () => {
  it('lists offers with company and package details', async () => {
    renderWithProviders(<MyOffersPage />)

    expect(await screen.findByText('Acme Corp')).toBeInTheDocument()
    expect(screen.getByText('₹12.5 LPA')).toBeInTheDocument()
    expect(screen.getByText('Pending')).toBeInTheDocument()
  })

  it('accepts an offer only through a strong confirmation dialog', async () => {
    let acceptCalled = false
    server.use(
      http.post(`${API_BASE_URL}/api/offers/:id/accept`, () => {
        acceptCalled = true
        return HttpResponse.json({ ...mockOffer, status: 'ACCEPTED' })
      }),
    )

    const { user } = renderWithProviders(<MyOffersPage />)
    await screen.findByText('Acme Corp')

    // The primary Accept button opens a binding confirmation — it does not act directly.
    await user.click(screen.getByRole('button', { name: /^accept$/i }))
    expect(
      await screen.findByRole('heading', { name: /accept the offer from acme corp\?/i }),
    ).toBeInTheDocument()
    expect(acceptCalled).toBe(false)

    await user.click(screen.getByRole('button', { name: /yes, accept offer/i }))
    await waitFor(() => expect(acceptCalled).toBe(true))
  })

  it('renders accepted offers as read-only (no actions)', async () => {
    server.use(
      http.get(`${API_BASE_URL}/api/offers/my`, () =>
        HttpResponse.json([{ ...mockOffer, status: 'ACCEPTED' }]),
      ),
    )

    renderWithProviders(<MyOffersPage />)

    expect(await screen.findByText('Acme Corp')).toBeInTheDocument()
    expect(
      screen.getByText(/this offer is accepted and can no longer be changed/i),
    ).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /^accept$/i })).not.toBeInTheDocument()
  })
})
