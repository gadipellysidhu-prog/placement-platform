import { describe, expect, it } from 'vitest'
import { http, HttpResponse } from 'msw'
import { Route, Routes } from 'react-router-dom'
import {
  renderWithProviders,
  screen,
  server,
  API_BASE_URL,
  mockAdminUserAccount,
  mockDormantUserAccount,
} from '@/test'
import { ROUTES } from '@/constants/routes'
import UserDetailPage from './UserDetailPage'

const DETAIL_ROUTE = `${ROUTES.ADMIN.USERS}/:id`
const detailPath = ROUTES.ADMIN.USER_DETAIL(mockAdminUserAccount.id)

function renderPage() {
  return renderWithProviders(
    <Routes>
      <Route path={DETAIL_ROUTE} element={<UserDetailPage />} />
      <Route path={ROUTES.ADMIN.USERS} element={<div>Users List Screen</div>} />
    </Routes>,
    { initialEntries: [detailPath] },
  )
}

describe('UserDetailPage', () => {
  it('renders the account details', async () => {
    renderPage()

    expect(
      await screen.findByRole('heading', { name: 'officer@university.edu' }),
    ).toBeInTheDocument()
    // Shown both as a detail row and as the role select's current value.
    expect(screen.getAllByText('Placement Officer').length).toBeGreaterThan(0)
    expect(screen.getByText('Verified')).toBeInTheDocument()
    expect(screen.getByText('Active')).toBeInTheDocument()
  })

  it('reports an account with no activity on record', async () => {
    server.use(
      http.get(`${API_BASE_URL}/api/admin/users/:id`, () =>
        HttpResponse.json(mockDormantUserAccount),
      ),
    )
    renderPage()

    expect(await screen.findByText('No activity recorded')).toBeInTheDocument()
  })

  it('confirms before disabling, then applies the change', async () => {
    const { user } = renderPage()
    await screen.findByRole('heading', { name: 'officer@university.edu' })

    await user.click(screen.getByRole('button', { name: 'Disable' }))

    // The mutation only fires after explicit confirmation.
    expect(await screen.findByText('Disable account?')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Disable' }))

    expect(await screen.findByText('Account disabled.')).toBeInTheDocument()
  })

  it('surfaces the last-active-admin 409 verbatim instead of suppressing it', async () => {
    server.use(
      http.post(`${API_BASE_URL}/api/admin/users/:id/disable`, () =>
        HttpResponse.json(
          {
            title: 'Conflict',
            status: 409,
            detail: 'Cannot disable the last active administrator.',
          },
          { status: 409 },
        ),
      ),
    )

    const { user } = renderPage()
    await screen.findByRole('heading', { name: 'officer@university.edu' })

    await user.click(screen.getByRole('button', { name: 'Disable' }))
    await screen.findByText('Disable account?')
    await user.click(screen.getByRole('button', { name: 'Disable' }))

    expect(
      await screen.findByText('Cannot disable the last active administrator.'),
    ).toBeInTheDocument()
  })

  it('confirms before changing a role', async () => {
    const { user } = renderPage()
    await screen.findByRole('heading', { name: 'officer@university.edu' })

    await user.click(screen.getByLabelText('Change role'))
    await user.click(await screen.findByRole('option', { name: 'Administrator' }))

    expect(await screen.findByText('Change role?')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Change role' }))

    expect(await screen.findByText('Role changed to Administrator.')).toBeInTheDocument()
  })

  it('shows an error state when the account cannot be loaded', async () => {
    server.use(
      http.get(`${API_BASE_URL}/api/admin/users/:id`, () =>
        HttpResponse.json({ title: 'Not Found', status: 404 }, { status: 404 }),
      ),
    )
    renderPage()

    expect(await screen.findByText('Failed to load user')).toBeInTheDocument()
  })
})
