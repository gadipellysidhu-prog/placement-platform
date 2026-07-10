import { beforeEach, describe, expect, it } from 'vitest'
import { http, HttpResponse } from 'msw'
import { Route, Routes } from 'react-router-dom'
import { renderWithProviders, screen } from '@/test'
import { server } from '@/test/msw/server'
import { API_BASE_URL } from '@/test'
import ResetPasswordPage from './ResetPasswordPage'
import { useAuthStore } from '@/stores/auth.store'
import { ROUTES } from '@/constants/routes'

function renderReset(entry: string) {
  return renderWithProviders(
    <Routes>
      <Route path={ROUTES.RESET_PASSWORD} element={<ResetPasswordPage />} />
      <Route path={ROUTES.LOGIN} element={<div>Login Screen</div>} />
      <Route path={ROUTES.FORGOT_PASSWORD} element={<div>Forgot Screen</div>} />
    </Routes>,
    { initialEntries: [entry] },
  )
}

beforeEach(() => {
  useAuthStore.setState({ accessToken: null, user: null, isAuthenticated: false })
})

describe('ResetPasswordPage', () => {
  it('renders the new-password form when a token is present', () => {
    renderReset(`${ROUTES.RESET_PASSWORD}?token=abc`)
    expect(screen.getByRole('heading', { name: /set a new password/i })).toBeInTheDocument()
    expect(screen.getByLabelText(/^new password/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/confirm new password/i)).toBeInTheDocument()
  })

  it('shows validation errors for short and mismatched passwords', async () => {
    const { user } = renderReset(`${ROUTES.RESET_PASSWORD}?token=abc`)

    await user.type(screen.getByLabelText(/^new password/i), 'short')
    await user.type(screen.getByLabelText(/confirm new password/i), 'different')
    await user.click(screen.getByRole('button', { name: /reset password/i }))

    expect(await screen.findByText('Password must be at least 8 characters')).toBeInTheDocument()

    // Fix length but keep a mismatch → confirmation error surfaces.
    await user.clear(screen.getByLabelText(/^new password/i))
    await user.type(screen.getByLabelText(/^new password/i), 'validpass123')
    await user.click(screen.getByRole('button', { name: /reset password/i }))

    expect(await screen.findByText('Passwords do not match')).toBeInTheDocument()
  })

  it('submits the token + new password to the exact endpoint and redirects to login', async () => {
    const seen: { url: string; body: unknown }[] = []
    server.use(
      http.post(`${API_BASE_URL}/auth/reset-password`, async ({ request }) => {
        seen.push({ url: request.url, body: await request.json() })
        return HttpResponse.json({ message: 'Password has been reset. Please sign in again.' })
      }),
    )

    const { user } = renderReset(`${ROUTES.RESET_PASSWORD}?token=reset-token-123`)

    await user.type(screen.getByLabelText(/^new password/i), 'newpassword456')
    await user.type(screen.getByLabelText(/confirm new password/i), 'newpassword456')
    await user.click(screen.getByRole('button', { name: /reset password/i }))

    expect(await screen.findByText('Login Screen')).toBeInTheDocument()
    expect(seen).toHaveLength(1)
    expect(seen[0].url).toBe(`${API_BASE_URL}/auth/reset-password`)
    expect(seen[0].body).toEqual({ token: 'reset-token-123', newPassword: 'newpassword456' })
  })

  it('shows an error and a "request new link" option when the token is expired', async () => {
    server.use(
      http.post(`${API_BASE_URL}/auth/reset-password`, () =>
        HttpResponse.json(
          { title: 'Bad Request', status: 400, detail: 'Invalid or expired verification token' },
          { status: 400 },
        ),
      ),
    )

    const { user } = renderReset(`${ROUTES.RESET_PASSWORD}?token=expired`)

    await user.type(screen.getByLabelText(/^new password/i), 'newpassword456')
    await user.type(screen.getByLabelText(/confirm new password/i), 'newpassword456')
    await user.click(screen.getByRole('button', { name: /reset password/i }))

    expect(await screen.findByText(/invalid or expired verification token/i)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /request a new reset link/i })).toBeInTheDocument()
    // Still on the reset page, not redirected.
    expect(screen.getByRole('heading', { name: /set a new password/i })).toBeInTheDocument()
  })

  it('shows an invalid-link state when the token is missing', () => {
    renderReset(ROUTES.RESET_PASSWORD)
    expect(screen.getByRole('heading', { name: /invalid reset link/i })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /request a new reset link/i })).toBeInTheDocument()
    expect(screen.queryByLabelText(/^new password/i)).not.toBeInTheDocument()
  })
})
