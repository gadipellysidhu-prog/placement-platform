import { describe, expect, it } from 'vitest'
import { http, HttpResponse } from 'msw'
import {
  renderWithProviders,
  screen,
  waitFor,
  server,
  API_BASE_URL,
  pageOf,
  mockAuditLog,
} from '@/test'
import AuditLogsPage from './AuditLogsPage'

describe('AuditLogsPage', () => {
  it('lists audit entries', async () => {
    renderWithProviders(<AuditLogsPage />)

    expect(await screen.findByText('USER_DISABLED')).toBeInTheDocument()
    expect(screen.getByText(/by admin@university.edu/)).toBeInTheDocument()
  })

  it('keeps the payload collapsed until the entry is expanded', async () => {
    const { user } = renderWithProviders(<AuditLogsPage />)
    await screen.findByText('USER_DISABLED')

    expect(screen.queryByText('Correlation ID')).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { expanded: false }))

    expect(await screen.findByText('Correlation ID')).toBeInTheDocument()
    expect(screen.getByText('corr-1')).toBeInTheDocument()
    expect(screen.getByText('DISABLED')).toBeInTheDocument()
  })

  it('filters by actor, sending the filter to the backend', async () => {
    let performedBy: string | null = null
    server.use(
      http.get(`${API_BASE_URL}/api/admin/audit-logs`, ({ request }) => {
        performedBy = new URL(request.url).searchParams.get('performedBy')
        return HttpResponse.json(pageOf([mockAuditLog]))
      }),
    )

    const { user } = renderWithProviders(<AuditLogsPage />)
    await screen.findByText('USER_DISABLED')

    await user.type(screen.getByLabelText('Actor'), 'admin@university.edu')

    await waitFor(() => expect(performedBy).toBe('admin@university.edu'))
  })

  it('converts the date filter to an ISO instant for the backend', async () => {
    let dateFrom: string | null = null
    server.use(
      http.get(`${API_BASE_URL}/api/admin/audit-logs`, ({ request }) => {
        dateFrom = new URL(request.url).searchParams.get('dateFrom')
        return HttpResponse.json(pageOf([mockAuditLog]))
      }),
    )

    const { user } = renderWithProviders(<AuditLogsPage />)
    await screen.findByText('USER_DISABLED')

    await user.type(screen.getByLabelText('From'), '2026-02-01T00:00')

    // The control yields 'YYYY-MM-DDTHH:mm'; the API contract requires an instant.
    await waitFor(() => expect(dateFrom).toMatch(/^\d{4}-\d{2}-\d{2}T.*Z$/))
  })

  it('shows an empty state when nothing matches', async () => {
    server.use(
      http.get(`${API_BASE_URL}/api/admin/audit-logs`, () => HttpResponse.json(pageOf([]))),
    )
    renderWithProviders(<AuditLogsPage />)

    expect(await screen.findByText('No audit entries yet')).toBeInTheDocument()
  })

  it('surfaces a retry affordance when the trail fails to load', async () => {
    server.use(
      http.get(`${API_BASE_URL}/api/admin/audit-logs`, () =>
        HttpResponse.json({ title: 'Server Error', status: 500 }, { status: 500 }),
      ),
    )
    renderWithProviders(<AuditLogsPage />)

    expect(await screen.findByText('Failed to load audit logs')).toBeInTheDocument()
  })
})
