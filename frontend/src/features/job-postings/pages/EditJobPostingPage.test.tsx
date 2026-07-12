import { beforeEach, describe, expect, it } from 'vitest'
import { http, HttpResponse } from 'msw'
import { Route, Routes } from 'react-router-dom'
import { renderWithProviders, screen, waitFor } from '@/test'
import { server, API_BASE_URL } from '@/test'
import { useAuthStore } from '@/stores/auth.store'
import { ROUTES } from '@/constants/routes'
import type { JobPostingResponse } from '@/lib/api'
import EditJobPostingPage from './EditJobPostingPage'

const POSTING_ID = 'jp-1'

function detail(overrides: Partial<JobPostingResponse> = {}): JobPostingResponse {
  return {
    id: POSTING_ID,
    companyId: 'co-1',
    companyName: 'Acme Corp',
    title: 'Backend Engineer',
    description: 'Build things',
    ctcMin: 8,
    ctcMax: 12,
    status: 'DRAFT',
    applicationDeadline: '2026-08-01',
    offerLimit: 5,
    requiredSkills: [],
    eligibleBranches: [],
    createdAt: '2026-07-01T10:00:00Z',
    updatedAt: '2026-07-01T10:00:00Z',
    ...overrides,
  }
}

function stubCompanies() {
  server.use(
    http.get(`${API_BASE_URL}/api/companies`, () =>
      HttpResponse.json({
        content: [],
        pageable: { pageNumber: 0, pageSize: 200, sort: { sorted: false } },
        totalElements: 0,
        totalPages: 0,
        last: true,
        first: true,
        numberOfElements: 0,
        size: 200,
        number: 0,
        empty: true,
      }),
    ),
  )
}

function renderEdit() {
  return renderWithProviders(
    <Routes>
      <Route path={`${ROUTES.OFFICER.JOB_POSTINGS}/:id/edit`} element={<EditJobPostingPage />} />
      <Route path={`${ROUTES.OFFICER.JOB_POSTINGS}/:id`} element={<div>Detail Screen</div>} />
    </Routes>,
    { initialEntries: [`${ROUTES.OFFICER.JOB_POSTINGS}/${POSTING_ID}/edit`] },
  )
}

beforeEach(() => {
  useAuthStore.setState({
    user: { email: 'o@u.edu', role: 'ROLE_PLACEMENT_OFFICER' },
    isAuthenticated: true,
  })
})

describe('EditJobPostingPage', () => {
  it('blocks editing of a non-draft posting', async () => {
    server.use(
      http.get(`${API_BASE_URL}/api/job-postings/${POSTING_ID}`, () =>
        HttpResponse.json(detail({ status: 'OPEN' })),
      ),
    )

    renderEdit()

    expect(await screen.findByText(/can no longer be edited/i)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /save changes/i })).not.toBeInTheDocument()
  })

  it('pre-fills the form and PUTs an update without companyId', async () => {
    stubCompanies()
    server.use(
      http.get(`${API_BASE_URL}/api/job-postings/${POSTING_ID}`, () =>
        HttpResponse.json(detail({ status: 'DRAFT' })),
      ),
    )
    let body: Record<string, unknown> | null = null
    server.use(
      http.put(`${API_BASE_URL}/api/job-postings/${POSTING_ID}`, async ({ request }) => {
        body = (await request.json()) as Record<string, unknown>
        return HttpResponse.json(detail({ title: 'Senior Backend Engineer' }))
      }),
    )

    const { user } = renderEdit()

    const titleInput = await screen.findByLabelText(/title/i)
    expect(titleInput).toHaveValue('Backend Engineer')

    await user.clear(titleInput)
    await user.type(titleInput, 'Senior Backend Engineer')
    await user.click(screen.getByRole('button', { name: /save changes/i }))

    expect(await screen.findByText('Detail Screen')).toBeInTheDocument()
    await waitFor(() => expect(body).not.toBeNull())
    expect(body).not.toHaveProperty('companyId')
    expect(body).toMatchObject({ title: 'Senior Backend Engineer', offerLimit: 5 })
  })
})
