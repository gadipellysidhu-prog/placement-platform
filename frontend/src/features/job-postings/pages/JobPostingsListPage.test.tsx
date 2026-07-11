import { beforeEach, describe, expect, it } from 'vitest'
import { http, HttpResponse } from 'msw'
import { Route, Routes } from 'react-router-dom'
import { renderWithProviders, screen, waitFor } from '@/test'
import { server, API_BASE_URL } from '@/test'
import { useAuthStore } from '@/stores/auth.store'
import { ROUTES } from '@/constants/routes'
import type { JobPostingResponse } from '@/lib/api'
import JobPostingsListPage from './JobPostingsListPage'

function summary(overrides: Partial<JobPostingResponse> = {}): JobPostingResponse {
  return {
    id: 'jp-1',
    companyId: 'co-1',
    companyName: 'Acme Corp',
    title: 'Backend Engineer',
    description: null,
    ctcMin: 8,
    ctcMax: 12,
    status: 'OPEN',
    applicationDeadline: '2026-08-01',
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

function renderList() {
  return renderWithProviders(
    <Routes>
      <Route path={ROUTES.STUDENT.JOB_POSTINGS} element={<JobPostingsListPage />} />
      <Route path={`${ROUTES.STUDENT.JOB_POSTINGS}/:id`} element={<div>Detail Screen</div>} />
    </Routes>,
    { initialEntries: [ROUTES.STUDENT.JOB_POSTINGS] },
  )
}

beforeEach(() => {
  useAuthStore.setState({ user: { email: 's@u.edu', role: 'ROLE_STUDENT' }, isAuthenticated: true })
})

describe('JobPostingsListPage', () => {
  it('renders open postings returned by the browse endpoint', async () => {
    server.use(
      http.get(`${API_BASE_URL}/api/job-postings`, () =>
        HttpResponse.json(page([summary(), summary({ id: 'jp-2', title: 'Data Scientist' })])),
      ),
    )

    renderList()

    expect(await screen.findByText('Backend Engineer')).toBeInTheDocument()
    expect(screen.getByText('Data Scientist')).toBeInTheDocument()
    expect(screen.getAllByText('Open')).toHaveLength(2)
  })

  it('shows an empty state when there are no open jobs', async () => {
    server.use(http.get(`${API_BASE_URL}/api/job-postings`, () => HttpResponse.json(page([]))))

    renderList()

    expect(await screen.findByText(/no open jobs right now/i)).toBeInTheDocument()
  })

  it('sends the debounced title filter as a query param', async () => {
    const seenTitles: (string | null)[] = []
    server.use(
      http.get(`${API_BASE_URL}/api/job-postings`, ({ request }) => {
        seenTitles.push(new URL(request.url).searchParams.get('title'))
        return HttpResponse.json(page([summary()]))
      }),
    )

    const { user } = renderList()
    await screen.findByText('Backend Engineer')

    await user.type(screen.getByRole('searchbox', { name: /search job postings/i }), 'engineer')

    await waitFor(() => expect(seenTitles).toContain('engineer'))
  })

  it('navigates to the detail screen on row click', async () => {
    server.use(
      http.get(`${API_BASE_URL}/api/job-postings`, () => HttpResponse.json(page([summary()]))),
    )

    const { user } = renderList()
    await user.click(await screen.findByText('Backend Engineer'))

    expect(await screen.findByText('Detail Screen')).toBeInTheDocument()
  })

  it('shows an error state and retries on failure', async () => {
    let calls = 0
    server.use(
      http.get(`${API_BASE_URL}/api/job-postings`, () => {
        calls += 1
        return calls === 1
          ? new HttpResponse(null, { status: 500 })
          : HttpResponse.json(page([summary()]))
      }),
    )

    const { user } = renderList()

    const retry = await screen.findByRole('button', { name: /try again/i })
    await user.click(retry)

    expect(await screen.findByText('Backend Engineer')).toBeInTheDocument()
  })
})
