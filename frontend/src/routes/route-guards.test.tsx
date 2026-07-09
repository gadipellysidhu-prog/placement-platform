import { beforeEach, describe, expect, it } from 'vitest'
import { Route, Routes } from 'react-router-dom'
import { renderWithProviders, screen } from '@/test'
import { ProtectedRoute } from './ProtectedRoute'
import { PublicRoute } from './PublicRoute'
import { RoleRoute } from './RoleRoute'
import { useAuthStore } from '@/stores/auth.store'
import { ROUTES } from '@/constants/routes'
import type { Role } from '@/types/auth'

function authenticateAs(role: Role) {
  useAuthStore.setState({
    accessToken: 'token',
    user: { email: `${role}@university.edu`, role },
    isAuthenticated: true,
  })
}

function logout() {
  useAuthStore.setState({ accessToken: null, user: null, isAuthenticated: false })
}

beforeEach(logout)

describe('ProtectedRoute', () => {
  function renderProtected(requiredRole?: Role, initialRoute = ROUTES.DASHBOARD) {
    return renderWithProviders(
      <Routes>
        <Route element={<ProtectedRoute requiredRole={requiredRole} />}>
          <Route path={ROUTES.DASHBOARD} element={<div>Protected Content</div>} />
        </Route>
        <Route path={ROUTES.LOGIN} element={<div>Login Screen</div>} />
        <Route path={ROUTES.FORBIDDEN} element={<div>Forbidden Screen</div>} />
      </Routes>,
      { initialEntries: [initialRoute] },
    )
  }

  it('renders the protected outlet when authenticated', () => {
    authenticateAs('ROLE_STUDENT')
    renderProtected()
    expect(screen.getByText('Protected Content')).toBeInTheDocument()
  })

  it('redirects to login when unauthenticated', () => {
    renderProtected()
    expect(screen.getByText('Login Screen')).toBeInTheDocument()
    expect(screen.queryByText('Protected Content')).not.toBeInTheDocument()
  })

  it('redirects to /403 when the required role is not met', () => {
    authenticateAs('ROLE_STUDENT')
    renderProtected('ROLE_ADMIN')
    expect(screen.getByText('Forbidden Screen')).toBeInTheDocument()
  })

  it('renders content when the exact required role matches', () => {
    authenticateAs('ROLE_ADMIN')
    renderProtected('ROLE_ADMIN')
    expect(screen.getByText('Protected Content')).toBeInTheDocument()
  })
})

describe('PublicRoute', () => {
  function renderPublic() {
    return renderWithProviders(
      <Routes>
        <Route element={<PublicRoute />}>
          <Route path={ROUTES.LOGIN} element={<div>Login Screen</div>} />
        </Route>
        <Route path={ROUTES.DASHBOARD} element={<div>Dashboard Screen</div>} />
      </Routes>,
      { initialEntries: [ROUTES.LOGIN] },
    )
  }

  it('renders public content when unauthenticated', () => {
    renderPublic()
    expect(screen.getByText('Login Screen')).toBeInTheDocument()
  })

  it('redirects authenticated users to the dashboard', () => {
    authenticateAs('ROLE_STUDENT')
    renderPublic()
    expect(screen.getByText('Dashboard Screen')).toBeInTheDocument()
    expect(screen.queryByText('Login Screen')).not.toBeInTheDocument()
  })
})

describe('RoleRoute', () => {
  function renderRole(minimumRole: Role) {
    return renderWithProviders(
      <Routes>
        <Route element={<RoleRoute minimumRole={minimumRole} />}>
          <Route path={ROUTES.OFFICER.STUDENTS} element={<div>Officer Area</div>} />
        </Route>
        <Route path={ROUTES.FORBIDDEN} element={<div>Forbidden Screen</div>} />
      </Routes>,
      { initialEntries: [ROUTES.OFFICER.STUDENTS] },
    )
  }

  it('allows access when the user meets the minimum role', () => {
    authenticateAs('ROLE_PLACEMENT_OFFICER')
    renderRole('ROLE_PLACEMENT_OFFICER')
    expect(screen.getByText('Officer Area')).toBeInTheDocument()
  })

  it('allows a higher role via the hierarchy (ADMIN ≥ OFFICER)', () => {
    authenticateAs('ROLE_ADMIN')
    renderRole('ROLE_PLACEMENT_OFFICER')
    expect(screen.getByText('Officer Area')).toBeInTheDocument()
  })

  it('forbids a lower role (STUDENT < OFFICER)', () => {
    authenticateAs('ROLE_STUDENT')
    renderRole('ROLE_PLACEMENT_OFFICER')
    expect(screen.getByText('Forbidden Screen')).toBeInTheDocument()
  })
})
