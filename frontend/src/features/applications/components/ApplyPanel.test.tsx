import { beforeEach, describe, expect, it } from 'vitest'
import { http, HttpResponse } from 'msw'
import { Route, Routes } from 'react-router-dom'
import { renderWithProviders, screen, waitFor } from '@/test'
import { server, API_BASE_URL } from '@/test'
import { ROUTES } from '@/constants/routes'
import type { JobApplicationResponse, JobPostingStatus, StudentResponse } from '@/lib/api'
import { ApplyPanel } from './ApplyPanel'

const POSTING_ID = 'jp-1'
const STUDENT_ID = 'stu-1'

function student(overrides: Partial<StudentResponse> = {}): StudentResponse {
  return {
    id: STUDENT_ID,
    userId: 'u-1',
    userEmail: 's@u.edu',
    rollNumber: 'CS-001',
    branchId: 'b1',
    branchName: 'CSE',
    cgpa: 8.5,
    currentYear: 4,
    placementEligible: true,
    status: 'ACTIVE',
    skillNames: [],
    createdAt: '',
    updatedAt: '',
    ...overrides,
  }
}

function application(overrides: Partial<JobApplicationResponse> = {}): JobApplicationResponse {
  return {
    id: 'app-1',
    studentId: STUDENT_ID,
    studentRollNumber: 'CS-001',
    jobPostingId: POSTING_ID,
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

function stubProfileAndApplications(myApplications: JobApplicationResponse[]) {
  server.use(
    http.get(`${API_BASE_URL}/api/students/me`, () => HttpResponse.json(student())),
    http.get(`${API_BASE_URL}/api/applications/my`, () => HttpResponse.json(myApplications)),
  )
}

function renderPanel(postingStatus: JobPostingStatus = 'OPEN') {
  return renderWithProviders(
    <Routes>
      <Route
        path="/"
        element={<ApplyPanel postingId={POSTING_ID} postingStatus={postingStatus} />}
      />
      <Route path={ROUTES.STUDENT.MY_APPLICATIONS} element={<div>My Applications Landing</div>} />
    </Routes>,
    { initialEntries: ['/'] },
  )
}

beforeEach(() => {
  stubProfileAndApplications([])
})

describe('ApplyPanel — successful apply', () => {
  it('submits the application and navigates to My Applications', async () => {
    let posted: { studentId: string; jobPostingId: string } | null = null
    server.use(
      http.post(`${API_BASE_URL}/api/applications`, async ({ request }) => {
        posted = (await request.json()) as typeof posted
        return HttpResponse.json(application(), { status: 201 })
      }),
    )

    const { user } = renderPanel('OPEN')

    const applyButton = await screen.findByRole('button', { name: /apply now/i })
    await waitFor(() => expect(applyButton).toBeEnabled())
    await user.click(applyButton)

    await waitFor(() => expect(posted).toEqual({ studentId: STUDENT_ID, jobPostingId: POSTING_ID }))
    expect(await screen.findByText('My Applications Landing')).toBeInTheDocument()
  })
})

describe('ApplyPanel — eligibility failure', () => {
  it('renders every backend ProblemDetail reason exactly', async () => {
    const detail =
      'You have been marked as ineligible for placement by the placement officer. ' +
      'Your branch (ECE) is not in the list of eligible branches for this posting.'
    server.use(
      http.post(`${API_BASE_URL}/api/applications`, () =>
        HttpResponse.json({ title: 'Unprocessable Entity', status: 422, detail }, { status: 422 }),
      ),
    )

    const { user } = renderPanel('OPEN')

    const applyButton = await screen.findByRole('button', { name: /apply now/i })
    await waitFor(() => expect(applyButton).toBeEnabled())
    await user.click(applyButton)

    // The full joined reason string is surfaced verbatim (inline alert + toast).
    const matches = await screen.findAllByText(detail)
    expect(matches.length).toBeGreaterThanOrEqual(1)
  })
})

describe('ApplyPanel — duplicate application', () => {
  it('derives "already applied" from GET /my and hides the Apply button', async () => {
    stubProfileAndApplications([application({ status: 'SHORTLISTED' })])

    renderPanel('OPEN')

    expect(await screen.findByText(/you’ve already applied/i)).toBeInTheDocument()
    expect(screen.getByText('Shortlisted')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /apply now/i })).not.toBeInTheDocument()
  })

  it('blocks re-applying even after a withdrawal (backend keeps the row)', async () => {
    stubProfileAndApplications([application({ status: 'WITHDRAWN' })])

    renderPanel('OPEN')

    expect(await screen.findByText(/you’ve already applied/i)).toBeInTheDocument()
    expect(screen.getByText('Withdrawn')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /apply now/i })).not.toBeInTheDocument()
  })
})

describe('ApplyPanel — posting not open', () => {
  it('disables Apply and explains the posting is not open', async () => {
    renderPanel('CLOSED')

    const applyButton = await screen.findByRole('button', { name: /apply now/i })
    expect(applyButton).toBeDisabled()
    expect(screen.getByText(/this posting is not open for applications/i)).toBeInTheDocument()
  })
})
