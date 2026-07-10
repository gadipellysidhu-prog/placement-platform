import { StrictMode } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { http, HttpResponse } from 'msw'
import { Route, Routes } from 'react-router-dom'
import { renderWithProviders, screen } from '@/test'
import { server } from '@/test/msw/server'
import { API_BASE_URL } from '@/test'
import VerifyEmailPage from './VerifyEmailPage'
import { useAuthStore } from '@/stores/auth.store'
import { ROUTES } from '@/constants/routes'

function renderVerify(entry: string) {
  return renderWithProviders(
    <Routes>
      <Route path={ROUTES.VERIFY_EMAIL} element={<VerifyEmailPage />} />
      <Route path={ROUTES.LOGIN} element={<div>Login Screen</div>} />
    </Routes>,
    { initialEntries: [entry] },
  )
}

beforeEach(() => {
  useAuthStore.setState({ accessToken: null, user: null, isAuthenticated: false })
})

describe('VerifyEmailPage', () => {
  it('auto-verifies with the token from the URL and hits the exact endpoint/payload', async () => {
    const seen: { url: string; body: unknown }[] = []
    server.use(
      http.post(`${API_BASE_URL}/auth/verify-email/confirm`, async ({ request }) => {
        seen.push({ url: request.url, body: await request.json() })
        return HttpResponse.json({ message: 'Email verified successfully.' })
      }),
    )

    renderVerify(`${ROUTES.VERIFY_EMAIL}?token=raw-verify-token`)

    expect(await screen.findByRole('heading', { name: /email verified/i })).toBeInTheDocument()
    expect(seen).toHaveLength(1)
    expect(seen[0].url).toBe(`${API_BASE_URL}/auth/verify-email/confirm`)
    expect(seen[0].body).toEqual({ token: 'raw-verify-token' })
    expect(screen.getByRole('link', { name: /continue to sign in/i })).toBeInTheDocument()
  })

  it('dispatches the verification exactly once even under StrictMode double-invoke', async () => {
    const handler = vi.fn(() => HttpResponse.json({ message: 'Email verified successfully.' }))
    server.use(http.post(`${API_BASE_URL}/auth/verify-email/confirm`, handler))

    renderWithProviders(
      <StrictMode>
        <Routes>
          <Route path={ROUTES.VERIFY_EMAIL} element={<VerifyEmailPage />} />
          <Route path={ROUTES.LOGIN} element={<div>Login Screen</div>} />
        </Routes>
      </StrictMode>,
      { initialEntries: [`${ROUTES.VERIFY_EMAIL}?token=once-token`] },
    )

    expect(await screen.findByRole('heading', { name: /email verified/i })).toBeInTheDocument()
    // Single-use token: the confirm call must fire once, never twice.
    expect(handler).toHaveBeenCalledTimes(1)
  })

  it('shows a failure state and a resend form for an invalid/expired token', async () => {
    server.use(
      http.post(`${API_BASE_URL}/auth/verify-email/confirm`, () =>
        HttpResponse.json(
          { title: 'Bad Request', status: 400, detail: 'Invalid or expired verification token' },
          { status: 400 },
        ),
      ),
    )

    renderVerify(`${ROUTES.VERIFY_EMAIL}?token=expired`)

    expect(await screen.findByRole('heading', { name: /verification failed/i })).toBeInTheDocument()
    expect(screen.getByText(/invalid or expired verification token/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /resend verification email/i })).toBeInTheDocument()
  })

  it('resends a verification email to the exact endpoint/payload', async () => {
    server.use(
      http.post(`${API_BASE_URL}/auth/verify-email/confirm`, () =>
        HttpResponse.json({ title: 'Bad Request', status: 400 }, { status: 400 }),
      ),
    )
    const seen: { url: string; body: unknown }[] = []
    server.use(
      http.post(`${API_BASE_URL}/auth/resend-verification`, async ({ request }) => {
        seen.push({ url: request.url, body: await request.json() })
        return HttpResponse.json({ message: 'sent' }, { status: 202 })
      }),
    )

    const { user } = renderVerify(`${ROUTES.VERIFY_EMAIL}?token=expired`)

    await user.type(await screen.findByLabelText(/email address/i), 'student@university.edu')
    await user.click(screen.getByRole('button', { name: /resend verification email/i }))

    expect(await screen.findByText(/a new verification link is on its way/i)).toBeInTheDocument()
    expect(seen).toHaveLength(1)
    expect(seen[0].url).toBe(`${API_BASE_URL}/auth/resend-verification`)
    expect(seen[0].body).toEqual({ email: 'student@university.edu' })
  })

  it('shows an invalid-link state and never calls the API when the token is missing', async () => {
    const handler = vi.fn(() => HttpResponse.json({ message: 'ok' }))
    server.use(http.post(`${API_BASE_URL}/auth/verify-email/confirm`, handler))

    renderVerify(ROUTES.VERIFY_EMAIL)

    expect(
      await screen.findByRole('heading', { name: /invalid verification link/i }),
    ).toBeInTheDocument()
    expect(handler).not.toHaveBeenCalled()
    // Recovery path is available.
    expect(screen.getByRole('button', { name: /resend verification email/i })).toBeInTheDocument()
  })
})
