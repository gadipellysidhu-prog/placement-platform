import { describe, expect, it } from 'vitest'
import { http, HttpResponse } from 'msw'
import { Route, Routes } from 'react-router-dom'
import {
  renderWithProviders,
  screen,
  waitFor,
  server,
  API_BASE_URL,
  pageOf,
  mockAdminUserAccount,
  mockDormantUserAccount,
} from '@/test'
import { ROUTES } from '@/constants/routes'
import UsersListPage from './UsersListPage'

function renderPage() {
  return renderWithProviders(
    <Routes>
      <Route path={ROUTES.ADMIN.USERS} element={<UsersListPage />} />
      <Route path={`${ROUTES.ADMIN.USERS}/:id`} element={<div>User Detail Screen</div>} />
    </Routes>,
    { initialEntries: [ROUTES.ADMIN.USERS] },
  )
}

describe('UsersListPage', () => {
  it('lists users returned by the backend', async () => {
    renderPage()

    expect(await screen.findByText('officer@university.edu')).toBeInTheDocument()
    expect(screen.getByText('invited@university.edu')).toBeInTheDocument()
  })

  it('renders last activity as a date, and says so when none is on record', async () => {
    renderPage()

    // The dormant account must never borrow createdAt as a stand-in.
    expect(await screen.findByText('No activity recorded')).toBeInTheDocument()
    expect(screen.getByText(/Feb.*2026/)).toBeInTheDocument()
  })

  it('navigates to the user detail on row click', async () => {
    const { user } = renderPage()

    await user.click(await screen.findByText('officer@university.edu'))

    expect(await screen.findByText('User Detail Screen')).toBeInTheDocument()
  })

  it('shows an empty state when no users match', async () => {
    server.use(http.get(`${API_BASE_URL}/api/admin/users`, () => HttpResponse.json(pageOf([]))))
    renderPage()

    expect(await screen.findByText('No users yet')).toBeInTheDocument()
  })

  it('surfaces a retry affordance when the list fails to load', async () => {
    server.use(
      http.get(`${API_BASE_URL}/api/admin/users`, () =>
        HttpResponse.json({ title: 'Server Error', status: 500 }, { status: 500 }),
      ),
    )
    renderPage()

    expect(await screen.findByText('Failed to load data')).toBeInTheDocument()
  })

  it('filters by search query, sending it to the backend', async () => {
    let requestedQuery: string | null = null
    server.use(
      http.get(`${API_BASE_URL}/api/admin/users`, ({ request }) => {
        requestedQuery = new URL(request.url).searchParams.get('query')
        return HttpResponse.json(pageOf([mockAdminUserAccount]))
      }),
    )

    const { user } = renderPage()
    await screen.findByText('officer@university.edu')
    await user.type(screen.getByLabelText(/search users by email/i), 'officer')

    await waitFor(() => expect(requestedQuery).toBe('officer'))
  })

  it('sends the invitation and reports the backend message', async () => {
    const { user } = renderPage()
    await screen.findByText('officer@university.edu')

    await user.click(screen.getByRole('button', { name: /invite user/i }))
    await user.type(screen.getByLabelText(/email address/i), 'newofficer@university.edu')
    await user.click(screen.getByRole('button', { name: /send invitation/i }))

    expect(await screen.findByText('Invitation sent.')).toBeInTheDocument()
  })

  it('surfaces the backend 409 rather than claiming the invite succeeded', async () => {
    server.use(
      http.post(`${API_BASE_URL}/api/admin/users/invite`, () =>
        HttpResponse.json(
          { title: 'Conflict', status: 409, detail: 'Email is already registered.' },
          { status: 409 },
        ),
      ),
    )

    const { user } = renderPage()
    await screen.findByText('officer@university.edu')

    await user.click(screen.getByRole('button', { name: /invite user/i }))
    await user.type(screen.getByLabelText(/email address/i), 'officer@university.edu')
    await user.click(screen.getByRole('button', { name: /send invitation/i }))

    expect(await screen.findByText('Email is already registered.')).toBeInTheDocument()
  })

  it('does not render an activity date for an account with none', async () => {
    server.use(
      http.get(`${API_BASE_URL}/api/admin/users`, () =>
        HttpResponse.json(pageOf([mockDormantUserAccount])),
      ),
    )
    renderPage()

    expect(await screen.findByText('No activity recorded')).toBeInTheDocument()
    expect(screen.queryByText(/2026/)).not.toBeInTheDocument()
  })
})
