import { beforeEach, describe, expect, it } from 'vitest'
import { http, HttpResponse } from 'msw'
import { Route, Routes } from 'react-router-dom'
import { renderWithProviders, screen, waitFor } from '@/test'
import { server, API_BASE_URL } from '@/test'
import { ROUTES } from '@/constants/routes'
import type { JobApplicationResponse } from '@/lib/api'
import type { Page } from '@/types'
import ApplicationsPipelinePage from './ApplicationsPipelinePage'

function application(overrides: Partial<JobApplicationResponse> = {}): JobApplicationResponse {
  return {
    id: 'app-1',
    studentId: 'stu-1',
    studentRollNumber: 'CS-001',
    jobPostingId: 'jp-1',
    jobPostingTitle: 'Backend Engineer',
    companyId: 'co-1',
    companyName: 'Acme Corp',
    status: 'APPLIED',
    appliedAt: '2026-07-05T10:00:00Z',
    createdAt: '2026-07-05T10:00:00Z',
    updatedAt: '2026-07-05T10:00:00Z',
    ...overrides,
  }
}

function pageOf(
  content: JobApplicationResponse[],
  { number = 0, totalPages = 1 } = {},
): Page<JobApplicationResponse> {
  return {
    content,
    pageable: { pageNumber: number, pageSize: 20, sort: { sorted: false } },
    totalElements: content.length,
    totalPages,
    last: number >= totalPages - 1,
    first: number === 0,
    numberOfElements: content.length,
    size: 20,
    number,
    empty: content.length === 0,
  }
}

let requestedParams: URLSearchParams[] = []

function stubList(handler: (params: URLSearchParams) => Page<JobApplicationResponse>) {
  server.use(
    http.get(`${API_BASE_URL}/api/applications`, ({ request }) => {
      const params = new URL(request.url).searchParams
      requestedParams.push(params)
      return HttpResponse.json(handler(params))
    }),
  )
}

function renderPage() {
  return renderWithProviders(
    <Routes>
      <Route path={ROUTES.OFFICER.APPLICATIONS} element={<ApplicationsPipelinePage />} />
      <Route path={ROUTES.OFFICER.APPLICATION_DETAIL(':id')} element={<div>Detail Landing</div>} />
    </Routes>,
    { initialEntries: [ROUTES.OFFICER.APPLICATIONS] },
  )
}

beforeEach(() => {
  requestedParams = []
})

describe('ApplicationsPipelinePage — table', () => {
  it('renders student, posting, company and status for each application', async () => {
    stubList(() => pageOf([application()]))
    renderPage()

    expect(await screen.findByText('CS-001')).toBeInTheDocument()
    expect(screen.getByText('Backend Engineer')).toBeInTheDocument()
    expect(screen.getByText('Acme Corp')).toBeInTheDocument()
    expect(screen.getByText('Applied')).toBeInTheDocument()
  })

  it('navigates to the detail page on row click', async () => {
    stubList(() => pageOf([application()]))
    const { user } = renderPage()

    await user.click(await screen.findByText('Backend Engineer'))
    expect(await screen.findByText('Detail Landing')).toBeInTheDocument()
  })
})

describe('ApplicationsPipelinePage — pagination (backend Pageable only, no filters)', () => {
  it('sends the correct page and size request parameters', async () => {
    stubList(() => pageOf([application()]))
    renderPage()

    await screen.findByText('Backend Engineer')
    await waitFor(() => expect(requestedParams.length).toBeGreaterThan(0))
    expect(requestedParams[0].get('page')).toBe('0')
    expect(requestedParams[0].get('size')).toBe('20')
  })

  it('requests the next page when pagination advances', async () => {
    stubList((params) =>
      pageOf(
        [
          application({
            id: `app-${params.get('page')}`,
            studentRollNumber: `CS-00${params.get('page')}`,
          }),
        ],
        {
          number: Number(params.get('page')),
          totalPages: 3,
        },
      ),
    )
    const { user } = renderPage()

    await screen.findByText('CS-000')
    await user.click(await screen.findByRole('button', { name: /next page/i }))

    await waitFor(() => expect(requestedParams.some((p) => p.get('page') === '1')).toBe(true))
  })
})
