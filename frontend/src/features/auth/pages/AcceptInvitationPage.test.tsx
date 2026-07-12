import { beforeEach, describe, expect, it } from 'vitest'
import { http, HttpResponse } from 'msw'
import { Route, Routes } from 'react-router-dom'
import { renderWithProviders, screen } from '@/test'
import { server } from '@/test/msw/server'
import { API_BASE_URL } from '@/test'
import AcceptInvitationPage from './AcceptInvitationPage'
import { useAuthStore } from '@/stores/auth.store'
import { ROUTES } from '@/constants/routes'

function renderAccept(entry: string) {
  return renderWithProviders(
    <Routes>
      <Route path={ROUTES.ACCEPT_INVITATION} element={<AcceptInvitationPage />} />
      <Route path={ROUTES.LOGIN} element={<div>Login Screen</div>} />
    </Routes>,
    { initialEntries: [entry] },
  )
}

beforeEach(() => {
  useAuthStore.setState({ accessToken: null, user: null, isAuthenticated: false })
})

describe('AcceptInvitationPage', () => {
  it('renders the password-setup form when a token is present', () => {
    renderAccept(`${ROUTES.ACCEPT_INVITATION}?token=abc`)
    expect(screen.getByRole('heading', { name: /accept your invitation/i })).toBeInTheDocument()
    expect(screen.getByLabelText(/^password/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/confirm password/i)).toBeInTheDocument()
  })

  it('shows a validation error when the passwords do not match', async () => {
    const { user } = renderAccept(`${ROUTES.ACCEPT_INVITATION}?token=abc`)

    await user.type(screen.getByLabelText(/^password/i), 'validpass123')
    await user.type(screen.getByLabelText(/confirm password/i), 'mismatchpass')
    await user.click(screen.getByRole('button', { name: /activate account/i }))

    expect(await screen.findByText('Passwords do not match')).toBeInTheDocument()
  })

  it('submits the token + new password to the exact endpoint and redirects to login', async () => {
    const seen: { url: string; body: unknown }[] = []
    server.use(
      http.post(`${API_BASE_URL}/auth/accept-invitation`, async ({ request }) => {
        seen.push({ url: request.url, body: await request.json() })
        return HttpResponse.json({ message: 'Invitation accepted.' })
      }),
    )

    const { user } = renderAccept(`${ROUTES.ACCEPT_INVITATION}?token=invite-token-123`)

    await user.type(screen.getByLabelText(/^password/i), 'brandnewpass1')
    await user.type(screen.getByLabelText(/confirm password/i), 'brandnewpass1')
    await user.click(screen.getByRole('button', { name: /activate account/i }))

    expect(await screen.findByText('Login Screen')).toBeInTheDocument()
    expect(seen).toHaveLength(1)
    expect(seen[0].url).toBe(`${API_BASE_URL}/auth/accept-invitation`)
    expect(seen[0].body).toEqual({ token: 'invite-token-123', newPassword: 'brandnewpass1' })
  })

  it('shows an error state when the invitation token is invalid or expired', async () => {
    server.use(
      http.post(`${API_BASE_URL}/auth/accept-invitation`, () =>
        HttpResponse.json(
          { title: 'Bad Request', status: 400, detail: 'Invalid or expired verification token' },
          { status: 400 },
        ),
      ),
    )

    const { user } = renderAccept(`${ROUTES.ACCEPT_INVITATION}?token=expired`)

    await user.type(screen.getByLabelText(/^password/i), 'brandnewpass1')
    await user.type(screen.getByLabelText(/confirm password/i), 'brandnewpass1')
    await user.click(screen.getByRole('button', { name: /activate account/i }))

    expect(await screen.findByText(/invalid or expired verification token/i)).toBeInTheDocument()
    expect(screen.getByText(/contact your administrator/i)).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: /accept your invitation/i })).toBeInTheDocument()
  })

  it('shows an invalid-link state when the token is missing', () => {
    renderAccept(ROUTES.ACCEPT_INVITATION)
    expect(screen.getByRole('heading', { name: /invalid invitation link/i })).toBeInTheDocument()
    expect(screen.queryByLabelText(/^password/i)).not.toBeInTheDocument()
  })
})
