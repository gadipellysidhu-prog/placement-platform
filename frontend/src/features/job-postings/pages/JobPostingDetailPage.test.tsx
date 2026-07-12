import { beforeEach, describe, expect, it } from 'vitest'
import { http, HttpResponse } from 'msw'
import { Route, Routes } from 'react-router-dom'
import { renderWithProviders, screen, waitFor } from '@/test'
import { server, API_BASE_URL } from '@/test'
import { useAuthStore } from '@/stores/auth.store'
import { ROUTES } from '@/constants/routes'
import type { JobPostingResponse } from '@/lib/api'
import JobPostingDetailPage from './JobPostingDetailPage'

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
    requiredSkills: [{ id: 's1', name: 'Java' }],
    eligibleBranches: [{ id: 'b1', name: 'CSE' }],
    createdAt: '2026-07-01T10:00:00Z',
    updatedAt: '2026-07-01T10:00:00Z',
    ...overrides,
  }
}

function stubDetail(posting: JobPostingResponse) {
  server.use(
    http.get(`${API_BASE_URL}/api/job-postings/${POSTING_ID}`, () => HttpResponse.json(posting)),
    // Officer detail mounts the AI panel — no analysis exists for these fixtures.
    http.get(`${API_BASE_URL}/api/job-intelligence/postings/${POSTING_ID}/latest`, () =>
      HttpResponse.json({ title: 'Not Found', status: 404 }, { status: 404 }),
    ),
    // Skill search backs the officer skill picker.
    http.get(`${API_BASE_URL}/api/skills/search`, () =>
      HttpResponse.json([
        {
          id: 's2',
          name: 'Go',
          category: 'Programming Languages',
          parentCategory: 'Software',
          popularityScore: 80,
          matchType: 'EXACT',
          score: 1,
        },
      ]),
    ),
    http.get(`${API_BASE_URL}/api/branches`, () =>
      HttpResponse.json([
        {
          id: 'b1',
          name: 'CSE',
          code: null,
          description: null,
          active: true,
          createdAt: '',
          updatedAt: '',
        },
        {
          id: 'b2',
          name: 'ECE',
          code: null,
          description: null,
          active: true,
          createdAt: '',
          updatedAt: '',
        },
      ]),
    ),
    // The student-only ApplyPanel resolves the current student and their existing
    // applications to decide whether Apply is enabled.
    http.get(`${API_BASE_URL}/api/students/me`, () =>
      HttpResponse.json({
        id: 'stu-1',
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
      }),
    ),
    http.get(`${API_BASE_URL}/api/applications/my`, () => HttpResponse.json([])),
  )
}

function renderDetail(route: string) {
  return renderWithProviders(
    <Routes>
      <Route path={`${ROUTES.OFFICER.JOB_POSTINGS}/:id`} element={<JobPostingDetailPage />} />
      <Route path={`${ROUTES.STUDENT.JOB_POSTINGS}/:id`} element={<JobPostingDetailPage />} />
    </Routes>,
    { initialEntries: [route] },
  )
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

const officerRoute = `${ROUTES.OFFICER.JOB_POSTINGS}/${POSTING_ID}`
const studentRoute = `${ROUTES.STUDENT.JOB_POSTINGS}/${POSTING_ID}`

beforeEach(() => {
  useAuthStore.setState({ user: null, isAuthenticated: false })
})

describe('JobPostingDetailPage — student', () => {
  it('shows posting details and read-only skills/branches with a live Apply button', async () => {
    asStudent()
    stubDetail(detail({ status: 'OPEN' }))

    renderDetail(studentRoute)

    expect(await screen.findByRole('heading', { name: 'Backend Engineer' })).toBeInTheDocument()
    expect(screen.getByText('Java')).toBeInTheDocument()
    expect(screen.getByText('CSE')).toBeInTheDocument()

    // Posting is OPEN and the student hasn't applied → Apply is enabled.
    await waitFor(() => expect(screen.getByRole('button', { name: /apply now/i })).toBeEnabled())

    // Students never see officer lifecycle or tagging controls.
    expect(screen.queryByRole('button', { name: /open for applications/i })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /^add$/i })).not.toBeInTheDocument()
  })
})

describe('JobPostingDetailPage — officer', () => {
  it('confirms and opens a DRAFT posting via the lifecycle endpoint', async () => {
    asOfficer()
    stubDetail(detail({ status: 'DRAFT' }))
    let opened = false
    server.use(
      http.post(`${API_BASE_URL}/api/job-postings/${POSTING_ID}/open`, () => {
        opened = true
        return HttpResponse.json(
          detail({ status: 'OPEN', requiredSkills: null, eligibleBranches: null }),
        )
      }),
    )

    const { user } = renderDetail(officerRoute)

    await user.click(await screen.findByRole('button', { name: /open for applications/i }))
    // A confirmation dialog appears before the mutation fires.
    await user.click(await screen.findByRole('button', { name: /^open posting$/i }))

    await waitFor(() => expect(opened).toBe(true))
  })

  it('surfaces a backend 422 lifecycle rejection as an error toast', async () => {
    asOfficer()
    stubDetail(detail({ status: 'DRAFT' }))
    server.use(
      http.post(`${API_BASE_URL}/api/job-postings/${POSTING_ID}/open`, () =>
        HttpResponse.json(
          {
            title: 'Unprocessable Entity',
            status: 422,
            detail: 'Only DRAFT postings can be opened',
          },
          { status: 422 },
        ),
      ),
    )

    const { user } = renderDetail(officerRoute)

    await user.click(await screen.findByRole('button', { name: /open for applications/i }))
    await user.click(await screen.findByRole('button', { name: /^open posting$/i }))

    expect(await screen.findByText(/only draft postings can be opened/i)).toBeInTheDocument()
  })

  it('adds a required skill through the tagging endpoint', async () => {
    asOfficer()
    stubDetail(detail({ status: 'DRAFT', requiredSkills: [] }))
    let taggedSkillId: string | null = null
    server.use(
      http.post(`${API_BASE_URL}/api/job-postings/${POSTING_ID}/skills/:skillId`, ({ params }) => {
        taggedSkillId = params.skillId as string
        return HttpResponse.json(detail({ requiredSkills: [{ id: 's2', name: 'Go' }] }))
      }),
    )

    const { user } = renderDetail(officerRoute)

    // Search for the skill in the intelligent picker, then click the result.
    await user.type(await screen.findByRole('combobox', { name: /search skills/i }), 'go')
    await user.click(await screen.findByRole('option', { name: /go/i }))

    await waitFor(() => expect(taggedSkillId).toBe('s2'))
    expect(await screen.findByText('Go')).toBeInTheDocument()
  })

  it('only offers a Cancel action for a closed posting', async () => {
    asOfficer()
    stubDetail(detail({ status: 'CLOSED' }))

    renderDetail(officerRoute)

    await screen.findByRole('heading', { name: 'Backend Engineer' })
    expect(screen.queryByRole('button', { name: /open for applications/i })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /close posting/i })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: /cancel posting/i })).toBeInTheDocument()
  })
})
