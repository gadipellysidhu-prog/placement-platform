import { beforeEach, describe, expect, it } from 'vitest'
import { http, HttpResponse } from 'msw'
import { Route, Routes } from 'react-router-dom'
import { renderWithProviders, screen, waitFor, within } from '@/test'
import { server, API_BASE_URL } from '@/test'
import { ROUTES } from '@/constants/routes'
import type { JobApplicationResponse } from '@/lib/api'
import MyApplicationsPage from './MyApplicationsPage'

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

function stubMy(applications: JobApplicationResponse[]) {
  server.use(http.get(`${API_BASE_URL}/api/applications/my`, () => HttpResponse.json(applications)))
}

function renderPage() {
  return renderWithProviders(
    <Routes>
      <Route path={ROUTES.STUDENT.MY_APPLICATIONS} element={<MyApplicationsPage />} />
    </Routes>,
    { initialEntries: [ROUTES.STUDENT.MY_APPLICATIONS] },
  )
}

describe('MyApplicationsPage — rendering & timeline', () => {
  beforeEach(() => {
    stubMy([
      application({ id: 'app-1', status: 'SHORTLISTED', jobPostingTitle: 'Backend Engineer' }),
    ])
  })

  it('renders posting, company, status badge and a derived timeline', async () => {
    renderPage()

    expect(await screen.findByText('Backend Engineer')).toBeInTheDocument()
    expect(screen.getByText('Acme Corp')).toBeInTheDocument()

    const timeline = screen.getByRole('list', { name: /application timeline/i })
    expect(within(timeline).getByText('Applied')).toBeInTheDocument()
    expect(within(timeline).getByText('Shortlisted')).toBeInTheDocument()
  })
})

describe('MyApplicationsPage — withdraw visibility rules', () => {
  it('offers Withdraw for a non-terminal application', async () => {
    stubMy([application({ status: 'APPLIED' })])
    renderPage()

    expect(await screen.findByText('Backend Engineer')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /^withdraw$/i })).toBeInTheDocument()
  })

  it('hides Withdraw for terminal states (OFFERED / REJECTED / WITHDRAWN)', async () => {
    stubMy([
      application({ id: 'a-offered', status: 'OFFERED', jobPostingTitle: 'Role Offered' }),
      application({ id: 'a-rejected', status: 'REJECTED', jobPostingTitle: 'Role Rejected' }),
      application({ id: 'a-withdrawn', status: 'WITHDRAWN', jobPostingTitle: 'Role Withdrawn' }),
    ])
    renderPage()

    expect(await screen.findByText('Role Offered')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /^withdraw$/i })).not.toBeInTheDocument()
  })
})

describe('MyApplicationsPage — withdraw flow', () => {
  it('confirms and withdraws via the backend endpoint', async () => {
    stubMy([application({ status: 'APPLIED' })])
    let withdrew = false
    server.use(
      http.post(`${API_BASE_URL}/api/applications/app-1/withdraw`, () => {
        withdrew = true
        return HttpResponse.json(application({ status: 'WITHDRAWN' }))
      }),
    )

    const { user } = renderPage()

    await user.click(await screen.findByRole('button', { name: /^withdraw$/i }))
    // A confirmation dialog appears before the mutation fires.
    await user.click(await screen.findByRole('button', { name: /withdraw application/i }))

    await waitFor(() => expect(withdrew).toBe(true))
  })

  it('surfaces a backend 409/422 rejection as a readable error', async () => {
    stubMy([application({ status: 'APPLIED' })])
    server.use(
      http.post(`${API_BASE_URL}/api/applications/app-1/withdraw`, () =>
        HttpResponse.json(
          {
            title: 'Unprocessable Entity',
            status: 422,
            detail: 'Cannot withdraw application in status: OFFERED',
          },
          { status: 422 },
        ),
      ),
    )

    const { user } = renderPage()

    await user.click(await screen.findByRole('button', { name: /^withdraw$/i }))
    await user.click(await screen.findByRole('button', { name: /withdraw application/i }))

    expect(
      await screen.findByText(/cannot withdraw application in status: offered/i),
    ).toBeInTheDocument()
  })
})

describe('MyApplicationsPage — empty state', () => {
  it('shows an empty state when the student has no applications', async () => {
    stubMy([])
    renderPage()

    expect(await screen.findByText(/no applications yet/i)).toBeInTheDocument()
  })
})
