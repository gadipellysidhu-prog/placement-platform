import { beforeEach, describe, expect, it } from 'vitest'
import { renderWithProviders, screen } from '@/test'
import { useAuthStore } from '@/stores/auth.store'
import { ROUTES } from '@/constants/routes'
import type { Role } from '@/types/auth'
import { Sidebar } from './Sidebar'

function authenticateAs(role: Role) {
  useAuthStore.setState({
    accessToken: 'token',
    user: { email: `${role}@university.edu`, role },
    isAuthenticated: true,
  })
}

beforeEach(() => {
  useAuthStore.setState({ accessToken: null, user: null, isAuthenticated: false })
})

describe('Sidebar — Administration section', () => {
  it('shows Administration to an admin, mirroring hasRole(ADMIN) on /api/admin/**', () => {
    authenticateAs('ROLE_ADMIN')
    renderWithProviders(<Sidebar />)

    expect(screen.getByRole('heading', { name: 'Administration' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Users' })).toHaveAttribute('href', ROUTES.ADMIN.USERS)
    expect(screen.getByRole('link', { name: 'Settings' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Audit Logs' })).toBeInTheDocument()
  })

  it('hides the whole Administration section from a placement officer', () => {
    authenticateAs('ROLE_PLACEMENT_OFFICER')
    renderWithProviders(<Sidebar />)

    expect(screen.queryByRole('heading', { name: 'Administration' })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Users' })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Audit Logs' })).not.toBeInTheDocument()
  })

  it('hides the Administration section from a student', () => {
    authenticateAs('ROLE_STUDENT')
    renderWithProviders(<Sidebar />)

    expect(screen.queryByRole('heading', { name: 'Administration' })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Settings' })).not.toBeInTheDocument()
  })

  it('still renders the ungrouped navigation for every role', () => {
    authenticateAs('ROLE_STUDENT')
    renderWithProviders(<Sidebar />)

    expect(screen.getByRole('link', { name: 'Dashboard' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'My Profile' })).toBeInTheDocument()
  })

  it('keeps officer-level navigation visible to an admin', () => {
    authenticateAs('ROLE_ADMIN')
    renderWithProviders(<Sidebar />)

    expect(screen.getByRole('link', { name: 'Students' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Skills' })).toBeInTheDocument()
  })
})
