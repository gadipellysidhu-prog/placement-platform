import { beforeEach, describe, expect, it } from 'vitest'
import { http, HttpResponse } from 'msw'
import { Route, Routes } from 'react-router-dom'
import { renderWithProviders, screen, waitFor } from '@/test'
import { server, API_BASE_URL } from '@/test'
import { useAuthStore } from '@/stores/auth.store'
import { ROUTES } from '@/constants/routes'
import type { JobPostingResponse } from '@/lib/api'
import ManageJobPostingsPage from './ManageJobPostingsPage'

function summary(overrides: Partial<JobPostingResponse> = {}): JobPostingResponse {
  return {
    id: 'jp-1',
    companyId: 'co-1',
    companyName: 'Acme Corp',
    title: 'Backend Engineer',
    description: null,
    ctcMin: 8,
    ctcMax: 12,
    status: 'DRAFT',
    applicationDeadline: null,
    offerLimit: 5,
    requiredSkills: null,
    eligibleBranches: null,
    createdAt: '2026-07-01T10:00:00Z',
    updatedAt: '2026-07-01T10:00:00Z',
    ...overrides,
  }
}

function page(content: JobPostingResponse[]) {
  return {
    content,
    pageable: { pageNumber: 0, pageSize: 20, sort: { sorted: false } },
    totalElements: content.length,
    totalPages: 1,
    last: true,
    first: true,
    numberOfElements: content.length,
    size: 20,
    number: 0,
    empty: content.length === 0,
  }
}

function renderManage() {
  return renderWithProviders(
    <Routes>
      <Route path={ROUTES.OFFICER.JOB_POSTINGS} element={<ManageJobPostingsPage />} />
      <Route path={ROUTES.OFFICER.CREATE_JOB_POSTING} element={<div>Create Screen</div>} />
    </Routes>,
    { initialEntries: [ROUTES.OFFICER.JOB_POSTINGS] },
  )
}

beforeEach(() => {
  useAuthStore.setState({
    user: { email: 'o@u.edu', role: 'ROLE_PLACEMENT_OFFICER' },
    isAuthenticated: true,
  })
})

describe('ManageJobPostingsPage', () => {
  it('lists postings across all statuses from GET /manage', async () => {
    server.use(
      http.get(`${API_BASE_URL}/api/job-postings/manage`, () =>
        HttpResponse.json(
          page([summary(), summary({ id: 'jp-2', title: 'Data Scientist', status: 'CLOSED' })]),
        ),
      ),
    )

    renderManage()

    expect(await screen.findByText('Backend Engineer')).toBeInTheDocument()
    expect(screen.getByText('Data Scientist')).toBeInTheDocument()
    expect(screen.getByText('Draft')).toBeInTheDocument()
    expect(screen.getByText('Closed')).toBeInTheDocument()
  })

  it('passes the selected status filter to the query', async () => {
    const seenStatuses: (string | null)[] = []
    server.use(
      http.get(`${API_BASE_URL}/api/job-postings/manage`, ({ request }) => {
        seenStatuses.push(new URL(request.url).searchParams.get('status'))
        return HttpResponse.json(page([summary({ status: 'OPEN' })]))
      }),
    )

    const { user } = renderManage()
    await screen.findByText('Backend Engineer')

    // Open the status Select and choose "Open".
    await user.click(screen.getByRole('combobox', { name: /filter by status/i }))
    await user.click(await screen.findByRole('option', { name: 'Open' }))

    await waitFor(() => expect(seenStatuses).toContain('OPEN'))
    // First load is unfiltered.
    expect(seenStatuses[0]).toBeNull()
  })

  it('links to the create page', async () => {
    server.use(
      http.get(`${API_BASE_URL}/api/job-postings/manage`, () =>
        HttpResponse.json(page([summary()])),
      ),
    )

    const { user } = renderManage()
    await user.click(await screen.findByRole('link', { name: /create posting/i }))

    expect(await screen.findByText('Create Screen')).toBeInTheDocument()
  })
})
