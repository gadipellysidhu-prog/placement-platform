import { beforeEach, describe, expect, it } from 'vitest'
import { http, HttpResponse } from 'msw'
import { Route, Routes } from 'react-router-dom'
import { renderWithProviders, screen, waitFor } from '@/test'
import { server } from '@/test/msw/server'
import { API_BASE_URL, mockStudentUser } from '@/test'
import LoginPage from './LoginPage'
import { useAuthStore } from '@/stores/auth.store'
import { ROUTES } from '@/constants/routes'

function renderLogin() {
  return renderWithProviders(
    <Routes>
      <Route path={ROUTES.LOGIN} element={<LoginPage />} />
      <Route path={ROUTES.DASHBOARD} element={<div>Dashboard Screen</div>} />
      <Route path={ROUTES.FORGOT_PASSWORD} element={<div>Forgot Screen</div>} />
      <Route path={ROUTES.REGISTER} element={<div>Register Screen</div>} />
    </Routes>,
    { initialEntries: [ROUTES.LOGIN] },
  )
}

beforeEach(() => {
  useAuthStore.setState({ accessToken: null, user: null, isAuthenticated: false })
})

describe('LoginPage', () => {
  it('renders the sign-in form', () => {
    renderLogin()
    expect(screen.getByRole('heading', { name: /sign in/i })).toBeInTheDocument()
    expect(screen.getByLabelText(/email address/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/^password/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument()
  })

  it('shows validation errors when submitting an empty form', async () => {
    const { user } = renderLogin()

    await user.click(screen.getByRole('button', { name: /sign in/i }))

    expect(await screen.findByText('Email is required')).toBeInTheDocument()
    expect(screen.getByText('Password is required')).toBeInTheDocument()
    // No auth state mutated on a validation failure.
    expect(useAuthStore.getState().isAuthenticated).toBe(false)
  })

  it('shows a validation error for a malformed email', async () => {
    const { user } = renderLogin()

    await user.type(screen.getByLabelText(/email address/i), 'not-an-email')
    await user.type(screen.getByLabelText(/^password/i), 'secret123')
    await user.click(screen.getByRole('button', { name: /sign in/i }))

    expect(await screen.findByText('Please enter a valid email address')).toBeInTheDocument()
  })

  it('logs in successfully and navigates to the dashboard', async () => {
    const { user } = renderLogin()

    await user.type(screen.getByLabelText(/email address/i), 'student@university.edu')
    await user.type(screen.getByLabelText(/^password/i), 'secret123')
    await user.click(screen.getByRole('button', { name: /sign in/i }))

    // Navigation to the dashboard proves the full login → /me → setAuth flow ran.
    expect(await screen.findByText('Dashboard Screen')).toBeInTheDocument()

    const state = useAuthStore.getState()
    expect(state.isAuthenticated).toBe(true)
    expect(state.user).toEqual(mockStudentUser)
    expect(sessionStorage.getItem('__access_token__')).toBe('mock-access-token')
  })

  it('shows the API error and stays on the page when credentials are rejected', async () => {
    server.use(
      http.post(`${API_BASE_URL}/auth/login`, () =>
        HttpResponse.json(
          { title: 'Unauthorized', status: 401, detail: 'Invalid email or password' },
          { status: 401 },
        ),
      ),
    )

    const { user } = renderLogin()

    await user.type(screen.getByLabelText(/email address/i), 'student@university.edu')
    await user.type(screen.getByLabelText(/^password/i), 'wrong-password')
    await user.click(screen.getByRole('button', { name: /sign in/i }))

    expect(await screen.findByText('Invalid email or password')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: /sign in/i })).toBeInTheDocument()
    expect(useAuthStore.getState().isAuthenticated).toBe(false)
    // Tokens written mid-flow are rolled back on error.
    expect(sessionStorage.getItem('__access_token__')).toBeNull()
  })

  it('disables the submit button and shows a spinner while the request is in flight', async () => {
    server.use(
      http.post(`${API_BASE_URL}/auth/login`, async () => {
        await new Promise((resolve) => setTimeout(resolve, 100))
        return HttpResponse.json({
          accessToken: 'mock-access-token',
          refreshToken: 'mock-refresh-token',
          accessTokenExpiresIn: 900,
          tokenType: 'Bearer',
        })
      }),
    )

    const { user } = renderLogin()

    await user.type(screen.getByLabelText(/email address/i), 'student@university.edu')
    await user.type(screen.getByLabelText(/^password/i), 'secret123')
    await user.click(screen.getByRole('button', { name: /sign in/i }))

    // Loading UI: button reads "Signing in…" and is disabled.
    expect(await screen.findByText(/signing in/i)).toBeInTheDocument()
    await waitFor(() => expect(screen.getByRole('button', { name: /signing in/i })).toBeDisabled())
  })
})
