import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { http, HttpResponse } from 'msw'
import { Route, Routes } from 'react-router-dom'
import { renderWithProviders, screen } from '@/test'
import { server, API_BASE_URL } from '@/test'
import { useAuthStore } from '@/stores/auth.store'
import { ROUTES } from '@/constants/routes'
import type { JobApplicationResponse } from '@/lib/api'
import ApplicationDetailPage from './ApplicationDetailPage'

const APP_ID = 'app-1'

function application(overrides: Partial<JobApplicationResponse> = {}): JobApplicationResponse {
  return {
    id: APP_ID,
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

function asStudent() {
  useAuthStore.setState({ user: { email: 's@u.edu', role: 'ROLE_STUDENT' }, isAuthenticated: true })
}
function asOfficer() {
  useAuthStore.setState({
    user: { email: 'o@u.edu', role: 'ROLE_PLACEMENT_OFFICER' },
    isAuthenticated: true,
  })
}

function renderDetail(route: string) {
  return renderWithProviders(
    <Routes>
      <Route path={ROUTES.OFFICER.APPLICATION_DETAIL(':id')} element={<ApplicationDetailPage />} />
      <Route path={ROUTES.STUDENT.MY_APPLICATIONS} element={<div>My Applications Landing</div>} />
      <Route path={ROUTES.OFFICER.APPLICATIONS} element={<div>Pipeline Landing</div>} />
    </Routes>,
    { initialEntries: [route] },
  )
}

const detailRoute = ROUTES.OFFICER.APPLICATION_DETAIL(APP_ID)

beforeEach(() => {
  useAuthStore.setState({ user: null, isAuthenticated: false })
})
afterEach(() => {
  useAuthStore.setState({ user: null, isAuthenticated: false })
})

describe('ApplicationDetailPage — officer', () => {
  it('loads via the officer endpoint and offers status transitions', async () => {
    asOfficer()
    server.use(
      http.get(`${API_BASE_URL}/api/applications/${APP_ID}`, () =>
        HttpResponse.json(application({ status: 'APPLIED' })),
      ),
    )

    renderDetail(detailRoute)

    expect(await screen.findByRole('heading', { name: 'Backend Engineer' })).toBeInTheDocument()
    expect(screen.getByText('CS-001')).toBeInTheDocument()
    // Officer transition control present; withdraw is not an officer action.
    expect(screen.getByRole('combobox', { name: /new status/i })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /^withdraw$/i })).not.toBeInTheDocument()
  })
})

describe('ApplicationDetailPage — owner student', () => {
  it('derives the application from /my and offers Withdraw', async () => {
    asStudent()
    server.use(
      http.get(`${API_BASE_URL}/api/applications/my`, () =>
        HttpResponse.json([application({ status: 'APPLIED' })]),
      ),
    )

    renderDetail(detailRoute)

    expect(await screen.findByRole('heading', { name: 'Backend Engineer' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /^withdraw$/i })).toBeInTheDocument()
    // Students never see the officer transition control.
    expect(screen.queryByRole('combobox', { name: /new status/i })).not.toBeInTheDocument()
  })
})

describe('ApplicationDetailPage — unauthorized user', () => {
  it('shows a 403-safe view when a student does not own the application', async () => {
    asStudent()
    // The student's /my list does not contain APP_ID.
    server.use(
      http.get(`${API_BASE_URL}/api/applications/my`, () =>
        HttpResponse.json([application({ id: 'other-app', jobPostingTitle: 'Something Else' })]),
      ),
    )

    renderDetail(detailRoute)

    expect(await screen.findByText(/application not available/i)).toBeInTheDocument()
    // No application data leaks through.
    expect(screen.queryByRole('heading', { name: 'Backend Engineer' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /^withdraw$/i })).not.toBeInTheDocument()
  })
})
