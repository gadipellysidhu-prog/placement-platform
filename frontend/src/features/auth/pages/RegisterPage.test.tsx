import { beforeEach, describe, expect, it, vi } from 'vitest'
import { http, HttpResponse } from 'msw'
import { Route, Routes } from 'react-router-dom'
import { renderWithProviders, screen } from '@/test'
import { server } from '@/test/msw/server'
import { API_BASE_URL } from '@/test'
import RegisterPage from './RegisterPage'
import LoginPage from './LoginPage'
import { useAuthStore } from '@/stores/auth.store'
import { ROUTES } from '@/constants/routes'

const REGISTER_MESSAGE =
  'Registration successful. Please check your email to verify your account before signing in.'

function renderRegister() {
  return renderWithProviders(
    <Routes>
      <Route path={ROUTES.REGISTER} element={<RegisterPage />} />
      <Route path={ROUTES.LOGIN} element={<LoginPage />} />
      <Route path={ROUTES.DASHBOARD} element={<div>Dashboard Screen</div>} />
    </Routes>,
    { initialEntries: [ROUTES.REGISTER] },
  )
}

beforeEach(() => {
  useAuthStore.setState({ accessToken: null, user: null, isAuthenticated: false })
  sessionStorage.clear()
  localStorage.clear()
})

describe('RegisterPage', () => {
  it('does not auto-authenticate: registration returns a message (no tokens) and lands on login', async () => {
    // The backend issues NO tokens on register and returns a MessageResponse.
    // A `/me` call here would mean the hook wrongly tried to auto-login — fail if hit.
    const meSpy = vi.fn()
    server.use(
      http.post(`${API_BASE_URL}/auth/register`, () =>
        HttpResponse.json({ message: REGISTER_MESSAGE }, { status: 201 }),
      ),
      http.get(`${API_BASE_URL}/api/users/me`, () => {
        meSpy()
        return HttpResponse.json({ email: 'student@university.edu', role: 'ROLE_STUDENT' })
      }),
    )

    const { user } = renderRegister()

    await user.type(screen.getByLabelText(/email address/i), 'newstudent@university.edu')
    await user.type(screen.getByLabelText(/^password/i), 'secret123')
    await user.click(screen.getByRole('button', { name: /create account/i }))

    // Redirected to the login page, showing the backend's confirmation notice.
    expect(await screen.findByRole('heading', { name: /sign in/i })).toBeInTheDocument()
    expect(screen.getByText(REGISTER_MESSAGE)).toBeInTheDocument()

    // Never auto-authenticated: no /me call, no tokens, no session.
    expect(meSpy).not.toHaveBeenCalled()
    expect(useAuthStore.getState().isAuthenticated).toBe(false)
    expect(sessionStorage.getItem('__access_token__')).toBeNull()
    expect(localStorage.getItem('__refresh_token__')).toBeNull()

    // Crucially, we did NOT end up on the dashboard.
    expect(screen.queryByText('Dashboard Screen')).not.toBeInTheDocument()
  })

  it('shows the API error and stays on the register page when registration fails', async () => {
    server.use(
      http.post(`${API_BASE_URL}/auth/register`, () =>
        HttpResponse.json(
          {
            title: 'Email is already registered.',
            status: 409,
            detail: 'Email is already registered.',
          },
          { status: 409 },
        ),
      ),
    )

    const { user } = renderRegister()

    await user.type(screen.getByLabelText(/email address/i), 'taken@university.edu')
    await user.type(screen.getByLabelText(/^password/i), 'secret123')
    await user.click(screen.getByRole('button', { name: /create account/i }))

    expect(await screen.findByText('Email is already registered.')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: /create account/i })).toBeInTheDocument()
    expect(useAuthStore.getState().isAuthenticated).toBe(false)
  })
})
